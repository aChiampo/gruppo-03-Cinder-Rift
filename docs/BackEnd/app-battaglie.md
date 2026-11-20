# Battaglie PvE nel backend applicativo

ID funzionalità: F07; dipendenze F01, F05 e F08.
Specifica: flusso PvE, strategia **L2 + T2**, transazione di snapshot e budget 8/9/10 secondi confermati dall'utente.
Implementazione applicativa: disponibile nella copia di lavoro del branch `feature/app_battle_pve_flow`; integrazione React non implementata. Responsabile: **DA DEFINIRE**.
Ultimo aggiornamento significativo: **2026-11-20**, implementazione richiesta dall'utente: L2 + T2, snapshot `REPEATABLE READ`, budget 8/9/10 secondi, rilascio al termine HTTP, regole F05 condivise e riparazione mirata del seed tramite V9.
Fonti: decisioni dell'utente nella conversazione del 2026-11-19/20; [PR #36](https://github.com/aChiampo/gruppo-03-Cinder-Rift/pull/36), revisione `7018b4f2a1f0e1bb33d5d0afc586e4170079451d`.
Verifica applicativa: test locali di orchestrazione con PostgreSQL e doppio HTTP del motore superati; verifiche finali della porzione corrente riportate sotto, distinte dalle evidenze precedenti.

## Scopo e riferimenti canonici

Descrivere l'orchestrazione di una battaglia PvE nel flusso `frontend → springboot_app/app → game-engine`. Il browser si rivolge esclusivamente all'applicazione; il motore rimane un servizio interno, raggiungibile in Compose su `http://game-engine:8083`. Il binding locale del motore resta disponibile per debug.

Questa pagina approfondisce la porzione applicativa di [F07 — Sfide e simulazione](sfide-simulazione.md). Il documento [API delle battaglie per il FrontEnd](app-battaglie-api.md) è l'unico riferimento canonico di endpoint, header, payload e risposte HTTP di questa porzione. Qui sono definiti flusso server, concorrenza, dipendenze e decisioni aperte, senza duplicare i payload.

La PR #36 è integrata in `dev` con il commit `786707a` ed è presente in questo branch tramite il merge `ec6a5dc`. Il branch corrente aggiunge l'orchestrazione in `springboot_app/app`, i controlli cooperativi di deadline nel motore, le regole mancanti del validatore condiviso F05 e la migrazione correttiva V9. L'integrazione React resta da implementare.

## API interna del motore implementata

Il motore espone `POST /api/battle/pve/{id}` nella rete Compose su `http://game-engine:8083`. Il browser usa il [contratto applicativo implementato](app-battaglie-api.md). Il corpo interno contiene metadati del partecipante e snapshot delle copie preparati dal backend applicativo. L'header interno `X-Battle-Deadline` contiene il termine assoluto UTC in formato ISO-8601; non è un header del contratto browser. Una deadline scaduta produce `504` prima della preparazione del mazzo; una malformata produce `400`. Senza header il motore applica comunque un limite cooperativo locale di 8 secondi, e non accetta estensioni oltre tale limite.

Aggiornamento del **2026-11-20**, su richiesta dell'utente: gli ID carta sono interi e il motore non carica né confronta pool di carte. Il backend applicativo verifica catalogo, tipo della carta, proprietà, possesso, energia, potenza, rarità e copie prima dell'invio, quindi fornisce i dati necessari alla simulazione. Il motore usa direttamente le statistiche ricevute, anche per ID assenti dai propri esempi. Il precedente payload con soli ID e destinazioni non è più sufficiente: l'adattatore applicativo produce gli snapshot descritti sotto.

L'API interna non autentica `challenger`: i suoi dati sono metadati, non una prova di identità. Decisione dell'utente del **2026-11-20**: autenticazione e autorizzazione degli utenti spettano all'applicazione; per questa implementazione la rete Docker è il confine di accesso al motore, senza ulteriori credenziali applicazione-motore. CORS non è configurato e il filtro rifiuta i preflight browser con `403`; questo filtro non autentica i servizi. Il binding host `127.0.0.1:8083`, o `ENGINE_PORT`, resta disponibile per debug e consente accesso anche ai client locali dell'host.

### Campi della richiesta interna corrente

| Campo | Tipo nel DTO | Significato |
| --- | --- | --- |
| Percorso `id` | stringa | Identificatore dello stage; sovrascrive il campo interno `stageId` del body. |
| `challenger` | oggetto facoltativo | `userId`, `username`, `displayName`, stringhe costruite dal backend dal principal F01. |
| `deck.entries` | array non vuoto | Una voce per copia, comprese eventuali copie ripetute. |
| `cardId` | intero positivo | ID numerico di `cards.id`; nessun codice editoriale o mock e nessun lookup locale. |
| `artworkId` | intero nullable e facoltativo | Metadato visivo; non influisce sulla simulazione. Alias supportati: `cardArtId`, `artwork_id`, `card_art_id`. |
| `destination` | stringa | `ROUND1`–`ROUND5` oppure `EQUIPMENT`; spazi esterni e maiuscole normalizzati. |
| `name` | stringa non vuota | Nome della carta nello snapshot. |
| `faction` | enum, creature | `CYBORG`, `MOSTRI_RADIOATTIVI`, `SAPIENS`, `CULTISTI`, `ALIENI`. Il backend converte il codice di fazione del database nel valore del motore. |
| `energyCost`, `powerPoints` | interi, creature | Costi dello snapshot, senza rivalidazione di budget o soglie nel motore. |
| `baseAtk`, `initialDef` | interi, creature | Statistiche iniziali ricevute dal backend; difesa residua inizializzata dal motore. |
| `effects` | array, entrambi i tipi | Effetti ordinati; omesso equivale a `[]`, mentre `null` è rifiutato. Sostituisce i precedenti campi di bonus fissi. |
| `effects[].effectId` | intero positivo | Identità database, mantenuta nello snapshot senza lookup locale. |
| `effects[].code` | stringa | Codice che seleziona una classe Java esplicitamente registrata nel motore. |
| `effects[].parameters` | oggetto | Parametri della classe; gli effetti attuali richiedono valori interi JSON, senza conversione da stringhe o decimali. |

