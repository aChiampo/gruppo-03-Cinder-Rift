# Motore e campagna di gioco

`game_engine` è un'unica applicazione Spring Boot che contiene il motore delle partite e i servizi dimostrativi della campagna. La campagna è organizzata nel package `com.benedetto.game_engine.campaign`; non è più un modulo Maven o un'applicazione avviabile separatamente.

> **Stato:** il codice presente è una prima implementazione dimostrativa. Include l'endpoint HTTP PvE descritto sotto, limitato alla simulazione di campagna/tutorial prevista per il SAL; non costituisce l'implementazione completa delle funzionalità F05, F07 o F08, né conferma le regole proposte come definitive.

Ultimo aggiornamento significativo: **2026-11-19**, documentazione dell'uso server-to-server dell'endpoint HTTP dimostrativo PvE. Fonte: controller, DTO, risorse e test del modulo.

## Motore di gioco

- `MatchEngine` orchestra fino a cinque round e interrompe la partita quando un giocatore raggiunge tre round vinti. Restituisce il resoconto come lista di stringhe.
- `CombatEngine` esegue scambi simultanei fra le creature attive, gestisce la sostituzione delle creature eliminate e il danno in eccesso ai punti vita del giocatore.
- Il bonus d'attacco di fazione è `+2` quando la fazione attaccante è avvantaggiata; al calcolo dell'attacco si somma `Equipment.atkBonus`.
- Ogni round ripristina i punti vita a 30. Il combattimento termina anche al raggiungimento del limite difensivo di 50 scambi.
- `CardLoaderService` deserializza un mazzo JSON da una risorsa del classpath. Il flag `deterministic` disabilita il mescolamento.

Questi comportamenti descrivono il codice attuale, non un contratto di gioco approvato. `Equipment` contiene anche `defBonus`, ma il flusso di combattimento corrente non lo applica alla difesa delle creature. Il loader non esegue la validazione completa dei mazzi prevista dalla specifica F05.

## Campagna

`CampaignService.executeStage` carica uno stage JSON, verifica l'eventuale codice evento richiesto, carica il mazzo nemico e avvia la partita tramite `MatchEngine`. Se l'evento richiesto è assente o inattivo, restituisce un messaggio nel resoconto senza iniziare la partita. I suggerimenti tutorial vengono anteposti al resoconto della partita.

`CampaignStageLoaderService` legge gli stage dal classpath; accetta `classpath:`, un percorso relativo a `campaign/stages/` oppure il nome del file. I dati sono in `src/main/resources/campaign/`:

| Percorso | Contenuto |
| --- | --- |
| `stages/` | Configurazioni di esempio per lo stage 1 e lo stage 3 |
| `decks/` | Mazzi nemici degli stage dimostrativi |

Lo stage 3 richiede l'evento `BLOOD_MOON`. L'applicazione usa H2 in memoria e crea lo schema JPA all'avvio; non è presente un seed per tale evento, quindi lo stage 3 non supera il controllo nella configurazione predefinita.

## API dimostrativa PvE

L'applicazione espone l'API interna `POST /api/battle/pve/{id}` per simulare una partita della campagna/tutorial. Nella rete Docker Compose è raggiungibile esclusivamente dal backend applicativo (`springboot_app/app`) all'indirizzo `http://game-engine:8083/api/battle/pve/{id}`. Nel flusso previsto `frontend -> springboot_app/app -> game-engine`, il frontend dovrà rivolgersi al backend applicativo; l'orchestrazione PvE in `springboot_app/app` non è ancora implementata e non è definita né disponibile una rotta pubblica dell'applicazione per questa funzione. Compose pubblica `127.0.0.1:8083` sulla macchina host; `ENGINE_PORT` modifica solo la porta host. Questo binding è riservato al debug e alle prove locali da terminale o `curl`, non all'accesso dal browser. CORS non è configurato e le richieste preflight sono rifiutate con `403`.

Gli identificativi accettati sono:

| Stage | Identificativi | Disponibilità |
| --- | --- | --- |
| Stage 1 | `1`, `stage_01`, `stage_01_fazioni` | Attivo per impostazione predefinita |
| Stage 3 | `3`, `stage_03`, `stage_03_evento_speciale` | Bloccato finché `BLOOD_MOON` non è attivo su H2 |

Il mazzo sfidante contiene da 1 a 5 creature per ciascuna destinazione `ROUND1`..`ROUND5` e esattamente 5 carte in `EQUIPMENT`, per un massimo di 30 voci totali. Ogni `cardId` deve esistere nei pool JSON `src/main/resources/mocks/pool_*.json`; per esempio, il pool Sapiens contiene `SAP_R1_1`..`SAP_R5_5` e il pool equipaggiamenti contiene `EQ_1`..`EQ_5`. L'endpoint non implementa autenticazione, persistenza del risultato o progressione.

