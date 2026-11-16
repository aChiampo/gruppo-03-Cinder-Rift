# Negozio delle offerte di pacchetti

ID: F03 · Perimetro: confermato; specifica sintetica.
Implementazione: non iniziata. Responsabile: DA DEFINIRE (Web Developer).
Fonte: traccia, risposte e chiarimenti del gruppo; le decisioni note sono riportate sotto.
Ultimo aggiornamento significativo: 2026-11-09, per le risorse economiche e per il sistema mercy sulle aperture confermato nel [contratto canonico F03](../BackEnd/acquisto-pacchetti.md). Verifiche applicative: non eseguite.

## Obiettivo e decisioni note

- Mostrare offerte ciascuna relativa a un solo pacchetto, secondo il contratto canonico F03; la risorsa utilizzabile per pagarle deve essere definita dal contratto e non dedotta dal vecchio termine generico «crediti».
- Distinguere Scaglie, Nuclei Rift e Rotami secondo la [terminologia economica confermata](../Digital/economia-crediti-pacchetti.md#risorse-economiche), usando i dati autorizzati dal backend senza accorpare le risorse in un unico saldo.
- Mostrare prezzo, risorsa richiesta e dati del singolo pacchetto usando le informazioni gestite dal backoffice. I tre saldi dell'inventario sono distinti; risorse ammesse nei prezzi e payload definitivi restano da precisare dal contratto backend.
- Collegare il risultato dell'apertura alla collezione.

## Progresso mercy per pacchetto

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