Il mapper controlla la struttura necessaria alla simulazione: ID positivi, nomi, destinazioni e presenza dei campi specifici, da 1 a 5 creature per round, esattamente 5 equipaggiamenti e massimo 30 voci. Questi controlli non sostituiscono F05: il backend applicativo applica la composizione completa di 25 creature e 5 equipaggiamenti e tutte le regole di giocabilità.

| Stage | Identificativi | Mazzo e disponibilità |
| --- | --- | --- |
| Stage 1 | `1`, `stage_01`, `stage_01_mirror` | `mocks/mirror.json`, attivo per impostazione predefinita |
| Stage 3 | `3`, `stage_03`, `stage_03_demo_event` | `mocks/demo.json`, richiede `BLOOD_MOON` attivo su H2 |

### Attivazione e responsabilità degli effetti

Decisioni confermate dall'utente il **2026-11-20**: gli effetti si attivano quando la creatura entra in battaglia; bersaglio e durata dipendono dalla classe Java dedicata. Entrambi i giocatori espongono la propria creatura prima dell'attivazione: prima gli effetti degli equipaggiamenti di entrambi, poi quelli delle creature. In ciascuna fase si procede nell'ordine dei giocatori del motore; gli effetti della singola fonte seguono l'ordine dell'array. Un futuro effetto rivolto all'avversario usa il contesto di battaglia, senza lookup del catalogo.

| Codice | Parametri | Classe e comportamento |
| --- | --- | --- |
| `ATTACK_BONUS` | `amount`, intero | `AttackBonusEffect`: modifica l'attacco della creatura ospite all'ingresso; dura finché quella creatura resta in campo. |
| `STARTING_DEFENSE_MODIFIER` | `amount`, intero; `minimum`, intero positivo facoltativo, default 1 | `StartingDefenseModifierEffect`: modifica una sola volta la difesa iniziale corrente; applica il minimo dopo il modificatore. Non rigenera difesa negli scambi. |
| `FIRST_EXCHANGE_ATTACK_BONUS` | `amount`, intero | `FirstExchangeAttackBonusEffect`: installa il bonus all'ingresso e lo rimuove dopo il primo scambio della singola creatura. |

Il registro contiene solo implementazioni note; il database conserva descrizioni e configurazioni, senza nomi di classi da caricare dinamicamente. Una carta può riutilizzare le stesse classi con parametri diversi. Il mapper rifiuta codici non supportati, ID effetto non positivi e parametri mancanti, extra o di tipo errato con `422`: è un controllo di eseguibilità dello snapshot, non un confronto con un pool di carte. L'applicazione traduce questo errore interno in `502`, senza attribuire il difetto dello snapshot al mazzo inviato dal browser.

I modificatori e il conteggio degli scambi appartengono all'istanza di battaglia della creatura: copie con lo stesso ID sono indipendenti, e lo snapshot originale non viene modificato. All'ingresso di una sostituta si riapplicano equipaggiamento e abilità proprie; i danni non passano alla sostituta. L'attacco effettivo include il bonus di fazione ed è almeno zero. Entrambi gli attacchi sono calcolati prima dell'applicazione simultanea dei danni; la scadenza del primo scambio è gestita successivamente dalla classe dell'effetto. Nessuna sostituta viene schierata se il round è già concluso.

Il resoconto conserva anche righe `[EFFECT]` con proprietario, fonte, creatura ospite, codice e attivazione o scadenza. Sono eventi della battaglia: l'adattatore applicativo mantiene queste righe quando rimuove il prefisso tutorial.

### Esempio interno completo

Esempio per lo stage 1 basato su `mocks/mirror.json`, riservato al debug interno. Gli ID 1–30 di carte e arti corrispondono a un database vuoto inizializzato con V1–V6; in un database con dati preesistenti, il backend usa gli ID effettivamente assegnati. Lo snapshot delle creature deriva da V5. Le associazioni e i parametri degli effetti derivano da V8, incluse quelle di Breach Capacitor. Il backend ordina `effects` secondo `card_effects.execution_order` e include `effects.code` nello snapshot; il motore non interroga il database.

