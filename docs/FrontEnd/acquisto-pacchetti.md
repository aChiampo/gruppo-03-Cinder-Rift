# Negozio delle offerte di pacchetti

Aggiornamento del **2026-11-26**: seguire il [contratto F03 aggiornato](../BackEnd/acquisto-pacchetti.md#catalogo-implementato-migrazione-e-api). Il catalogo restituisce un array completo senza paginazione; USER vede solo pacchetti acquisibili e ADMIN tutti. Disabilitare l’acquisizione conserva il possesso. Dopo PUT amministrativo rileggere i dati del pacchetto; dopo DELETE riallineare inventario e saldo, secondo il rimborso e la pulizia mercy definiti dal backend. Slot e probabilità restano dati autorevoli; le probabilità non vanno stimate dalle anteprime. Interfaccia e flusso di acquisto non implementati in questa attività.

ID: F03 · Perimetro: confermato; specifica sintetica.
Implementazione: non iniziata. Responsabile: DA DEFINIRE (Web Developer).
Fonte: traccia, risposte e chiarimenti del gruppo; le decisioni note sono riportate sotto.
Ultimo aggiornamento significativo: **2026-11-26**, aggiunta la pianificazione dell’apertura differita su richiesta e annotazioni dell’utente, oltre a catalogo per ruolo e riconciliazione amministrativa. Il [contratto canonico F03](../BackEnd/acquisto-pacchetti.md) distingue decisioni confermate, proposta HTTP e regole ancora aperte. Implementazione del client e verifiche applicative: non eseguite.

Aggiornamento backend del **2026-11-24**: disponibili [catalogo e CRUD amministrativo F03](../BackEnd/acquisto-pacchetti.md#catalogo-implementato-migrazione-e-api), con migrazione senza seed dei pacchetti e configurazione artwork iniziale. Il client deve seguire quel contratto; negozio integrato, acquisto e apertura restano da realizzare. Apertura e assegnazione agli slot rinviate su indicazione dell’utente; tipi e percentuali artwork confermati nel contratto.

## Obiettivo e decisioni note

- Mostrare offerte ciascuna relativa a un solo pacchetto, secondo il contratto canonico F03; la risorsa utilizzabile per pagarle deve essere definita dal contratto e non dedotta dal vecchio termine generico «crediti».
- Distinguere Scaglie, Nuclei Rift e Rotami secondo la [terminologia economica confermata](../Digital/economia-crediti-pacchetti.md#risorse-economiche), usando i dati autorizzati dal backend senza accorpare le risorse in un unico saldo.
- Mostrare prezzo, risorsa richiesta e dati del singolo pacchetto usando le informazioni gestite dal backoffice. I tre saldi dell'inventario sono distinti; risorse ammesse nei prezzi e payload definitivi restano da precisare dal contratto backend.
- Collegare il risultato dell'apertura alla collezione.

## Presentazione degli artwork

Il client segue i [quattro tipi e la configurazione restituita da F03](../BackEnd/acquisto-pacchetti.md#tipi-e-probabilità-degli-artwork), senza stimare probabilità dagli esempi. Le varianti usano immagini e layout; non progettare né introdurre livelli di effetti speciali, foil o olografici. Showcase è escluso. `test-connessione` resta il riferimento per la futura integrazione frontend; questa modifica backend non importa né modifica quel branch.

## Inventario e apertura differita pianificati

Percorsi, payload, ruoli, limite e codici sono nella [proposta HTTP canonica](../BackEnd/acquisto-pacchetti.md#Inventario%20e%20apertura%20differita%3A%20proposta%20HTTP), ancora non implementata. Il client previsto:

- Carica saldi e pacchetti posseduti dalle letture dedicate, distinguendo inventario vuoto, caricamento ed errore. Visualizza anche il possesso non più acquisibile senza dipendere dalla visibilità nel negozio.
- Permette di scegliere un solo tipo e una quantità entro possesso e limite definiti dal contratto; la validazione del server resta autorevole.
- Disabilita invii accidentali durante la richiesta e mostra il conflitto previsto dal contratto quando la riga è già occupata; non accoda automaticamente un nuovo tentativo.
- Mostra le carte restituite direttamente dopo il commit, con rivelazione per pacchetto e slot e duplicati distinti. Non presume successi parziali del lotto.
- Riallinea inventario e collezione usando le rispettive letture. Non genera chiavi di richiesta e non usa identificativi storici o API di recupero delle pescate: questi flussi sono esclusi su richiesta esplicita di Chiampo.

Flussi di acquisto, disegno delle schermate e animazioni restano da definire; questa pianificazione non modifica componenti React.

## Progresso mercy per pacchetto

Comportamento previsto per la futura integrazione: apertura e progresso non sono ancora disponibili tramite API.

- Per i pacchetti abilitati, mostrare soglia, aperture residue e rarità minima usando lo stato autorevole restituito dal backend per l'utente e quel pacchetto, secondo [F03](../BackEnd/acquisto-pacchetti.md#mercy-sulle-aperture).
- Il progresso viene aggiornato dopo un'apertura riuscita. Un esito della rarità richiesta o superiore ripristina la soglia; il backend applica la garanzia quando il conto raggiunge zero e restituisce il nuovo stato dopo il commit.
- Quando mercy è disabilitato, il contatore non è attivo. L'interfaccia non ricava il progresso dal saldo o dalla quantità totale di pacchetti posseduti e non decide localmente la rarità estratta.
- Dopo un’apertura riuscita mostrare il progresso finale restituito direttamente dal server. La policy di generazione della garanzia resta da concordare.

## Da definire

- Schermate, conferma dell’acquisto, animazioni di apertura e presentazione degli stati; l’apertura differita e i suoi vincoli seguono la proposta canonica.
- Contenuti effettivi dei prodotti; nessun prezzo o nome commerciale è fissato dagli esempi.

## Collegamenti

- [Servizio pacchetti](../BackEnd/acquisto-pacchetti.md)
- [Economia](../Digital/economia-crediti-pacchetti.md)
- [Collezione](collezione-carte.md)
- [Backoffice](gestione-amministrativa.md)
- [Indice FrontEnd](README.md)
