# API delle battaglie PvE per il FrontEnd

ID funzionalità: F07. Ultimo aggiornamento significativo: **2026-11-23**, allineamento dello stato ai sorgenti su richiesta dell'utente; contratto HTTP invariato.
Stato: endpoint applicativo e chiamata React implementati nei sorgenti correnti. L'API interna del motore deriva dalla PR #36, integrata con `786707a`; include ID carta numerici, snapshot completi e deadline cooperativa, come descritto in [App battaglie](app-battaglie.md#api-interna-del-motore-implementata).

Questo file è il riferimento canonico di endpoint, header e payload pubblici usati dal FrontEnd. [App battaglie](app-battaglie.md) contiene il comportamento server, il contratto interno del motore e le decisioni infrastrutturali. Il [client React](../FrontEnd/sfide-simulazione.md) applica la specifica; l'integrazione browser non è verificata in questa revisione.

## Endpoint e header

| Metodo | Percorso applicativo | Autenticazione | Successo | Timeout |
| --- | --- | --- | --- | --- |
| `POST` | `/api/battle/pve/{stageId}` | Bearer JWT F01 | `200 OK`, `application/json` | 10 secondi complessivi HTTP |

Il browser chiama il backend applicativo attraverso la base API configurata o il proxy `/api/`. Non chiama `http://localhost:8083` né `http://game-engine:8083`: quest'ultimo è l'indirizzo interno usato dal backend. La forma singolare `battle` mantiene la continuità con il percorso del motore discusso; è un'eccezione specifica rispetto alla convenzione generale delle risorse plurali.

```http
POST /api/battle/pve/stage_01 HTTP/1.1
Authorization: Bearer <access-token>
Content-Type: application/json
Accept: application/json, application/problem+json

{
  "deckId": 17
}
```

Il Bearer è un access token ottenuto dal flusso F01, non un refresh token. Identità e sessione sono validate dal backend. Questa rotta non introduce autenticazione tramite cookie né richiede l'header CSRF delle scritture F01; vedere il [contratto F01](accesso-utenti.md#guida-autenticazione-header-e-openapi).

## Payload della richiesta

| Posizione/campo | Tipo | Obbligatorietà e vincolo |
| --- | --- | --- |
| Percorso `stageId` | stringa | Obbligatorio; identificatore dello stage. Il frontend usa la forma canonica, per esempio `stage_01`. |
| Body `deckId` | intero JSON | Obbligatorio, positivo e rappresentabile come `int` Java, da 1 a 2147483647; deve identificare un mazzo del chiamante. |

```json
{
  "deckId": 17
}
```

Il body è un oggetto JSON con il solo campo `deckId`: body assente/null, campo mancante/null, stringhe numeriche, valori frazionari, valori fuori intervallo e campi aggiuntivi producono `400`. In particolare non inviare `userId`, `challenger`, `stageId` nel body, carte, statistiche o dati del profilo. L'identità viene ricavata esclusivamente dal token validato; nessun campo può selezionare un altro utente.

L'applicazione carica mazzo, copie e metadati dal server. Un mazzo inesistente e uno altrui restituiscono entrambi `404`, senza distinguerli. La giocabilità è rivalidata secondo F05; `is_challengeable` non è richiesto per PvE.

Nella revisione interna consultata esistono `stage_01` e `stage_03`; il secondo richiede l'evento `BLOOD_MOON`. Sono esempi della configurazione dimostrativa, non un elenco pubblico definitivo. Gli alias numerici o basati su filename accettati dal motore non costituiscono una promessa del contratto frontend. L'API di scoperta degli stage è **DA DEFINIRE**.

## Payload di risposta positiva

`200 OK` indica che la simulazione è conclusa. Vittoria, sconfitta e pareggio hanno tutti questo stato HTTP.

