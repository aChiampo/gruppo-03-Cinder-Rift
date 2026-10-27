# Consultazione dello storico

ID: F08 · Perimetro: confermato; specifica in approfondimento.
Implementazione: non iniziata. Responsabile: DA DEFINIRE (Web Developer).
Fonte: traccia, risposte e chiarimenti del gruppo; le decisioni note sono riportate sotto.
Fonte dell'approfondimento: regolamento Versione 2.0, proposta per il prototipo e le prove di bilanciamento; integrazione documentale approvata, regole definitive da confermare.
Ultimo aggiornamento significativo: 2026-10-27. Verifiche applicative: non eseguite.

## Obiettivo e decisioni note

- Mostrare le sfide giocate e i risultati disponibili all'utente.
- Consentire la consultazione del dettaglio della simulazione.

## Implicazioni della proposta di regolamento

- La [sezione 14 — Resoconto della partita](../Digital/regolamento-base.md#14-resoconto-della-partita) propone il contenuto da rendere consultabile. La presentazione dovrà distinguere il risultato della partita, i round e i singoli scambi di attacco, dopo la conferma dei requisiti.
- Il client mostrerà i dati autorevoli forniti dal [contratto canonico BackEnd di F08](../BackEnd/storico-partite.md), ancora da definire, senza ricostruire un risultato indipendente della partita.
- Il riferimento al regolamento non approva un layout, un formato degli eventi o un meccanismo di riproduzione della partita.

## Da definire

- Filtri, paginazione, struttura del dettaglio e stati dell'interfaccia.
- Presentazione accessibile del resoconto, stati vuoto/caricamento/errore e gestione di eventuali dati non disponibili secondo il contratto BackEnd.

## Collegamenti

- [Servizio storico](../BackEnd/storico-partite.md)
- [Sfide](sfide-simulazione.md)
- [Regolamento e resoconto proposto](../Digital/regolamento-base.md#14-resoconto-della-partita)
- [Indice FrontEnd](README.md)
