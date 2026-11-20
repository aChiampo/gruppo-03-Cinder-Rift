# Sfide e simulazione

ID: F07 · Perimetro: confermato; specifica in approfondimento.
Implementazione: endpoint HTTP dimostrativo PvE per campagna/tutorial disponibile per il SAL; implementazione completa F07 non iniziata. Responsabile: DA DEFINIRE (Software Developer).
Fonte: traccia, risposte e chiarimenti del gruppo; le decisioni note sono riportate sotto.
Fonte dell'approfondimento: regolamento Versione 2.1, proposta per il prototipo e le prove di bilanciamento; integrazione documentale approvata, regole definitive da confermare.
Ultimo aggiornamento significativo il 2026-11-19, documentazione dell'uso server-to-server dell'endpoint dimostrativo PvE. Verifica della suite JUnit disponibile; integrazione applicativa completa F07 e bilanciamento da verificare.
Fonte dati: [specifica canonica v1.1](Database/database-schema-v1.1.md#storico-battaglie).

## Obiettivo e decisioni note

- Un utente propone una sfida scegliendo un mazzo; l'avversario accetta scegliendo il proprio.
- Il server mescola i mazzi e simula la modalità base senza decisioni interattive durante la partita.
- Il motore deve essere un componente Java isolato, coperto da test JUnit.
- Risultato e dettaglio della simulazione devono essere disponibili ai due giocatori.

## Dati e confini della persistenza v1.1

- Utenti, mazzi, carte, arti e battaglie hanno identificativi `int`; le PK autonome sono auto-incrementali. Le FK copiano gli ID esistenti. `DECKS.is_challengeable`, obbligatorio con default `false`, abilita la selezione nel matchmaking senza attestare la validità del mazzo.
- Prima dello snapshot il backend dovrà rivalidare proprietà, composizione, possesso della precisa variante e configurazioni di `ROUND_CONFIGURATIONS`, inclusi numero massimo di carte, energia di ogni carta entro `max_card_energy` e potenza aggregata per destinazione entro `max_round_power`. Il [contratto F05](gestione-mazzi.md#implementazione-backend-disponibile) descrive i limiti iniziali, incluso `EQUIPMENT`; l’integrazione della rivalidazione in F07 resta da implementare.
- `BATTLE_HISTORY` conserva due partecipanti, vincitore nullable, `deck_player1` e `deck_player2` JSONB obbligatori, data e `ranking_reward` nullable. Vincitore nullo significa pareggio; altrimenti il `CHECK` lo limita a uno dei partecipanti. La diversità dei partecipanti è applicativa.
- `ranking_reward` non nullo è la variazione con segno applicata esclusivamente a `player1_id`, nella transazione del risultato. `player2_id` non riceve variazioni per questa battaglia; nelle amichevoli il valore è `NULL`. Le formule non sono fissate dallo schema.
- Gli snapshot preservano carte e arti usate dopo modifiche dei mazzi o del catalogo. Lo schema non contiene sfide attive, log dei round o un vincolo contro doppi avvii della stessa partita; contratto JSON e idempotenza restano da definire.
- Validazione e snapshot devono condividere un confine coerente con il [protocollo transazionale comune](Database/database-schema-v1.1.md#protocollo-transazionale-comune). Giocabilità delle copie prenotate e lock per tutta la partita restano aperti.

## Riferimenti alla proposta di regolamento

- Le sezioni 6–13 della Versione 2.1 propongono il comportamento del motore: [fazioni](../Digital/regolamento-base.md#6-fazioni), [equipaggiamenti](../Digital/regolamento-base.md#7-equipaggiamenti), [preparazione](../Digital/regolamento-base.md#8-preparazione-della-partita), [inizio del round](../Digital/regolamento-base.md#9-inizio-di-ogni-round), [combattimento](../Digital/regolamento-base.md#10-combattimento) e condizioni di [fine round](../Digital/regolamento-base.md#12-conclusione-del-round) e [fine partita](../Digital/regolamento-base.md#13-conclusione-della-partita).
- La [formula dei danni ai giocatori](../Digital/regolamento-base.md#11-danni-ai-giocatori) richiede ancora conferma con gli insegnanti. Gli altri punti aperti sono raccolti nella [sezione 15](../Digital/regolamento-base.md#15-elementi-da-completare-e-testare).
- Le regole restano nella pagina Digital. Questa pagina ospita il riferimento tecnico canonico: l'endpoint dimostrativo PvE per il SAL è descritto sotto, mentre il contratto completo F07 (sfide PvP, stati, autorizzazioni e persistenza) resta da consolidare.
- Il risultato destinato allo [storico](storico-partite.md) dovrà riferirsi al [resoconto proposto](../Digital/regolamento-base.md#14-resoconto-della-partita), dopo la conferma del relativo contratto.

## Endpoint dimostrativo PvE per il SAL

L'implementazione disponibile espone l'API interna `POST /api/battle/pve/{id}` per simulare uno stage di campagna/tutorial. Nella rete Docker Compose è raggiungibile da `springboot_app/app` all'indirizzo `http://game-engine:8083/api/battle/pve/{id}` ed è destinata esclusivamente alla chiamata server-to-server dal backend applicativo. Il flusso previsto è `frontend -> springboot_app/app -> game-engine`: il frontend dovrà rivolgersi al backend applicativo. L'orchestrazione PvE in `springboot_app/app` non è ancora implementata; non è definita né disponibile una rotta pubblica futura dell'applicazione per questa funzione. Il binding host `127.0.0.1:8083`, modificabile con `ENGINE_PORT`, è riservato al debug e alle prove locali da terminale o `curl`. È una dimostrazione per il SAL, non il contratto completo F07 ancora da consolidare: non sostituisce le sfide PvP, l'autenticazione JWT o la persistenza dei risultati su PostgreSQL. Il servizio usa H2 in memoria. CORS non è configurato e le richieste preflight sono rifiutate con `403`.

Gli identificativi risolti sono `1`, `stage_01` e `stage_01_fazioni` per lo stage 1, attivo per impostazione predefinita, e `3`, `stage_03` e `stage_03_evento_speciale` per lo stage 3, bloccato finché l'evento `BLOOD_MOON` non è attivo su H2.

### Richiesta

Il controller usa l'identificativo nel percorso e sovrascrive l'eventuale `stageId` nel body. Il body può contenere il partecipante dimostrativo e il mazzo sfidante:

| Posizione/campo | Tipo | Obbligo e significato |
| --- | --- | --- |
| Percorso `id` | stringa | Identificativo dello stage, nei formati elencati sopra. |
| `stageId` | stringa | Facoltativo nel DTO; il controller lo sostituisce con `id` del percorso. |
| `challenger` | oggetto | Facoltativo; metadati `userId`, `username` e `displayName`. Non rappresenta autenticazione. |
| `deck.entries` | array | Necessario e non vuoto; contiene le carte sfidanti. |
| `deck.entries[].cardId` | stringa | Necessario; deve identificare una carta esistente nei pool JSON `src/main/resources/mocks/pool_*.json`. |
| `deck.entries[].destination` | stringa | Necessario; `ROUND1`..`ROUND5` o `EQUIPMENT`. |
| `deck.entries[].artworkId` | intero | Facoltativo nel DTO; la mappatura demo corrente non lo usa. |

Il mazzo deve assegnare da 1 a 5 creature a ciascuno dei cinque round e esattamente 5 carte alla destinazione `EQUIPMENT`; il totale non può superare 30 carte. Il tipo di carta deve corrispondere alla destinazione. Gli ID dell'esempio sono presenti in `pool_sapiens.json` e `pool_equipments.json`.

Esempio completo e utilizzabile per `POST /api/battle/pve/1`:

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

### Risposta

Con `200 OK`, il body segue `PveBattleResponse`:

| Campo | Tipo | Significato |
| --- | --- | --- |
| `stageId` | stringa | ID canonico letto dallo stage, per esempio `stage_01`. |
| `stageNumber` | intero | Numero dello stage. |
| `stageTitle` | stringa | Titolo dello stage. |
| `challenger` | oggetto o `null` | Metadati del partecipante ricevuti nella request. |
| `enemyName` | stringa | Nome del nemico configurato per lo stage. |
| `outcome` | stringa | `VICTORY`, `DEFEAT` o `DRAW`. |
| `winner` | stringa | Nome del vincitore oppure `NONE` in caso di pareggio. |
| `tutorialHints` | array di stringhe | Suggerimenti dello stage. |
| `battleLog` | array di stringhe | Suggerimenti seguiti dal resoconto prodotto dal motore. |

Esempio di risposta per lo stage 1. `battleLog` è abbreviato qui; la risposta effettiva contiene tutto il resoconto della simulazione.

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

### Errori

Gli errori sono restituiti come `ProblemDetail` secondo RFC 7807. Gli status gestiti sono:

| HTTP | Caso | Estensioni specifiche |
| --- | --- | --- |
| `400 Bad Request` | JSON non leggibile o argomenti non validi | Nessuna |
| `404 Not Found` | Stage inesistente | `stageId`, se disponibile |
| `409 Conflict` | Evento richiesto assente o inattivo, come `BLOOD_MOON` | `eventCode`, se disponibile |
| `422 Unprocessable Entity` | Mazzo sfidante non valido | `violations` |

Il formato standard include `type`, `title`, `status` e `detail`; può includere `instance`. Le estensioni indicate dipendono dal caso di errore.

La suite `./mvnw test` è stata eseguita il **2026-11-19**: 25 test JUnit, 0 fallimenti e 0 errori, inclusi i 6 test aggiornati di `BattleControllerTest`. I test verificano anche la POST senza header `Origin` e il rifiuto delle richieste preflight senza abilitare CORS. La suite usa H2 e non verifica l'integrazione con PostgreSQL, l'autenticazione, l'orchestrazione in `springboot_app/app` o la persistenza prevista per F07. Questa evidenza aggiorna la precedente registrazione del 2026-11-18, che non rifletteva la rimozione di CORS e i test aggiornati.

## Da definire

- Conferma delle regole proposte, della formula del danno e dei casi limite; effetti degli equipaggiamenti e ordine di risoluzione restano da definire.
- Stati e persistenza della sfida, implementazione della rivalidazione prima dello snapshot, deduplicazione degli avvii e formato definitivo dei JSON.
- Contratto tecnico completo condiviso: sfide PvP, dati, autorizzazioni JWT, errori, persistenza PostgreSQL e criteri di accettazione del motore. La demo PvE sopra non determina tali decisioni.

## Collegamenti

- [Interfaccia sfide](../FrontEnd/sfide-simulazione.md)
- [Regolamento](../Digital/regolamento-base.md)
- [Mazzi](gestione-mazzi.md)
- [Storico](storico-partite.md)
- [Indice BackEnd](README.md)
