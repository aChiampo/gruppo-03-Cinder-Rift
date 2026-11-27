# Sfide tra utenti

ID: F07 · Perimetro: confermato; flusso PvE client implementato, client PvP da realizzare.
Implementazione: React usa il backend PvE e conserva l'ultimo report in memoria; il backend espone coda Ranked e sfide Amichevoli, ma il frontend non ha ancora chiamate o schermate PvP. Responsabile: DA DEFINIRE (Web Developer).
Ultimo aggiornamento significativo: **2026-11-27**, flussi client documentati in base al contratto backend corrente.

## Integrazione PvE implementata

Per endpoint, header, payload, codici di errore e vincoli temporali usare esclusivamente [API delle battaglie PvE](../BackEnd/app-battaglie-api.md). `BattleScreen` e `battleService` chiamano l'endpoint applicativo con la sessione F01; questa pagina descrive soltanto il client. Orchestrazione, validazione, pulizia del resoconto, lock e recupero sono definiti in [App battaglie](../BackEnd/app-battaglie.md).

La schermata carica i mazzi dell'utente, richiede lo stage, mostra l'attesa e impedisce un nuovo avvio dalla stessa schermata. Al successo apre `BattleResultScreen`; il report è navigabile e riproducibile localmente, ma si perde al ricaricamento o all'uscita. Questo risultato in memoria non è lo storico F08.

## Flusso client PvP da implementare

Le API richiedono Bearer JWT F01 e derivano l'utente dal token. Il browser chiama l'applicazione, mai direttamente `game_engine`. Le sequenze seguenti descrivono il contratto disponibile; non attestano componenti React esistenti. Per payload e codici autorevoli vedere [contratto BackEnd F07](../BackEnd/sfide-simulazione.md).

### Coda Ranked

1. Selezionare un mazzo e inviare `POST /api/pvp/queue` con `{"deckId": 17}`.
2. Se la risposta è `202`, il body contiene `ticketId`, `status: "WAITING"` e nessun `battle`. Conservare l'ID e interrogare `GET /api/pvp/queue/{ticketId}` per il polling.
3. `GET` restituisce `200` con `WAITING`, `MATCHED` o `CANCELLED`. Quando `MATCHED`, il campo `battle` contiene il report persistito; presentarlo all'utente senza ricostruire il risultato dal solo stato.
4. Se `POST` trova e completa subito un match, restituisce direttamente `200` con `status: "MATCHED"` e `battle`, senza necessità di polling.
5. Per interrompere l'attesa inviare `DELETE /api/pvp/queue/{ticketId}`. Se ancora `WAITING`, la risposta `200` ha stato `CANCELLED`; se il ticket è già stato preso o abbinato, gestire `409` e leggere lo stato con `GET`.

In caso di errore del motore, la richiesta che ha avviato il match fallisce: il ticket del chiamante passa a `CANCELLED` e quello dell'avversario torna `WAITING`. Il client deve mostrare l'errore e consentire una nuova iscrizione; il backend non definisce retry automatici o un intervallo di polling.

### Sfide Amichevoli

1. Scegliere un amico con relazione accettata (F13 lo espone nell'elenco `friends`) e inviare `POST /api/pvp/challenges` con `{"opponentUserId": 24, "deckId": 17}`. `201` restituisce la sfida `PENDING`.
2. Usare `GET /api/pvp/challenges` per ricevere `{ "incoming": [...], "outgoing": [...] }`; gli elenchi contengono le sfide pendenti in entrata e in uscita.
3. Interrogare `GET /api/pvp/challenges/{challengeId}` per il polling. Entrambi i partecipanti possono leggere la sfida; dopo l'accettazione il body include anche `battle`.
4. L'opponent sceglie il proprio mazzo e invia `POST /api/pvp/challenges/{challengeId}/accept` con `{"deckId": 31}`. Il `200` restituisce lo stato `COMPLETED` e il report `FRIENDLY`.
5. L'opponent può rifiutare inviando `POST /api/pvp/challenges/{challengeId}/decline` senza body: risposta `200`, stato `DECLINED`. Il challenger usa la stessa rotta per annullare la propria sfida: risposta `200`, stato `CANCELLED`.

Auto-sfida produce `400`; un utente non amico o non partecipante non può accedere alle operazioni protette (`403`); ID inesistente produce `404`; transizioni non più valide producono `409`; mazzi non giocabili producono `422`. Una chiamata al motore fallita durante l'accettazione lascia la sfida `PENDING`, senza report, e il client può mostrare l'errore senza simulare un esito.

Il polling è HTTP; il backend non dichiara un intervallo o una strategia di backoff da imporre al client. Non ci sono WebSocket o SSE. I payload non comprendono snapshot inviati dal browser: il server valida e fotografa i mazzi selezionati.

## Report e storico

Il report PvP è contenuto in `battle` ed è una voce F08 con `mode`, `playerAId`, `playerBId`, `winnerId`, `outcome`, `deckASnapshot`, `deckBSnapshot`, `battleLog`, `rankingDelta` e `createdAt` (oltre all'ID). Ranked e Friendly persistono; `rankingDelta` è fisso a `0`, senza Elo, classifica o ricompense.

Il client F08 resta da implementare. L'API `GET /api/battles/history` restituisce un array per l'utente autenticato, con filtro facoltativo `mode` e senza paginazione; il contratto canonico è in [Storico F08](../BackEnd/storico-partite.md). Le simulazioni PvE non vengono salvate in questo storico.

## Risultato PvE della sessione

`parseBattleReport` interpreta i marcatori riconosciuti del log corrente per mostrare round, creature, equipaggiamenti, statistiche e scambi simultanei; non deduce il vincitore dai nomi. Dati mancanti, ambigui o non riconosciuti restano indisponibili e il log originale è sempre consultabile. La navigazione del risultato e il replay sono locali e non effettuano nuove simulazioni.

## Collegamenti

- [Servizio e contratto PvP F07](../BackEnd/sfide-simulazione.md)
- [Storico F08](storico-partite.md)
- [API PvE](../BackEnd/app-battaglie-api.md)
- [Amicizie F13](amicizie.md)
- [Indice FrontEnd](README.md)
