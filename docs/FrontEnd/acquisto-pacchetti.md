# Negozio delle offerte di pacchetti

Aggiornamento del **2026-11-26**: seguire il [contratto F03 aggiornato](../BackEnd/acquisto-pacchetti.md#catalogo-implementato-migrazione-e-api). Il catalogo restituisce un array completo senza paginazione; USER vede solo pacchetti acquisibili e ADMIN tutti. Disabilitare l’acquisizione conserva il possesso. Dopo PUT amministrativo rileggere i dati del pacchetto; dopo DELETE riallineare inventario e saldo, secondo il rimborso e la pulizia mercy definiti dal backend. Slot e probabilità restano dati autorevoli; le probabilità non vanno stimate dalle anteprime. Interfaccia e flusso di acquisto non implementati in questa attività.

ID: F03 · Perimetro: confermato; specifica sintetica.
Implementazione: non iniziata. Responsabile: DA DEFINIRE (Web Developer).
Fonte: traccia, risposte e chiarimenti del gruppo; le decisioni note sono riportate sotto.
Ultimo aggiornamento significativo: **2026-11-26**, catalogo per ruolo senza paginazione e riconciliazione del CRUD backend; risorse economiche e sistema mercy del 2026-11-09 restano descritti nel [contratto canonico F03](../BackEnd/acquisto-pacchetti.md). Verifiche applicative: non eseguite.

Aggiornamento backend del **2026-11-24**: disponibili [catalogo e CRUD amministrativo F03](../BackEnd/acquisto-pacchetti.md#catalogo-implementato-migrazione-e-api), con migrazione senza seed dei pacchetti e configurazione artwork iniziale. Il client deve seguire quel contratto; negozio integrato, acquisto e apertura restano da realizzare. Apertura e assegnazione agli slot rinviate su indicazione dell’utente; tipi e percentuali artwork confermati nel contratto.

## Obiettivo e decisioni note

- Mostrare offerte ciascuna relativa a un solo pacchetto, secondo il contratto canonico F03; la risorsa utilizzabile per pagarle deve essere definita dal contratto e non dedotta dal vecchio termine generico «crediti».
- Distinguere Scaglie, Nuclei Rift e Rotami secondo la [terminologia economica confermata](../Digital/economia-crediti-pacchetti.md#risorse-economiche), usando i dati autorizzati dal backend senza accorpare le risorse in un unico saldo.
- Mostrare prezzo, risorsa richiesta e dati del singolo pacchetto usando le informazioni gestite dal backoffice. I tre saldi dell'inventario sono distinti; risorse ammesse nei prezzi e payload definitivi restano da precisare dal contratto backend.
- Collegare il risultato dell'apertura alla collezione.

## Presentazione degli artwork

Il client segue i [quattro tipi e la configurazione restituita da F03](../BackEnd/acquisto-pacchetti.md#tipi-e-probabilità-degli-artwork), senza stimare probabilità dagli esempi. Le varianti usano immagini e layout; non progettare né introdurre livelli di effetti speciali, foil o olografici. Showcase è escluso. `test-connessione` resta il riferimento per la futura integrazione frontend; questa modifica backend non importa né modifica quel branch.

## Progresso mercy per pacchetto

Comportamento previsto per la futura integrazione: apertura e progresso non sono ancora disponibili tramite API.

- Per i pacchetti abilitati, mostrare soglia, aperture residue e rarità minima usando lo stato autorevole restituito dal backend per l'utente e quel pacchetto, secondo [F03](../BackEnd/acquisto-pacchetti.md#mercy-sulle-aperture).
- Il progresso viene aggiornato dopo un'apertura riuscita. Un esito della rarità richiesta o superiore ripristina la soglia; il backend applica la garanzia quando il conto raggiunge zero e restituisce il nuovo stato dopo il commit.
- Quando mercy è disabilitato, il contatore non è attivo. L'interfaccia non ricava il progresso dal saldo o dalla quantità totale di pacchetti posseduti e non decide localmente la rarità estratta.
- Durante l'apertura impedire invii involontari ripetuti; dopo errore o risposta incerta rileggere stato e possesso. La deduplicazione autorevole richiede il contratto backend ancora da definire. Payload e rappresentazione del risultato garantito restano da concordare.

## Da definire

- Schermate, conferma dell'acquisto, modalità di apertura e stati di errore.
- Contenuti effettivi dei prodotti; nessun prezzo o nome commerciale è fissato dagli esempi.

## Collegamenti

- [Servizio pacchetti](../BackEnd/acquisto-pacchetti.md)
- [Economia](../Digital/economia-crediti-pacchetti.md)
- [Collezione](collezione-carte.md)
- [Backoffice](gestione-amministrativa.md)
- [Indice FrontEnd](README.md)
