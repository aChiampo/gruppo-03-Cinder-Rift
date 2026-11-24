# Consultazione della collezione

ID: F04 · Perimetro: confermato; specifica sintetica.
Implementazione: non iniziata. Responsabile: DA DEFINIRE (Web Developer).
Fonte: traccia, risposte e chiarimenti del gruppo; le decisioni note sono riportate sotto.

## Obiettivo e decisioni note

- Mostrare le carte possedute e i dati forniti dal backend.
- Riflettere le variazioni conseguenti all'apertura dei pacchetti e agli scambi.

## Varianti grafiche

Decisione dell’utente del **2026-11-24**: seguire i [quattro tipi canonici F04/F03](../BackEnd/acquisto-pacchetti.md#tipi-e-probabilità-degli-artwork). Standard usa il riquadro ordinario, Extended Art e Full Art richiedono layout adatti e immagini sufficientemente estese, Alternate Art usa un’illustrazione dedicata. Non progettare livelli di effetti speciali; Showcase è escluso. La futura integrazione usa `test-connessione` come riferimento e mantiene l’identità `cardArtId` della variante selezionata. Implementazione frontend non inclusa in questo branch backend.

## Da definire

- Filtri, ordinamento, dettaglio carta, visualizzazione delle copie e stati dell'interfaccia.

## Collegamenti

- [Servizio collezione](../BackEnd/collezione-carte.md)
- [Mazzi](gestione-mazzi.md)
- [Scambi](scambi-mercato.md)
- [Indice FrontEnd](README.md)
