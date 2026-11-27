# Sfide e simulazione

ID: F07 · Perimetro: confermato; specifica in approfondimento.
Implementazione: PvE applicativo e client React disponibili; API backend Ranked e Sfide Amichevoli PvP disponibili; interfaccia React PvP non implementata. Responsabile: DA DEFINIRE (Software Developer).
Ultimo aggiornamento significativo: **2026-11-27**, documentazione allineata ai sorgenti dei quattro step PvP.
Il regolamento Versione 2.1 resta una proposta per il prototipo e il bilanciamento; le regole definitive devono essere confermate.

## Architettura e responsabilità

Il flusso PvP separa calcolo e orchestrazione:

1. `game_engine` espone `POST /api/battle/pvp`: riceve i due partecipanti e gli snapshot dei mazzi, esegue il calcolo e restituisce l'esito e il log. È stateless e non accede al database applicativo, non autentica gli utenti e non persiste la partita.
2. `springboot_app/app` autentica l'utente, ricava la sua identità dal Bearer JWT, verifica amicizia e proprietà del mazzo, valida e fotografa i mazzi tramite `BattleSnapshotService`, orchestra la coda o la sfida, protegge le operazioni concorrenti con `PostgresBattleGuard` e registra il risultato in PostgreSQL.
3. La chiamata HTTP al motore avviene fuori dalla transazione DB. Il backend salva storia e stato finale con le proprie transazioni dopo aver ricevuto il report valido.

`BattleEngineClient.executePvp` invia al motore `Content-Type`, `Accept` e `X-Battle-Deadline`; non inoltra il JWT dell'utente e non invia un token di servizio. Il controller PvP del motore non richiede autenticazione. Compose colloca applicazione e motore sulla rete interna, ma pubblica anche la porta del motore su loopback per debug: la rete non equivale a un'autenticazione del servizio. **Un token interno tra app e motore non è implementato**; va trattato come limite di sicurezza, non come requisito già soddisfatto. Il motore usa `X-Battle-Deadline` per la deadline cooperativa (8 secondi nel flusso applicativo).

Gli endpoint `/api/pvp/**` e `/api/battles/history` richiedono un Bearer JWT valido e i ruoli `USER` o `ADMIN`. Il backend autorizza l'accesso alle proprie risorse; non presume una gerarchia tra i ruoli. Non sono implementati WebSocket o SSE.

## Endpoint PvP stateless del motore

`POST /api/battle/pvp` accetta una richiesta interna con i quattro oggetti non null `playerA`, `deckA`, `playerB`, `deckB` e il campo stringa `mode`. `playerA` e `playerB` hanno i campi stringa `userId`, `username` e `displayName`; `deckA` e `deckB` contengono `entries`, un array di carte dello snapshot. Ogni voce ha i campi `cardId`, `artworkId`, `destination`, `name`, `faction`, `energyCost`, `powerPoints`, `baseAtk`, `initialDef` ed `effects`; gli elementi `effects` hanno `effectId`, `code` e `parameters`. Gli ID e i valori numerici sono forniti come interi, i campi stringa come stringhe e `parameters` come JSON. Il motore usa i dati ricevuti e non effettua lookup nel database. `mode` ammette `RANKED` o `FRIENDLY` (normalizzato con trim e maiuscole); se assente o nullo il default è `RANKED`. Gli ID dei due giocatori non possono coincidere quando sono presenti.

In caso di successo il motore restituisce `200` e `mode`, `outcome` (`PLAYER_A_WIN`, `PLAYER_B_WIN` o `DRAW`), `winnerUserId`, `winnerUsername` e `battleLog`. Il controller non applica autenticazione Spring Security. Gli errori verificati dal contratto del motore comprendono `400` per richiesta/deadline non valide, `422` per dati di battaglia non validi e `504` per deadline esaurita; l'applicazione tratta una risposta non valida come errore d'integrazione.

## Coda Ranked

Il polling REST è il meccanismo di consultazione: l'intervallo tra le richieste non è fissato dal backend.

| Metodo e percorso | Richiesta | Esito |
| --- | --- | --- |
| `POST /api/pvp/queue` | `{"deckId": 17}` | `202` e ticket `WAITING` se accodato; `200` e ticket `MATCHED` con report se il match è completato nella stessa richiesta. |
| `GET /api/pvp/queue/{ticketId}` | Nessun body | `200` con stato del ticket; solo il proprietario può leggerlo. |
| `DELETE /api/pvp/queue/{ticketId}` | Nessun body | `200` con stato `CANCELLED`, se il ticket è ancora `WAITING`. |

Il body di risposta è `{ "ticketId", "status", "battle" }`; `battle` è omesso quando non esiste ancora un report. Gli stati esposti sono `WAITING`, `MATCHED` e `CANCELLED`. Lo stato persistito `MATCHING` è restituito come `WAITING`.

I body pubblici sono oggetti JSON rigorosi: la coda accetta solo `deckId`, la creazione sfida solo `opponentUserId` e `deckId`, l'accettazione solo `deckId`. Gli ID devono essere interi JSON positivi nel range `int`; campi mancanti, extra, nulli, stringhe o valori non integrali producono `400`.

