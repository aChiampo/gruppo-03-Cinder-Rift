# Scambi e marketplace

ID: **F06**. Perimetro: confermato, scambi pubblici e privati di carte e token.
Specifica: **in approfondimento**. Implementazione: **non iniziata**.
Responsabile: **DA DEFINIRE (Software Developer)**.
Ultimo aggiornamento significativo: **2026-11-05**, allineamento alla base di dati v1.0 su richiesta dell'utente. Verifiche applicative: **non eseguite**.
Fonte dati e invarianti: [specifica canonica v1.0](Database/database-schema-v1.0.md#sistema-di-scambi-e-negoziazioni).

## Obiettivo, perimetro e dipendenze

Questa pagina descrive il contratto tecnico F06 in conformità alla base di dati v1.0. La specifica del database è autonoma e prevale per struttura e invarianti; API ed errori qui proposti richiedono ancora accordo.

A è il pubblicatore e B il rispondente o destinatario privato. Gli identificativi sono `int`; le PK autonome sono auto-incrementali. Destinatari e partecipanti usano `USERS.id`, senza un secondo identificativo pubblico. Le FK e le chiavi composte ereditano gli interi e non generano valori. Token e crediti sono una sola valuta.

La pubblicazione pubblica non prenota risorse. Gli hold appartengono sempre a una `CURRENT_NEGOTIATIONS` esistente e a un'offerta accettata. Una modifica rilascia tutti gli hold, incrementa la versione e azzera entrambi i consensi: non mantiene prenotazioni di lati non accettanti. Non sono necessari hold di annuncio senza trattativa né uno stato persistito separato di “lato già prenotato”.

Dipendenze: F01 per identità, F03/F04 per disponibilità, F05/F07 per mazzi e gioco, F10 per catalogo e conversioni. Nessuna integrazione è già implementata.

## Flussi

La durata è due giorni dalla creazione. `expires_at` è immutabile: avvio, controproposte e ripubblicazione non rinnovano la scadenza.

## Annuncio pubblico

1. **Preparazione:** A definisce offerte uscenti e richieste, precise carte/arti/quantità, token e negoziabilità. Il backend valida forma e catalogo; una disponibilità letta qui non costituisce una riserva.
2. **Pubblicazione:** crea `ORIGINAL_ANNOUNCES` con `is_private = false`, `PUBLISHED`, proposta originale e scadenza. Nessuna trattativa e nessun hold. Visibilità solo dopo il commit, finché pubblicato e non scaduto.
3. **Consultazione:** B legge senza acquisire l'annuncio. Può avviare sull'originale, accettarlo direttamente o inviare una prima controproposta se negoziabile.
4. **Avvio:** acquisisce condizionalmente l'annuncio valido, crea una trattativa univoca e imposta `PENDING` nella stessa transazione; il ramo scelto determina versione, flag e hold.
5. **Trattativa:** ciascun partecipante può modificare soltanto se negoziabile. Una modifica valida sostituisce l'intera proposta, rilascia entrambi gli insiemi di hold, incrementa versione e azzera i flag atomicamente. Le nuove offerte verranno prenotate alle nuove accettazioni.
6. **Chiusura:** doppio consenso e hold esatti completano atomicamente. Il rifiuto pubblico ancora valido elimina trattativa e hold e ripubblica l'originale senza prenotazioni. La scadenza porta a `EXPIRED`; l'annullamento del pubblicatore porta a `CANCELLED` se ancora valido. Nessun rinnovo del termine.

### Prima risposta e prenotazioni

| Prima risposta di B | Versione e consensi | Effetti atomici |
| --- | --- | --- |
| Avvio sull'originale senza consenso di B | Versione `1`; A vero, B falso | Crea la trattativa, verifica e prenota A, poi ne imposta il consenso |
| Accettazione diretta dell'originale | Versione `1`; entrambi veri | Prenota entrambi, trasferisce, registra storico e chiude nello stesso commit |
| Prima controproposta negoziabile | Versione finale `2`; entrambi falsi | Compone avvio e modifica senza esporre una versione intermedia committata; nessun hold |

Il consenso alla proposta originale non passa alla controproposta. Se un avvio o un'accettazione diretta fallisce per risorse insufficienti, l'intera risposta viene annullata: l'annuncio resta pubblicato senza hold. La controproposta non garantisce disponibilità fino all'accettazione.

### Risposte simultanee

L'acquisizione cambia `PUBLISHED → PENDING` solo se pubblico e non scaduto al cutoff. Deve acquisire esattamente una riga e creare la trattativa nello stesso commit, insieme agli hold o trasferimenti richiesti dal ramo. L'unicità di `CURRENT_NEGOTIATIONS.announce_id` consente una sola trattativa. Il perdente riceve un conflitto senza effetti parziali e rilegge lo stato.

## Richiesta privata

1. A identifica B tramite il suo intero `USERS.id`; il backend verifica utenti distinti e amicizia `accepted`.
2. Crea annuncio `PENDING` e trattativa privata versione `1`, prenota A e soltanto dopo pone `p_acceptance = true`; B resta falso. Tutto nello stesso commit o rollback.
3. B può accettare o rifiutare. Se negoziabile, una modifica rilascia tutti gli hold, incrementa versione e azzera i flag; ciascun nuovo consenso prenota il lato corrispondente.
4. Il doppio consenso completa; un rifiuto valido chiude con `CANCELLED`, altrimenti con `EXPIRED`. La richiesta non diventa pubblica e il destinatario non cambia.

## Attori, autorizzazioni e offerte

L'identità deriva dalla sessione. Solo A e B leggono o modificano la trattativa; B non può acquisire il proprio annuncio. Il solo ID numerico non autorizza l'accesso e non è assunta una gerarchia di ruoli. Disponibilità dettagliata e hold sono visibili solo al proprietario. Il client non crea direttamente prenotazioni o flag autorevoli.

`P` indica A e `R` B: ogni offerta è ciò che il lato cederà. `original_r_*` è la richiesta iniziale di A. I JSON di carte sono obbligatori con default `[]`, anche vuoti.

```typescript
type CardOffer = {
    cardId: number;    // Integer CARDS.id.
    cardArtId: number; // Integer CARDS_ARTS.id_art.
    quantity: number;  // Positive integer.
};
```

Ogni coppia compare una sola volta. Il backend controlla ID interi validi, arte appartenente alla carta, quantità positive, importi token non negativi e disponibilità al momento della prenotazione. Non sono FK annidate e non si sostituisce implicitamente un'arte. Token e `offer_version` restano `bigint`: serializzazione a 64 bit e payload definitivi sono da concordare.

## Modello dati: annunci, negoziazioni e prenotazioni

Le cinque tabelle seguenti derivano dalla v1.0. Gli ID autonomi si generano nel database; i riferimenti storici e le componenti delle PK degli hold non si auto-incrementano. Non è modellato un registro dei movimenti o delle revisioni intermedie.

### ORIGINAL_ANNOUNCES

Conserva annuncio e proposta iniziale. Pubblicare non riserva beni e non crea una trattativa. I campi `original_*` non cambiano; dopo un rifiuto pubblico valido si può ripubblicare l'originale senza hold e senza rinnovare la scadenza.

| Campo | Tipo | Significato |
| --- | --- | --- |
| `id` | `int` | Identificativo auto-incrementale dell’annuncio; PK. |
| `publisher_id` | `int` | ID intero di A; FK a USERS.id. |
| `created_at` | `timestamptz` | Creazione autorevole lato server. |
| `expires_at` | `timestamptz` | Scadenza immutabile a due giorni dalla creazione. |
| `status` | `varchar` | PUBLISHED, PENDING, EXPIRED, COMPLETED o CANCELLED; CHECK sul dominio. |
| `is_private` | `boolean` | Distingue richiesta privata da annuncio pubblico. |
| `is_negotiable` | `boolean` | Abilita la sostituzione della proposta corrente. |
| `original_p_cards_offer` | `jsonb` | Carte/arti e quantità inizialmente offerte da A. |
| `original_p_token_offer` | `bigint` | Token inizialmente offerti da A. |
| `original_r_cards_offer` | `jsonb` | Carte/arti e quantità inizialmente richieste a B. |
| `original_r_token_offer` | `bigint` | Token inizialmente richiesti a B. |

### CURRENT_NEGOTIATIONS

Conserva soltanto la proposta corrente e viene eliminata quando la trattativa termina. `publisher_id` e i due flag sono copie intenzionali: il backend ne mantiene la coerenza con l’annuncio. Le modifiche non cambiano partecipanti, scadenza o negoziabilità.

| Campo | Tipo | Significato |
| --- | --- | --- |
| `id` | `int` | Identificativo auto-incrementale della trattativa; PK. |
| `announce_id` | `int` | FK obbligatoria e univoca all’annuncio: al massimo una trattativa attiva. |
| `publisher_id` | `int` | ID intero di A; deve coincidere con il pubblicatore dell’annuncio. |
| `responder_id` | `int` | ID intero di B, fisso per la trattativa. |
| `is_private` | `boolean` | Copia coerente e immutabile del flag dell’annuncio. |
| `is_negotiable` | `boolean` | Copia coerente e immutabile del flag dell’annuncio. |
| `current_p_cards_offer` | `jsonb` | Carte/arti e quantità attualmente offerte da A. |
| `current_p_token_offer` | `bigint` | Token attualmente offerti da A. |
| `current_r_cards_offer` | `jsonb` | Carte/arti e quantità attualmente offerte da B. |
| `current_r_token_offer` | `bigint` | Token attualmente offerti da B. |
| `offer_version` | `bigint` | Parte da 1 e cresce a ogni modifica; la prima controproposta composta all’avvio produce 2. |
| `p_acceptance` | `boolean` | Consenso di A alla versione corrente, con hold corrispondenti; default false. |
| `r_acceptance` | `boolean` | Consenso di B alla versione corrente, con hold corrispondenti; default false. |

### TRADES_COMPLETED

Conserva una fotografia definitiva per il periodo di conservazione previsto. L’unicità di `announce_id` impedisce un secondo completamento; l’assenza di FK all’annuncio permette di mantenerla dopo la pulizia. Le FK verso gli utenti restano nel DBML: cancellazione fisica e anonimizzazione sono ancora da definire.

| Campo | Tipo | Significato |
| --- | --- | --- |
| `id` | `int` | Identificativo auto-incrementale dello scambio; PK. |
| `announce_id` | `int` | ID intero storico univoco, senza FK all’annuncio. |
| `is_private` | `boolean` | Visibilità originaria, conservata anche dopo la pulizia dell’annuncio. |
| `publisher_id` | `int` | ID intero di A. |
| `responder_id` | `int` | ID intero di B. |
| `final_p_cards_offer` | `jsonb` | Carte/arti e quantità effettivamente cedute da A. |
| `final_p_token_offer` | `bigint` | Token effettivamente ceduti da A. |
| `final_r_cards_offer` | `jsonb` | Carte/arti e quantità effettivamente cedute da B. |
| `final_r_token_offer` | `bigint` | Token effettivamente ceduti da B. |
| `created_at` | `timestamptz` | Creazione del record definitivo. |
| `completed_at` | `timestamptz` | Completamento effettivo del trasferimento. |

### TRADE_TOKEN_RESERVATIONS

Conserva l’hold uscente corrente per un partecipante, con PK `(negotiation_id, id_user)`. Non modifica il saldo: limita la disponibilità spendibile fino al consumo o rilascio.

| Campo | Tipo | Significato |
| --- | --- | --- |
| `negotiation_id` | `int` | Parte della PK e FK alla trattativa con delete: cascade. |
| `id_user` | `int` | Parte della PK; FK a INVENTORY.id_user, proprietario dei token. |
| `offer_version` | `bigint` | Versione della proposta coperta dall’hold; solo per il lato accettato; una modifica elimina gli hold di entrambe le parti. |
| `token_amount` | `bigint` | Importo uscente positivo; nessuna riga per zero token. |
| `created_at` | `timestamptz` | Creazione della prenotazione. |
| `expires_at` | `timestamptz` | Stessa scadenza immutabile dell’annuncio. |

### TRADE_CARD_RESERVATIONS

Conserva gli hold uscenti per variante, con PK `(negotiation_id, id_user, id_card, id_card_art)`. Il riferimento composto a `CARDS_ARTS.(id_card, id_art)` garantisce che l’arte appartenga alla carta. Possesso e quantità sono verificati sotto i guard delle risorse.

| Campo | Tipo | Significato |
| --- | --- | --- |
| `negotiation_id` | `int` | Parte della PK e FK alla trattativa con delete: cascade. |
| `id_user` | `int` | Parte della PK e FK all’utente proprietario. |
| `id_card` | `int` | Parte della PK e della FK composta verso CARDS_ARTS. |
| `id_card_art` | `int` | Parte della PK; identifica l’arte tramite CARDS_ARTS.id_art. |
| `offer_version` | `bigint` | Versione della proposta coperta dall’hold; solo per il lato accettato; una modifica elimina gli hold di entrambe le parti. |
| `quantity` | `int` | Copie uscenti positive della precisa variante; verifica backend. |
| `created_at` | `timestamptz` | Creazione della prenotazione. |
| `expires_at` | `timestamptz` | Stessa scadenza immutabile dell’annuncio. |

## Dati e invarianti delle prenotazioni

`CARDS_ARTS` usa PK `(id_card, art_code)`, `id_art` intero auto-incrementale univoco e unicità `(id_card, id_art)`. Collezione, mazzi e hold verificano la coppia carta/arte. Il possesso è la tripla `COLLECTIONS(id_user, id_card, id_card_art)`. Gli hold non riferiscono la riga volatile di possesso: quantità e proprietà sono controlli backend.

Per ogni trattativa attiva:

1. Proprietario degli hold fra i due partecipanti, versione corrente e scadenza originale.
2. Un lato accettato ha esattamente gli hold dell'offerta uscente: riga token solo per importo positivo e righe per precise coppie carta/arte.
3. Un lato non accettato non conserva hold. Un'offerta vuota accettata è rappresentata dal flag senza righe vuote.
4. Gli hold non scaduti complessivi non superano saldo o copie scambiabili.
5. Una modifica elimina gli hold di entrambe le parti e richiede nuovi consensi.

## Prenotazioni e calcolo della disponibilita

Pubblicare non riserva. Avvio sull'originale e invio privato prenotano A; una prima controproposta finisce senza hold. Ogni accettazione riserva l'esatto lato uscente della versione corrente.

```text
reservedTokens(user) = SUM(token_amount WHERE id_user = user AND expires_at > cutoff)
availableTokens(user) = token_balance - reservedTokens(user)

reservedCopies(user, card, art) = SUM(quantity for that triple WHERE expires_at > cutoff)
availableTradableCopies(user, card, art) = tradable_replica - reservedCopies(user, card, art)
```

Somme vuote valgono zero. Una tripla assente vale zero dopo una lettura riuscita. Si contano tutti gli hold non scaduti: incoerenze impediscono l'operazione, senza ignorare record per liberare artificialmente risorse.

Prenotare non modifica `token_balance`, `owned_replica` o `tradable_replica`. Il completamento consuma i propri hold e preserva quelli altrui, senza sottrarre due volte la propria riserva o finanziare l'uscita con i beni attesi in entrata.

## Stati, transizioni e scadenza

| Evento | Stato e flag | Hold |
| --- | --- | --- |
| Pubblicazione | `PUBLISHED`, nessuna trattativa | Nessuno |
| Avvio originale | `PENDING`, versione 1, A vero | Solo A |
| Prima controproposta | `PENDING`, versione 2, entrambi falsi | Nessuno |
| Invio privato | `PENDING`, versione 1, A vero | Solo A |
| Modifica | Versione incrementata, entrambi falsi | Tutti rilasciati |
| Accettazione | Versione attesa, relativo flag vero | Esatto lato accettato |
| Rifiuto pubblico valido | `PUBLISHED`, trattativa eliminata | Tutti rilasciati |
| Rifiuto privato valido | `CANCELLED` | Tutti rilasciati |
| Annullamento del pubblicatore | `CANCELLED`, o `EXPIRED` se scaduto | Tutti rilasciati |
| Scadenza | `EXPIRED` | Non contano già al cutoff; poi eliminati |
| Completamento | `COMPLETED`, trattativa eliminata | Consumati con trasferimenti e storico |
| Invalidazione forzata | Annuncio aperto annullato | Tutti rilasciati prima della conversione |

Il cutoff viene letto dopo i lock; serve `expires_at > cutoff`, mentre l'uguaglianza è già scadenza. Il worker non è l'autorità della scadenza e non riapre stati terminali. Il default a due giorni non impedisce da solo scritture applicative errate.

## Transazioni, concorrenza e richieste ripetute

### Guard comuni e ordine dei lock

Applicare il [protocollo transazionale comune v1.0](Database/database-schema-v1.0.md#protocollo-transazionale-comune): guard catalogo condiviso, utenti per ID numerico crescente, eventuali mazzi, annunci, trattative e dettagli in ordine deterministico. Le cancellazioni di catalogo usano il guard esclusivo; la primitiva comune fra processi resta da definire.

Le righe stabili `USERS` serializzano anche risorse ancora assenti. Usare `READ COMMITTED`, letture aggiornate dopo i lock e `clock_timestamp()` per il cutoff dopo l'attesa. Se servono altri guard si annulla e si ripianifica. Nessuna transazione resta aperta in attesa della persona.

### Accettazione e modifica atomiche

Accettare autorizza il partecipante, verifica stato, versione e scadenza, riserva il suo lato e pone il flag nello stesso commit. Un retry già accettato verifica gli hold esatti e non li duplica; una mancanza di risorse annulla soltanto la richiesta corrente e preserva lo stato precedentemente committato.

Modificare richiede negoziabilità e versione attesa; valida la proposta, rilascia entrambi gli insiemi di hold, incrementa versione e azzera i flag atomicamente. Nessuna prenotazione della nuova proposta viene conservata prima dei nuovi consensi. Se la modifica fallisce, proposta, versione, flag e hold precedenti restano intatti.

### Completamento e recupero

1. Verificare identità, stato, cutoff, versione, doppio consenso e hold esatti non scaduti.
2. Verificare possesso, scambiabilità, saldo e copertura delle altre prenotazioni: un ID d'arte valido non dimostra possesso.
3. Consumare gli hold e trasferire carte/token; creare triple ricevute mancanti ed eliminare quelle a possesso finale zero, preservando gli altri hold vivi.
4. Inserire lo storico unico per `announce_id`, impostare `COMPLETED` ed eliminare la trattativa.
5. Committare tutti gli effetti oppure annullarli. Le notifiche diventano inviabili dopo il commit; consegna affidabile da definire.

La cascata dalla trattativa elimina gli hold residui, ma non sostituisce i controlli. Dopo un commit incerto si rilegge lo stato e lo storico autorizzato per `announce_id`, senza trasferire di nuovo. PK e versioni rendono ripetibili le accettazioni; la creazione di annunci non ha una deduplicazione generale già modellata.

## Operazioni esterne e invalidazione forzata

Acquisti, regali, scambi, conversioni e riduzioni di scambiabilità rispettano gli stessi guard e le somme vive. Una riduzione ordinaria che intaccherebbe hold viene rifiutata.

Cancellare logicamente una carta o espansione richiede il guard esclusivo, identificazione di annunci e trattative interessati, lock di tutti i partecipanti, annullamento e rilascio di tutti gli hold anche della controparte. Solo dopo si convertono copie e si puliscono collezione e mazzi. I pubblici senza trattativa non hanno hold, ma vanno comunque invalidati se coinvolgono il catalogo cancellato. Lotti, tassi, frammenti e ripresa idempotente restano aperti.

Le prenotazioni non cambiano possesso o righe dei mazzi. I mazzi scelgono già l'arte in `DECKS_CARDS.id_card_art`; la giocabilità delle copie prenotate resta da concordare, così come gli effetti dei trasferimenti sui mazzi e i lock per l'intera partita.

## Messaggi, statistiche e pulizia

Il servizio separato correla i messaggi all'annuncio prima della risposta e alla trattativa durante la negoziazione; le cinque tabelle non contengono testo dei messaggi. Notifiche e recupero della consegna seguono il commit.

Aggregazione prima della rimozione, deduplicazione dei conteggi, frequenza e lotti sono da definire. Non è modellato un registro di aggregazione o un timestamp di annullamento; `CANCELLED` non permette di ricostruirne il momento esatto.

Prima di eliminare un annuncio si chiudono trattative e hold. La FK `CURRENT_NEGOTIATIONS.announce_id` impedisce orfani. Lo storico concluso non dipende dall'annuncio, ma mantiene le FK utenti. Eliminare una trattativa privata non conclusa rimuove B dall'annuncio: non è garantita una consultazione storica per il destinatario né l'autorizzazione ricavabile da messaggi separati.

## Endpoint necessari

Proposte da concordare, **non API implementate**. Prefisso `/api`, risorse nominali e ID interi; status, payload, paginazione, errori e serializzazione degli importi restano da definire.

| Metodo e percorso proposto | Responsabilità |
| --- | --- |
| `POST /api/announcements` | Pubblico: annuncio senza hold. Privato: destinatario `USERS.id`, amicizia, annuncio/trattativa e hold di A atomicamente |
| `GET /api/announcements?scope=marketplace` | Soli pubblici `PUBLISHED` non scaduti, senza acquisizione |
| `GET /api/announcements?scope=mine` | Annunci del pubblicatore finché conservati |
| `GET /api/announcements/{announceId}` | Proposta originale e stato con autorizzazioni; `PENDING` solo ai partecipanti |
| `POST /api/announcements/{announceId}/negotiations` | Avvio originale, accettazione diretta o prima controproposta secondo versioni e hold sopra |
| `GET /api/negotiations?scope=mine` | Trattative attive cui partecipa l'utente |
| `GET /api/negotiations/{negotiationId}` | Proposta, versione, consensi e scadenza; disponibilità privata solo al proprietario |
| `PUT /api/negotiations/{negotiationId}/offers/current` | Sostituzione completa, rilascio di tutti gli hold, versione incrementata e flag azzerati |
| `POST /api/negotiations/{negotiationId}/acceptances` | Prenota il lato della versione attesa; il secondo consenso completa |
| `POST /api/negotiations/{negotiationId}/rejections` | Rilascia tutto; pubblico valido ripubblicato senza hold, privato cancellato, scaduto `EXPIRED` |
| `POST /api/announcements/{announceId}/cancellations` | Il pubblicatore annulla, rilasciando hold ed eliminando l'eventuale trattativa |
| `GET /api/trades?scope=mine`, `GET /api/trades/{tradeId}` | Storico concluso ai soli partecipanti |
| `GET /api/users/{userId}/tradable-cards` | Varianti visibili e disponibilità al netto degli hold, senza saldo o prenotazioni private altrui |

Nessun CRUD pubblico degli hold. Le operazioni di messaggistica sono separate; pulizia e scadenza sono processi interni. Identità, partecipanti e autorizzazioni non sono campi client autorevoli.

## Errori e comportamento del client

Dopo conflitto, scadenza o commit incerto il client rilegge lo stato. Mostra saldo totale/riservato/disponibile solo dal server e distingue le arti. Categorie da distinguere: identità/proprietà, arte incoerente, offerta invalida, versione superata, annuncio occupato o terminale, disponibilità insufficiente e guasto transazionale. Nessuna esposizione di risorse altrui o scrittura parziale.

## Criteri di accettazione

Scenari previsti, **non eseguiti**. Gli ID locali restano stabili; gli esiti sono allineati alla v1.0.

| ID | Dato / quando | Risultato previsto |
| --- | --- | --- |
| F06-01 | Pubblicazione pubblica, poi avvio sull'originale | Nessun hold alla pubblicazione; all'avvio versione 1 e A accettante solo dopo prenotazione |
| F06-02 | Due rispondenti sullo stesso annuncio | Una sola trattativa, nessun effetto parziale per il perdente |
| F06-03 | Due trattative prenotano gli stessi beni | Esito seriale, totale riservato entro disponibilità |
| F06-04 | Arte richiesta insufficiente ma un'altra posseduta | Nessuna sostituzione; accettazione rifiutata senza hold parziali |
| F06-05 | Retry di consenso della stessa versione | Nessun raddoppio di hold o trasferimento |
| F06-06 | Modifica e consenso concorrenti | Versione precedente rifiutata o invalidata; modifica rilascia tutto e azzera i flag |
| F06-07 | Risorse insufficienti all'avvio originale, invio privato o accettazione | Rollback dell'operazione; nessun nuovo hold parziale; stato prima della richiesta preservato |
| F06-08 | Scadenza durante attesa dei lock, worker in ritardo | Cutoff dopo i lock; nessun consenso/completamento; hold scaduti non conteggiati |
| F06-09 | Acquisto tenta di spendere token prenotati | Rifiuto senza intaccare gli hold vivi |
| F06-10 | Secondo consenso valido | Trasferimenti, consumo hold, storico e chiusura atomici |
| F06-11 | Guasto durante il completamento | Rollback di trasferimenti, hold e stato |
| F06-12 | Rifiuto, annullamento, scadenza o retry dopo completamento | Rilascio senza doppio trasferimento; ripubblicazione pubblica valida senza hold |
| F06-13 | Cancellazione catalogo con annunci/trattative coinvolti | Invalidazione e rilascio di entrambi i lati prima della conversione |
| F06-14 | Privato ad amico, non amico o sé stesso | Solo amico accettato e distinto; casi rifiutati senza hold |
| F06-15 | Offerta vuota accettata | Consenso nel flag, nessuna riga token/carte vuota |
| F06-16 | Ultima copia trasferita, vecchi hold scaduti | Rimozione della tripla a zero senza FK al possesso |
| F06-17 | Prima controproposta pubblica negoziabile | Versione 2, entrambi falsi e nessun hold |
| F06-18 | Prima risposta accetta direttamente | Prenotazione di entrambi e completamento nello stesso commit; fallimento lascia pubblico senza hold |
| F06-19 | Rifiuto pubblico prima/dopo scadenza | Prima: originale ripubblicato senza hold; dopo: `EXPIRED`; TTL immutato |
| F06-20 | Messaggi prima e durante la trattativa | Correlazione annuncio/trattativa nel servizio separato |
| F06-21 | Pulizia ripetuta | Storico concluso preservato; deduplicazione statistiche richiede il meccanismo da definire |
| F06-22 | Modifica di carte/arti/quantità/token | Tutti gli hold rilasciati al commit, versione incrementata e flag falsi |
| F06-23 | Nuova offerta valida ma risorse non disponibili | Modifica senza prenotazione; la successiva accettazione insufficiente fallisce senza effetti parziali |
| F06-24 | Lato accettato con offerta vuota, poi modifica non vuota | Flag azzerato; nessun hold fino al nuovo consenso |

## Verifiche, decisioni aperte e impatto

Allineamento documentale alla v1.0; nessuna API, migrazione, pulizia o prenotazione è implementata. Le prove transazionali devono essere eseguite dopo l'implementazione.

Restano da definire rotte e payload definitivi, errori, serializzazione a 64 bit, paginazione, deduplicazione delle creazioni, guard catalogo, timeout/retry, lotti, statistiche, conservazione, frammenti, giocabilità e consegna affidabile. Non sono più lacune della v1.0 la prenotazione di pubblicazione o lo stato separato di lato già prenotato: questi comportamenti non fanno parte del modello corrente.

## Collegamenti

- [Interfaccia scambi](../FrontEnd/scambi-mercato.md)
- [Collezione](collezione-carte.md)
- [Pacchetti](acquisto-pacchetti.md)
- [Mazzi](gestione-mazzi.md)
- [Sfide e simulazione](sfide-simulazione.md)
- [Gestione amministrativa](gestione-amministrativa.md)
- [Indice BackEnd](README.md)
- [Indice e stato delle funzionalità](../README.md)