| Campo | Tipo | Significato |
| --- | --- | --- |
| `stageId` | stringa | Identificatore canonico restituito dal motore. |
| `stageNumber` | intero | Numero dello stage. |
| `stageTitle` | stringa | Titolo restituito dal motore. |
| `challenger` | oggetto | Metadati costruiti dal backend dal profilo autenticato. |
| `challenger.userId` | stringa | ID utente database in rappresentazione decimale, coerente con il DTO della PR. |
| `challenger.username` | stringa | Nickname del profilo server utilizzato nell'adattatore; non è fornito dal browser. |
| `challenger.displayName` | stringa | Nickname di visualizzazione dal profilo server. |
| `enemyName` | stringa | Nome dell'avversario PvE. |
| `outcome` | stringa enumerata | `VICTORY`, `DEFEAT` o `DRAW`, dal punto di vista dello sfidante. |
| `winner` | stringa | Nome di visualizzazione del vincitore; `"NONE"` in caso di pareggio. |
| `battleLog` | array di stringhe | Resoconto testuale completo del combattimento, nell'ordine ricevuto e senza messaggi tutorial. |

Tutti i campi elencati sono presenti nella risposta pubblica positiva. Il backend popola i metadati del partecipante prima della chiamata interna. Non aggiungere al client un account utente fittizio: l'ID di esempio non è una credenziale o una fixture garantita.

Esempio illustrativo, con **log abbreviato** per mostrare la forma; la risposta reale contiene tutte le righe di combattimento:

```json
{
  "stageId": "stage_01",
  "stageNumber": 1,
  "stageTitle": "Tutorial: Il Vantaggio di Fazione",
  "challenger": {
    "userId": "42",
    "username": "Player42",
    "displayName": "Player42"
  },
  "enemyName": "Recluta Cultista",
  "outcome": "VICTORY",
  "winner": "Player42",
  "battleLog": [
    "RISULTATO FINALE: Player42 3 - 1 Recluta Cultista",
    "VINCITORE PARTITA: Player42"
  ]
}
```

Per una sconfitta `outcome` è `DEFEAT` e `winner` contiene il nome dell'avversario. Per un pareggio `outcome` è `DRAW` e `winner` è esattamente `"NONE"`. Non dedurre l'esito dal nome o dal testo del log: nomi uguali non identificano il vincitore.

Il campo `tutorialHints` è **assente**, non restituito come array vuoto o null. Anche i messaggi tutorial anteposti dal motore sono rimossi dal log dal backend; il frontend non deve filtrarli. Il titolo dello stage resta un metadato invariato. Le altre stringhe conservano ordine, spazi e ritorni a capo. Non sono presenti `battleResult`, round annidati, `battleId`, premi o avanzamento della campagna.

## Codici HTTP e payload di errore

Gli errori applicativi con body usano `Content-Type: application/problem+json`. Il body segue Problem Details; `status`, quando presente, coincide con lo stato HTTP. Il client non deve assumere che ogni errore di rete, proxy o autenticazione abbia un JSON utilizzabile: in assenza del body usa lo stato HTTP.

| HTTP | Significato | Proprietà specifiche previste |
| --- | --- | --- |
| `200` | Simulazione completata. | Payload positivo definito sopra. |
| `400` | JSON, struttura o campi della richiesta pubblica non validi. | `detail`. |
| `401` | Bearer assente/non valido/scaduto, account o sessione non più attivi. | Body non garantito; gestire tramite F01. |
| `404` | Mazzo non disponibile al chiamante oppure stage inesistente. | `stageId` nel caso stage; mai indicare che il mazzo appartiene ad altri. |
| `409` | Battaglia già attiva per l'utente oppure evento dello stage inattivo. | `code: "BATTLE_IN_PROGRESS"` per il lock; `eventCode` per l'evento. |
| `415` | Content type non supportato. | `detail`. |
| `422` | Il mazzo selezionato non supera le regole di giocabilità. | `violations`, array di messaggi testuali. |
| `500` | Errore applicativo inatteso. | Messaggio generico, senza stack trace o segreti. |
| `502` | Risposta del motore non conforme o difetto del contratto/allineamento interno. | Messaggio generico di integrazione. |
| `503` | Motore non disponibile o non raggiungibile. | Messaggio di indisponibilità. |
| `504` | Deadline della chiamata al motore esaurita nel budget complessivo. | Messaggio di timeout. |

