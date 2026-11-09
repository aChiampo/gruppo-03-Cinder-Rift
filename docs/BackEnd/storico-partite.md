# Storico delle partite

ID: F08 · Perimetro: confermato; specifica in approfondimento.
Implementazione: non iniziata. Responsabile: DA DEFINIRE (Software Developer).
Fonte: traccia, risposte e chiarimenti del gruppo; le decisioni note sono riportate sotto.
Fonte dell'approfondimento: regolamento Versione 2.0, proposta per il prototipo e le prove di bilanciamento; integrazione documentale approvata, regole definitive da confermare.
Ultimo aggiornamento significativo: 2026-11-05, allineamento alla base di dati v1.0 su richiesta dell'utente. Verifiche applicative: non eseguite.
Fonte dati: [specifica canonica v1.0](Database/database-schema-v1.0.md#storico-battaglie).

## Obiettivo e decisioni note

- Conservare le sfide giocate e i risultati per entrambi i partecipanti.
- Rendere consultabile il dettaglio prodotto dalla simulazione.

## Modello persistente v1.0

- `BATTLE_HISTORY.id` è una PK `int` auto-incrementale. `player1_id` e `player2_id` sono FK intere obbligatorie a `USERS.id`; `winner_id` è una FK intera nullable. Il `CHECK` ammette soltanto uno dei due giocatori oppure `NULL` per il pareggio.
- `battle_date` è `timestamptz` obbligatorio con default `now()`, con connessioni e serializzazione UTC.
- `deck_player1` e `deck_player2` sono JSONB obbligatori senza FK ai mazzi correnti; il contratto deve conservare composizione, arte e dati necessari a interpretare la partita dopo modifiche del catalogo. Il formato definitivo resta aperto.
- `ranking_reward` è un intero nullable e rappresenta soltanto la variazione con segno del primo giocatore. Le amichevoli hanno `NULL`; il secondo giocatore non riceve variazioni per questa battaglia. Registrazione del risultato e aggiornamento del primo giocatore sono atomici.
- La v1.0 non modella eventi di combattimento, righe per round, revisioni delle regole o sfide attive. Il dettaglio proposto dal regolamento non è quindi già rappresentato da tabelle aggiuntive. La PK generata non deduplica la stessa partita.

## Riferimenti alla proposta di regolamento

- La [sezione 14 — Resoconto della partita](../Digital/regolamento-base.md#14-resoconto-della-partita) propone il contenuto informativo dello storico, compresi risultati dei round e dettaglio degli scambi di attacco.
- Il contenuto proposto resta nella pagina Digital. Il formato tecnico degli eventi, le eventuali estensioni della persistenza v1.0 e il contratto di consultazione saranno definiti qui, in coordinamento con [F07 — Sfide e simulazione](sfide-simulazione.md).
- La base di dati v1.0 definisce la persistenza corrente. L'integrazione del regolamento non approva un formato di log aggiuntivo o una politica di conservazione.

## Da definire

- Estensioni per il dettaglio dei round, formato degli snapshot e degli eventi, idempotenza della registrazione, API, errori, ricerca e paginazione.
- Politica di conservazione e visibilità oltre ai partecipanti.

## Collegamenti

- [Interfaccia storico](../FrontEnd/storico-partite.md)
- [Sfide](sfide-simulazione.md)
- [Regolamento e resoconto proposto](../Digital/regolamento-base.md#14-resoconto-della-partita)
- [Indice BackEnd](README.md)