La selezione del ticket avversario usa una transazione breve e `FOR UPDATE ... SKIP LOCKED`; un indice univoco parziale ammette un solo ticket attivo (`WAITING` o `MATCHING`) per utente. Validazione e snapshot usano `BattleSnapshotService`; il motore viene invocato fuori dalla transazione. Una risposta valida crea lo storico `RANKED` con `rankingDelta = 0` e porta i ticket a `MATCHED`. Se l'esecuzione fallisce, il ticket dell'avversario torna `WAITING` e quello del chiamante diventa `CANCELLED`.

Codici rilevanti: `400` per body non valido, `401` per autenticazione assente/non valida, `403` per ticket non proprio, `404` per ticket non trovato, `409` per utente già in coda, battaglia in corso o cancellazione di ticket non più `WAITING`, `422` per mazzo non giocabile; gli errori d'integrazione/indisponibilità/timeout del motore sono restituiti dal backend come errori di servizio. Un ticket `MATCHED` non è annullabile.

## Sfide Amichevoli dirette

La creazione richiede che challenger e avversario abbiano un'amicizia accettata: nel database F13 il valore è `accepted` e il servizio espone l'elenco `friends`. La verifica usa `FriendshipService`/`FriendshipRepository`. L'auto-sfida è rifiutata. Gli snapshot dei mazzi sono prodotti dal backend e non accettati dal client.

| Metodo e percorso | Richiesta | Esito |
| --- | --- | --- |
| `POST /api/pvp/challenges` | `{"opponentUserId": 24, "deckId": 17}` | `201` con sfida `PENDING`; il target deve essere un amico con relazione accettata. |
| `GET /api/pvp/challenges` | Nessun body | `200` con `incoming` e `outgoing`, contenenti le sfide `PENDING` dell'utente. |
| `GET /api/pvp/challenges/{challengeId}` | Nessun body | `200` per uno dei due partecipanti; consente il polling e include il report quando completata. |
| `POST /api/pvp/challenges/{challengeId}/accept` | `{"deckId": 31}` | `200` con sfida `COMPLETED` e report `FRIENDLY`; solo l'opponent può accettare. |
| `POST /api/pvp/challenges/{challengeId}/decline` | Nessun body | `200`; opponent → `DECLINED`, challenger → `CANCELLED`. |

La risposta della sfida contiene `id`, `challengerId`, `challengerNickname`, `opponentId`, `opponentNickname`, `challengerDeckId`, `opponentDeckId`, `status`, `battleHistoryId`, `createdAt`, `respondedAt` e, quando disponibile, `battle`. Le proprietà nullable assenti sono omesse. La lista è `{ "incoming": [...], "outgoing": [...] }`.

Codici rilevanti: creazione `400` per payload non valido o auto-sfida, `403` se non c'è un'amicizia accettata, `404` per mazzo non disponibile e `422` per mazzo non giocabile. Le letture restituiscono `404` per ID inesistente e `403` a un non partecipante. L'accettazione restituisce `403` se chiamata da un utente diverso dall'opponent, `404` per sfida/mazzo non trovato, `409` se non è più `PENDING`, il challenger non è attivo o c'è un conflitto di battaglia, `422` per mazzo non giocabile. Il rifiuto/annullamento è limitato ai partecipanti e allo stato `PENDING` (`403`, `404` o `409` negli altri casi).

`PostgresBattleGuard` e il locking/aggiornamento condizionale del repository impediscono doppie accettazioni. Il mazzo dell'opponent viene validato e fotografato; l'app invoca `executePvp(..., "FRIENDLY")` fuori dalla transazione DB. Dopo un report valido, salva una riga `battle_history` con `rankingDelta = 0` e completa la sfida. Se il motore fallisce, la sfida resta `PENDING`, così l'opponent può riprovare.

## Persistenza e storico (F08)

La migrazione V12 crea `battle_history`, `pvp_matchmaking_queue` e `pvp_challenges`; schema e azioni FK sono descritti in [Storico delle partite](storico-partite.md). Le battaglie PvP Ranked e Friendly sono persistite. Il flusso PvE non crea righe di storico: il relativo report resta disponibile solo nella sessione React.

`GET /api/battles/history` restituisce un array per l'utente autenticato. Il parametro facoltativo `mode` filtra per modalità; il valore è ripulito dagli spazi e confrontato senza distinzione di maiuscole/minuscole. Non ci sono parametri di paginazione. L'ordine è `created_at DESC, id DESC`. Ogni elemento contiene `id`, `mode`, `playerAId`, `playerBId`, `winnerId`, `outcome`, `deckASnapshot`, `deckBSnapshot`, `battleLog`, `rankingDelta` e `createdAt`. L'implementazione attuale registra `rankingDelta = 0`; non calcola Elo, classifica o ricompense.

## Regole di gioco e limiti

Il server usa la simulazione automatica; non sono previste scelte interattive durante il combattimento. La specifica del [contratto PvE](app-battaglie-api.md) e l'orchestrazione sono separate dal protocollo PvP descritto qui. Stato e decisioni della proposta di regolamento restano nelle pagine [Digital](../Digital/regolamento-base.md).

## Collegamenti

- [Interfaccia sfide](../FrontEnd/sfide-simulazione.md)
- [Storico](storico-partite.md)
- [Amicizie F13](amicizie.md)
- [Mazzi F05](gestione-mazzi.md)
- [API PvE](app-battaglie-api.md)
- [Indice BackEnd](README.md)