Un errore interno relativo a snapshot incompleti o malformati non viene inoltrato automaticamente come colpa del mazzo dell'utente. I codici del motore non sostituiscono i controlli del backend. Il precedente caso `USER_MISMATCH` non esiste: il body non contiene `userId`.

### Richiesta concorrente

```json
{
  "type": "about:blank",
  "title": "Conflict",
  "status": 409,
  "detail": "A battle request is already in progress for this user.",
  "instance": "/api/battle/pve/stage_01",
  "code": "BATTLE_IN_PROGRESS"
}
```

### Evento richiesto non attivo

```json
{
  "type": "about:blank",
  "title": "Inactive Stage Event",
  "status": 409,
  "detail": "The special event required for this stage is inactive.",
  "instance": "/api/battle/pve/stage_03",
  "eventCode": "BLOOD_MOON"
}
```

### Mazzo non giocabile

```json
{
  "type": "about:blank",
  "title": "Invalid Deck",
  "status": 422,
  "detail": "The selected deck does not meet the battle requirements.",
  "instance": "/api/battle/pve/stage_01",
  "violations": [
    "Expected 5 creatures in ROUND1 but found 4."
  ]
}
```

### Mazzo non disponibile

```json
{
  "type": "about:blank",
  "title": "Not Found",
  "status": 404,
  "detail": "Deck not found.",
  "instance": "/api/battle/pve/stage_01"
}
```

I messaggi degli esempi sono illustrativi e non vanno confrontati come identificatori. Distinguere i due casi `409` tramite `code` o `eventCode`. Non è definita una classificazione generale aggiuntiva dei codici applicativi.

## Semantica temporale e degli invii

Il timeout HTTP concordato è 10 secondi. Un secondo invio mentre il guard dell'utente è attivo restituisce `409`, anche se arriva da un'altra scheda o sessione o usa un mazzo/stage differente; non viene accodato. A conclusione e rilascio del guard, un nuovo invio è una nuova battaglia.

L'utente ha selezionato **L2 + T2** il **2026-11-20**: advisory lock PostgreSQL e deadline cooperativa del motore, come descritto nelle [opzioni applicative](app-battaglie.md#opzioni-dopo-timeout-http). Il guard viene rilasciato al termine HTTP anche se l'arresto remoto non è confermato; dopo un guasto può quindi esserci sovrapposizione remota. Al crash applicativo il lock termina quando PostgreSQL rileva la chiusura della sessione. Nessuna risposta di timeout promette che il calcolo remoto sia già fermo.

Il budget approvato è cumulativo: deadline motore a **8 secondi**, obiettivo di risposta backend entro **9 secondi**, abort frontend a **10 secondi**. Preparazione e attese consumano lo stesso budget; i limiti operativi sono descritti nella specifica applicativa. Il wrapper frontend rappresenta gli errori di trasporto con stato interno `0`: non è un codice HTTP né una risposta del backend. Non ripetere automaticamente il POST dopo un esito incerto. Il contratto non definisce polling, recupero del risultato, cancellazione pubblica o un header `Retry-After`.

## Collegamenti e stato

- [Specifica applicativa, decisioni e alternative](app-battaglie.md).
- [F07 — Sfide e simulazione](sfide-simulazione.md) · [Riepilogo API](GUIDA-API-FRONTEND.md) · [Indice BackEnd](README.md).
- [F01 — Autenticazione](accesso-utenti.md) · [F05 — Mazzi](gestione-mazzi.md) · [Interfaccia F07](../FrontEnd/sfide-simulazione.md).

Questo documento definisce esclusivamente il contratto HTTP PvE. Il contratto applicativo PvP Ranked/Sfide Amichevoli, la persistenza V12 e il confine di sicurezza tra applicazione e motore sono descritti in [F07 — Sfide e simulazione](sfide-simulazione.md); lo storico PvP in [F08](storico-partite.md). Per strategia di lock ed evidenze PvE consultare [App battaglie](app-battaglie.md). Persistenza PvE, ricompense, progressione della campagna e scoperta degli stage restano **DA DEFINIRE**.
