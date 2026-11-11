# Offerte e acquisto di pacchetti

ID: F03 · Perimetro: confermato; specifica in approfondimento.
Implementazione: non iniziata. Responsabile: DA DEFINIRE (Software Developer).
Fonte: traccia, risposte e chiarimenti del gruppo; le decisioni note sono riportate sotto.
Ultimo aggiornamento significativo: 2026-11-09, per i tre saldi dell'inventario e per il sistema mercy sulle aperture confermato dall'utente. Verifiche applicative: non eseguite.
Fonte dati: [specifica canonica v1.1](Database/database-schema-v1.1.md#inventario-e-pacchetti).

## Obiettivo e decisioni note

- Ogni offerta acquistabile contiene esattamente una unità di un pacchetto, senza insiemi di pacchetti differenti.
- Scaglie, Nuclei Rift e Rotami sono risorse distinte secondo la [terminologia economica confermata](../Digital/economia-crediti-pacchetti.md#risorse-economiche), rappresentate rispettivamente da `INVENTORY.cobalt_shards`, `rift_cores` e `scraps`. Quali risorse siano ammesse nei prezzi resta da definire.
- Prezzo e configurazione sono gestibili dal backoffice; lo schema v1.1 non include una colonna prezzo o una tabella delle offerte. Fonte del prezzo e collegamento offerta/pacchetto restano da definire.
- Distinguere offerta, singolo pacchetto e carte risultanti dall'apertura.
- La traccia richiede carte casuali con quantità fissate per livello di rarità; valori e distribuzione non sono ancora definiti.
- Per i pacchetti con mercy abilitato, il conto alla rovescia avanza all'apertura del pacchetto corrispondente. Ogni pesca della rarità richiesta o superiore, naturale o garantita, lo riporta alla soglia del pacchetto.

## Dati, transazioni e disponibilità

- `PACKS.id` e `INVENTORY.id` sono `int` auto-incrementali. I riferimenti a utente e pacchetto sono interi assegnati dai record esistenti, senza auto-incremento autonomo.
- `PACKS` conserva nome e descrizione obbligatori, immagine facoltativa e JSONB obbligatori per fazioni, tipi, espansioni, rarità e `slot_pulls`. I filtri usano i codici dei cataloghi; gli ID delle carte negli slot sono interi. Formato e validazione dei JSON restano applicativi, senza FK annidate.
- `INVENTORY_PACKS` usa PK `(id_inventory, id_pack)`. `item_quantity` è obbligatoria, senza default e con `CHECK > 0`; consumare l'ultimo pacchetto elimina la riga. Una coppia assente vale zero dopo una lettura riuscita.
- I tre saldi di `INVENTORY` sono `int`, obbligatori, con default `0` e senza nuovi `CHECK` di non negatività: il backend verifica quantità, disponibilità, saldo finale non negativo e risultati nel dominio `int`.
- L'acquisto deve conoscere prezzo e risorsa richiesta dall'offerta, addebitare il relativo saldo disponibile e incrementare i pacchetti nello stesso commit. Il modello non contiene ancora la fonte del prezzo o il selettore della risorsa dell'offerta. L'apertura decrementa i pacchetti e incrementa `COLLECTIONS` per l'esatta coppia carta/arte estratta; errori annullano tutta l'operazione.
- Le scritture rispettano il [protocollo transazionale comune](Database/database-schema-v1.1.md#protocollo-transazionale-comune), con guard utente e letture aggiornate dopo i lock. `TRADE_TOKEN_RESERVATIONS.token_amount` non distingue una delle tre risorse: il suo abbinamento a un saldo, oppure un'estensione del contratto di prenotazione, deve essere definito prima di proteggere addebiti monetari condivisi. L'apertura non addebita nuovamente il prezzo.

## Mercy sulle aperture

Regole confermate dall'utente il **2026-11-09**, rappresentate anche nella [specifica autonoma del database](Database/database-schema-v1.1.md#inventario-e-pacchetti).

- `PACKS.mercy_threshold` è un intero nullable: `NULL` disabilita mercy; una soglia attiva deve essere maggiore di zero. Non è aggiunto un default fisso di dieci.
- `USER_MERCY` contiene una riga per `(id_user, id_pack)`: questa coppia è l'unica PK composta e le colonne sono FK obbligatorie a `USERS.id` e `PACKS.id`, senza auto-incremento. `mercy_rarity` è una FK obbligatoria a `RARITIES.id`, interpretata come rarità minima della garanzia.
- Il progresso persiste anche quando l'ultimo pacchetto posseduto viene consumato: eliminare la riga a quantità zero da `INVENTORY_PACKS` non elimina `USER_MERCY`.
- Alla prima apertura con mercy abilitato, il backend inizializza `mercy_counter` dalla soglia del pacchetto. Non esiste un default SQL che imposti lo stesso numero a tutti i pacchetti. La rarità iniziale deve provenire da una policy backend concordata; il client non sceglie una rarità autorevole.
- Ogni apertura riuscita del corrispondente `id_pack` usa quel progresso: `remaining = max(mercy_counter - 1, 0)`. Se arriva a zero, almeno uno degli slot previsti deve produrre una carta di rarità pari o superiore a `mercy_rarity`, rispettando configurazione, filtri e arte. Slot e distribuzione della pesca garantita restano da precisare; non si aggiunge implicitamente una carta oltre la composizione del pacchetto.
- Una pesca qualificante, anche naturale prima dello zero, salva nuovamente `PACKS.mercy_threshold`. Se nessuna carta qualifica, salva `remaining`. Un contatore già a zero forza la garanzia nell'apertura corrente, senza scendere sotto zero.
- Con soglia `10`, nove aperture senza esito qualificante lasciano `9, 8, ... , 1`; la decima garantisce la rarità richiesta e salva `10`. Un esito qualificante alla terza apertura salva subito `10`. I progressi sono indipendenti per ciascun pacchetto; il conto avanza sulle aperture riuscite, comprese quelle di pacchetti ottenuti come ricompensa.
- Il `CHECK` di riga impone `mercy_counter >= 0`. Il backend verifica anche soglia abilitata, limite superiore rispetto alla soglia, rarità interpretabili e disponibilità di una carta/arte valida per la garanzia. Non confronta gli ID delle rarità per stabilirne il livello.

### Apertura atomica

Flusso descrittivo da implementare, non un endpoint o un trigger esistente:

```text
function openPack(authenticatedUser, packId):
    begin transaction
    acquireCommonGuards(authenticatedUser, packId)
    pack = readCurrentPackConfiguration(packId)
    requireOwnedPack(authenticatedUser, packId)

    if pack.mercy_threshold is null:
        cards = drawNormalComposition(pack)
    else:
        T = requirePositiveThreshold(pack.mercy_threshold)
        progress = loadOrInitializeMercy(authenticatedUser, packId,
                                         T, approvedMinimumRarity(packId))
        requireCounterBetween(progress.mercy_counter, 0, T)
        remaining = max(progress.mercy_counter - 1, 0)
        if remaining == 0:
            cards = drawCompositionWithMinimumRarity(pack, progress.mercy_rarity)
        else:
            cards = drawNormalComposition(pack)

    requireValidCompositionAndRarityMetadata(cards, pack)
    if pack.mercy_threshold is not null:
        qualifies = hasQualifyingCard(cards, progress.mercy_rarity)
        if remaining == 0:
            require(qualifies)
        progress.mercy_counter = T if qualifies else remaining
        saveMercy(progress)

    consumeOneOwnedPack(authenticatedUser, packId)
    grantCardVariants(authenticatedUser, cards)
    commit
    return {cards, mercy: progress if pack.mercy_threshold is not null else null}
```

Consumo del pacchetto, inizializzazione/decremento/reset del progresso e accredito delle carte condividono il commit. Un errore annulla tutti gli effetti; una garanzia incompatibile con i filtri non consuma il pacchetto o il progresso. Il guard utente serializza le aperture concorrenti e protegge anche la riga mercy ancora assente. Se la soglia è `NULL`, i progressi esistenti non vengono avanzati o eliminati automaticamente.

La chiave del progresso non deduplica richieste ripetute dopo un commit incerto. Chiave di richiesta, conservazione del risultato e recupero devono ancora essere definiti; il client rilegge lo stato invece di ripetere alla cieca. Modifiche di soglia/rarità e riattivazione richiedono una policy di riconciliazione dei progressi esistenti. La garanzia viene risolta nella composizione dell'apertura stessa, senza record di ricompensa differita.

## Da definire

- Risorse ammesse nei prezzi, fonte del prezzo e selettore di risorsa delle offerte; corrispondenza delle prenotazioni, configurazione di slot/probabilità, filtri vuoti, assegnazione delle arti e modalità di apertura.
- API, errori, deduplicazione delle richieste e gestione degli esiti di commit incerti. Non è modellato un registro dei movimenti.
- Policy iniziale di `mercy_rarity`, ordinamento delle rarità, slot/distribuzione della garanzia, gestione dei progressi dopo modifiche o riattivazione di mercy e deduplicazione delle aperture.

## Collegamenti

- [Interfaccia negozio](../FrontEnd/acquisto-pacchetti.md)
- [Economia](../Digital/economia-crediti-pacchetti.md)
- [Backoffice](gestione-amministrativa.md)
- [Collezione](collezione-carte.md)
- [Indice BackEnd](README.md)
