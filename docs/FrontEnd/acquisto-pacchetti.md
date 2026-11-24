# Negozio delle offerte di pacchetti

ID: F03 · Perimetro: confermato; specifica in approfondimento.
Implementazione: non iniziata. Responsabile: DA DEFINIRE (Web Developer).
Fonte: traccia, risposte e chiarimenti del gruppo; le decisioni note sono riportate sotto.
Ultimo aggiornamento significativo: 2026-11-23, su richiesta dell’utente: descrizione strutturata, prezzo intero e riuso del componente carta secondo il [contratto canonico F03](../BackEnd/acquisto-pacchetti.md#descrizione-strutturata-e-prezzo-del-pacchetto). Verifiche applicative: non eseguite.

## Obiettivo e decisioni note

- Mostrare offerte ciascuna relativa a un solo pacchetto, secondo il contratto canonico F03; la risorsa utilizzabile per pagarle deve essere definita dal contratto e non dedotta dal vecchio termine generico «crediti».
- Distinguere Scaglie, Nuclei Rift e Rotami secondo la [terminologia economica confermata](../Digital/economia-crediti-pacchetti.md#risorse-economiche), usando i dati autorizzati dal backend senza accorpare le risorse in un unico saldo.
- Mostrare prezzo, risorsa richiesta e dati del singolo pacchetto usando le informazioni gestite dal backoffice. I tre saldi dell'inventario sono distinti; risorse ammesse nei prezzi e payload definitivi restano da precisare dal contratto backend.
- Collegare il risultato dell'apertura alla collezione.
- Il percorso server di acquisto, addebito, apertura e aggiornamento della collezione è descritto nel [contratto canonico F03](../BackEnd/acquisto-pacchetti.md#percorso-acquisto-addebito-estrazione-e-collezione).

## Descrizione e anteprima delle carte

- Usare `pack_price` come prezzo restituito dal backend; la risorsa di pagamento resta da definire e non va dedotta dal nome della colonna.
- La descrizione è un oggetto: leggere il testo da `pack_description.pack_description` e presentare quantità e rarità secondo il [contratto dati F03](../BackEnd/acquisto-pacchetti.md#descrizione-strutturata-e-prezzo-del-pacchetto). Un client che si aspetti una stringa deve essere aggiornato.
- `example_pull` è una mappa di carte dimostrative. Iterare i valori, ad esempio con `Object.values(pack.pack_description.example_pull)`, e passarli al componente carta esistente. Ogni valore rispetta lo [schema JSON completo della carta](../BackEnd/acquisto-pacchetti.md#schema-json-completo-di-una-carta), che definisce tutti i campi, i riferimenti annidati, le varianti grafiche, le proprietà obbligatorie e i valori nullable. Il contratto completo e l’esempio sono mantenuti soltanto nella pagina BackEnd.
- Mostrare l’anteprima come esempio del contenuto, distinta dalle carte effettivamente ottenute. La consultazione non modifica possesso o collezione; le cinque carte dell’esempio non fissano la quantità di tutti i prodotti.
- Questa sezione descrive l’integrazione da realizzare: API F03 e negozio funzionante non sono ancora implementati.

## Progresso mercy per pacchetto

- Per i pacchetti abilitati, mostrare soglia, aperture residue e rarità minima usando lo stato autorevole restituito dal backend per l'utente e quel pacchetto, secondo [F03](../BackEnd/acquisto-pacchetti.md#mercy-sulle-aperture).
- Il progresso viene aggiornato dopo un'apertura riuscita. Un esito della rarità richiesta o superiore ripristina la soglia; il backend applica la garanzia quando il conto raggiunge zero e restituisce il nuovo stato dopo il commit.
- Quando mercy è disabilitato, il contatore non è attivo. L'interfaccia non ricava il progresso dal saldo o dalla quantità totale di pacchetti posseduti e non decide localmente la rarità estratta.
- Durante l'apertura impedire invii involontari ripetuti; dopo errore o risposta incerta rileggere stato e possesso. La deduplicazione autorevole richiede il contratto backend ancora da definire. Payload e rappresentazione del risultato garantito restano da concordare.

## Da definire

- Schermate, conferma dell'acquisto, modalità di apertura e stati di errore.
- Contenuti effettivi dei prodotti; nessun prezzo o nome commerciale è fissato dagli esempi.
- Stati UI e riallineamento del saldo e della collezione dopo successo, errore o retry, in coerenza con il contratto F03.

## Collegamenti

- [Servizio pacchetti](../BackEnd/acquisto-pacchetti.md)
- [Economia](../Digital/economia-crediti-pacchetti.md)
- [Collezione](collezione-carte.md)
- [Backoffice](gestione-amministrativa.md)
- [Indice FrontEnd](README.md)