```json
{
  "challenger": { "userId": "1", "username": "test_challenger", "displayName": "Commander Test" },
  "deck": {
    "entries": [
      {"cardId": 1, "artworkId": 1, "destination": "ROUND1", "name": "Sentinel K-17", "faction": "CYBORG", "energyCost": 1, "powerPoints": 2, "baseAtk": 3, "initialDef": 5, "effects": []},
      {"cardId": 2, "artworkId": 2, "destination": "ROUND1", "name": "Explorer of the ruins", "faction": "SAPIENS", "energyCost": 1, "powerPoints": 2, "baseAtk": 3, "initialDef": 5, "effects": []},
      {"cardId": 3, "artworkId": 3, "destination": "ROUND1", "name": "Slaghound", "faction": "MOSTRI_RADIOATTIVI", "energyCost": 1, "powerPoints": 2, "baseAtk": 4, "initialDef": 4, "effects": []},
      {"cardId": 4, "artworkId": 4, "destination": "ROUND1", "name": "Orion’s Predator", "faction": "ALIENI", "energyCost": 1, "powerPoints": 2, "baseAtk": 5, "initialDef": 3, "effects": []},
      {"cardId": 5, "artworkId": 5, "destination": "ROUND1", "name": "Acolyte of the Fracture", "faction": "CULTISTI", "energyCost": 1, "powerPoints": 2, "baseAtk": 4, "initialDef": 4, "effects": []},
      {"cardId": 8, "artworkId": 8, "destination": "ROUND2", "name": "Scrap Runner", "faction": "CYBORG", "energyCost": 1, "powerPoints": 2, "baseAtk": 2, "initialDef": 3, "effects": []},
      {"cardId": 9, "artworkId": 9, "destination": "ROUND2", "name": "Ashfang", "faction": "MOSTRI_RADIOATTIVI", "energyCost": 1, "powerPoints": 2, "baseAtk": 3, "initialDef": 2, "effects": []},
      {"cardId": 10, "artworkId": 10, "destination": "ROUND2", "name": "Bunker Lookout", "faction": "SAPIENS", "energyCost": 1, "powerPoints": 2, "baseAtk": 2, "initialDef": 3, "effects": []},
      {"cardId": 11, "artworkId": 11, "destination": "ROUND2", "name": "Ash Novice", "faction": "CULTISTI", "energyCost": 1, "powerPoints": 2, "baseAtk": 3, "initialDef": 2, "effects": []},
      {"cardId": 12, "artworkId": 12, "destination": "ROUND2", "name": "Void Scout", "faction": "ALIENI", "energyCost": 1, "powerPoints": 2, "baseAtk": 3, "initialDef": 2, "effects": []},
      {"cardId": 13, "artworkId": 13, "destination": "ROUND3", "name": "Hydraulic Enforcer", "faction": "CYBORG", "energyCost": 3, "powerPoints": 2, "baseAtk": 4, "initialDef": 6, "effects": []},
      {"cardId": 14, "artworkId": 14, "destination": "ROUND3", "name": "Cindermaw", "faction": "MOSTRI_RADIOATTIVI", "energyCost": 3, "powerPoints": 3, "baseAtk": 7, "initialDef": 4, "effects": []},
      {"cardId": 15, "artworkId": 15, "destination": "ROUND3", "name": "Gauss Sharpshooter", "faction": "SAPIENS", "energyCost": 3, "powerPoints": 2, "baseAtk": 6, "initialDef": 4, "effects": []},
      {"cardId": 16, "artworkId": 16, "destination": "ROUND3", "name": "Eclipse Herald", "faction": "CULTISTI", "energyCost": 3, "powerPoints": 4, "baseAtk": 5, "initialDef": 7, "effects": []},
      {"cardId": 17, "artworkId": 17, "destination": "ROUND3", "name": "Obsidian Harvester", "faction": "ALIENI", "energyCost": 3, "powerPoints": 2, "baseAtk": 5, "initialDef": 5, "effects": []},
      {"cardId": 18, "artworkId": 18, "destination": "ROUND4", "name": "Siege Warden", "faction": "CYBORG", "energyCost": 4, "powerPoints": 4, "baseAtk": 5, "initialDef": 9, "effects": []},
      {"cardId": 19, "artworkId": 19, "destination": "ROUND4", "name": "Pier Nine Abomination", "faction": "MOSTRI_RADIOATTIVI", "energyCost": 4, "powerPoints": 3, "baseAtk": 8, "initialDef": 5, "effects": []},
      {"cardId": 20, "artworkId": 20, "destination": "ROUND4", "name": "Ark Defender", "faction": "SAPIENS", "energyCost": 4, "powerPoints": 2, "baseAtk": 4, "initialDef": 8, "effects": []},
      {"cardId": 21, "artworkId": 21, "destination": "ROUND4", "name": "Keeper of the Last Ember", "faction": "CULTISTI", "energyCost": 4, "powerPoints": 2, "baseAtk": 6, "initialDef": 6, "effects": []},
      {"cardId": 22, "artworkId": 22, "destination": "ROUND4", "name": "Quantum Stalker", "faction": "ALIENI", "energyCost": 4, "powerPoints": 3, "baseAtk": 7, "initialDef": 6, "effects": []},
      {"cardId": 23, "artworkId": 23, "destination": "ROUND5", "name": "Second Dawn Colossus", "faction": "CYBORG", "energyCost": 5, "powerPoints": 5, "baseAtk": 8, "initialDef": 8, "effects": []},
      {"cardId": 24, "artworkId": 24, "destination": "ROUND5", "name": "Fallout Behemoth", "faction": "MOSTRI_RADIOATTIVI", "energyCost": 5, "powerPoints": 2, "baseAtk": 9, "initialDef": 5, "effects": []},
      {"cardId": 25, "artworkId": 25, "destination": "ROUND5", "name": "Last Line Commander", "faction": "SAPIENS", "energyCost": 5, "powerPoints": 2, "baseAtk": 6, "initialDef": 8, "effects": []},
      {"cardId": 26, "artworkId": 26, "destination": "ROUND5", "name": "Voice Beyond the Rift", "faction": "CULTISTI", "energyCost": 5, "powerPoints": 2, "baseAtk": 7, "initialDef": 7, "effects": []},
      {"cardId": 27, "artworkId": 27, "destination": "ROUND5", "name": "Harvest Overseer", "faction": "ALIENI", "energyCost": 5, "powerPoints": 2, "baseAtk": 8, "initialDef": 6, "effects": []},
      {"cardId": 6, "artworkId": 6, "destination": "EQUIPMENT", "name": "Pulse Blade", "effects": [{"effectId": 1, "code": "ATTACK_BONUS", "parameters": {"amount": 1}}]},
      {"cardId": 7, "artworkId": 7, "destination": "EQUIPMENT", "name": "Recovery Armor", "effects": [{"effectId": 2, "code": "STARTING_DEFENSE_MODIFIER", "parameters": {"amount": 2, "minimum": 1}}]},
      {"cardId": 28, "artworkId": 28, "destination": "EQUIPMENT", "name": "Breach Capacitor", "effects": [{"effectId": 3, "code": "FIRST_EXCHANGE_ATTACK_BONUS", "parameters": {"amount": 2}}]},
      {"cardId": 29, "artworkId": 29, "destination": "EQUIPMENT", "name": "Overdrive Harness", "effects": [{"effectId": 1, "code": "ATTACK_BONUS", "parameters": {"amount": 2}}, {"effectId": 2, "code": "STARTING_DEFENSE_MODIFIER", "parameters": {"amount": -1, "minimum": 1}}]},
      {"cardId": 30, "artworkId": 30, "destination": "EQUIPMENT", "name": "Bastion Rig", "effects": [{"effectId": 1, "code": "ATTACK_BONUS", "parameters": {"amount": 1}}, {"effectId": 2, "code": "STARTING_DEFENSE_MODIFIER", "parameters": {"amount": 2, "minimum": 1}}]}
    ]
  }
}
```

