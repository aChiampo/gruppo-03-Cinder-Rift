# Economia delle risorse e dei pacchetti

Decisione dell’utente del **2026-11-27**: gli hold monetari F06 riservano Scaglie e non sono spendibili nell’acquisto. [Acquisto F03 implementato](../BackEnd/acquisto-pacchetti.md#acquisto-pacchetti-contratto-http) con addebito e pacchetti chiusi atomici, senza cronologia acquisti o deduplicazione. Prezzi e seed commerciale restano da definire; non sono introdotte nuove regole di assegnazione o conversione delle risorse.

Aggiornamento F03 del **2026-11-26**, su decisione dell’utente: la cancellazione amministrativa di un pacchetto rimborsa le quantità ancora possedute in Scaglie (`cobalt_shards`) al `pack_price` corrente. Il prezzo originario non è registrato. Disabilitare l’acquisizione conserva il possesso; PUT e DELETE eliminano i progressi mercy relativi. [Contratto e limiti](../BackEnd/acquisto-pacchetti.md#catalogo-implementato-migrazione-e-api). Restano configurabili distribuzioni per rarità e disponibilità, senza nuovi prezzi commerciali o regole di apertura; gli acquisti usano Scaglie e i prezzi del seed restano in definizione.

ID: D03 · Perimetro: confermato; specifica sintetica.
Implementazione: non iniziata. Responsabile: DA DEFINIRE (Digital Strategist).
Fonte: traccia, risposte e chiarimenti del gruppo; le decisioni note sono riportate sotto.
Ultimo aggiornamento significativo: **2026-11-27**, confermata dall’utente la corrispondenza degli hold monetari F06 con le Scaglie e l’acquisto senza cronologia o deduplicazione. Rimborso amministrativo al prezzo corrente e accredito delle carte al solo possesso confermati il 2026-11-26; percentuali artwork del 2026-11-24 e terminologia economica del 2026-11-09 invariate. Verifiche dei materiali Digital: non eseguite.

## Obiettivo e decisioni note

- Distinguere offerte composte da pacchetti e contenuto dei singoli pacchetti.
- La conversione monetaria futura va pianificata ai fini delle proiezioni economiche richieste dalla traccia.
- Definire prezzi, rarità, composizioni e proiezioni a 12/24/48 mesi.
- Gli esempi commerciali nel README non sono valori approvati.

## Risorse economiche

Terminologia confermata dall'utente il **2026-11-09**:

| Risorsa | Significato confermato |
| --- | --- |
| Scaglie | Risorsa guadagnata nel mondo di gioco |
| Nuclei Rift | Riserva premium acquistabile |
| Rotami | Residui recuperabili dai doppioni |

Le tre risorse sono concetti distinti: non vanno considerate sinonimi di un unico saldo. La definizione non stabilisce importi, tassi di conversione, risorse ammesse nei prezzi o trasferibilità fra utenti. Il recupero dai doppioni non definisce automaticamente il trattamento delle carte cancellate dal catalogo.

La [specifica dell'inventario](../BackEnd/Database/database-schema-v1.1.md#inventario-e-pacchetti) associa Scaglie a `cobalt_shards`, Nuclei Rift a `rift_cores` e Rotami a `scraps`: tre saldi interi obbligatori, inizializzati a zero. Canali di acquisto della riserva premium, regole di assegnazione delle Scaglie e condizioni/quantità di recupero dei Rotami restano **DA DEFINIRE**. Prezzi del negozio e importi monetari degli scambi usano Scaglie; le quantità generiche dei premi richiedono ancora una corrispondenza esplicita con le risorse.

## Probabilità degli artwork

Distribuzione iniziale approvata dall’utente il **2026-11-24**: Standard Art **81%**, Extended Art **10%**, Full Art **7,5%**, Alternate Art **1,5%**; totale **100%**. Showcase è escluso e il suo precedente 1% passa a Standard. Si tratta di una scelta di bilanciamento del progetto, ispirata ai riferimenti pubblicati sui giochi di carte, non della distribuzione ufficiale di un prodotto esistente. I [tipi editoriali D01](lore-identita-carte.md#tipi-di-artwork) non includono livelli di effetti speciali.

Le percentuali valgono per una singola estrazione del tipo di artwork; non fissano numero o posizione degli slot in un pacchetto e non sono le probabilità delle rarità delle carte. La configurazione è associata direttamente al pacchetto secondo [F03](../BackEnd/acquisto-pacchetti.md#tipi-e-probabilità-degli-artwork). Estrazione indipendente per slot e fallback Standard se la variante manca sono approvati; apertura backend implementata il 2026-11-27.

## Copie ottenute dalle aperture

Decisione dell’utente del **2026-11-26**: le carte estratte incrementano solo il possesso, senza aumentare le copie scambiabili. Per una variante già posseduta il contatore scambiabile resta invariato; per una variante nuova parte da zero. La regola vale anche per doppioni e non introduce conversioni in Rotami o nuove regole per rendere scambiabili le copie. [Contratto tecnico F03](../BackEnd/acquisto-pacchetti.md#Inventario%20e%20apertura%20differita%3A%20contratto%20HTTP), implementato nel backend il 2026-11-27.

## Garanzia mercy all'apertura

Il sistema mercy segue le aperture riuscite del pacchetto corrispondente. Per ogni utente e pacchetto abilitato il contatore parte dalla soglia configurata e scende verso zero; l'apertura che raggiunge zero garantisce una carta della rarità minima richiesta o superiore. Ogni pesca qualificante, anche naturale prima dello zero, ripristina la soglia. I progressi di pacchetti diversi restano separati.

Con soglia dieci e rarità minima epica, la decima apertura senza una precedente epica o superiore è garantita; una pesca qualificante precedente fa ripartire il conto da dieci. È un esempio di configurazione, non una soglia o un ID di rarità fissati per tutti i pacchetti. Soglia e rarità nullable sono configurate nel pacchetto; mercy richiede entrambe. Alla scadenza senza pesca naturale qualificante, l’ultimo slot è sostituito con la rarità esatta configurata. Il [contratto tecnico F03](../BackEnd/acquisto-pacchetti.md#mercy-sulle-aperture) descrive progressi e apertura atomica.

## Da definire

- Modalità di assegnazione delle Scaglie, acquisto dei Nuclei Rift e recupero dei Rotami dai doppioni.
- Acquisti F03 in Scaglie confermati; conversioni e trasferibilità restano da definire. Nessuna equivalenza fra risorse.
- Offerte di pacchetti, prezzi, quantità per rarità e ipotesi economiche. Nessun limite commerciale di possesso; i limiti tecnici seguono F03.
- Costi e sostenibilità coerenti con gli obiettivi della traccia.
- Valori iniziali di soglia e rarità per ciascun pacchetto. Sostituzione dell’ultimo slot e cancellazione dei progressi dopo PUT sono confermate.

## Collegamenti

- [Terminologia commerciale](../../README.md#terminologia-commerciale)
- [Offerte pacchetti](../BackEnd/acquisto-pacchetti.md)
- [Schema dati preliminare](../BackEnd/Database/database-schema-v1.1.md) — proposta tecnica da coordinare con le decisioni economiche e sulla composizione.
- [Piano di lancio](comunicazione-lancio.md)
- [Indice Digital](README.md)
