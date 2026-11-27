# Motore e campagna di gioco

`game_engine` è un'unica applicazione Spring Boot che contiene il motore delle partite e i servizi dimostrativi della campagna. La campagna è organizzata nel package `com.benedetto.game_engine.campaign`; non è più un modulo Maven o un'applicazione avviabile separatamente.

> **Stato:** il modulo contiene il motore stateless e gli endpoint HTTP interni PvE/PvP. Il PvP `POST /api/battle/pvp` calcola il report senza autenticazione o persistenza: utenti, validazione, snapshot, concorrenza e storico sono responsabilità di `springboot_app/app`. Il modulo non costituisce l'implementazione completa di F05, F07 o F08 e non conferma le regole proposte come definitive.

Ultimo aggiornamento significativo: **2026-11-27**, endpoint interno PvP e contratto aggiornati ai sorgenti.

## Motore di gioco

- `MatchEngine` orchestra fino a cinque round e interrompe la partita quando un giocatore raggiunge tre round vinti. Restituisce il resoconto come lista di stringhe.
- `CombatEngine` esegue scambi simultanei fra le creature attive, gestisce la sostituzione delle creature eliminate e il danno in eccesso ai punti vita del giocatore.
- Il bonus d'attacco di fazione è `+2` quando la fazione attaccante è avvantaggiata; al calcolo dell’attacco si sommano i modificatori installati dagli effetti, con attacco effettivo minimo zero.
- Ogni round ripristina i punti vita a 30. Il combattimento termina anche al raggiungimento del limite difensivo di 50 scambi.
- `CardLoaderService` deserializza un mazzo JSON da una risorsa del classpath. Il flag `deterministic` disabilita il mescolamento.

Questi comportamenti descrivono il codice attuale, non un contratto di gioco definitivo. Equipaggiamenti e creature espongono `effects`: ogni elemento contiene `effectId` numerico, `code` e `parameters`. `EffectEngine` usa un registro esplicito con `AttackBonusEffect`, `StartingDefenseModifierEffect` e `FirstExchangeAttackBonusEffect`. Gli effetti si attivano all'ingresso della creatura, prima quelli degli equipaggiamenti e poi quelli delle creature. Le classi gestiscono bersagli e durata; Breach Capacitor installa il bonus all'ingresso e lo rimuove dopo il primo scambio di ciascuna creatura. Stato e modificatori sono separati dallo snapshot. Il log include attivazioni e scadenze con prefisso `[EFFECT]`.

Il payload interno con `atkBonus`, `defBonus` e `firstExchangeAtkBonus` è sostituito dall'array `effects`. Il backend deve inviare i dati degli effetti ordinati per `card_effects.execution_order`; il motore controlla codici e parametri supportati, senza confrontare ID o statistiche con pool locali. Parametri e ciclo di vita sono descritti nel [contratto interno](../../docs/BackEnd/app-battaglie.md#attivazione-e-responsabilità-degli-effetti).

## Dati dimostrativi CR01

I vecchi pool e mazzi sono rimossi. Le due fixture in `src/main/resources/mocks/` derivano esclusivamente da V5/V6/V8 in `../app/src/main/resources/db/migration/`:

- `mirror.json`: una copia di ognuna delle 30 carte possedute dal giocatore di test V6; V6 non crea un mazzo salvato. Le dieci creature di energia 1 sono divise tra i round 1 e 2; le successive fasce occupano i round 3–5. Potenza creature 60, equipaggiamenti 10.
- `demo.json`: composizione alternativa per il bot, con creature duplicate entro il limite di due, potenza creature 57 ed equipaggiamenti 10. Rispetta quantità, soglie energetiche, budget e rarità del regolamento V2.1. Le copie duplicate richiedono una collezione adeguata se il mazzo viene importato nell'applicazione: l'utente V6 possiede una sola copia per carta.

Gli ID numerici 1–30 seguono l'inserimento ordinato per codice in un database vuoto; non rappresentano un mapping per database con dati preesistenti. Le statistiche mantengono i valori del seed. Gli effetti dei cinque equipaggiamenti riproducono le sette associazioni di V8. Le creature attuali hanno `effects: []`; il formato e il motore supportano abilità delle creature senza inventarne per il set CR01. La verifica della composizione delle fixture non esegue la validazione applicativa F05 né ne dimostra il possesso.