In caso di successo la risposta è `PveBattleResponse`: `stageId`, `stageNumber`, `stageTitle`, `challenger`, `enemyName`, `outcome` (`VICTORY`, `DEFEAT`, `DRAW`), `winner` (nome o `NONE`), `tutorialHints` e `battleLog`. I suggerimenti sono anche anteposti al log; l'adattatore applicativo li rimuove dal payload pubblico secondo la sezione dedicata sotto.

Gli esiti HTTP sono `200` per la simulazione completata, `400` per richiesta malformata o argomenti non validi, `404` per stage inesistente, `409` se l'evento richiesto non è attivo e `422` per uno snapshot strutturalmente incompleto. Gli errori usano `ProblemDetail`; le estensioni includono `violations` per `422`, `stageId` per `404` ed `eventCode` per `409`.

L'endpoint resta dimostrativo: non implementa autenticazione, persistenza dei risultati, progressione o sfide PvP e non conferma il regolamento definitivo.

## Decisioni consolidate

| Tema | Decisione |
| --- | --- |
| Identità | L'applicazione ricava l'utente dal principal autenticato dopo la validazione del Bearer JWT e della sessione F01. L'identità non si riceve nel body. |
| Mazzo | Il frontend invia il solo ID del mazzo; il backend verifica la proprietà e carica le copie con i dati server. |
| ID delle carte | Si usa direttamente l'ID del database, senza tabella di mapping né identificativi alternativi. Il DTO del motore usa un intero positivo, coerente con `cards.id`. |
| Snapshot delle carte | Il backend fornisce dati completi di simulazione. I vecchi pool sono rimossi; i due mazzi dimostrativi usano i dati delle migrazioni. |
| Validazione | L’applicazione verifica catalogo, proprietà e giocabilità secondo F05. Il motore controlla soltanto la struttura dello snapshot e non confronta i dati con pool locali. |
| Resoconto | Si mantiene la struttura della risposta della PR #36 e il suo log testuale ordinato. Non si introduce il modello annidato di round, incontri e scambi precedentemente proposto. |
| Tutorial | I messaggi tutorial sono esclusi dalla risposta pubblica, sia dal campo dedicato sia dal prefisso del log. |
| Concorrenza | Una richiesta di battaglia attiva per utente. Gli invii sovrapposti sono rifiutati, senza accodamento né seconda chiamata al motore. |
| Strategia di esecuzione | **L2 + T2**, selezionata dall'utente il 2026-11-20: advisory lock PostgreSQL e deadline cooperativa nel motore. Il guard termina con la richiesta HTTP anche in esito incerto. |
| Snapshot | Transazione applicativa breve `REPEATABLE READ`, con lock utente e mazzo; validazione e snapshot condividono la stessa vista committata. La transazione termina prima della chiamata remota. |
| Accesso interno al motore | Confine di rete Docker, senza credenziali aggiuntive tra applicazione e motore; autenticazione e autorizzazione degli utenti nell'applicazione. Il binding locale di debug resta disponibile. |
| Timeout | Budget cumulativo dall'ingresso nell'applicazione: deadline del motore a **8 secondi**, obiettivo di risposta applicativa entro **9 secondi**, abort frontend a **10 secondi**. |
| Persistenza PvE | **DA DEFINIRE**. |
| Ricompense e progressione della campagna | **DA DEFINIRE**. |
| Scoperta degli stage | **DA DEFINIRE**. |

L'autenticazione e la proprietà sono obbligatorie per ogni chiamante. Non si introduce una delega all'uso dei mazzi altrui o una gerarchia implicita tra `ADMIN` e `USER`. Il flag `is_challengeable` riguarda la disponibilità al matchmaking: non certifica da solo la validità e non diventa un prerequisito PvE.

## Flusso applicativo implementato

