# Gestione e validazione dei mazzi

- ID: F05 · Perimetro: confermato; specifica in approfondimento.
- Implementazione: non iniziata. Responsabile: DA DEFINIRE (Software Developer).
- Ultimo aggiornamento significativo: **2026-11-05**, allineamento alla base di dati v1.0 su richiesta dell'utente.
- Branch documentale richiesto: `docs/f05-deck-validation-v2`.

Fonti: richiesta e pseudocodice dell'utente del 2026-10-28, aggiornamento del modello e chiarimenti su configurazione, assenza di revisioni e retry del 2026-10-29; [regolamento Versione 2.1](../Digital/regolamento-base.md), sezioni 2–4, 7, 8 e 15, e richiesta di allineamento del 2026-11-02; [specifica canonica della base di dati v1.1](Database/database-schema-v1.1.md#dettaglio-struttura-delle-carte), fonte corrente di strutture, identificativi e vincoli.

L'integrazione documentale è approvata; la proposta di riferimento è ora la V2.1, mentre regole definitive e bilanciamento restano da confermare. Le decisioni aggiornate dall'utente il 2026-10-29 prevedono una PK autonoma `DECKS_CARDS.id`, una destinazione testuale `ROUND1` … `ROUND5` oppure `EQUIPMENT` e nessuna quantità per riga. Questo modello sostituisce la precedente PK composta e risolve la rappresentazione delle due copie nello stesso round e del gruppo equipaggiamenti. Restano scelti i controlli distinti per operazione e l'atomicità di eventuali aggregati persistiti. Firme, errori e strategie transazionali restano **proposte da concordare**, non API o migrazioni implementate. Verifiche applicative e di bilanciamento: **non eseguite**.

## Obiettivo, perimetro e dipendenze

Adattare il flusso «aggiunta → raccolta regole e dati → validazione → salvataggio oppure errori» alla V2.1, mantenendo `submitDeck(userId, deckId)` come verifica completa prima dell'utilizzo. Ogni utente può comporre e salvare più mazzi; salvare una bozza incompleta non significa renderla utilizzabile.

Questa pagina approfondisce il riferimento tecnico canonico F05 già esistente. Il precedente aggiornamento del 2026-11-02 resta la fonte delle proposte V2.1. La versione corrente recepisce invece la base di dati v1.1: destinazioni e limiti dei round sono persistiti in `ROUND_CONFIGURATIONS`, mentre le altre regole proposte devono ancora essere concordate. Restano lo pseudocodice descrittivo con l'eccezione preesistente dei commenti in italiano e l'assenza di revisioni/versioni di mazzi e catalogo. Il protocollo di accesso è comune fra servizi. Non sono eseguite migrazioni o prove applicative; regole di gioco e API ancora proposte restano riconoscibili come tali.

Dipendenze: F01 per l'identità, F04 per il possesso, F06 per gli scambi, F07 per validazione e snapshot prima della partita, F10 per catalogo e regole. Mescolamento, effetti e simulazione spettano a F07. Gli equipaggiamenti formano un gruppo separato: F05 non li assegna a round specifici.


## Requisiti di validazione derivati dalla V2.1