Verifica storica dell'importazione parziale del **2026-11-22**, branch `feature/backend-catalog-and-tutorial`, JDK 21.0.11 e H2: `mvnw.cmd -o "-Daether.syncContext.named.factory=rwlock-local" test` supera **51 test JUnit**, con zero fallimenti, errori o casi saltati. Eseguiti anche i due test di regressione sul mescolamento tutorial PvE e CLI; nessuna nuova prova HTTP o browser.

## Campagna

`CampaignService.executeStage` carica uno stage JSON, verifica l'eventuale codice evento richiesto, carica il mazzo nemico e avvia la partita tramite `MatchEngine`. Se l'evento richiesto è assente o inattivo, restituisce un messaggio nel resoconto senza iniziare la partita. I suggerimenti tutorial vengono anteposti al resoconto della partita.

Decisione dell'utente del **2026-11-22**: anche il tutorial usa il mescolamento normale, sia nell'API PvE sia nella campagna CLI. Gli equipaggiamenti sono mescolati all'inizio della partita e le creature del relativo mazzetto all'inizio di ciascun round, per entrambi i giocatori. Gli stage non espongono più un'opzione per disabilitarlo; il flag interno dei mazzi rimane disponibile per simulazioni dirette controllate, ma i mazzi dimostrativi lo mantengono disattivato.

Verifica del **2026-11-22**, copia di lavoro con base `d2e81e2`, JDK 21 e H2: `mvnw.cmd -o "-Daether.syncContext.named.factory=rwlock-local" test` supera **51 test JUnit**, inclusi due controlli di regressione sul mescolamento dei partecipanti nel tutorial PvE e CLI. Packaging riuscito; dieci chiamate HTTP al JAR locale sullo stage 1, alternate tra cinque mazzi mirror e cinque demo, restituiscono `200` con dieci log distinti. Controllati statistiche degli scambi, effetti, danni ai giocatori e vincitori senza incoerenze. La verifica è limitata al motore; browser, autenticazione applicativa e nuove immagini Compose non verificati.

`CampaignStageLoaderService` legge gli stage dal classpath; accetta `classpath:`, un percorso relativo a `campaign/stages/` oppure il nome del file. I dati sono in `src/main/resources/campaign/`:

| Percorso | Contenuto |
| --- | --- |
| `stages/` | Configurazioni di esempio per lo stage 1 e lo stage 3 |
| `../mocks/mirror.json`, `../mocks/demo.json` | Mazzi condivisi tra CLI e stage; nessuna copia separata nella campagna |

Lo stage 3 richiede l'evento `BLOOD_MOON`. L'applicazione usa H2 in memoria e crea lo schema JPA all'avvio; non è presente un seed per tale evento, quindi lo stage 3 non supera il controllo nella configurazione predefinita.

## API interne PvE e PvP