1. Validare autenticazione F01 e forma della richiesta pubblica. Ricavare ID e nickname dal principal; non accettare identità o statistiche inviate dal browser.
2. Tentare l'acquisizione atomica del guard di battaglia associato all'ID utente. Se è già occupato, terminare secondo il contratto API senza attendere la battaglia precedente.
3. Caricare il mazzo con una ricerca vincolata al proprietario autenticato. Un mazzo assente e uno appartenente ad altri rimangono indistinguibili al chiamante.
4. Rivalidare le copie secondo [F05](gestione-mazzi.md) e costruire lo snapshot nella stessa transazione breve `REPEATABLE READ`. I dati rappresentano la vista committata all'inizio della preparazione, senza letture miste fra validazione e statistiche. Le modifiche successive non cambiano lo snapshot già preparato. Questo approccio F07, scelto dall'utente, non implementa il guard catalogo del [protocollo comune proposto](Database/database-schema-v1.1.md#protocollo-transazionale-comune). I lock utente e mazzo coordinano i flussi F05 esistenti; futuri scrittori F06/F10 devono essere integrati esplicitamente.
5. Costruire i metadati del partecipante usando il profilo server; espandere le copie nel formato interno corrente, includendo ID numerici, statistiche e modificatori degli equipaggiamenti provenienti dai dati server.
6. Chiamare l'API interna con una deadline cooperativa T2 e un budget temporale compatibile con i 10 secondi complessivi. Non inoltrare il Bearer dell'utente: il confine di accesso scelto è la rete Docker, senza credenziali aggiuntive tra i servizi; il motore non autentica i metadati del partecipante.
7. Validare la risposta, eliminare i contenuti tutorial e costruire la risposta pubblica. Gli errori interni vanno tradotti nel contratto API; uno snapshot interno malformato non deve essere presentato come errore imputabile al mazzo dell’utente.
8. Terminare la richiesta e rilasciare il guard posseduto anche dopo timeout, errore o chiusura osservata del trasporto. Al timeout applicativo si interrompe l'attesa e si scarta il risultato tardivo. Non si ritenta automaticamente il POST; non si attende una riconciliazione del motore.

La scelta di lock non autorizza a mantenere automaticamente aperta una transazione sui dati utente per l'intera chiamata remota. Validazione, preparazione dei dati e guard di esecuzione sono responsabilità distinte.

## Adattamento del resoconto della PR

Nella revisione consultata, `PveBattleService` costruisce il log completo anteponendo i suggerimenti tutorial al log del motore e restituisce anche i suggerimenti separatamente. Eliminare soltanto il campo dedicato lascerebbe quindi gli stessi messaggi nel resoconto pubblico.

L'adattatore applicativo deve verificare il blocco iniziale corrispondente ai suggerimenti ricevuti, rimuoverlo e omettere il campo tutorial. Il resto del log conserva ordine, stringhe, spazi e ritorni a capo. Non filtrare per parola chiave e non eliminare globalmente messaggi uguali: l'operazione riguarda il prefisso definito dal contratto interno. Un'incoerenza del formato interno richiede gestione come errore di contratto, senza inoltrare suggerimenti per errore. Il titolo dello stage è metadato e rimane invariato, anche se contiene la parola «Tutorial».

L'esito strutturato già presente nella PR è autorevole rispetto al testo del log. Non si deduce il vincitore confrontando nomi o analizzando frasi. Non vengono introdotti ID di battaglia, eventi strutturati, premi o stato della campagna nella risposta.

## Concorrenza, durata del lock e richieste ripetute

Il guard è riferito all'utente, non alla sessione, al mazzo o allo stage: due schede o due sessioni dello stesso utente concorrono per lo stesso accesso. Gli altri utenti restano indipendenti. Dopo il termine e il rilascio del guard, un nuovo invio avvia una nuova simulazione; non si definisce una deduplicazione permanente delle richieste concluse.

L'acquisizione deve essere atomica. Un controllo «libero» seguito da un'assegnazione separata non protegge dalla concorrenza. Solo il proprietario corrente può rilasciare o rinnovare il guard. La disabilitazione del pulsante nel frontend non sostituisce questa protezione server.

Il termine della richiesta è osservato dal backend: non è possibile attendere una prova che il browser abbia ricevuto la risposta. La sola disconnessione del browser non dimostra che il lavoro del motore sia terminato.

Occorre distinguere due garanzie:

- **Una richiesta HTTP attiva per utente:** il guard può essere rilasciato alla conclusione HTTP, incluso il timeout. Una precedente simulazione potrebbe però continuare nel motore.
- **Una simulazione attiva per utente anche dopo un guasto:** il guard resta finché completamento o arresto sono accertati. Può sopravvivere alla risposta di timeout; durante il recupero gli invii restano rifiutati.

L'utente ha confermato il **2026-11-20** la combinazione **L2 + T2** e il rilascio del guard al termine HTTP anche quando l'arresto remoto non è confermato. Il prototipo garantisce una richiesta HTTP attiva per utente; dopo guasti può esserci sovrapposizione di calcoli remoti. La deadline cooperativa limita l'esecuzione ordinaria, senza costituire prova di arresto durante un guasto. Le altre combinazioni seguenti restano alternative non selezionate.

## Opzioni per lock, istanze multiple e recupero

| Opzione | Meccanismo | Coordinamento tra istanze | Recupero e limiti |
| --- | --- | --- | --- |
| **L1 — Guard in memoria** | Struttura atomica per ID utente nel processo Java. | No; ammissibile solo con una singola istanza dichiarata. | Al crash il guard locale scompare, ma il motore remoto può continuare. Non soddisfa da solo il coordinamento tra più backend. |
| **L2 — Advisory lock PostgreSQL** | Acquisizione non bloccante su chiave di dominio battaglie e ID utente. | Sì, usando lo stesso PostgreSQL. | Nessuna nuova tabella; il lock di sessione termina al rilascio esplicito o alla chiusura della sessione DB. Occorre riservare una connessione per richiesta attiva. La perdita della sessione non arresta il motore. |
| **L3 — Registro operativo PostgreSQL** | Claim atomico per utente con identificativo richiesta, proprietario, stato e scadenza operativa. | Sì. | Stato ispezionabile da altre istanze; recupero dei record abbandonati e riconciliazione col motore. Richiede migrazione e logica operativa, senza implicare che lo storico F08 sia definito. |
| **L4 — Lease Redis** | Chiave per utente con token proprietario, durata e rinnovo. | Sì. | Nuovo servizio e dipendenza da concordare. La scadenza permette recupero, ma non prova la cessazione del vecchio lavoro; richiede regole di proprietà, rinnovo e guasto. |

### L2: gestione della connessione

Usare una funzione di tentativo, ad esempio `pg_try_advisory_lock`, per non accodare la battaglia. Un lock di sessione si acquisisce e si rilascia sulla stessa connessione fisica, mantenuta riservata fino alla pulizia. Restituirla al pool non chiude la sessione: il lock potrebbe restare attivo o essere riacquisito dalla stessa sessione. Il rollback della transazione non rilascia un lock di sessione.

Al crash il rilascio avviene quando PostgreSQL rileva la fine della sessione; non è necessariamente immediato. I lock di transazione, invece, richiederebbero di mantenere aperta una transazione per tutta la loro durata: non sono la variante implicitamente scelta da L2.

### L3: registro operativo e proprietà

Campi indicativi, non uno schema approvato: `user_id`, `request_id`, `owner_id`, `status`, `lease_until`, `updated_at`. Claim, rinnovo e pulizia sono operazioni atomiche con transazioni brevi. La pulizia deve verificare il proprietario e l'ID della richiesta, impedendo a una vecchia esecuzione di eliminare il guard di quella nuova.

La scadenza indica che il proprietario potrebbe essere perso, non che il motore sia fermo. Con la garanzia forte, una lease scaduta entra nel recupero prima di autorizzare una nuova simulazione. Un token che impedisce a una vecchia richiesta di scrivere risultati non impedisce, da solo, la sovrapposizione fisica di due calcoli.

### L4: dipendenza aggiuntiva

Redis non è richiesto dall'architettura corrente e non viene aggiunto da questa attività. L'opzione richiede rilascio condizionato al token proprietario e una politica di scadenza/rinnovo coerente con il lavoro del motore. Un TTL fisso di 10 secondi, da solo, non equivale a cancellazione né a esclusione sicura dopo un guasto.

## Opzioni dopo timeout HTTP

| Opzione | Comportamento | Quando si libera l'utente | Lavoro richiesto al motore |
| --- | --- | --- | --- |
| **T1 — Interrompere l'attesa** | Il backend termina la richiesta e scarta l'eventuale risultato tardivo. | Alla conclusione HTTP. | Minimo; il calcolo può continuare e sovrapporsi al successivo. |
| **T2 — Deadline cooperativa** | Il backend comunica un termine di esecuzione; il motore lo verifica prima dell'avvio e tra round/scambi. | Nel prototipo scelto, al termine HTTP anche in esito incerto; si accetta possibile sovrapposizione remota dopo guasti. | Propagazione della deadline, controlli nel ciclo e uscita cooperativa. Non risolve da sola la perdita del backend. |
| **T3 — Tracciare, cancellare e riconciliare** | Operazione identificata; dopo timeout si richiede cancellazione o si consulta lo stato finché diventa certo. | Dopo completamento, fallimento o cancellazione effettivamente accertati; può essere oltre i 10 secondi. | Identificativi operativi, controllo degli avvii, stato durevole, consultazione/cancellazione e recupero. |

Il timeout HTTP resta **10 secondi**. L'utente ha approvato deadline motore a **8 secondi**, obiettivo di risposta backend a **9 secondi** e abort frontend a **10 secondi**, misurati sullo stesso invio: preparazione e attese consumano il budget, senza aggiungere 8 secondi a ogni chiamata. Il timer applicativo copre preparazione e chiamata remota; i timeout SQL limitano le attese dei lock dati. L'autenticazione e la decodifica precedono l'avvio asincrono: il tempo già trascorso viene sottratto, ma un blocco in tali fasi o del runtime non è interrompibile dal timer dell'orchestratore. Il termine assoluto richiede orologi dei servizi sincronizzati. Il wrapper React interrompe l'attesa a 10.000 ms, senza provare l'arresto remoto.

### T1: garanzia limitata alla richiesta

La risposta tardiva viene ignorata e non si ritenta automaticamente la simulazione. Questa opzione permette un nuovo invio mentre il lavoro precedente può essere ancora attivo. È selezionabile soltanto accettando tale limite; non soddisfa la garanzia di un solo calcolo attivo per utente.

### T2: cancellazione cooperativa

Il motore controlla il termine anche prima di iniziare un lavoro arrivato in ritardo. Il solo annullamento della chiamata HTTP non esegue questi controlli. `Future.cancel(true)` tenta un'interruzione, che il codice deve gestire; `CompletableFuture.cancel(true)` non interrompe il calcolo sottostante. La cancellazione di un future non è una prova sufficiente dell'uscita del worker.

La cooperazione è applicata prima della preparazione, prima della simulazione, fra round e scambi e prima del risultato finale. Non garantisce un arresto istantaneo di thread sospesi o operazioni non interrompibili. Per decisione dell'utente, dopo un esito incerto si rilascia il guard alla conclusione HTTP e non si tenta di accertare l'arresto tramite un registro operativo.

### T3: ciclo operativo e riconciliazione

Stati indicativi da concordare: `RUNNING`, `COMPLETED`, `FAILED`, `CANCEL_REQUESTED`, `CANCELLED`. Una richiesta di cancellazione non equivale a una cancellazione completata.

Il backend assegna un ID interno; il motore controlla atomicamente gli avvii per richiesta e per utente. Un processo di recupero consulta o cancella la stessa operazione, senza rilanciarla alla cieca. Lo stato deve sopravvivere ai riavvii: H2 in memoria e un registro locale del solo processo non bastano. Un risultato «non trovato» non dimostra da solo che nulla stia eseguendo, soprattutto con richieste ritardate o istanze differenti.

I futuri endpoint interni di stato/cancellazione, il protocollo contro gli avvii tardivi, la durata di conservazione operativa e l'accesso del motore allo stato durevole sono **DA DEFINIRE**. Non si introduce ora alcuna rotta di polling o cancellazione per il frontend. Se il motore è irraggiungibile, la garanzia forte può mantenere l'utente bloccato fino al recupero.

## Combinazioni da scegliere

| Combinazione | Uso previsto | Limite o costo |
| --- | --- | --- |
| **L2 + T1** | Minimo intervento con coordinamento tra backend. | Il lavoro remoto può sopravvivere al timeout o al crash. |
| **L2 + T2** | Soluzione intermedia per il prototipo con PostgreSQL esistente. | Connessione riservata e incertezze residue dopo crash o partizioni; non è una garanzia forte di recupero. |
| **L3 + T3**, con controlli di deadline | Recupero esplicito e controllo delle esecuzioni durante esiti incerti. | Registro operativo, protocollo del motore e possibile blocco oltre la risposta HTTP. |
| **L4 + T3** | Coordinamento Redis se si sceglie tale infrastruttura. | Nuovo servizio, semantica delle lease e registro delle operazioni. |

**Decisione dell'utente del 2026-11-20: implementare L2 + T2**, budget 8/9/10 secondi e rilascio alla conclusione HTTP anche in esito incerto. L1 resta un'alternativa limitata a una sola istanza; L3 + T3 e L4 + T3 non sono selezionate. Non si introduce la garanzia forte di recupero T3.

## Persistenza, ricompense e campagna

[F08](storico-partite.md) e la [specifica del database](Database/database-schema-v1.1.md#storico-battaglie) documentano già `BATTLE_HISTORY`: due partecipanti con FK obbligatorie a utenti, vincitore nullable, fotografie dei mazzi, data e variazione di classifica. Le migrazioni applicative V1–V6 non creano lo storico.

Non è corretto dichiarare assente ogni documentazione della tabella, ma quella esistente non definisce il bot PvE né la conservazione del log testuale della PR. Rappresentazione dell'avversario, record PvE, formato del log salvato, conservazione e migrazione restano **DA DEFINIRE**. Non inventare un utente bot o una nuova tabella di storico in questa fase.

Ricompense e progressione della campagna restano **DA DEFINIRE**, senza applicazione implicita delle regole di ranking PvP. Anche la scoperta degli stage rimane **DA DEFINIRE**. Un eventuale registro operativo L3/T3 non decide automaticamente questi aspetti di dominio.

## Criteri di accettazione da verificare

| ID | Scenario | Risultato atteso |
| --- | --- | --- |
| F07-APP-01 | Richiesta valida con proprio mazzo e sessione F01 attiva. | Una chiamata al motore, risposta conforme al documento API. |
| F07-APP-02 | Token assente, scaduto o sessione revocata. | Nessuna chiamata al motore e rifiuto di autenticazione. |
| F07-APP-03 | ID mazzo assente dal database oppure appartenente ad altri. | Stessa risposta pubblica, nessuna esposizione della proprietà altrui. |
| F07-APP-04 | Invii concorrenti dello stesso utente da sessioni, schede, mazzi o stage differenti. | Uno solo acquisisce il guard; gli altri sono rifiutati senza accodamento. Per L2/L3/L4 provare due istanze. |
| F07-APP-05 | Errore di validazione dopo acquisizione del guard e prima dell'avvio remoto. | Nessuna simulazione; pulizia del solo guard posseduto. |
| F07-APP-06 | Risposta con suggerimenti separati e anteposti al log. | Nessun suggerimento nel payload pubblico; resto del log invariato. |
| F07-APP-07 | Snapshot di un mazzo database. | ID carta numerici e statistiche server preservati, nessun lookup o confronto con pool nel motore. |
| F07-APP-08 | Deadline HTTP raggiunta o browser disconnesso. | Risposta/errore di trasporto coerenti con il contratto; comportamento del guard conforme all'opzione selezionata. |
| F07-APP-09 | Crash, perdita della sessione DB, lease scaduta o risposta del motore persa. | Garanzie e recupero verificati secondo L/T selezionati; nessuna pulizia da parte di un vecchio proprietario. |
| F07-APP-10 | Risposta inattesa del motore o cataloghi non allineati. | Errore di integrazione, senza attribuire il difetto al chiamante. |

Per gli scenari di crash e perdita di sessione DB valgono i limiti L2 + T2 accettati dall'utente. Non sono stati iniettati guasti del processo o della rete; le verifiche correnti distinguono questi limiti dal timeout HTTP effettivamente provato.

## Decisioni aperte e stato delle verifiche

- **IMPLEMENTATA:** combinazione L2 + T2, su decisione dell'utente del 2026-11-20; guard PostgreSQL, deadline cooperativa e budget cumulativo 8/9/10 secondi.
- **CONFERMATO:** rilascio alla conclusione HTTP anche in esito incerto, senza registro operativo o recupero forte; possibile sovrapposizione remota dopo guasti accettata.
- **IMPLEMENTATA:** consistenza di validazione e snapshot tramite transazione breve applicativa `REPEATABLE READ`; coordinamento del futuro protocollo condiviso F06/F10 ancora da integrare.
- **CONFERMATO:** autenticazione e autorizzazione degli utenti nell'applicazione; confine di accesso applicazione-motore affidato alla rete Docker, senza ulteriori credenziali interne; binding locale di debug mantenuto.
- **DA DEFINIRE:** persistenza PvE, ricompense, progressione della campagna e scoperta degli stage.
- Dipendenze note: adattatore frontend da implementare; persistenza F08 e funzionalità complete PvP escluse dalla porzione corrente.

La revisione della PR dichiara 25 test JUnit superati il 2026-11-19. È un'evidenza riportata dall'autore, non una nuova esecuzione di questa attività né una verifica dell'integrazione applicativa.

Verifica corrente dell'orchestrazione, **2026-11-20**, copia di lavoro su base `83262dd`: **212 test applicativi** e **49 del motore** superati con JDK 21.0.11, PostgreSQL 18.6 isolato e H2 per la campagna. JAR prodotti; provato il flusso HTTP fra applicazione e motore reali con JWT F01 e mazzo CR01 migrato: `200`, 216 righe, esito `DRAW`; verificati anche evento inattivo `409`, stage sconosciuto `404` e presenza della rotta OpenAPI. [Report con comandi, ambiente, copertura e limiti](../report/2026-11-20-orchestrazione-pve.md). Integrazione React, deploy delle nuove immagini e recupero forte dopo guasti non verificati.

Verifica precedente degli effetti, **2026-11-20**: `mvnw.cmd clean test -q` con JDK 21.0.11 su Windows ha superato **45 test del motore** e **139 test applicativi**, senza fallimenti, errori o casi saltati. Il motore usa H2 per la campagna; la suite applicativa usa PostgreSQL **18.6** in un container isolato. Le prove coprono ordine equipaggiamento/creatura, attivazione unica, scadenza al primo scambio, copie e round indipendenti, danni simultanei, nessuna nuova entrata dopo la fine del round, payload HTTP, installazione V1–V8, aggiornamento da V6 e vincoli SQL. Confrontate inoltre le 60 occorrenze di carte nelle fixture con gli effetti del database migrato. Quella verifica non eseguiva l'orchestrazione applicativa PvE o il browser.

Verifica precedente del motore, **2026-11-20**, prima del modello di effetti: `mvnw.cmd clean test -q` con JDK 21.0.11 su Windows, 35 test JUnit superati, senza fallimenti, errori o test saltati. Verificati ID numerici, mapping degli snapshot senza pool, composizione delle fixture e modificatori degli equipaggiamenti. Confrontate separatamente le statistiche delle creature con V5 e controllati gli esempi JSON interni e pubblici. La suite usa H2; orchestrazione applicativa, autenticazione server-to-server e PostgreSQL non sono verificati da questa esecuzione.

Verifica documentale iniziale del **2026-11-20**, precedente al merge della PR #36: 6 esempi JSON validi e 204 link relativi verificati sui due nuovi file e sui sei riferimenti aggiornati; `git diff --check` senza errori. In quella fase sono stati confrontati anche gli hash dei 16 file preesistenti nel checkout originario. Si tratta di evidenze della prima redazione, non di una verifica dei file risultanti dal merge. Nessuna build o prova runtime è attestata da questa attività documentale.

Verifica dell'allineamento successivo al merge, **2026-11-20**, sulla base `ec6a5dc`: eliminata la guida precedente e controllati gli 11 documenti esistenti modificati, con 8 esempi JSON validi e 242 link relativi/ancore risolti. Verificati staticamente i tipi del DTO e gli ID/tipi/destinazioni delle 30 copie nell'esempio interno rispetto ai pool correnti, il prefisso tutorial della risposta interna rispetto allo stage e la forma del payload pubblico senza identità in input né suggerimenti in output. Rimossi i riferimenti alla sezione non più esistente; la documentazione FrontEnd conserva soltanto comportamento client e rimandi ai contratti BackEnd. `git diff --check` senza errori. Questi controlli documentali non eseguono il motore né verificano l'integrazione runtime.

## Fonti tecniche e collegamenti

- [PR #36 — Endpoint PvE del motore](https://github.com/aChiampo/gruppo-03-Cinder-Rift/pull/36) e [servizio nella revisione consultata](https://github.com/aChiampo/gruppo-03-Cinder-Rift/blob/7018b4f2a1f0e1bb33d5d0afc586e4170079451d/springboot_app/game_engine/src/main/java/com/benedetto/game_engine/battle/service/PveBattleService.java).
- [PostgreSQL 18 — Advisory lock](https://www.postgresql.org/docs/18/explicit-locking.html#ADVISORY-LOCKS) e [funzioni di tentativo e rilascio](https://www.postgresql.org/docs/18/functions-admin.html#FUNCTIONS-ADVISORY-LOCKS).
- [Redis — Lock distribuiti](https://redis.io/docs/latest/develop/clients/patterns/distributed-locks/).
- [Java 21 — Future](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/concurrent/Future.html#cancel(boolean)) e [CompletableFuture](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/concurrent/CompletableFuture.html#cancel(boolean)).
- [API pubblica per il FrontEnd](app-battaglie-api.md) · [F07](sfide-simulazione.md) · [F01](accesso-utenti.md) · [F05](gestione-mazzi.md) · [F08](storico-partite.md).
- [Interfaccia F07](../FrontEnd/sfide-simulazione.md) · [Regolamento](../Digital/regolamento-base.md) · [Indice BackEnd](README.md) · [Stato delle funzionalità](../README.md#stato-delle-funzionalità).
