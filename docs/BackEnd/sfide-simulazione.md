# Sfide e simulazione

ID: F07 · Perimetro: confermato; specifica in approfondimento.
Implementazione: non iniziata. Responsabile: DA DEFINIRE (Software Developer).
Fonte: traccia, risposte e chiarimenti del gruppo; le decisioni note sono riportate sotto.
Fonte dell'approfondimento: regolamento Versione 2.0, proposta per il prototipo e le prove di bilanciamento; integrazione documentale approvata, regole definitive da confermare.
Ultimo aggiornamento significativo: 2026-11-05, allineamento alla base di dati v1.0 su richiesta dell'utente. Verifiche applicative e di bilanciamento: non eseguite.
Fonte dati: [specifica canonica v1.0](Database/database-schema-v1.0.md#storico-battaglie).

## Obiettivo e decisioni note

- Un utente propone una sfida scegliendo un mazzo; l'avversario accetta scegliendo il proprio.
- Il server mescola i mazzi e simula la modalità base senza decisioni interattive durante la partita.
- Il motore deve essere un componente Java isolato, coperto da test JUnit.
- Risultato e dettaglio della simulazione devono essere disponibili ai due giocatori.

## Dati e confini della persistenza v1.0

- Utenti, mazzi, carte, arti e battaglie hanno identificativi `int`; le PK autonome sono auto-incrementali. Le FK copiano gli ID esistenti. `DECKS.is_challengeable`, obbligatorio con default `false`, abilita la selezione nel matchmaking senza attestare la validità del mazzo.
- Prima dello snapshot il backend rivalida proprietà, composizione, possesso della precisa variante e configurazioni di `ROUND_CONFIGURATIONS`, inclusi numero massimo di carte e somma dei costi energetici per destinazione. Numeri iniziali, minimi e regole di equipaggiamento restano da definire.
- `BATTLE_HISTORY` conserva due partecipanti, vincitore nullable, `deck_player1` e `deck_player2` JSONB obbligatori, data e `ranking_reward` nullable. Vincitore nullo significa pareggio; altrimenti il `CHECK` lo limita a uno dei partecipanti. La diversità dei partecipanti è applicativa.
- `ranking_reward` non nullo è la variazione con segno applicata esclusivamente a `player1_id`, nella transazione del risultato. `player2_id` non riceve variazioni per questa battaglia; nelle amichevoli il valore è `NULL`. Le formule non sono fissate dallo schema.
- Gli snapshot preservano carte e arti usate dopo modifiche dei mazzi o del catalogo. Lo schema non contiene sfide attive, log dei round o un vincolo contro doppi avvii della stessa partita; contratto JSON e idempotenza restano da definire.
- Validazione e snapshot devono condividere un confine coerente con il [protocollo transazionale comune](Database/database-schema-v1.0.md#protocollo-transazionale-comune). Giocabilità delle copie prenotate e lock per tutta la partita restano aperti.

## Riferimenti alla proposta di regolamento

- Le sezioni 6–13 della Versione 2.0 propongono il comportamento del motore: [fazioni](../Digital/regolamento-base.md#6-fazioni), [equipaggiamenti](../Digital/regolamento-base.md#7-equipaggiamenti), [preparazione](../Digital/regolamento-base.md#8-preparazione-della-partita), [inizio del round](../Digital/regolamento-base.md#9-inizio-di-ogni-round), [combattimento](../Digital/regolamento-base.md#10-combattimento) e condizioni di [fine round](../Digital/regolamento-base.md#12-conclusione-del-round) e [fine partita](../Digital/regolamento-base.md#13-conclusione-della-partita).
- La [formula dei danni ai giocatori](../Digital/regolamento-base.md#11-danni-ai-giocatori) richiede ancora conferma con gli insegnanti. Gli altri punti aperti sono raccolti nella [sezione 15](../Digital/regolamento-base.md#15-elementi-da-completare-e-testare).
- Le regole restano nella pagina Digital. Questa pagina ospiterà il contratto tecnico canonico di sfide e simulazione; la proposta non stabilisce endpoint, payload, stati della sfida o garanzie tecniche non ancora concordate.
- Il risultato destinato allo [storico](storico-partite.md) dovrà riferirsi al [resoconto proposto](../Digital/regolamento-base.md#14-resoconto-della-partita), dopo la conferma del relativo contratto.

## Da definire

- Conferma delle regole proposte, della formula del danno e dei casi limite; effetti degli equipaggiamenti e ordine di risoluzione restano da definire.
- Stati e persistenza della sfida, implementazione della rivalidazione prima dello snapshot, deduplicazione degli avvii e formato definitivo dei JSON.
- Contratto tecnico condiviso: operazioni, dati, autorizzazioni, errori, persistenza e criteri di accettazione del motore.

## Collegamenti

- [Interfaccia sfide](../FrontEnd/sfide-simulazione.md)
- [Regolamento](../Digital/regolamento-base.md)
- [Mazzi](gestione-mazzi.md)
- [Storico](storico-partite.md)
- [Indice BackEnd](README.md)
