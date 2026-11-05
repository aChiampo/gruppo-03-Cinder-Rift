# Collezione delle carte

ID: F04 · Perimetro: confermato; specifica in approfondimento.
Implementazione: non iniziata. Responsabile: DA DEFINIRE (Software Developer).
Fonte: traccia, risposte e chiarimenti del gruppo; le decisioni note sono riportate sotto.
Ultimo aggiornamento significativo: 2026-11-05, allineamento alla base di dati v1.0 su richiesta dell'utente. Verifiche applicative: non eseguite.
Fonte dati: [specifica canonica v1.0](Database/database-schema-v1.0.md#dettaglio-struttura-delle-carte).

## Obiettivo e decisioni note

- Consentire all'utente di consultare le carte possedute.
- L'apertura dei pacchetti e gli scambi aggiornano le proprietà delle carte.

## Dati, copie e varianti grafiche

- Gli ID generati di utenti, carte e arti sono `int` auto-incrementali. `COLLECTIONS` eredita gli interi nella PK `(id_user, id_card, id_card_art)` e non genera nuovi identificativi.
- `CARDS_ARTS` ha PK `(id_card, art_code)`, `id_art` auto-incrementale univoco e unicità `(id_card, id_art)`. La FK composta della collezione garantisce che l'arte appartenga alla carta; l'URL dell'arte rimane nullable.
- `owned_replica` è obbligatorio e maggiore di zero; `tradable_replica` è obbligatorio e compreso fra zero e il possesso. Una tripla assente significa zero solo dopo una lettura riuscita. Il trasferimento dell'ultima copia elimina la riga.
- Le copie scambiabili disponibili sono `tradable_replica` meno la somma degli hold non scaduti della stessa tripla. Prenotare non modifica i due contatori. Prima di eliminare una riga occorre consumare o rilasciare gli hold attivi; non esiste una FK degli hold alla riga volatile di possesso.
- I mazzi scelgono esplicitamente `id_card_art`. Possesso per variante, effetti dei trasferimenti sui mazzi e disponibilità per il gioco richiedono validazione backend; le prenotazioni economiche non decidono da sole la giocabilità.
- Ogni modifica applica il [protocollo transazionale comune](Database/database-schema-v1.0.md#protocollo-transazionale-comune). La cancellazione logica di carte o espansioni invalida prima le trattative e rilascia entrambi i lati, poi converte le copie e ripulisce collezione e mazzi; tassi e frammenti restano da definire.

## Da definire

- Dati esposti, filtri, paginazione, riutilizzo delle copie nei mazzi e giocabilità delle copie prenotate.
- Contratti e regole di visibilità della collezione.

## Collegamenti

- [Interfaccia collezione](../FrontEnd/collezione-carte.md)
- [Pacchetti](acquisto-pacchetti.md)
- [Scambi](scambi-mercato.md)
- [Indice BackEnd](README.md)