Esempio completo di richiesta valida per lo stage 1:

```json
{
  "challenger": {
    "userId": "user-test-01",
    "username": "test_challenger",
    "displayName": "Commander Test"
  },
  "deck": {
    "entries": [
      { "cardId": "SAP_R1_1", "destination": "ROUND1" },
      { "cardId": "SAP_R1_2", "destination": "ROUND1" },
      { "cardId": "SAP_R1_3", "destination": "ROUND1" },
      { "cardId": "SAP_R1_4", "destination": "ROUND1" },
      { "cardId": "SAP_R1_5", "destination": "ROUND1" },
      { "cardId": "SAP_R2_1", "destination": "ROUND2" },
      { "cardId": "SAP_R2_2", "destination": "ROUND2" },
      { "cardId": "SAP_R2_3", "destination": "ROUND2" },
      { "cardId": "SAP_R2_4", "destination": "ROUND2" },
      { "cardId": "SAP_R2_5", "destination": "ROUND2" },
      { "cardId": "SAP_R3_1", "destination": "ROUND3" },
      { "cardId": "SAP_R3_2", "destination": "ROUND3" },
      { "cardId": "SAP_R3_3", "destination": "ROUND3" },
      { "cardId": "SAP_R3_4", "destination": "ROUND3" },
      { "cardId": "SAP_R3_5", "destination": "ROUND3" },
      { "cardId": "SAP_R4_1", "destination": "ROUND4" },
      { "cardId": "SAP_R4_2", "destination": "ROUND4" },
      { "cardId": "SAP_R4_3", "destination": "ROUND4" },
      { "cardId": "SAP_R4_4", "destination": "ROUND4" },
      { "cardId": "SAP_R4_5", "destination": "ROUND4" },
      { "cardId": "SAP_R5_1", "destination": "ROUND5" },
      { "cardId": "SAP_R5_2", "destination": "ROUND5" },
      { "cardId": "SAP_R5_3", "destination": "ROUND5" },
      { "cardId": "SAP_R5_4", "destination": "ROUND5" },
      { "cardId": "SAP_R5_5", "destination": "ROUND5" },
      { "cardId": "EQ_1", "destination": "EQUIPMENT" },
      { "cardId": "EQ_2", "destination": "EQUIPMENT" },
      { "cardId": "EQ_3", "destination": "EQUIPMENT" },
      { "cardId": "EQ_4", "destination": "EQUIPMENT" },
      { "cardId": "EQ_5", "destination": "EQUIPMENT" }
    ]
  }
}
```

In caso di successo la risposta è `PveBattleResponse`; gli elementi di `battleLog` dipendono dall'esecuzione. Esempio abbreviato (il log effettivo contiene l'intero resoconto):

```json
{
  "stageId": "stage_01",
  "stageNumber": 1,
  "stageTitle": "Tutorial: Il Vantaggio di Fazione",
  "challenger": {
    "userId": "user-test-01",
    "username": "test_challenger",
    "displayName": "Commander Test"
  },
  "enemyName": "Recluta Cultista",
  "outcome": "VICTORY",
  "winner": "Commander Test",
  "tutorialHints": [
    "[TUTORIAL] Benvenuto comandante! In Cinder Rift ogni fazione possiede un vantaggio strategico su due fazioni rivali.",
    "[TUTORIAL] Quando una creatura attacca una fazione su cui ha vantaggio, riceve un bonus di +2 ATK per tutto lo scontro!",
    "[TUTORIAL] Ricorda: i SAPIENS hanno vantaggio sui CULTISTI (+2 ATK) e sugli ALIENI. Approfittane per vincere i round!"
  ],
  "battleLog": [
    "[TUTORIAL] Benvenuto comandante! In Cinder Rift ogni fazione possiede un vantaggio strategico su due fazioni rivali.",
    "[TUTORIAL] Quando una creatura attacca una fazione su cui ha vantaggio, riceve un bonus di +2 ATK per tutto lo scontro!",
    "[TUTORIAL] Ricorda: i SAPIENS hanno vantaggio sui CULTISTI (+2 ATK) e sugli ALIENI. Approfittane per vincere i round!",
    "=========================================="
  ]
}
```

Gli esiti HTTP sono `200` per la simulazione completata, `400` per richiesta malformata o argomenti non validi, `404` per stage inesistente, `409` se l'evento richiesto non è attivo e `422` per un mazzo non valido. Gli errori usano RFC 7807 `ProblemDetail`; le estensioni includono `violations` per `422`, `stageId` per `404` ed `eventCode` per `409`.

