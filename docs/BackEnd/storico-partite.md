# Storico delle partite

ID: F08 · Perimetro: confermato; specifica tecnica backend approfondita, presentazione frontend da completare.
Implementazione: persistenza e lettura delle battaglie PvP implementate in `springboot_app/app`; PvE non persistito e interfaccia storico non integrata. Responsabile: DA DEFINIRE (Software Developer).
Ultimo aggiornamento significativo: **2026-11-27**, allineamento al codice e alla migrazione V12.

## Persistenza V12

`V12__create_pvp_and_battle_history.sql` introduce tre tabelle PostgreSQL:

- `battle_history`: ID identity, modalità, partecipanti, vincitore nullable, esito, snapshot JSONB dei mazzi, log JSONB, `ranking_delta` obbligatorio con default `0` e `created_at` con default `now()`. `player_a_id` è obbligatorio e ha FK `ON DELETE CASCADE`; `player_b_id` e `winner_id` sono nullable con `ON DELETE SET NULL`. `deck_a_snapshot` e `battle_log` sono obbligatori; `deck_b_snapshot` è nullable.
- `pvp_matchmaking_queue`: ticket, utente, mazzo, snapshot JSONB, stato, riferimento facoltativo allo storico e timestamp. Gli stati ammessi dal vincolo sono `WAITING`, `MATCHING`, `MATCHED`, `CANCELLED`. Un indice univoco parziale consente un solo ticket attivo per utente tra `WAITING` e `MATCHING`. Utente e mazzo sono rimossi a cancellazione fisica; il riferimento allo storico diventa nullo.
- `pvp_challenges`: challenger, opponent, mazzo e snapshot del challenger, mazzo nullable dell'opponent, stato, riferimento facoltativo allo storico e timestamp di creazione/risposta. Il vincolo esclude l'auto-sfida; gli stati ammessi sono `PENDING`, `COMPLETED`, `DECLINED`, `CANCELLED`. Gli utenti e il mazzo challenger hanno FK `ON DELETE CASCADE`; il mazzo opponent e il riferimento allo storico usano `ON DELETE SET NULL`. Indici per partecipante, stato e data supportano le letture.

Le sfide completate e i match Ranked persistono il report ricevuto dal motore; `ranking_delta` è attualmente `0`. Nessun record F08 viene creato per le simulazioni PvE. Snapshot e log conservano il risultato PvP senza dipendere dalla successiva composizione dei mazzi o dal log di sessione del client.

La cancellazione account applicativa è un soft delete: conserva la riga utente, quindi le FK della V12 non eliminano automaticamente righe PvP. Il cleanup F01/F13 elimina amicizie, non sfide o storico. Le azioni `CASCADE`/`SET NULL` descritte sopra si applicano a una cancellazione fisica, non all'endpoint di cancellazione logica.

## API di consultazione

`GET /api/battles/history` richiede un Bearer JWT e ruolo `USER` o `ADMIN`. Il risultato è un array JSON dei record in cui l'utente autenticato è `playerA` o `playerB`; l'identità non è selezionabile tramite parametri.

| Parametro | Comportamento |
| --- | --- |
| `mode` (facoltativo) | Filtro per modalità; gli spazi esterni sono rimossi e il confronto non distingue maiuscole/minuscole. Modalità persistite dal PvP: `RANKED` e `FRIENDLY`. |

Non sono implementati parametri di pagina, dimensione o offset. L'ordinamento è `created_at DESC, id DESC`. Ogni elemento restituisce:

| Campo | Tipo/semantica |
| --- | --- |
| `id` | ID del record |
| `mode` | Modalità PvP (`RANKED` o `FRIENDLY`) |
| `playerAId`, `playerBId` | ID partecipanti; il secondo può essere `null` dopo cancellazione fisica |
| `winnerId` | ID vincitore o `null` |
| `outcome` | Esito prodotto dal motore |
| `deckASnapshot`, `deckBSnapshot` | Snapshot JSON dei mazzi; il secondo può essere `null` |
| `battleLog` | Array di righe di log |
| `rankingDelta` | Intero, `0` nell'implementazione attuale |
| `createdAt` | Data/ora di creazione |

Il servizio non applica paginazione. La risposta HTTP di successo è `200`; l'autenticazione e le politiche di accesso seguono F01 e Spring Security.

## Limiti e punti aperti

- Non sono implementati storico PvE, classifiche Elo, ricompense o aggiornamenti di `rankingDelta`.
- La schermata React F08 e le chiamate client allo storico non sono implementate.
- Retention, esportazione e ulteriori filtri non sono definiti.
- Il dettaglio delle regole e del report di combattimento resta coordinato con [F07](sfide-simulazione.md) e con la [proposta Digital](../Digital/regolamento-base.md#14-resoconto-della-partita).

## Collegamenti

- [Interfaccia storico](../FrontEnd/storico-partite.md)
- [Sfide e API PvP F07](sfide-simulazione.md)
- [Schema canonico v1.1](Database/database-schema-v1.1.md)
- [Indice BackEnd](README.md)
