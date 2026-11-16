# Catalogo e collezione delle carte

ID: F04 · Perimetro: confermato; contratto API condiviso in approfondimento.
Implementazione: API backend aggiunte, interfaccia e integrazione non implementate.
Ultimo aggiornamento significativo: 2026-11-15. Filtri, ordinamenti e restituzione delle varianti grafiche seguono le scelte dell'utente; il [contratto BackEnd F04](../BackEnd/collezione-carte.md) è la fonte canonica di percorsi, parametri, DTO, permessi ed errori.

## Obiettivo e comportamento previsto

- Consultare il catalogo delle carte e la collezione dell'utente autenticato.
- Chiedere `GET /api/cards` per il catalogo e `GET /api/users/me/collections` per la collezione. Il client invia il bearer token secondo il [contratto F01](../BackEnd/accesso-utenti.md).
- Usare i filtri condivisi: nome, fazione, rarità, tipo, espansione e intervalli min/max per energia, attacco, difesa e punti potenza.
- Usare gli ordinamenti per nome, fazione, rarità, energia, attacco, difesa o punti potenza, con direzione crescente/decrescente. Paginazione: `page` a base zero, `size` predefinito 20 e massimo 100.
- I codici dei cataloghi sono i valori dei filtri categoriali. L'API restituisce i codici associati alle carte, ma non una rotta separata per tutte le opzioni dei filtri.
- La collezione è identificata dal token, non da un ID utente scelto dal client. Ogni risultato distingue la variante grafica e mostra i contatori di copie forniti dal backend.
- Statistiche, riferimenti e URL delle immagini possono essere null. Il client deve tollerare immagini mancanti; il trattamento visivo è ancora da definire.

## Stati e integrazione

- `401`: sessione assente o non valida; seguire il flusso di accesso F01.
- `400`: parametri non validi; mostrare un errore recuperabile e conservare, se possibile, i filtri già inseriti.
- `200` con lista vuota: stato senza risultati per il catalogo o collezione ancora vuota.
- In caso di errore temporaneo, contenuto vuoto o pagina vuota non deve essere presentato come risultato riuscito.
- Dopo apertura di pacchetti o scambi, la collezione dovrà essere ricaricata secondo i contratti F03/F06. Questi flussi non sono implementati in questa attività.

## Da definire

- Schermate, filtri visuali, combinazione/reset dei filtri, dettagli carta e visualizzazione delle varianti.
- Origine delle opzioni complete per fazioni, rarità, tipi ed espansioni, nonché comportamento responsive e accessibile.
- Stati di caricamento, retry, errori di rete e conservazione della query durante la navigazione.
- Integrazione con apertura dei pacchetti F03, scambi F06 e composizione dei mazzi F05.

## Collegamenti

- [Contratto API catalogo e collezione F04](../BackEnd/collezione-carte.md)
- [Accesso utenti F01](../BackEnd/accesso-utenti.md)
- [Apertura pacchetti F03](../BackEnd/acquisto-pacchetti.md)
- [Composizione dei mazzi F05](gestione-mazzi.md)
- [Scambi F06](scambi-mercato.md)
- [Indice FrontEnd](README.md)
