# Guida FrontEnd: simulazione PvE

Questa guida descrive la chiamata dal browser al servizio `game-engine` e il
formato necessario per avviare una battaglia PvE.

## Endpoint e rete

Chiamare direttamente:

```text
POST http://localhost:8083/api/battle/pve/1
```

Il servizio `game-engine` risponde sulla porta `8083`. **Non anteporre un proxy
relativo come `/api` sulle porte `8081` o `8082`**: quelle porte non sono
l'endpoint di questa chiamata. Il CORS del servizio è già abilitato per le
origini `http://localhost:8082` e `http://localhost:5173`.

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
  "stageId": "1",
  "stageNumber": 1,
  "stageTitle": "Stage 1",
  "challenger": {
    "userId": "frontend-demo",
    "username": "frontend-demo",
    "displayName": "Giocatore demo"
  },
  "enemyName": "Avversario",
  "outcome": "VICTORY",
  "winner": "frontend-demo",
  "tutorialHints": [
    "Suggerimento restituito dal server"
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

## Snippet React / `fetch`

La funzione accetta l'ID dello stage, l'oggetto `challenger` e l'array di
`entries`. In caso di errore HTTP estrae `violations` quando disponibili e le
espone insieme allo status, senza nascondere la risposta del server.

```js
async function startPveBattle(stageId, challenger, entries) {
  const response = await fetch(
    `http://localhost:8083/api/battle/pve/${encodeURIComponent(stageId)}`,
    {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        Accept: "application/json"
      },
      body: JSON.stringify({
        stageId: String(stageId),
        challenger,
        deck: { entries }
      })
    }
  );

  const result = await response.json().catch(() => null);

  if (!response.ok) {
    const violations = Array.isArray(result?.violations)
      ? result.violations
      : [];
    const message = result?.detail || `Errore HTTP ${response.status}`;
    const error = new Error(message);
    error.status = response.status;
    error.violations = violations;
    error.problem = result;
    throw error;
  }

  return result;
}
```

Esempio d'uso:

```js
try {
  const battle = await startPveBattle("1", challenger, entries);
  setBattle(battle);
} catch (error) {
  if (error.status === 422) {
    setErrors(error.violations);
  } else {
    setError(error.message);
  }
}
```
