# Guida FrontEnd: simulazione PvE

Questa guida descrive il payload e la risposta dell'API interna PvE di
`game_engine`, come riferimento per l'integrazione applicativa.

## Flusso e rete

L'endpoint interno è `POST /api/battle/pve/{id}` ed è raggiungibile nella rete
Docker Compose all'indirizzo:

```text
http://game-engine:8083/api/battle/pve/{id}
```

È destinato esclusivamente alla chiamata server-to-server da
`springboot_app/app`. Nel flusso previsto
`frontend -> springboot_app/app -> game-engine`, il frontend deve rivolgersi al
backend applicativo, non direttamente a `game_engine`. L'orchestrazione PvE in
`springboot_app/app` non è ancora implementata; non è definita né disponibile
una rotta pubblica dell'applicazione per questo flusso.

Compose mantiene il binding host `127.0.0.1:8083` (modificabile con
`ENGINE_PORT`) esclusivamente per debug e prove locali da terminale o `curl`.
Non usarlo come endpoint del browser.

- `stageId` `1`: stage attivo subito, utilizzabile per provare la chiamata.
- `stageId` `3`: stage legato all'evento `BLOOD_MOON`; se l'evento non è attivo,
  la risposta è `409 Conflict`.

L'identificativo è nel percorso dell'URL. Il controller assegna questo valore
alla richiesta anche se nel JSON è presente un altro `stageId`.

## Regole tassative sul payload

Il corpo ha due oggetti principali, `challenger` e `deck`:

- `challenger`: informazioni identificative del giocatore. `userId`,
  `username` e `displayName` possono identificare il giocatore; il backend
  seleziona il primo valore non vuoto in quest'ordine. Se nessuno è valorizzato,
  usa l'identificativo generico `challenger`.
- `deck.entries`: elenco delle carte assegnate a round o equipaggiamento.
- `cardId`: ID della carta nei pool caricati dal backend. È il dato usato per
  trovare la carta: deve corrispondere esattamente a un ID reale.
- `artworkId`: metadato visivo per il FrontEnd. Il mapper backend consultato non
  lo usa per validare o ricavare le carte; non sostituirlo a `cardId`. Nel payload
  d'esempio è valorizzato con lo stesso ID della carta.
- `destination`: destinazione della carta. Per una creatura usare uno tra
  `ROUND1`, `ROUND2`, `ROUND3`, `ROUND4`, `ROUND5`; per un equipaggiamento usare
  `EQUIPMENT`. I valori sono confrontati senza distinzione tra maiuscole e
  minuscole, ma inviarli sempre nel formato maiuscolo indicato.

La validazione rifiuta il mazzo con `422 Unprocessable Entity` se non rispetta
tutti questi vincoli:

1. Ogni round, da `ROUND1` a `ROUND5`, deve contenere **almeno una creatura** e
   può contenerne **al massimo cinque**.
2. `EQUIPMENT` deve contenere **esattamente cinque carte**.
3. Il mazzo può avere **al massimo 30 carte totali**.
4. Le carte devono essere assegnate a destinazioni del tipo corretto: le
   creature ai round e gli equipaggiamenti a `EQUIPMENT`.

Usare esclusivamente questi `cardId` reali dei pool indicati. Gli ID fittizi
usati dai vecchi mock FrontEnd non sono riconosciuti e producono violazioni.

- `pool_sapiens.json` (creature): `SAP_R1_1`, `SAP_R1_2`, `SAP_R1_3`,
  `SAP_R1_4`, `SAP_R1_5`, `SAP_R1_6`, `SAP_R1_7`, `SAP_R1_8`, `SAP_R1_9`,
  `SAP_R1_10`, `SAP_R2_1`, `SAP_R2_2`, `SAP_R2_3`, `SAP_R2_4`, `SAP_R2_5`,
  `SAP_R2_6`, `SAP_R2_7`, `SAP_R2_8`, `SAP_R2_9`, `SAP_R2_10`, `SAP_R3_1`,
  `SAP_R3_2`, `SAP_R3_3`, `SAP_R3_4`, `SAP_R3_5`, `SAP_R3_6`, `SAP_R3_7`,
  `SAP_R3_8`, `SAP_R3_9`, `SAP_R3_10`, `SAP_R4_1`, `SAP_R4_2`, `SAP_R4_3`,
  `SAP_R4_4`, `SAP_R4_5`, `SAP_R4_6`, `SAP_R4_7`, `SAP_R4_8`, `SAP_R4_9`,
  `SAP_R4_10`, `SAP_R5_1`, `SAP_R5_2`, `SAP_R5_3`, `SAP_R5_4`, `SAP_R5_5`,
  `SAP_R5_6`, `SAP_R5_7`, `SAP_R5_8`, `SAP_R5_9`, `SAP_R5_10`.