Il limite è intenzionale: questo endpoint dimostrativo copre la simulazione campagna/tutorial per il SAL, non sostituisce il contratto completo F07 ancora da consolidare né l'implementazione di sfide PvP, autenticazione JWT e persistenza su PostgreSQL. Il [contratto BackEnd F07](../../docs/BackEnd/sfide-simulazione.md#endpoint-dimostrativo-pve-per-il-sal) raccoglie i dettagli tecnici.

## Avvio e verifiche

Dalla directory `springboot_app/game_engine/`:

```bash
./mvnw test
./mvnw spring-boot:run
```

All'avvio il `CommandLineRunner` presenta un menu:

1. **Campagna** — avvia il primo stage tutorial (`stage_01_fazioni.json`).
2. **Scontro online** — simula uno scontro usando i due mazzi dimostrativi JSON.

Il menu è un flusso CLI dimostrativo, non un matchmaking online né un'interfaccia web. La scelta del giocatore determina quale flusso invocare; un input terminato prima della scelta produce un errore esplicito. Per disabilitare il prompt in un ambiente senza input interattivo, avviare con `--game-engine.cli.enabled=false`. I test verificano il caricamento del contesto, entrambe le scelte, l'input non valido e il caricamento e l'esecuzione degli stage.

Verifica del **2026-11-19**: `./mvnw test` completato con **25 test JUnit eseguiti, 0 fallimenti e 0 errori**, inclusi i 6 test aggiornati di `BattleControllerTest`. Sono verificati anche la POST senza header `Origin` e il rifiuto dei preflight, senza configurare CORS. La verifica usa H2; non attesta l'integrazione con PostgreSQL, l'orchestrazione in `springboot_app/app` né le funzionalità applicative complete.

La verifica Docker del **2026-11-16** ha superato **19 test** nello stage Java del build, costruito l'immagine e rilevato il servizio sano su `8083` nello stack Compose isolato, con menu disabilitato e filesystem in sola lettura. [Evidenze e limiti](../../docs/report/2026-11-16-verifica-docker-compose.md). Questa evidenza resta distinta dalla suite eseguita il 2026-11-19.

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

La configurazione resta H2 in memoria, con schema ricreato all'avvio: non usa il PostgreSQL applicativo e non conserva gli eventi al riavvio. È disponibile soltanto l'endpoint HTTP dimostrativo PvE interno descritto sopra; il controllo di salute non attesta l'orchestrazione applicativa, che non è implementata. Il menu dimostrativo resta disponibile nell'avvio Maven interattivo descritto sopra.

## Struttura essenziale

| Percorso | Contenuto |
| --- | --- |
| `src/main/java/.../model/` e `service/` | Modelli e servizi del motore |
| `src/main/java/.../campaign/` | Modello, servizi e accesso agli eventi della campagna |
| `src/main/resources/mocks/` | Mazzi dimostrativi del motore |
| `src/main/resources/campaign/` | Stage e mazzi nemici della campagna |
| `src/test/java/` | Test JUnit del combattimento e della campagna |

Non sono presenti API HTTP per le sfide PvP, persistenza dei risultati di partita o avanzamento della campagna; non vengono implementati sblocco e completamento degli stage. La modalità CLI usa mazzi predefiniti perché il deckbuilder è ancora in sviluppo; non autentica un utente e non salva la progressione.

Nel regolamento proposto il giocatore prende decisioni strategiche prima della partita, componendo e distribuendo il mazzo e scegliendo se inviare o accettare una sfida. Durante il combattimento non sono previste decisioni manuali. La scelta CLI fra campagna e scontro è solo una selezione di modalità; non sostituisce le scelte di mazzo e sfida previste per il prodotto.

## Specifiche collegate

- [F05 — Gestione e validazione dei mazzi](../../docs/BackEnd/gestione-mazzi.md)
- [F07 — Sfide e simulazione](../../docs/BackEnd/sfide-simulazione.md)
- [F08 — Storico delle partite](../../docs/BackEnd/storico-partite.md)
- [Regolamento e svolgimento del gioco](../../docs/Digital/regolamento-base.md)
- [Indice generale e stato delle funzionalità](../../docs/README.md#stato-delle-funzionalità)

Le specifiche collegate distinguono le regole proposte dai contratti tecnici ancora da concordare. La campagna non ha un ID Fxx assegnato nelle specifiche correnti e non è presentata come funzionalità approvata del perimetro.
