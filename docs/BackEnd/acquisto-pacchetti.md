# Offerte e acquisto di pacchetti

ID: F03 · Perimetro: confermato; specifica in approfondimento.
Implementazione: non iniziata. Responsabile: DA DEFINIRE (Software Developer).
Fonte: traccia, risposte e chiarimenti del gruppo; le decisioni note sono riportate sotto.
Ultimo aggiornamento significativo: 2026-11-05, allineamento alla base di dati v1.0 su richiesta dell'utente. Verifiche applicative: non eseguite.
Fonte dati: [specifica canonica v1.0](Database/database-schema-v1.0.md#inventario-e-pacchetti).

## Obiettivo e decisioni note

- Token e crediti indicano la stessa valuta; ogni offerta acquistabile contiene esattamente una unità di un pacchetto, senza insiemi di pacchetti differenti.
- Prezzo e configurazione sono gestibili dal backoffice; lo schema v1.0 non include una colonna prezzo o una tabella delle offerte. Fonte del prezzo e collegamento offerta/pacchetto restano da definire.
- Distinguere offerta, singolo pacchetto e carte risultanti dall'apertura.
- La traccia richiede carte casuali con quantità fissate per livello di rarità; valori e distribuzione non sono ancora definiti.

## Dati, transazioni e disponibilità

- `PACKS.id` e `INVENTORY.id` sono `int` auto-incrementali. I riferimenti a utente e pacchetto sono interi assegnati dai record esistenti, senza auto-incremento autonomo.
- `PACKS` conserva nome e descrizione obbligatori, immagine facoltativa e JSONB obbligatori per fazioni, tipi, espansioni, rarità e `slot_pulls`. I filtri usano i codici dei cataloghi; gli ID delle carte negli slot sono interi. Formato e validazione dei JSON restano applicativi, senza FK annidate.
- `INVENTORY_PACKS` usa PK `(id_inventory, id_pack)`. `item_quantity` è obbligatoria, senza default e con `CHECK > 0`; consumare l'ultimo pacchetto elimina la riga. Una coppia assente vale zero dopo una lettura riuscita.
- L'acquisto verifica il prezzo da una fonte concordata e addebita soltanto `token_balance` al netto degli hold non scaduti, incrementando i pacchetti nello stesso commit. L'apertura decrementa i pacchetti e incrementa `COLLECTIONS` per l'esatta coppia carta/arte estratta; errori annullano tutta l'operazione.
- Le scritture rispettano il [protocollo transazionale comune](Database/database-schema-v1.0.md#protocollo-transazionale-comune), con guard utente e letture aggiornate dopo i lock. Un acquisto non può spendere token prenotati; l'apertura non duplica la sottrazione del saldo.

## Da definire

- Fonte del prezzo, offerte acquistabili, configurazione di slot/probabilità, filtri vuoti, assegnazione delle arti e modalità di apertura.
- API, errori, deduplicazione delle richieste e gestione degli esiti di commit incerti. Non è modellato un registro dei movimenti.

## Collegamenti

- [Interfaccia negozio](../FrontEnd/acquisto-pacchetti.md)
- [Economia](../Digital/economia-crediti-pacchetti.md)
- [Backoffice](gestione-amministrativa.md)
- [Collezione](collezione-carte.md)
- [Indice BackEnd](README.md)