- `pool_equipments.json` (equipaggiamenti): `EQ_1`, `EQ_2`, `EQ_3`, `EQ_4`,
  `EQ_5`, `EQ_6`, `EQ_7`, `EQ_8`, `EQ_9`, `EQ_10`.

## Payload JSON minimo pronto all'uso

Il seguente JSON contiene una creatura per round e cinque equipaggiamenti
(dieci carte totali). È pronto per la chiamata allo stage `1`.

```json
{
  "stageId": "1",
  "challenger": {
    "userId": "frontend-demo",
    "username": "frontend-demo",
    "displayName": "Giocatore demo"
  },
  "deck": {
    "entries": [
      {
        "cardId": "SAP_R1_1",
        "artworkId": "SAP_R1_1",
        "destination": "ROUND1"
      },
      {
        "cardId": "SAP_R2_1",
        "artworkId": "SAP_R2_1",
        "destination": "ROUND2"
      },
      {
        "cardId": "SAP_R3_1",
        "artworkId": "SAP_R3_1",
        "destination": "ROUND3"
      },
      {
        "cardId": "SAP_R4_1",
        "artworkId": "SAP_R4_1",
        "destination": "ROUND4"
      },
      {
        "cardId": "SAP_R5_1",
        "artworkId": "SAP_R5_1",
        "destination": "ROUND5"
      },
      {
        "cardId": "EQ_1",
        "artworkId": "EQ_1",
        "destination": "EQUIPMENT"
      },
      {
        "cardId": "EQ_2",
        "artworkId": "EQ_2",
        "destination": "EQUIPMENT"
      },
      {
        "cardId": "EQ_3",
        "artworkId": "EQ_3",
        "destination": "EQUIPMENT"
      },
      {
        "cardId": "EQ_4",
        "artworkId": "EQ_4",
        "destination": "EQUIPMENT"
      },
      {
        "cardId": "EQ_5",
        "artworkId": "EQ_5",
        "destination": "EQUIPMENT"
      }
    ]
  }
}
```

## Risposta `200 OK` e gestione degli errori

La risposta `200 OK` contiene `stageId`, `stageNumber`, `stageTitle`,
`challenger`, `enemyName`, `outcome`, `winner`, `tutorialHints` e `battleLog`.
Esempio completo della struttura (i testi e l'esito sono illustrativi: mostrare
i valori restituiti dal server):

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
    "[TUTORIAL] Suggerimento restituito dal server"
  ],
  "battleLog": [
    "Evento di battaglia restituito dal server"
  ]
}
```

- Visualizzare `outcome` come risultato: `VICTORY` (vittoria), `DEFEAT`
  (sconfitta) o `DRAW` (pareggio). Gestire questi valori esplicitamente.
- Mostrare `winner` come vincitore usando il valore restituito; non dedurre
  l'esito dal nome del vincitore.
- Presentare ogni elemento di `tutorialHints` come suggerimento. L'array può
  essere vuoto.
- Mostrare gli elementi di `battleLog` nell'ordine ricevuto; l'array rappresenta
  il resoconto della battaglia.

Gli errori sono `ProblemDetail` RFC 7807. La risposta include normalmente
`type`, `title`, `status` e `detail`; può inoltre contenere le proprietà
specifiche riportate sotto.

| HTTP | Titolo tipico | Significato e gestione |
| --- | --- | --- |
| `400` | `Bad Request` | Corpo JSON malformato o argomento non valido. Correggere il payload prima di ritentare. |
| `404` | `Stage Not Found` | Stage inesistente. La risposta può includere `stageId`. |
| `409` | `Inactive Stage Event` | Evento necessario allo stage non attivo; per lo stage `3`, verificare l'attivazione di `BLOOD_MOON`. La risposta può includere `eventCode`. |
| `422` | `Invalid Deck` | Mazzo non valido. Leggere `violations`, un array di messaggi testuali: mostrarli all'utente e correggere ciascun problema. |

## Stato dell'integrazione e verifica

La guida descrive il contratto interno del motore e non un'integrazione
disponibile al browser. Il frontend dovrà passare dal backend applicativo;
fino all'implementazione dell'orchestrazione in `springboot_app/app`, non è
disponibile un flusso PvE completo lato applicazione. Per i dettagli canonici
del contratto interno vedere [F07 — Sfide e simulazione](sfide-simulazione.md#endpoint-dimostrativo-pve-per-il-sal).

La verifica della suite del motore eseguita il **2026-11-19** ha completato
25 test JUnit con 0 fallimenti e 0 errori, inclusi i 6 test aggiornati di
`BattleControllerTest`: la chiamata POST senza header `Origin` riesce, mentre
le richieste preflight vengono rifiutate e non è configurato CORS. Questa
verifica usa H2 e non verifica l'orchestrazione applicativa né l'integrazione
del frontend, che restano da implementare.