Verifica dell'orchestrazione del **2026-11-20**: **49 test del motore** superati con JDK 21.0.11 e H2, inclusi rifiuto di deadline scadute/malformate e interruzione tra scambi. La suite applicativa ha superato **212 test** su PostgreSQL 18.6 isolato. Verificato anche il flusso fra JAR reali, senza integrare il browser o distribuire nuove immagini sullo stack dell'utente. [Report storico](https://github.com/aChiampo/gruppo-03-Cinder-Rift/blob/73ad701a20b4fbd3e540689e92b5569a4c1e4da0/docs/report/2026-11-20-orchestrazione-pve.md). La successiva verifica del motore del 2026-11-22 è descritta nella sezione Campagna; nessuna nuova esecuzione JUnit nella revisione documentale del 2026-11-23.

L'endpoint PvE interno è descritto nella [specifica BackEnd](../../docs/BackEnd/app-battaglie.md#api-interna-del-motore-implementata). Il modulo espone anche `POST /api/battle/pvp`: riceve `playerA`, `deckA`, `playerB`, `deckB` e `mode` (`RANKED` o `FRIENDLY`), quindi restituisce `mode`, `outcome`, `winnerUserId`, `winnerUsername` e `battleLog`. Il contratto PvP completo, inclusi i valori di esito, è documentato nella [specifica F07](../../docs/BackEnd/sfide-simulazione.md#endpoint-pvp-stateless-del-motore).

Il controller del motore non autentica le richieste PvP. `BattleEngineClient` invia JSON e `X-Battle-Deadline`, senza Bearer utente né token interno di servizio. La comunicazione è prevista sulla rete Compose interna, ma l'endpoint non implementa una propria verifica del token; il limite è riportato nella [specifica F07](../../docs/BackEnd/sfide-simulazione.md#architettura-e-responsabilità).

L'orchestrazione applicativa e il [contratto pubblico](../../docs/BackEnd/app-battaglie-api.md) sono implementati nei sorgenti correnti e usati da React, con visualizzatore del risultato della sessione. Allineamento documentale del **2026-11-23**; browser integrato non verificato in questa revisione. Il frontend si integra con l'applicazione, non direttamente con il motore. Le chiamate interne usano `http://game-engine:8083`; il binding host resta disponibile per debug come indicato nella configurazione Compose.

L'applicazione invia l'header interno `X-Battle-Deadline` con un termine UTC ISO-8601. Il motore lo controlla prima di preparare la battaglia, prima dell'avvio, tra round e scambi e prima del risultato finale; deadline scaduta o interruzione cooperativa producono `504`, header malformato `400`. Senza header si applica un limite cooperativo locale di 8 secondi, che non può essere esteso dal chiamante. I checkpoint appartengono alla singola esecuzione e non aggiungono stato globale al combattimento. Autenticazione degli utenti nell'applicazione e confine Docker senza credenziali interne aggiuntive sono scelte esplicite dell'utente. Il budget complessivo e i limiti dopo timeout sono nella [specifica applicativa](../../docs/BackEnd/app-battaglie.md#opzioni-dopo-timeout-http).

## Avvio e verifiche

Dalla directory `springboot_app/game_engine/`:

```bash
./mvnw test
./mvnw spring-boot:run
```

All'avvio il `CommandLineRunner` presenta un menu:

1. **Campagna** — avvia il primo stage tutorial (`stage_01_mirror.json`).
2. **Scontro online** — simula uno scontro usando i due mazzi dimostrativi JSON.

Il menu è un flusso CLI dimostrativo, non un matchmaking online né un'interfaccia web. La scelta del giocatore determina quale flusso invocare; un input terminato prima della scelta produce un errore esplicito. Per disabilitare il prompt in un ambiente senza input interattivo, avviare con `--game-engine.cli.enabled=false`. I test verificano il caricamento del contesto, entrambe le scelte, l'input non valido e il caricamento e l'esecuzione degli stage.

Verifica del **2026-11-19**: `./mvnw test` completato con **25 test JUnit eseguiti, 0 fallimenti e 0 errori**, inclusi i 6 test aggiornati di `BattleControllerTest`. Sono verificati anche la POST senza header `Origin` e il rifiuto dei preflight, senza configurare CORS. La verifica usa H2; non attesta l'integrazione con PostgreSQL, l'orchestrazione in `springboot_app/app` né le funzionalità applicative complete.

Verifica storica del **2026-11-20**, dopo l'introduzione degli effetti: `mvnw.cmd clean test -q`, JDK 21.0.11 su Windows, **45 test JUnit superati**, nessun fallimento, errore o caso saltato. Verificati ordine delle fonti, abilità delle creature, attivazione unica, scadenza, indipendenza di copie e round, danni simultanei, fine round e payload HTTP invalidi. Confrontate le 60 occorrenze delle fixture con i dati V8 su PostgreSQL 18.6 isolato; la suite applicativa ha superato separatamente 139 test, inclusi installazione e aggiornamento delle migrazioni. L'orchestrazione PvE dell'applicazione e l'integrazione browser restano da implementare/verificare.

Verifica precedente del **2026-11-20**, prima del modello di effetti: `mvnw.cmd clean test -q` con JDK 21.0.11 su Windows, **35 test JUnit, 0 fallimenti, 0 errori e 0 saltati**. La suite comprende ID numerici, statistiche ricevute senza lookup, copie indipendenti, struttura degli snapshot, composizione dei due mazzi e modificatori degli equipaggiamenti. Controllati separatamente i valori delle creature rispetto a V5, i vincoli di composizione, i percorsi degli stage e gli esempi JSON del contratto. La verifica resta locale con H2; non esegue l'orchestrazione applicativa o PostgreSQL.

La verifica Docker del **2026-11-16** ha superato **19 test** nello stage Java del build, costruito l'immagine e rilevato il servizio sano su `8083` nello stack Compose isolato, con menu disabilitato e filesystem in sola lettura. [Evidenze e limiti](https://github.com/aChiampo/gruppo-03-Cinder-Rift/blob/400ceeb0a81aca58d7abd6baff355f60baf2c12c/docs/report/2026-11-16-verifica-docker-compose.md). Questa evidenza resta distinta dalla suite eseguita il 2026-11-19.

Il POM dichiara Java 21 e Spring Boot 4.1.2. La configurazione predefinita usa H2; il driver PostgreSQL è incluso come dipendenza runtime.

## Avvio con Docker Compose

Dalla radice del repository:

```text
docker compose build game-engine
docker compose up --wait game-engine
```

Il secondo comando avvia anche backend e PostgreSQL e richiede la [configurazione JWT e Flyway](../../guida-avvio.md). Il contesto è `springboot_app/game_engine/`, mentre il nome del servizio resta `game-engine`.

Il [Dockerfile](Dockerfile) usa Java 21, Maven Wrapper e un’immagine runtime JRE Alpine con `curl`; il processo gira con UID/GID `10001`. Il packaging salta l’esecuzione dei test, da eseguire separatamente con `./mvnw test` o `mvnw.cmd test` su Windows con JDK 21. Il POM genera anche un JAR non eseguibile: Docker seleziona precisamente `game_engine-0.0.1-SNAPSHOT-exec.jar`, con classifier `exec`. Aggiornare il percorso se cambiano artifactId o versione.

L’immagine imposta `SERVER_PORT=8083` e `GAME_ENGINE_CLI_ENABLED=false`, anche per l’avvio diretto senza Compose. Lo starter Actuator fornisce `GET /actuator/health`, usato da Docker e Compose. Per debug locale da terminale o `curl`, il controllo è disponibile su `http://127.0.0.1:8083/actuator/health`; `ENGINE_PORT` modifica solo la porta host. Compose usa filesystem in sola lettura e `/tmp` scrivibile.

La configurazione resta H2 in memoria, con schema ricreato all'avvio: non usa il PostgreSQL applicativo e non conserva gli eventi al riavvio. Espone gli endpoint HTTP interni PvE e PvP; le partite PvP sono persistite dall'applicazione chiamante, non da questo modulo. Il controllo di salute non verifica il flusso applicativo completo. Il menu dimostrativo resta disponibile nell'avvio Maven interattivo descritto sopra.

## Struttura essenziale

| Percorso | Contenuto |
| --- | --- |
| `src/main/java/.../model/` e `service/` | Modelli e servizi del motore |
| `src/main/java/.../campaign/` | Modello, servizi e accesso agli eventi della campagna |
| `src/main/resources/mocks/` | Mazzi dimostrativi del motore |
| `src/main/resources/campaign/` | Stage e mazzi nemici della campagna |
| `src/test/java/` | Test JUnit del combattimento e della campagna |

Il modulo non implementa matchmaking, sfide amichevoli, autenticazione utenti, persistenza dei risultati o avanzamento della campagna: questi aspetti sono dell'applicazione o restano da realizzare. Non vengono implementati sblocco e completamento degli stage. La modalità CLI usa mazzi predefiniti e non è collegata al deckbuilder React F05; non autentica un utente e non salva la progressione.

Nel regolamento proposto il giocatore prende decisioni strategiche prima della partita, componendo e distribuendo il mazzo e scegliendo se inviare o accettare una sfida. Durante il combattimento non sono previste decisioni manuali. La scelta CLI fra campagna e scontro è solo una selezione di modalità; non sostituisce le scelte di mazzo e sfida previste per il prodotto.

## Specifiche collegate

- [F05 — Gestione e validazione dei mazzi](../../docs/BackEnd/gestione-mazzi.md)
- [F07 — Sfide e simulazione](../../docs/BackEnd/sfide-simulazione.md)
- [F08 — Storico delle partite](../../docs/BackEnd/storico-partite.md)
- [Regolamento e svolgimento del gioco](../../docs/Digital/regolamento-base.md)
- [Indice generale e stato delle funzionalità](../../docs/README.md#stato-delle-funzionalità)

Le specifiche collegate distinguono le regole proposte dai contratti tecnici ancora da concordare. La campagna non ha un ID Fxx assegnato nelle specifiche correnti e non è presentata come funzionalità approvata del perimetro.