La tabella è una **traduzione tecnica della proposta V2.1**, non una fonte di regole indipendente. I numeri proposti non sono seed o parametri già inseriti nella v1.1. Per persistenza, arti e limiti configurabili prevale la specifica della base di dati; il raccordo delle restanti regole di gioco richiede conferma. I valori restano governati dalle [sezioni 2–4 del regolamento](../Digital/regolamento-base.md#2-struttura-del-mazzo). Ogni occorrenza logica vale una copia; i totali si calcolano contando le occorrenze, non gli ID carta distinti. Ogni riga persistita rappresenta una copia, senza campo quantità, e ha un ID autonomo anche quando carta e destinazione coincidono con un'altra riga.

| Ambito | Controllo |
| --- | --- |
| Mazzo | Esattamente 30 carte: 25 creature e 5 equipaggiamenti |
| Creature | Cinque gruppi assegnati ai round 1–5, esattamente 5 carte per gruppo |
| Equipaggiamenti | Un gruppo separato di 5 carte differenti, senza assegnazione al round |
| Energia persistente v1.1 | Somma di `CARDS.energy_cost` per destinazione ≤ `ROUND_CONFIGURATIONS.max_energy`; valori iniziali DA DEFINIRE |
| Potenza | Totale ≤ 75; creature ≤ 65; equipaggiamenti ≤ 10; ogni gruppo creature ≤ 14 |
| Rarità creature, intero mazzo | Comuni ≥ 13; rare ≤ 8; epiche ≤ 3; leggendarie ≤ 1 |
| Copie creature | ≤ 2 per stessa carta nell'insieme dei cinque gruppi, anche divise fra round |
| Rarità equipaggiamenti | Comuni ≥ 3; rare ≤ 2; epiche ≤ 1; leggendarie = 0 nel primo set |
| Copie equipaggiamenti | ≤ 1 per stessa carta |
| Possesso | Occorrenze per coppia carta/arte in tutte le destinazioni ≤ copie possedute di quella variante; limite copie di gioco aggregato per carta |
| Fazioni | Composizione libera, nessuna quota aggiuntiva |

Nella V2.1 non si applica il precedente limite di un'epica o leggendaria per gruppo creature: più copie epiche e/o leggendarie possono condividere un round se rispettano tutti gli altri vincoli. Restano i massimi di rarità per categoria riportati nelle tabelle della sezione 4 del regolamento: creature epiche ≤ 3 e leggendarie ≤ 1; equipaggiamenti epici ≤ 1 e nessun leggendario nel primo set.

I budget sono massimi indipendenti, non obiettivi esatti: risparmi in una categoria non aumentano il limite dell'altra. La v1.1 richiede il controllo della somma dei costi energetici per destinazione, distinto dalla potenza. L'accesso individuale proposto dalla V2.1 è una regola aggiuntiva da riconciliare, non sostituisce `max_energy`. La potenza è il costo fisso della carta, da caricare dal catalogo, non da dedurre dalla sola rarità. Le fasce V2.1 per carta sono: comune 1–2, rara 2–3, epica 3–4, leggendaria 5; valgono anche per gli equipaggiamenti ammessi.

Soglie e accesso delle creature meno costose ai round successivi restano proposti, come indicato nella [sezione 15](../Digital/regolamento-base.md#15-elementi-da-completare-e-testare). Non introdurre limiti statistici o quote di fazione non definiti. Il controllo aggregato dell'energia è invece richiesto dalla v1.1; i numeri proposti non sono default di `ROUND_CONFIGURATIONS`. La restrizione sugli equipaggiamenti leggendari riguarda il primo set: la gestione di eventuali set successivi è **DA DEFINIRE**.

## Attori, autorizzazioni e precondizioni

- `userId` deriva dall'identità autenticata, non da un campo client arbitrario.
- Caricamento e modifica verificano `DECKS.id_user == userId`. Identità assente, mazzo inesistente o non autorizzato interrompono il flusso prima di esporre composizione e collezione. Status HTTP e politica di visibilità sono DA DEFINIRE.
- I privilegi di `ADMIN` e `USER` richiedono un’assegnazione esplicita.
- Regole incomplete o letture fallite producono errori tecnici: nessun salvataggio né esito positivo. Una lettura fallita non equivale a una collezione vuota.
- Una riga collezione assente vale zero copie solo dopo una lettura riuscita; valori presenti nulli, negativi o non interi indicano dati incoerenti.

## Dati e vincoli della base di dati v1.1

| Concetto | Base di dati v1.1 | Regola |
| --- | --- | --- |
| Identificativi | PK autonome e `CARDS_ARTS.id_art`: `int` auto-incrementali | Le FK ereditano gli interi; il client non genera la PK |
| Mazzo/proprietario | `DECKS.id`, `id_user` obbligatorio | Autorizzazione a ogni operazione |
| Matchmaking | `DECKS.is_challengeable`, default `false` | Abilita la selezione, non certifica la validità |
| Composizione | `DECKS_CARDS.id`, `id_deck`, `id_card`, `id_card_art`, `destination` | Una riga per copia; carta, arte e destinazione obbligatorie |
| Carta/arte | FK composta a `CARDS_ARTS.(id_card, id_art)` | L'arte deve appartenere alla carta |
| Tipo/rarità/fazione | `CARDS.id_type → CARD_TYPES.id`, `id_rarity → RARITIES.id`, `id_faction → FACTIONS.id` | Usare i codici dei cataloghi, senza dedurre regole dagli ID |
| Energia/potenza | `CARDS.energy_cost`, `power`, nullable | Metadati mancanti bloccano i controlli che li richiedono |
| Possesso | `COLLECTIONS(id_user, id_card, id_card_art)` | Contare per variante; limite copie di gioco per carta logica |
| Scambiabilità | `tradable_replica` meno hold non scaduti | Non coincide con la disponibilità per giocare |
| Destinazioni e limiti | `ROUND_CONFIGURATIONS.round_name`, `round_number`, `max_energy`, `max_cards` | FK dalla destinazione; parametri positivi; aggregati verificati dal backend |
| Parole chiave | `CARDS_KEYWORDS.id_card` PK e `keyword_list` JSONB | Al massimo una lista per carta; ID interi annidati senza FK |
| Altre regole di gioco | Non sono colonne della v1.1 | Proposte V2.1 da concordare; nessun seed o valore implicito |
| Validazione precedente | Nessuno stato persistito di validazione | Nessuna revisione di mazzo/catalogo; rivalidare i dati correnti |



- Le creature usano `ROUND1` … `ROUND5`; gli equipaggiamenti usano `EQUIPMENT`, senza assegnazione anticipata a un round di gioco. Non sono necessari marcatori numerici o valori nulli. La compatibilità tipo/destinazione è verificata dal servizio, perché il tipo appartiene alla tabella `CARDS`.
- Non aggiungere un vincolo `UNIQUE(id_deck, id_card, destination)`: impedirebbe nuovamente due copie della stessa creatura nello stesso round. Due righe con stessa carta e destinazione hanno ID distinti e vengono contate entrambe; i limiti V2.1 restano responsabilità della validazione transazionale.
- Inserire una carta aggiunge una riga con ID assegnato dal server; rimuoverla elimina la specifica riga per ID, dopo averne verificato l'appartenenza al mazzo autorizzato. La PK `DECKS_CARDS.id` è `int` auto-incrementale generata dal database; le FK ereditano gli interi esistenti. Non esistono quantità di riga da incrementare o azzerare.
- Eventuali totali aggregati persistiti devono essere aggiornati nella stessa transazione delle righe e uguagliare i rispettivi conteggi. Un errore annulla entrambe le scritture; gli aggregati non sono una nuova fonte indipendente della composizione.

La [sezione 4](../Digital/regolamento-base.md#4-budget-e-vincoli-di-composizione) ammette due copie della stessa creatura anche nello stesso mazzetto: il nuovo modello le rappresenta mediante due righe con ID distinti e la stessa destinazione. Il valore `EQUIPMENT` rappresenta esplicitamente il gruppo separato. I due precedenti problemi strutturali sono quindi risolti; le altre regole V2.1 richiedono comunque il validatore e non sono garantite dalla sola PK.

Il DBML dichiara già le FK per proprietario, composizione, carte e collezione. Le quantità della collezione sono obbligatorie: `owned_replica > 0` e `0 <= tradable_replica <= owned_replica`; quando il possesso scende a zero la riga va eliminata. `DECKS.id_user`, carta, arte e destinazione sono obbligatori. Diversi metadati di `CARDS` restano nullable e devono essere verificati prima dell'uso. Un `CHECK` di riga non copre i limiti aggregati. La specifica e il DBML v1.1 sono allineati; non è stata creata o eseguita una migrazione database.

## Controlli per operazione e utilizzo

Approccio scelto dall'utente: funzioni distinte per verificare una modifica e per verificare l'intero mazzo, con helper condivisi per calcoli e vincoli. Ogni funzione ha uno scopo fisso; il chiamante non seleziona una modalità di validazione.

| Operazione | Controlli applicati | Significato del successo |
| --- | --- | --- |
| `addCardToDeck` → `validateDeckChange` | Struttura e catalogo, possesso aggregato, destinazioni, energia, massimi di quantità, copie, rarità e potenza sull'intero mazzo proposto | La modifica può essere salvata; non certifica l'idoneità a giocare |
| `submitDeck` → `validateDeck` | Gli stessi vincoli più conteggi esatti e minimi di comuni, anche se il mazzo è incompleto | Il mazzo soddisfa tutti i controlli sui dati letti in quel momento |
| Preparazione partita F07 → `validateDeck` | Verifica completa su entrambi i mazzi, atomicamente con lo snapshot | Le composizioni verificate possono essere fissate per quella partita |

Un mazzo vuoto o incompleto può essere salvato durante la composizione. L'aggiunta applica tutti i massimi anche prima del completamento; quantità esatte e minimi si verificano tramite `submitDeck`. Il riepilogo dei conteggi consente di mostrare l'avanzamento; per ottenere le violazioni di idoneità si richiede la verifica completa. Non viene restituita una seconda lista di requisiti rinviati.

Non sono introdotti stati persistiti di validazione. Una modifica, uno scambio o un aggiornamento delle regole/catalogo può rendere obsoleto il successo di `submitDeck`; l'utilizzo richiede la verifica sui dati correnti. Un totale di 30 non rende automaticamente conforme il mazzo.

**Proposta di riparazione:** consentire la rimozione di righe da bozze già invalide, verificando proprietà del mazzo ed esistenza dell'occorrenza; salvare la diagnostica residua senza richiedere subito l'idoneità completa. La rimozione non aggiunge occorrenze né introduce nuove violazioni strutturali. Una carta non più reperibile deve restare rimuovibile tramite ID della riga di composizione. Aggiunte e spostamenti applicano invece `validateDeckChange` al mazzo proposto; uno spostamento aggiorna la destinazione dell'occorrenza individuata per ID con un solo commit. Verificare sempre che l'ID appartenga al mazzo richiesto prima di modificarlo. Operazioni CRUD e riparazione sono **DA CONCORDARE**: non fanno parte delle firme iniziali dell'utente.

## Contratto logico proposto

Firme di servizio e dati illustrativi, **non endpoint approvati**. Metodi HTTP, percorsi conformi al prefisso `/api`, status, limiti, paginazione e OpenAPI sono DA DEFINIRE prima dell'integrazione.

```typescript
type DeckDestination = "ROUND1" | "ROUND2" | "ROUND3" | "ROUND4" | "ROUND5" | "EQUIPMENT";

type DeckEntry = {
    entryId?: number; // Presente per le copie salvate; assegnato dal server.
    cardId: number;
    cardArtId: number; // Selected artwork; must belong to cardId.
    destination: DeckDestination; // Ogni elemento rappresenta una copia, senza quantità.
};

type Violation = {
    code: string;
    scope: "deck" | "creatures" | "equipment" | "round" | "card";
    cardId?: number;
    cardArtId?: number; // Selected variant when the error concerns ownership or artwork.
    destination?: DeckDestination;
};

type ValidationResult = {
    success: boolean;  // Indica se i controlli dell'operazione sono superati.
    errors: Violation[];
    summary: object | null; // Nullo se struttura o catalogo impediscono i calcoli.
};

addCardToDeck(userId, deckId, cardId, cardArtId, destination): ValidationResult;
submitDeck(userId, deckId): ValidationResult;

// Le funzioni interne condividono conteggi e controlli dei vincoli.
validateDeckChange(proposedDeck, context): ValidationResult;
validateDeck(deck, context): ValidationResult;
```

Gli ID sono interi positivi nel dominio `int` della v1.1; i campi numerici del contratto proposto devono rispettarlo. Ogni elemento di `entries` rappresenta una copia, senza proprietà `quantity`; `entryId` corrisponde alla PK `DECKS_CARDS.id` delle occorrenze salvate. L'aggiunta richiede carta, arte e destinazione e non permette al client di scegliere la PK. `destination` è obbligatoria e usa gli stessi valori testuali del DBML. Due elementi con stessi `cardId`, `cardArtId` e `destination` sono due copie distinte: non vengono deduplicati e, se altrimenti conformi, possono essere salvati con ID diversi. Ripetere lo stesso `entryId` in una composizione è invece un errore strutturale, perché identificherebbe due volte la stessa riga. I limiti di copie si contano per carta logica; il possesso si conta per coppia carta/arte in tutto il mazzo.

`summary` contiene almeno totale carte/potenza, conteggi e potenza per categoria, rarità per categoria, conteggio/potenza e somma energetica per destinazione e occorrenze per coppia carta/arte. Non richiede un contatore epiche + leggendarie per round, poiché nella V2.1 non esiste più quel vincolo. Serializzazione definitiva DA DEFINIRE. Il risultato non contiene token di revisione/versione e non autorizza utilizzi futuri.

Esempio: aggiungere una creatura comune posseduta da 1 punto, energia 1, con destinazione `ROUND1` in un mazzo vuoto restituisce `success: true`, `errors: []` e un riepilogo con una carta. Questo successo riguarda il salvataggio della modifica. `submitDeck` sullo stesso mazzo restituisce `success: false` con conteggi incompleti e minimi di comuni mancanti in `errors`. Una seconda copia in `ROUND1` è salvabile con un nuovo ID se possesso, budget e altri vincoli lo consentono. La terza copia produce `COPY_LIMIT_EXCEEDED`, anche in un round diverso. In caso di rifiuto, nessuna aggiunta viene salvata; errori e riepilogo descrivono il mazzo proposto e la composizione persistita resta invariata.

| Codici proposti | Significato |
| --- | --- |
| `INVALID_DECK_STRUCTURE` | Occorrenze logiche malformate, ID o tipi dei campi non validi; nessun campo quantità nelle righe del mazzo |
| `CARD_NOT_FOUND`, `CARD_METADATA_INVALID` | Carta assente, tipo/rarità non mappati, potenza mancante/fuori fascia, energia creatura non interpretabile |
| `CARD_NOT_PLAYABLE` | Utilizzabile soltanto dopo aver concordato la politica di legalità catalogo |
| `INVALID_DESTINATION`, `ENERGY_LIMIT_EXCEEDED` | Destinazione assente/sconosciuta, tipo nel gruppo errato oppure somma energetica sopra il limite della destinazione |
| `COPY_LIMIT_EXCEEDED`, `INSUFFICIENT_OWNERSHIP` | Copie aggregate oltre limite o possesso |
| `CARD_COUNT_EXCEEDED`, `CARD_COUNT_MISMATCH` | Massimi superati durante modifica o verifica completa; conteggi sotto target rilevati dalla verifica completa |
| `POWER_BUDGET_EXCEEDED` | Totale/categoria/round fuori budget |
| `RARITY_LIMIT_EXCEEDED`, `COMMON_MINIMUM_NOT_MET` | Massimi di rarità per categoria e minimi comuni |
| `RULESET_UNAVAILABLE`, `VALIDATION_UNAVAILABLE` | Errori tecnici di configurazione/lettura, distinti dalle violazioni del mazzo |
| `CONCURRENT_MODIFICATION` | Conflitto di accesso concorrente o timeout dei lock, nessun salvataggio parziale |

Gli errori riportano scope, riferimenti e valori pertinenti; il client localizza le descrizioni senza sostituire i codici. Per autenticazione Bearer, risposte `401` e relativi header fare riferimento al [contratto F01](accesso-utenti.md#contratto-tecnico-condiviso), già disponibile. Il formato degli errori tecnici e di dominio specifici di F05 resta DA DEFINIRE, senza simulare una validazione riuscita. Riferimento F01 allineato il **2026-11-12**, su richiesta dell’utente.

## Flusso in pseudocodice

Pseudocodice descrittivo, non TypeScript eseguibile. Le funzioni di supporto sono da implementare; i commenti sono in italiano come richiesto per questa revisione. `rules` contiene i valori V2.1 della tabella, fasce potenza, tipi/rarità ammessi e soglie per round; non contiene un massimo combinato di epiche e leggendarie per round. I controlli mantengono tutti i vincoli attivi, raggruppando quelli ripetitivi in funzioni con uno scopo esplicito.

### Aggiunta e raccolta del contesto

```text
function addCardToDeck(userId, deckId, cardId, cardArtId, destination):
    requireAuthenticatedIdentity(userId)
    requireValidIdentifiersAndDestinationSyntax(deckId, cardId, cardArtId, destination)

    return inTransaction:
        // Blocca le risorse e verifica la proprietà prima di leggere il contesto.
        deck, rules = prepareOwnedDeckValidation(userId, deckId)
        proposedDeck = copy(deck)
        appendCardOccurrence(proposedDeck, cardId, cardArtId, destination)
        context = loadValidationContext(userId, proposedDeck, rules)
        result = validateDeckChange(proposedDeck, context)

        if not result.success:
            rollback
            return result

        // Salva solo la modifica validata; il mazzo può essere ancora incompleto.
        saveDeckChange(proposedDeck)
        commit
        return result


function loadValidationContext(userId, deck, rules):
    // Deduplica gli ID per le letture, mantenendo tutte le copie nel mazzo.
    cardIds = distinctCardIds(deck.entries)
    cards = loadCardDetailsInBatch(cardIds)
    arts = loadArtworkPairsInBatch(deck.entries)
    owned = loadOwnedQuantitiesByCardArtInBatch(userId, deck.entries)
    requireSuccessfulReads(cards, arts, owned)
    requireNonNegativeIntegerOwnedQuantities(owned)
    return { rules, cards, arts, owned }
```

`prepareOwnedDeckValidation` acquisisce nell'ordine il guard condiviso del catalogo, il guard della collezione dell'utente e il lock del mazzo; carica il mazzo verificandone la proprietà, poi carica destinazioni e limiti da `ROUND_CONFIGURATIONS`, completandoli soltanto con le regole di gioco aggiuntive esplicitamente concordate. Verifica presenza, tipi e coerenza delle regole e mantiene lo stesso insieme immutabile per tutta l'operazione: nessun valore mancante viene saltato o sostituito da default. Valori iniziali e trattamento di `EQUIPMENT` restano DA DEFINIRE; formato e distribuzione delle regole aggiuntive non modellate restano da concordare. Il protocollo resta quello proposto nella sezione sulle transazioni.

`inTransaction` garantisce rollback e rilascio lock su errori di autorizzazione, lettura, salvataggio e commit. La risposta positiva segue un commit riuscito. Il batch comprende carte già presenti e carta proposta, con tipo e rarità senza query per riga. Una riga collezione assente vale zero solo dopo una lettura riuscita; valori presenti nulli, negativi o non interi interrompono il flusso. Aggiungere al mazzo non scala copie dalla collezione.

`appendCardOccurrence` aggiunge una copia senza quantità e senza PK scelta dal client. `saveDeckChange` conserva gli ID esistenti, assegna un nuovo ID server alle nuove copie e salva `destination` senza conversioni. Verifica ID unici e destinazioni ammesse, senza imporre unicità su carta e destinazione. Salva righe ed eventuali aggregati nello stesso commit; ogni aggregato deve uguagliare il conteggio delle relative righe. PK, `NOT NULL` e `CHECK` restano applicati dal database. Gli ID nuovi sono generati dal database con auto-incremento; non vengono salvate revisioni o versioni.

### Controlli della modifica e validazione completa

`validateDeckChange` applica le condizioni necessarie al salvataggio dell'aggiunta o dello spostamento. `validateDeck` verifica inoltre tutti i requisiti di completamento. Entrambe riutilizzano `checkCompositionConstraints`, che raccoglie dati e violazioni dei vincoli strutturali, di possesso e dei massimi, senza decidere se il mazzo sia pronto a giocare.

```text
function validateDeckChange(proposedDeck, context):
    return checkCompositionConstraints(proposedDeck, context)


function validateDeck(deck, context):
    result = checkCompositionConstraints(deck, context)
    if result.summary is null:
        return result

    // Controlla il completamento anche se sono già presenti altri errori.
    checkMissingCards(result.summary, context.rules, result.errors)
    checkCommonMinimums(result.summary, context.rules, result.errors)
    result.success = result.errors.isEmpty()
    return result


function checkCompositionConstraints(deck, context):
    // Interrompi solo quando struttura o metadati impediscono i calcoli.
    errors = checkCanonicalEntries(deck.entries)
    if errors is not empty:
        return invalidResult(errors, context, deck)
    errors = checkReferencedCardMetadata(deck.entries, context.cards, context.arts, context.rules)
    if errors is not empty:
        return invalidResult(errors, context, deck)

    totals = summarizeOccurrences(deck.entries, context.cards, context.rules)
    checkDestinationsAndEnergy(deck.entries, context, errors)

    // Possesso e limite copie si verificano sull'intero mazzo, non per round.
    for each (cardId, quantity) in totals.quantityByCard:
        card = context.cards[cardId]
        checkMax(errors, COPY_LIMIT_EXCEEDED, cardId,
                 quantity, context.rules.copyLimit[card.type])
    for each (cardArtPair, quantity) in totals.quantityByCardArt:
        checkMax(errors, INSUFFICIENT_OWNERSHIP, cardArtPair,
                 quantity, context.owned.getOrZero(cardArtPair))

    // Le bozze rispettano i massimi; conteggi esatti e minimi attendono submitDeck.
    checkCountMaximums(totals, context.rules, errors)
    checkPowerBudgets(totals, context.rules, errors)
    checkRarityMaximums(totals, context.rules, errors)
    return validationResult(deck, context, totals, errors)
```

Le funzioni di supporto applicano questi controlli, usando i valori della tabella V2.1:

| Funzione | Responsabilità |
| --- | --- |
| `checkCanonicalEntries` | Forma e tipi dei campi, ID salvati non ripetuti, destinazioni testuali presenti nelle configurazioni e ammesse dal servizio; rifiuta quantità, destinazioni nulle o sconosciute, senza deduplicare le copie |
| `checkReferencedCardMetadata` | Esistenza di carte e arti coerenti, tipo/rarità mappati, potenza valida ed energia interpretabile per la configurazione; trattamento di `EQUIPMENT` DA DEFINIRE |
| `summarizeOccurrences` | Conta ogni riga una volta e somma la sua potenza nel mazzo e nella categoria; nelle creature conta anche il round se la destinazione è compatibile. Produce rarità per categoria, copie per carta e per coppia carta/arte e somma energetica per destinazione; inizializza a zero anche gruppi vuoti e rarità assenti e rileva overflow |
| `checkDestinationsAndEnergy` | Destinazioni esistenti e tipo compatibile; somma energetica ≤ `max_energy` per destinazione, letto da `ROUND_CONFIGURATIONS`; nessun limite dedotto dal nome del round. Regole di `EQUIPMENT` DA DEFINIRE |
| `checkCountMaximums` | Numero per destinazione ≤ `max_cards`; gli ulteriori massimi proposti di 30 carte, 25 creature, 5 equipaggiamenti e 5 creature per round richiedono il raccordo delle regole di gioco |
| `checkPowerBudgets` | Potenza mazzo ≤ 75, creature ≤ 65, equipaggiamenti ≤ 10, creature per round ≤ 14; nessun trasferimento di budget |
| `checkRarityMaximums` | Massimi di rarità per categoria, contando le copie; zero equipaggiamenti leggendari nel primo set. Nessun limite combinato di epiche e leggendarie per round |
| `checkMissingCards` | Aggiunge `CARD_COUNT_MISMATCH` per minimi e conteggi esatti soltanto dalle regole concordate; i target V2.1 proposti non sono ricavati da `max_cards`, che è un massimo |
| `checkCommonMinimums` | Almeno 13 creature comuni e 3 equipaggiamenti comuni, solo nella verifica completa |

`checkMax` aggiunge un errore quando `actual > limit`; i controlli dei minimi quando `actual < expected`. Massimi condivisi e conteggi mancanti impongono insieme le quantità esatte, senza duplicare gli errori sui massimi. Tutti i controlli accumulano gli errori con scope, riferimenti e valori pertinenti. `validationResult` restituisce `success: errors.isEmpty()`, errori e riepilogo; `invalidResult` restituisce lo stesso formato con `success: false` e `summary: null`. Con un riepilogo calcolabile, `validateDeck` controlla il completamento anche in presenza di altre violazioni.

### Verifica completa e collegamento all'utilizzo

```text
function submitDeck(userId, deckId):
    requireAuthenticatedIdentity(userId)

    return inTransaction:
        deck, rules = prepareOwnedDeckValidation(userId, deckId)
        context = loadValidationContext(userId, deck, rules)
        // Verifica completa sui dati correnti, senza salvare un flag di validità.
        result = validateDeck(deck, context)
        commit
        return result


function validateAndFreezeMatchDecks(authenticatedChallenge):
    return inTransaction:
        // Autorizza entrambi i mazzi e blocca le risorse nello stesso ordine di F05.
        decks, rules = prepareMatchDeckValidation(authenticatedChallenge)
        results = []
        for each deck in decks:
            context = loadValidationContext(deck.ownerId, deck, rules)
            results.add(validateDeck(deck, context))

        if any result has success == false:
            rollback
            return validationFailureForAuthorizedRecipients(results)

        // Validazione e snapshot condividono la transazione: niente dati intermedi.
        snapshot = freezeDecksAndReferencedCardData(decks, rules, results)
        saveMatchSnapshotExactlyOnce(authenticatedChallenge, snapshot)
        commit
        return snapshotReference(snapshot)
```

`submitDeck` non persiste un flag «valido» né riserva carte; ripeterlo non duplica effetti. Il successo riguarda solo la lettura coerente appena eseguita. F07 deve rivalidare **entrambi** i mazzi all'accettazione/avvio effettivo e salvare lo snapshot nella stessa transazione: un precedente `submitDeck` esterno non basta.

`prepareMatchDeckValidation` verifica il contesto sfida, acquisisce il guard del catalogo, poi i guard delle collezioni per ID utente crescente e i lock dei mazzi per ID crescente. Carica entrambi i mazzi autorizzati e acquisisce una sola volta le configurazioni dei round dal database e le eventuali regole aggiuntive concordate, verificandole come nel flusso F05 e usando lo stesso insieme per entrambi i mazzi. `saveMatchSnapshotExactlyOnce` richiede un meccanismo di unicità/idempotenza ancora DA DEFINIRE; il nome della funzione esprime la garanzia richiesta, non un meccanismo già disponibile.

Il flusso F07 è un'integrazione **proposta**, non un contratto già approvato. Il contesto sfida autorizza ogni partecipante all'uso del proprio mazzo e filtra gli errori per destinatario, senza esporre collezioni avversarie. Autorizzazioni, persistenza e chiave univoca contro doppi avvii restano da concordare con F07. Lo snapshot conserva assegnazioni, quantità, dati delle carte e valori delle regole necessari al motore, senza introdurre revisioni/versioni dei mazzi o del catalogo. Simulazione e storico usano questi dati immutabili: i soli ID carta non proteggono da successive variazioni delle statistiche.

## Transazioni, scambi e richieste concorrenti

**Protocollo proposto**, da concordare e applicare in comune con F04/F06/F07/F10:

1. Acquisire il guard condiviso sul catalogo e leggere configurazioni dei round e regole correnti dopo i lock, mantenendole immutabili per l'operazione; chi modifica carte o metadati prende lo stesso guard in esclusiva. La primitiva del guard è DA DEFINIRE e deve coordinare tutti i processi applicativi.
2. Guard stabile per utente, ad esempio la riga `USERS`, per serializzare variazioni della collezione e controlli. Protegge anche righe `COLLECTIONS` assenti, che il lock delle sole righe esistenti non coprirebbe.
3. Ordine unico: guard catalogo → utenti per ID numerico crescente → mazzi per ID crescente → annunci e trattative, se coinvolti, per ID crescente → dettagli in ordine deterministico. Ogni flusso prende i guard necessari prima di modificare le risorse protette.
4. Leggere valori correnti **dopo** l'acquisizione dei guard e mantenerli fino a commit/rollback, con isolamento `READ COMMITTED` come nel protocollo v1.1. La primitiva comune del guard catalogo resta DA DEFINIRE; non riutilizzare una snapshot DB anteriore all'attesa dei lock.
5. Conflitto/timeout: rollback e retry limitato soltanto se sicuro; non ripetere alla cieca un'aggiunta dopo un esito di commit sconosciuto.

Il lock del mazzo evita aggiornamenti persi; quello della collezione coordina possesso e scambi; quello del catalogo evita variazioni dei metadati a metà verifica. Il protocollo usa lock e letture correnti, senza tracciamento di revisioni/versioni. Il DBML non implementa il protocollo: il solo lock del mazzo non è sufficiente e tutti i flussi interessati devono rispettare gli stessi guard. Le letture rispettano il [protocollo transazionale comune v1.1](Database/database-schema-v1.1.md#protocollo-transazionale-comune); la distribuzione delle sole regole aggiuntive non modellate resta DA DEFINIRE.

### Effetti degli scambi

I lock DB brevi sono distinti dalle prenotazioni persistenti. La proposta base usa `owned_replica`, non sottrae `tradable_replica` e non riserva permanentemente carte perché inserite nel mazzo. Non somma quantità di mazzi differenti: riutilizzare le stesse copie in più mazzi è una **proposta da confermare**.

Proposta minima: uno scambio può lasciare il mazzo non idoneo; le righe restano, con rivalidazione al controllo/uso successivo. Un controllo pre-scambio può informare sui mazzi interessati, ma non sostituisce la verifica atomica al trasferimento e non introduce automaticamente un veto. Rifiuto dello scambio o prenotazione delle copie dei mazzi sono alternative **DA DEFINIRE con F06**.

La v1.1 modella `TRADE_TOKEN_RESERVATIONS` e `TRADE_CARD_RESERVATIONS`, collegati alle trattative e alla versione accettata. Occorre decidere se le copie prenotate restano utilizzabili per giocare: soltanto allora si può definire `availableForPlay`. Non inferirla dal solo `tradable_replica` o dal possesso. Prenotare non cambia possesso o composizione del mazzo.

Per la validazione sul possesso, uno scambio committato prima dello snapshot F07 modifica il possesso da verificare e può impedirne l'avvio. Se lo snapshot committa prima, la partita usa la composizione fissata; lo scambio successivo incide sugli utilizzi futuri. Questo comportamento e l'eventuale lock delle carte per tutta la partita richiedono accordo F06/F07.

### Cambiamenti di regole/catalogo e retry

Ogni utilizzo futuro ricalcola la validazione usando configurazioni dei round, regole aggiuntive concordate, catalogo e possesso per variante correnti. Nessuna cache di validità viene riutilizzata per autorizzare il gioco. Le partite già fissate mantengono dati delle carte e valori delle regole nello snapshot proposto. Nessuna conversione automatica dei mazzi o deroga alle nuove regole è decisa.

Per le aggiunte non è prevista deduplicazione automatica delle richieste: ogni ripetizione viene trattata come una nuova aggiunta e rivalidata. Se supera un limite viene rifiutata; se tutti i vincoli restano rispettati può salvare un'altra copia con un nuovo ID. La validazione garantisce i vincoli del mazzo, non il riconoscimento dei retry. In caso di esito incerto il client deve rileggere la composizione prima di proporre un nuovo invio; l'utente può rimuovere una copia indesiderata tramite il suo `entryId`, secondo la proposta di riparazione. Questo comportamento non risolve i doppi avvii di partita: la protezione dello snapshot F07 resta separata e DA DEFINIRE.

## Criteri di accettazione e casi limite

Scenari **previsti, non eseguiti**; ID locali F05, non issue assegnate. Preparare fixture con potenza fissa entro fascia e, salvo il vincolo in esame, dati validi.

| ID | Dato / quando | Risultato previsto |
| --- | --- | --- |
| F05-01 | Mazzo vuoto; aggiunta comune posseduta, energia/potenza 1, round 1 | `addCardToDeck` salva la modifica; `submitDeck` restituisce conteggi incompleti e minimi mancanti |
| F05-02 | 25 creature in cinque gruppi + 5 equipaggiamenti distinti, tutti i vincoli rispettati | `submitDeck` positivo, nessuna violazione |
| F05-03 | 29/31 carte; oppure 30 con 24 creature e 6 equipaggiamenti | `submitDeck` fallisce; un'aggiunta che supera un massimo viene rifiutata prima del salvataggio |
| F05-04 | 25 creature ripartite 4/6/5/5/5 | Errori sui primi due gruppi, indipendenti dal totale |
| F05-05 | Somma energetica uguale/sopra/sotto `max_energy` di una destinazione | Uguale/inferiore ammesse; superiore respinta anche se ogni carta singolarmente rientra nel limite |
| F05-06 | Potenza creature 65/66 con gruppi ≤ 14 e totale ≤ 75 | 65 ammesso, 66 respinto senza trasferimenti di budget |
| F05-07 | Potenza equipaggiamenti 10/11 con creature sotto 65 | 10 ammesso, 11 respinto anche con punti creature inutilizzati |
| F05-08 | Gruppo potenza 14/15, totale creature ≤ 65 | 14 ammesso, 15 respinto con riferimento al round |
| F05-09 | Creature: comuni 13/12, rare 8/9, epiche 3/4, leggendarie 1/2 | Soglie ammesse; minimi verificati da `submitDeck`, massimi anche durante l'aggiunta |
| F05-10 | Un'epica + una leggendaria nello stesso gruppo, oppure due copie della stessa epica, entro energia, potenza, possesso, copie e massimi di rarità per categoria | Nessun rifiuto per la sola somma epiche + leggendarie nel round; modifica ammessa se tutti gli altri vincoli sono rispettati |
| F05-11 | Due copie creatura nello stesso gruppo o divise fra round; poi terza | Due copie salvabili con ID distinti se gli altri vincoli sono rispettati; la terza viene rifiutata indipendentemente dalla destinazione |
| F05-12 | Equipaggiamenti: comuni 3/2, rare 2/3, epiche 1/2, leggendarie 0/1; seconda copia uguale | Vincoli separati dalle creature; leggendaria e duplicato respinti |
| F05-13 | Due copie della stessa arte fra round con possesso 1; oppure solo un'altra arte posseduta | Errore sul possesso della variante richiesta; assenza vale zero dopo lettura riuscita, senza sostituzione implicita |
| F05-14 | Campo quantità inviato su una riga, occorrenza malformata, creatura in `EQUIPMENT` oppure equipaggiamento in `ROUND1` | Errori strutturali/assegnazione, nessuna scrittura |
| F05-15 | Carta assente, metadati mancanti, tipo/rarità sconosciuti, potenza fuori fascia o regole indisponibili | Nessuna idoneità, nessun default a zero |
| F05-16 | Identità assente o mazzo altrui | Accesso rifiutato senza esposizione di collezioni né scritture |
| F05-17 | Due aggiunte concorrenti raggiungono terza copia o sesto posto nel round | Esito seriale, nessun superamento salvato né aggiornamento perso |
| F05-18 | Scambio sottrae copie dopo `submitDeck`, prima dell'avvio | F07 rivalida il possesso corrente; vecchio successo insufficiente |
| F05-19 | Scambio e snapshot concorrenti; catalogo/regole cambiano durante verifica | Esito equivalente a un ordine seriale; nessuna lettura mista; snapshot immutabile |
| F05-20 | Variazione possesso, energia, potenza, rarità o budget dopo successo | Nuovo controllo sui dati correnti |
| F05-21 | Guasto scrittura/commit, conflitto o richiesta ripetuta con esito incerto | Nessuna scrittura parziale; rilettura dopo esito incerto. Una richiesta ripetuta può salvare una seconda copia se consentita, oppure essere rifiutata dai vincoli; nessuna garanzia di deduplicazione |
| F05-22 | Bozza invalidata da scambio/carta assente; rimozione di copie | Riparazione consentita nella proposta, diagnostica residua, nessun flag permanente |
| F05-23 | Fazioni tutte uguali o miste; budget non interamente spesi | Nessun vincolo aggiuntivo né obbligo di spendere tutti i 75 punti |
| F05-24 | Creatura in `ROUND1` … `ROUND5` oppure equipaggiamento in `EQUIPMENT` | Destinazione testuale persistita senza conversioni; PK costituita dal solo ID della riga |
| F05-25 | Stessa creatura in destinazioni differenti oppure seconda copia nella stessa destinazione | Due righe con ID distinti in entrambi i casi, entro i vincoli V2.1; nessuna unicità sulla terna e nessuna deduplicazione |
| F05-26 | Modifica righe con aggregati persistiti; guasto durante una delle scritture | Aggregati uguali al conteggio delle righe dopo commit, oppure rollback completo; rimuovere una carta elimina una riga |
| F05-27 | Mazzo incompleto con riepilogo calcolabile e un massimo già superato; richiesta di `submitDeck` | Sono restituiti sia il massimo superato sia i conteggi e i minimi mancanti; nessun controllo di completamento omesso a causa del primo errore |
| F05-28 | Due occorrenze di una comune posseduta nella stessa `ROUND1`, entro tutti gli altri massimi | `validateDeckChange` accetta l'aggiunta e vengono salvate due righe con ID distinti; conteggi e potenza includono entrambe le copie |
| F05-29 | Destinazione nulla, numerica, sconosciuta oppure con grafia diversa dai sei valori ammessi | Richiesta rifiutata dal servizio; `NOT NULL` impedisce valori nulli e la FK a `ROUND_CONFIGURATIONS.round_name` impedisce destinazioni inesistenti; non esiste un `CHECK` sui sei nomi |
| F05-30 | Rimozione o spostamento di una copia per `entryId`, anche con un'altra copia identica nello stesso gruppo; ID appartenente a un altro mazzo | Si modifica solo l'occorrenza autorizzata, preservando l'altra; l'ID di un altro mazzo viene rifiutato senza scritture |

Fixture illustrativa F05-02: 25 creature comuni differenti da 1 punto e con energia 1, cinque per round; cinque equipaggiamenti comuni differenti da 1 punto, tutti posseduti nella precisa arte richiesta. Le configurazioni sintetiche della fixture ammettono cinque carte e almeno cinque punti energia per destinazione; non sono seed o valori iniziali approvati. Isolare le frontiere con fixture ammissibili: un gruppo di sole comuni, per esempio, non può raggiungere 14 punti dato il massimo di 2 per carta.

## Decisioni aperte e impatto

| Decisione | Impatto prima dell'implementazione |
| --- | --- |
| Conferma V2.1, soglie energia e accesso ai round successivi | Regole attive e prove definitive |
| Statistiche, potenza fissa, codici tipi/rarità | Catalogo interpretabile senza deduzioni arbitrarie |
| Migrazione delle vecchie quantità e versioni del modello | Distribuzione ai round da fornire esplicitamente; il vecchio `deck_replica` non indica i round e non può essere cancellato dai dati esistenti senza una trasformazione verificata |
| Riutilizzo copie in più mazzi; carte prenotate | Possesso/disponibilità per giocare e rapporto con F06 |
| Avviso/veto pre-scambio, prenotazioni o lock partita | Comportamento F06/F07, senza introdurre politiche economiche implicite |
| Significato dei flag di eliminazione e di attività delle espansioni | Legalità delle carte dopo variazioni del catalogo |
| Valori iniziali dei round e regole aggiuntive | Parametri in `ROUND_CONFIGURATIONS`; raccordo V2.1, energia di `EQUIPMENT` e distribuzione delle regole non modellate |
| Protocollo atomico e snapshot | Coerenza fra scambi, avvio e pubblicazione regole |
| Riparazione, API, payload, errori, retry | Integrazione FrontEnd e recupero delle bozze |

La formula del danno non blocca questi controlli di composizione; simulazione ed effetti degli equipaggiamenti restano nelle rispettive fonti. Non sono inventati assegnatari, issue, PR, endpoint o approvazioni.


## Collegamenti

- [Interfaccia mazzi](../FrontEnd/gestione-mazzi.md)
- [Regolamento Versione 2.1](../Digital/regolamento-base.md)
- [Accesso](accesso-utenti.md)
- [Collezione](collezione-carte.md)
- [Scambi](scambi-mercato.md)
- [Sfide e simulazione](sfide-simulazione.md)
- [Gestione amministrativa](gestione-amministrativa.md)
- [Indice BackEnd](README.md)
- [Indice generale e stato delle funzionalità](../README.md)
- [Modello di funzionalità](../modello-funzionalita.md)
