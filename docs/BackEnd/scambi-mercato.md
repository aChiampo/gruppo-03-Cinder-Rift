# Scambi e marketplace

ID: **F06**. Perimetro: confermato, scambi pubblici e privati di carte e token.
Specifica: **in approfondimento**, con prenotazioni persistenti richieste dal committente.
Implementazione: **non iniziata**. Responsabile: **DA DEFINIRE (Software Developer)**.
Fonte dei flussi: @aChiampo istruzioni del **2026-11-03** e schema del db al **2026-11-03**. Precisazioni della stessa conversazione: prenotazione alla pubblicazione e adeguamento delle prenotazioni alle offerte modificate.
Ultimo aggiornamento significativo: **2026-11-03**. Verifiche applicative: **non eseguite**.

## Obiettivo, perimetro e dipendenze

Questa pagina è il contratto tecnico canonico F06. Riprende i flussi pubblici e privati, compresi risposta iniziale con controproposta, accettazione diretta, messaggi separati, statistiche e pulizia.

La pubblicazione di un annuncio pubblico o l’invio di una richiesta privata prenota l’offerta uscente del pubblicatore A, sia token sia carte con arte e quantità specificate. B prenota il proprio lato quando accetta. Le risorse prenotate restano possedute dall’utente ma non possono essere spese o cedute altrove fino al rilascio o al trasferimento. Le prenotazioni persistono fra richieste; ogni operazione usa una transazione breve, senza mantenere lock durante l’attesa della risposta umana. Non sono introdotti un registro dei movimenti o uno storico delle controfferte.

| Elemento della baseline                    | Adattamento corrente                                                                                                                                           |
| ------------------------------------------ | -------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| Identificativi numerici di entità e utenti | UUID; il destinatario privato si risolve tramite `USERS.public_id`                                                                                             |
| Crediti e campi `*_credit_offer`           | Unica valuta token, campi `*_token_offer`                                                                                                                      |
| Array di ID ripetuti                       | Oggetti `{cardId, cardArtId, quantity}` con arte esplicita e quantità                                                                                          |
| `P_acceptance` / `R_acceptance`            | `p_acceptance` / `r_acceptance`, default `false`, veri solo con prenotazione coerente del lato; una prenotazione può restare attiva dopo il reset del consenso |
| Date `timestamp`                           | `timestamptz`, connessioni e serializzazione UTC                                                                                                               |
| Controllo disponibilità al trasferimento   | Prenotazione di A alla pubblicazione/invio, di B all’accettazione e verifica atomica anche al completamento                                                    |
| Percorsi API indicativi                    | Proposte con `/api`, risorse nominali e segnaposto camelCase secondo le convenzioni del progetto                                                               |

La baseline permette di richiedere carte del catalogo senza conoscere una copia individuale di B. Il modello attuale conserva questa distinzione dalle singole copie, ma richiede la coppia carta/arte: non sceglie implicitamente un’illustrazione diversa. Un’estensione «qualsiasi arte» resta da concordare.

Dipendenze: F01 per identità, F03/F04 per saldo e collezione, F05/F07 per mazzi e gioco, F10 per cancellazioni del catalogo. Le integrazioni descritte sono requisiti da implementare, non codice già disponibile.

## Flussi

Sono previsti un annuncio pubblico nel marketplace e una richiesta privata da A a B. A è il pubblicatore; B è il primo rispondente pubblico oppure il destinatario privato. La scadenza è fissata a **due giorni dalla creazione**, resta immutabile durante ogni trattativa e vale anche per le prenotazioni.

## Annuncio pubblico

1. **Preparazione:** A seleziona dal catalogo carte e arti desiderate, specifica ciò che offrirà e ciò che chiede, può includere token e imposta `is_negotiable`. Il backend valida struttura e catalogo; la disponibilità letta durante la preparazione non è ancora una prenotazione. La prenotazione avviene nella transazione di pubblicazione del punto seguente.
2. **Pubblicazione:** nella stessa transazione il backend crea `ORIGINAL_ANNOUNCES` con `is_private = false`, `status = PUBLISHED`, proposta iniziale ed `expires_at` definitivo, verifica la disponibilità e prenota l’intera offerta uscente di A: token e copie delle precise coppie carta/arte. Non prenota il lato richiesto a B. L’annuncio diventa visibile nel marketplace solo dopo il commit, mentre è pubblicato e non scaduto. Se manca una sola risorsa di A, rollback completo: nessun annuncio pubblicato e nessuna prenotazione parziale. Non è ancora creata una trattativa.
3. **Consultazione:** B apre l’annuncio senza modificarne stato o risorse. Può accettare la proposta iniziale oppure, se negoziabile, presentare una controproposta completa come prima risposta. Non basta consultare l’annuncio per acquisirlo.
4. **Avvio della negoziazione:** il backend acquisisce atomicamente l’annuncio ancora valido, crea `CURRENT_NEGOTIATIONS`, copia i partecipanti e i flag e imposta `PENDING`. L’annuncio scompare dal marketplace. Sono distinti i casi della tabella seguente.
5. **Trattativa:** se negoziabile, A e B possono sostituire l’intera proposta corrente. Ogni modifica aggiorna atomicamente le prenotazioni dei lati già prenotati per coprire le rispettive offerte uscenti modificate, incrementa la versione e azzera le accettazioni. Le risorse rimosse tornano disponibili; quelle aggiunte devono essere disponibili, altrimenti l’intera modifica viene annullata e restano proposta, versione, consensi e prenotazioni precedenti. Un lato non ancora prenotato viene prenotato quando accetta. Il semplice invio di una controproposta non equivale ad accettarla. I messaggi usano il servizio separato.
6. **Chiusura:** il doppio consenso della stessa versione, con prenotazioni valide, completa lo scambio atomicamente. Il rifiuto di una trattativa pubblica ripristina la proposta originale e la prenotazione di A prima della ripubblicazione, soltanto se l’annuncio non è scaduto; rilascia gli hold di B ed elimina la trattativa nella stessa transazione. Se scaduto, rilascia tutti gli hold e usa `EXPIRED`. Solo A può annullare l’annuncio, con `CANCELLED` se ancora valido e rilascio di tutti gli hold. La ripubblicazione conserva proposta originale e scadenza. Se dopo modifiche non è più possibile prenotare l’originale, la politica di chiusura/recupero è **DA DEFINIRE**: non è consentito ripubblicare senza copertura.

### Prima risposta e prenotazioni

| Prima risposta di B | Versione e consensi | Effetti atomici |
| --- | --- | --- |
| Avvio sulla proposta iniziale, senza accettazione di B | Versione `1`; A accettante, B non accettante | Verifica e riusa la prenotazione originale di A senza duplicarla, poi imposta `p_acceptance = true` |
| Accettazione diretta della proposta iniziale | Versione `1`; entrambi accettanti | Riusa gli hold di A, prenota B, verifica e trasferisce tutto, registra lo storico e chiude nello stesso commit, anche se negoziabile |
| Controproposta iniziale, solo se negoziabile | Copia iniziale di riferimento `1`, prima modifica `2`; entrambi i flag `false` | Aggiorna la prenotazione di A per coprire la sua offerta uscente di versione `2`; B non è ancora prenotato. Nessun consenso automatico alla controproposta |

La controproposta iniziale compone avvio e modifica nella stessa transazione. La versione `1` non viene esposta come trattativa già committata prima della modifica; si salva direttamente lo stato finale di versione `2`. La prenotazione di A esiste già dalla pubblicazione e viene sostituita atomicamente con quella della sua nuova offerta, senza conservare anche un hold separato dell’originale. Se la nuova offerta di A non è copribile, rollback dell’intera risposta: annuncio ancora `PUBLISHED` e prenotazione originale intatta. B prenota quando accetta; nessun consenso sopravvive sulla proposta modificata. Anche una risposta diretta sull’originale fallita lascia intatti annuncio e hold di A precedentemente committati.

### Risposte simultanee

Il backend cambia `PUBLISHED → PENDING` soltanto se l’annuncio è ancora pubblico, pubblicato e non scaduto al cutoff. L’aggiornamento condizionale, la creazione della trattativa e gli eventuali hold/trasferimenti fanno parte della stessa transazione SQL, rispettando l’ordine dei lock definito sotto.

Si prosegue solo se è stato acquisito esattamente un annuncio. Se nessuna riga è stata aggiornata, il backend restituisce un conflitto: l’annuncio può essere occupato, annullato o scaduto. Se creazione, prenotazione o trasferimento falliscono, anche il cambio di stato viene annullato. L’unicità di `CURRENT_NEGOTIATIONS.announce_id` garantisce inoltre una sola trattativa attiva. Il frontend aggiorna lo stato per il rispondente non accettato, ma non gestisce autonomamente la concorrenza.

## Richiesta privata

1. **Preparazione:** A identifica B tramite il suo `public_id`, consulta le carte e le arti scambiabili visibili, definisce le due offerte e `is_negotiable`. Il backend risolve l’UUID interno di B e verifica che sia un amico con amicizia accettata. A può allegare un messaggio tramite il servizio separato.
2. **Invio:** nella stessa transazione il backend crea `ORIGINAL_ANNOUNCES` e `CURRENT_NEGOTIATIONS`, con A e B già identificati, `is_private = true`, `status = PENDING` e scadenza a due giorni. Copia la proposta originale nella versione `1`, prenota i beni uscenti di A e solo allora imposta `p_acceptance = true`; `r_acceptance` resta `false`. Se A non ha disponibilità, rollback completo. La richiesta non compare nel marketplace.
3. **Trattativa:** B può accettare o rifiutare l’originale. Se negoziabile, A e B possono modificare l’intera proposta: il backend adegua atomicamente le prenotazioni dei lati già prenotati alle nuove offerte, incrementa la versione e azzera entrambi i consensi. Se le nuove risorse sono insufficienti, rollback dell’intera modifica. Ogni nuova accettazione verifica e riusa la prenotazione del lato, oppure la crea se quel lato non è ancora prenotato; quella conclusiva trasferisce tutto nello stesso commit.
4. **Chiusura:** il doppio consenso della stessa versione completa lo scambio. Il rifiuto di A o B elimina trattativa e hold e porta la richiesta a `CANCELLED` se valida, a `EXPIRED` se scaduta. Una richiesta privata non torna mai a `PUBLISHED` e non cambia destinatario tramite una controproposta.

## Attori, autorizzazioni e offerte

- Il backend ricava l’identità dalla sessione autenticata, senza fidarsi di un proprietario inviato dal client. Gli ID interni sono UUID; il `public_id` serve a individuare il destinatario privato.
- Il pubblicatore A crea l’annuncio e può annullarlo. Solo A e il rispondente B possono leggere e modificare la trattativa, accettarla o rifiutarla. B non può acquisire un proprio annuncio. Non è assunta una gerarchia di privilegi fra ruoli.
- Una richiesta privata è consentita verso un amico con amicizia accettata, verificata alla creazione. Il destinatario è `CURRENT_NEGOTIATIONS.responder_id`; l’annuncio privato non compare nel marketplace.
- Gli annunci pubblici visibili sono soltanto `PUBLISHED` e non scaduti. Prenotazioni e disponibilità dettagliate sono visibili al proprietario; il client non può creare o rilasciare direttamente record di prenotazione.

`P` indica A e `R` indica B. Ogni lato descrive ciò che cederà all’altro. `original_r_*` rappresenta quindi la richiesta iniziale di A verso B. I campi monetari sono `*_token_offer`, obbligatori con default `0`.

Le offerte di carte sono array JSON non nulli, anche vuoti, con questo contenuto logico:

```typescript
type CardOffer = {
    cardId: UUID;
    cardArtId: UUID;
    quantity: PositiveInteger;
};

type Offer = {
    cards: CardOffer[];
    tokens: Int64;
};
```

Questi sono tipi logici del contratto, non un SDK o un’API implementata. `cardId` corrisponde a `CARDS.id`; `cardArtId` a `CARDS_ARTS.id_art`. Ogni coppia compare una sola volta. Il backend verifica quantità, importi, tipi, appartenenza dell’arte alla carta e scambiabilità. Le quantità positive e i limiti economici restano controlli backend, senza nuovi `CHECK` numerici. Gli array di ID ripetuti della baseline sono rappresentati nel modello corrente come voci con arte e quantità esplicite.

L’offerta identifica l’arte esatta: non viene scelta automaticamente una variante alternativa se quella richiesta manca. Un’eventuale richiesta «qualsiasi arte» non è prevista. Serializzazione degli interi a 64 bit, rotte HTTP definitive, status, paginazione e formato degli errori sono **DA DEFINIRE**, rispettando il prefisso `/api` e le convenzioni del progetto.

## Modello dati: annunci, negoziazioni e prenotazioni

Il modello separa annuncio originale, negoziazione attiva e scambio concluso. Le due tabelle di prenotazione estendono quel modello e rappresentano soltanto vincoli correnti, senza conservare versioni intermedie. Le stesse entità supportano pubblico e privato; `is_private` e `is_negotiable` sono definiti alla creazione e copiati coerentemente nella trattativa.

I tipi e i campi seguenti sono quelli presenti nella documentazione del db (aggiungere link alla doc quando disponibile). I JSON di carte sono non nulli, anche vuoti; i campi token sono obbligatori con default `0`. Le quantità sono validate dal backend.

**Adeguamento dello schema DA DEFINIRE:** le tabelle di prenotazione descritte sotto richiedono attualmente `negotiation_id`, ma la pubblicazione pubblica deve prenotare A prima che esista `CURRENT_NEGOTIATIONS`. Il modello persistente deve quindi supportare prenotazioni associate all’annuncio anche senza trattativa. PK, FK e regole di eliminazione devono essere allineate a questo requisito; le tabelle seguenti descrivono lo schema corrente, non un adeguamento già eseguito. Non si crea una trattativa fittizia per poter pubblicare.

Il modello deve inoltre distinguere il consenso da un lato già prenotato: la modifica azzera il consenso ma aggiorna e conserva la prenotazione. Questo stato va rappresentato anche per un’offerta vuota, che non produce righe token/carte; la sua persistenza è **DA DEFINIRE** e non può essere dedotta dal solo flag di accettazione.

### ORIGINAL_ANNOUNCES

Conserva annuncio e proposta iniziale. La pubblicazione prenota solo i beni del pubblicatore. I campi `original_*` non cambiano: durante la trattativa le prenotazioni seguono la proposta corrente; il rifiuto pubblico richiede il ripristino della proposta originale e della relativa prenotazione di A prima della ripubblicazione.

| Campo | Tipo | Significato |
| --- | --- | --- |
| `id` | `uuid` | Identificativo generato dell’annuncio; PK. |
| `publisher_id` | `uuid` | UUID interno di A; FK a USERS.id. |
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
| `id` | `uuid` | Identificativo generato della trattativa; PK. |
| `announce_id` | `uuid` | FK obbligatoria e univoca all’annuncio: al massimo una trattativa attiva. |
| `publisher_id` | `uuid` | UUID interno di A; deve coincidere con il pubblicatore dell’annuncio. |
| `responder_id` | `uuid` | UUID interno di B, fisso per la trattativa. |
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
| `id` | `uuid` | Identificativo generato dello scambio; PK. |
| `announce_id` | `uuid` | UUID storico univoco, senza FK all’annuncio. |
| `is_private` | `boolean` | Visibilità originaria, conservata anche dopo la pulizia dell’annuncio. |
| `publisher_id` | `uuid` | UUID interno di A. |
| `responder_id` | `uuid` | UUID interno di B. |
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
| `negotiation_id` | `uuid` | Parte della PK e FK alla trattativa con delete: cascade. |
| `id_user` | `uuid` | Parte della PK; FK a INVENTORY.id_user, proprietario dei token. |
| `offer_version` | `bigint` | Versione della proposta coperta dall’hold; dopo una modifica può non essere ancora accettata. |
| `token_amount` | `bigint` | Importo uscente positivo; nessuna riga per zero token. |
| `created_at` | `timestamptz` | Creazione della prenotazione. |
| `expires_at` | `timestamptz` | Stessa scadenza immutabile dell’annuncio. |

### TRADE_CARD_RESERVATIONS

Conserva gli hold uscenti per variante, con PK `(negotiation_id, id_user, id_card, id_card_art)`. Il riferimento composto a `CARDS_ARTS.(id_card, id_art)` garantisce che l’arte appartenga alla carta. Possesso e quantità sono verificati sotto i guard delle risorse.

| Campo | Tipo | Significato |
| --- | --- | --- |
| `negotiation_id` | `uuid` | Parte della PK e FK alla trattativa con delete: cascade. |
| `id_user` | `uuid` | Parte della PK e FK all’utente proprietario. |
| `id_card` | `uuid` | Parte della PK e della FK composta verso CARDS_ARTS. |
| `id_card_art` | `uuid` | Parte della PK; identifica l’arte tramite CARDS_ARTS.id_art. |
| `offer_version` | `bigint` | Versione della proposta coperta dall’hold; dopo una modifica può non essere ancora accettata. |
| `quantity` | `int` | Copie uscenti positive della precisa variante; verifica backend. |
| `created_at` | `timestamptz` | Creazione della prenotazione. |
| `expires_at` | `timestamptz` | Stessa scadenza immutabile dell’annuncio. |

## Dati e invarianti delle prenotazioni

`CARDS_ARTS` ha PK `(id_card, art_code)`, `id_art` univoco e unicità composta `(id_card, id_art)`. Quest’ultima rende coerenti sia la FK della collezione sia quella delle prenotazioni. Il possesso è distinto per `(id_user, id_card, id_card_art)` in `COLLECTIONS`.

Le prenotazioni di copie non hanno una FK alla riga volatile di `COLLECTIONS`: una prenotazione già scaduta non deve impedire la rimozione della riga quando il possesso arriva a zero. Possesso e quantità disponibili sono verificati dal backend sotto i guard delle risorse. Le FK di utente e catalogo impediscono riferimenti inesistenti, ma non garantiscono da sole il possesso.

Per ogni annuncio pubblico `PUBLISHED` non scaduto, A ha esattamente le prenotazioni della propria offerta originale uscente, anche senza una trattativa; B non è ancora identificato e non ha prenotazioni. La pubblicazione vale come consenso di A all’originale; `p_acceptance` sarà materializzato solo all’avvio di una trattativa sulla stessa proposta.

Per ogni trattativa attiva, il backend mantiene questi invarianti:

1. Il proprietario di una prenotazione è uno dei partecipanti; `offer_version` coincide con la versione corrente.
2. Un lato accettato ha esattamente le prenotazioni della propria offerta uscente: un record token se l’importo è positivo e un record per coppia carta/arte. Un’offerta vuota non richiede record; il flag ne conserva l’accettazione.
3. Un lato già prenotato mantiene la copertura della proposta corrente anche quando una modifica ne azzera il consenso. A è prenotato dalla pubblicazione/invio; B dalla sua prima accettazione valida. Un lato non ancora prenotato non ha hold. Le accettazioni hanno default `false`; il servizio le imposta solo con risorse verificate e prenotazioni coerenti, senza duplicare hold già esistenti.
4. Le somme non scadute non superano saldo o copie scambiabili. Non sono mantenuti contatori duplicati di riservato in inventario o collezione.
5. La scadenza della prenotazione coincide con `ORIGINAL_ANNOUNCES.expires_at`; nessuna modifica o accettazione prolunga la durata.

## Prenotazioni e calcolo della disponibilita

Pubblicare un annuncio pubblico prenota subito token e carte uscenti di A; l’invio privato fa lo stesso. L’avvio pubblico sull’originale riusa questa prenotazione senza duplicarla. L’accettazione diretta di B prenota il suo lato e completa nello stesso commit. Una controproposta aggiorna le prenotazioni dei lati già prenotati e azzera entrambi i consensi: una prenotazione non equivale quindi da sola all’accettazione della versione corrente.

Per aggiornare una prenotazione, il backend verifica che il saldo e le copie scambiabili, al netto degli hold degli altri annunci, coprano per intero la nuova offerta uscente. La vecchia prenotazione dello stesso lato viene sostituita nella stessa transazione: non si sommano vecchia e nuova quantità e non esiste una finestra committata di risorse liberate prima della sostituzione. Se la verifica fallisce, il vecchio stato resta intatto. La prenotazione aggiornata mantiene la scadenza originale e assume la nuova `offer_version` anche se il consenso è stato azzerato.

Per un istante di decisione `cutoff` ricavato dall’orologio del database dopo aver acquisito i lock:

```text
reservedTokens(user) = SUM(token_amount WHERE id_user = user AND expires_at > cutoff)
availableTokens(user) = token_balance - reservedTokens(user)

reservedCopies(user, card, art) = SUM(quantity for that triple WHERE expires_at > cutoff)
availableTradableCopies(user, card, art) = tradable_replica - reservedCopies(user, card, art)
```

Somme vuote valgono zero. Una riga di collezione assente significa zero copie dopo una lettura riuscita. Si contano tutte le prenotazioni non scadute, anche su altri annunci; una riga anomala non va ignorata per rendere artificialmente disponibili beni. Le incongruenze impediscono l’operazione e richiedono riallineamento.

Riservare non addebita `token_balance` e non riduce `owned_replica` o `tradable_replica`: sottrarre anche quei contatori conterebbe due volte lo stesso hold. Ogni altro addebito deve lasciare risorse sufficienti a tutte le prenotazioni vive. Non si finanzia un’offerta uscente con i beni che si spera di ricevere nello stesso scambio.

Al completamento, l’offerta consuma le **proprie** prenotazioni: disponibilità al netto delle sole prenotazioni altrui deve coprire i trasferimenti uscenti. La propria prenotazione non è un ulteriore addebito. Gli indici per proprietario/risorsa/scadenza supportano queste letture; le chiavi devono anche permettere il rilascio per annuncio senza trattativa, nell’adeguamento dello schema ancora da definire.

## Regole operative

- **Annuncio pubblico:** persistere proposta originale, flag e scadenza e prenotare token e carte uscenti di A nella stessa transazione; in caso di indisponibilità eseguire il rollback completo. Mostrare solo annunci `PUBLISHED` non scaduti dopo il commit. Il tempo residuo è sempre `expires_at` meno l’ora corrente, senza proroghe.
- **Invio privato:** verificare l’amicizia, creare annuncio e trattativa, prenotare A e registrarne il consenso all’originale nella stessa transazione. B è fisso; il privato non diventa mai pubblico.
- **Prima risposta pubblica:** acquisire esattamente un annuncio e applicare il ramo originale, accettazione diretta o controproposta descritto nei flussi. Se il ramo richiede hold o trasferimenti, essi fanno parte della stessa transazione; nessun annuncio acquisito a metà.
- **Modifica e accettazione:** consentire modifiche soltanto se negoziabile, richiedere la versione attesa, aggiornare atomicamente gli hold dei lati già prenotati e azzerare i consensi incrementando la versione. Se manca copertura della nuova offerta, rollback completo. Una nuova accettazione riusa e verifica gli hold di quel lato o li crea se assenti; la seconda completa lo scambio.
- **Rifiuto:** nel pubblico ancora valido ripristinare atomicamente offerta originale e prenotazione di A, rilasciare B, eliminare la trattativa e ripubblicare. Se non è possibile riprenotare l’originale dopo modifiche, la politica di chiusura/recupero è **DA DEFINIRE**; mai ripubblicare senza copertura. Se scaduto, rilasciare tutti gli hold e usare `EXPIRED`; il privato valido rilascia tutti gli hold e diventa `CANCELLED`. Non cambiare scadenza o proposta originale.
- **Completamento:** verificare doppio consenso e hold esatti non scaduti, rivalidare disponibilità, consumare gli hold, trasferire carte e token, salvare `TRADES_COMPLETED`, impostare `COMPLETED` ed eliminare la trattativa con un unico commit o rollback.
- **Annullamento e scadenza:** verificare il tempo nelle operazioni e nelle liste, senza affidarsi al solo worker. Solo A può annullare l’annuncio; B può rifiutare la trattativa. Rilascio e transizione terminale devono impedire il completamento concorrente.
- **Statistiche e pulizia:** aggregare gli annunci chiusi prima dell’eliminazione, senza conteggi duplicati nei retry; conservare gli scambi conclusi secondo una politica distinta. La pulizia non deve lasciare hold vivi su trattative eliminate o riaprire stati terminali.

La validazione eseguita durante preparazione o consultazione non sostituisce quella alla pubblicazione, alla modifica delle prenotazioni, all’accettazione e al completamento. Un aggiornamento degli array JSON può riscrivere il valore intero; la versione attesa protegge da modifiche concorrenti, mentre i guard delle risorse proteggono le disponibilità aggregate.

## Stati, transizioni e scadenza

La durata è **due giorni dalla creazione**, in UTC, e non viene prorogata. Il backend imposta `created_at` e `expires_at` coerentemente; il default DBML non impedisce scritture applicative errate. Un’operazione valida richiede `expires_at > cutoff`; l’uguaglianza significa scaduto. Il punto di decisione è dopo i lock e prima degli effetti atomici, anche se il commit avviene più tardi.

| Evento | Condizione e stato finale | Effetto sulle prenotazioni |
| --- | --- | --- |
| Pubblicazione pubblica | Nuovo annuncio `PUBLISHED`, visibile solo dopo il commit | Prenota token e carte uscenti di A; rollback completo se insufficienti |
| Avvio pubblico sull’originale | Acquisizione condizionale di un annuncio valido; `PENDING` | Riusa e verifica la prenotazione di A senza duplicarla; se B accetta direttamente, prenota B e completa nello stesso commit |
| Prima controproposta pubblica | Annuncio negoziabile acquisito; `PENDING`, versione `2` | Aggiorna gli hold di A alla nuova offerta; entrambi i flag falsi, B non ancora prenotato; rollback completo se A non ha copertura |
| Invio privato | Amicizia accettata; annuncio e trattativa `PENDING` | Prenota A nello stesso commit |
| Modifica | `PENDING`, negoziabile, versione attesa corretta | Aggiorna gli hold dei lati già prenotati, incrementa versione, azzera entrambi i flag; rollback completo se insufficienti |
| Accettazione | Versione corrente e annuncio valido | Verifica e riusa gli hold del lato o li crea se non ancora prenotato; se entrambi accettati, completa nello stesso commit |
| Rifiuto | Pubblico valido → `PUBLISHED` solo dopo ripristino della copertura originale; privato → `CANCELLED`; scaduto → `EXPIRED` | Nel pubblico ripristina gli hold originali di A e rilascia B; negli altri casi rilascia tutto; recupero se originale non copribile **DA DEFINIRE** |
| Annullamento | Solo A, annuncio aperto → `CANCELLED`, oppure `EXPIRED` se già scaduto | Rilascia entrambi i lati |
| Scadenza | Annuncio aperto → `EXPIRED`; elimina la trattativa | Le prenotazioni cessano già di contare alla scadenza; la pulizia le elimina |
| Completamento | Doppio consenso della stessa versione → `COMPLETED` | Consuma gli hold insieme a trasferimenti e storico |
| Invalidazione forzata | Carta/espansione cancellata → annuncio `CANCELLED` se ancora aperto, eventuale trattativa annullata | Rilascia tutti gli hold dell’annuncio, anche senza trattativa, prima della conversione |

Il processo di pulizia lavora per lotti e applica lo stesso protocollo di lock; frequenza e dimensione dei lotti sono **DA DEFINIRE**. Un ritardo del processo non blocca i beni oltre la scadenza e non permette di accettare uno scambio scaduto. Nessun annuncio terminale viene riaperto dal processo.

## Transazioni, concorrenza e richieste ripetute

### Guard comuni e ordine dei lock

Il modello sceglie transazioni `READ COMMITTED` con guard stabili e letture aggiornate **dopo** i lock. Un normale `SELECT` seguito da inserimento di hold non basta a evitare sovraprenotazioni. La riga `USERS` è il guard stabile di tutte le risorse del proprietario e copre anche righe di collezione o prenotazione ancora assenti.

L’ordine da rispettare in tutti i flussi coinvolti è: guard catalogo → utenti per UUID crescente → mazzi per ID crescente, se coinvolti → annunci per ID crescente → trattative per ID crescente → dettagli di inventario, collezione e prenotazione in ordine deterministico. Questo raffina il [protocollo proposto F05](gestione-mazzi.md#transazioni-scambi-e-richieste-concorrenti); non introduce un ordine inverso nei flussi che coinvolgono i mazzi.

F06 usa il guard catalogo in modalità condivisa e il servizio di modifica/cancellazione lo usa in esclusiva. La sua primitiva distribuita resta **DA DEFINIRE**. Per i guard utente si richiede un lock di riga, per esempio `SELECT ... FOR UPDATE`, che sia rispettato anche da acquisti, regali, conversioni e altri scambi. Leggere l’annuncio o i partecipanti prima della transazione serve soltanto a pianificare i lock: dopo averli acquisiti si rilegge e si verifica l’identità della trattativa. Se occorrono altri guard, si annulla e si ripianifica, senza acquisirli fuori ordine.

Il cutoff usa `clock_timestamp()` dopo l’attesa dei guard, non l’ora del client o un istante fissato prima di un’attesa lunga. Timeout e deadlock provocano rollback; retry limitati richiedono la stessa versione attesa e una rilettura dello stato. Nessun lock resta aperto in attesa della risposta umana. Riferimenti: [lock PostgreSQL](https://www.postgresql.org/docs/18/explicit-locking.html) e [isolamento](https://www.postgresql.org/docs/18/transaction-iso.html).

### Accettazione e modifica atomiche

L’accettazione autorizza il partecipante, blocca le risorse nell’ordine comune, rilegge annuncio e proposta e verifica scadenza/versione. Se quel lato è già accettato per la stessa versione, verifica la corrispondenza degli hold e restituisce lo stato senza aggiungerne altri. Se è già prenotato ma non accettante, verifica e riusa i suoi hold della versione corrente e imposta il flag; altrimenti verifica tutta la disponibilità, crea l’intero insieme di hold e imposta il flag. Se manca una sola risorsa, rollback di flag e prenotazioni di quella richiesta: tutte le prenotazioni precedentemente committate restano intatte.

Quando arriva il secondo consenso valido, prenotazione e completamento appartengono alla stessa transazione. Una modifica concorrente può vincere prima, invalidando la versione dell’accettazione, oppure avvenire dopo un consenso ancora singolo; non può modificare uno scambio già completato. Ogni modifica aggiorna le prenotazioni dei lati già prenotati, con controllo di copertura al netto degli altri annunci, e resetta entrambi i flag. Anche se cambiano solo i beni richiesti alla controparte, il consenso alla nuova proposta va espresso di nuovo. Offerta, hold aggiornati, versione e reset dei flag devono riuscire insieme oppure restare tutti allo stato precedente.

### Completamento e recupero

Nella stessa transazione il backend:

1. Verifica identità, stato, cutoff, versione, doppio consenso e corrispondenza esatta fra offerte e hold non scaduti.
2. Verifica possesso, scambiabilità, saldo e copertura delle altre prenotazioni vive; un UUID d’arte valido non dimostra il possesso.
3. Consuma gli hold della trattativa e aggiorna carte e token per entrambe le parti; crea le triple ricevute mancanti e rimuove le triple a possesso finale zero, preservando gli altri hold vivi. Un errore successivo ripristina anche gli hold consumati.
4. Inserisce lo storico unico per `announce_id`, comprensivo di arte e quantità, imposta `COMPLETED` ed elimina la trattativa.
5. Esegue il commit unico. Notifiche e messaggi vanno pubblicati dopo il commit; consegna e recupero dei guasti del servizio separato restano **DA DEFINIRE**.

Gli hold consumati o residui devono essere eliminati esplicitamente nella transazione di completamento. La cascata attuale dalla trattativa non copre le prenotazioni di pubblicazione senza trattativa e deve essere rivista nell’adeguamento dello schema; nessuna cascata sostituisce i controlli sul trasferimento. Dopo un esito di commit sconosciuto si rilegge lo stato per `announce_id`. Se lo storico esiste ed è autorizzato, si restituisce lo stato già concluso senza un nuovo trasferimento. L’unicità non sostituisce la transazione: una collisione deve annullare tutti gli effetti della richiesta.

PK delle prenotazioni e verifica di versione rendono ripetibili le accettazioni senza duplicare quantità. Una modifica ripetuta con la vecchia versione viene rifiutata. Rilascio, annullamento e pulizia ripetuti non possono sottrarre risorse o riaprire stati terminali. Deduplicazione della creazione di nuovi annunci dopo un esito incerto: **DA DEFINIRE**; non è dichiarata un’idempotenza generale delle API.

## Operazioni esterne e invalidazione forzata

Acquisti, regali, scambi, conversioni e riduzioni della scambiabilità rispettano gli stessi guard. Un’operazione ordinaria che consumerebbe beni prenotati viene rifiutata. Una riduzione di `tradable_replica` non può scendere sotto la quantità riservata viva di quella precisa arte.

La cancellazione logica di carte o espansioni richiede un’invalidazione forzata: acquisire il guard esclusivo del catalogo, individuare gli annunci pubblicati anche senza trattativa e le trattative interessate, acquisire i guard di **tutti** i partecipanti e dei mazzi coinvolti nell’ordine comune, rileggere lo stato, annullare gli annunci e le trattative aperte e rilasciare tutti i relativi hold, anche token e carte della controparte. Solo dopo il rilascio si convertono le copie e si puliscono collezioni e mazzi. La conversione e il rilascio devono avere confini atomici coerenti; lotti e ripresa idempotente sono **DA DEFINIRE**. Non si può cancellare una sola prenotazione lasciando veri i consensi della trattativa.

Le prenotazioni non modificano il possesso né cancellano righe dei mazzi. La possibilità di giocare con copie prenotate e la selezione dell’arte nei mazzi restano decisioni F05/F07 **DA DEFINIRE**. Al trasferimento il possesso cambia e il controllo successivo del mazzo deve usare i dati correnti; una prenotazione non certifica la validità del mazzo. Non viene deciso un blocco delle carte per tutta la partita.

## Messaggi, statistiche e pulizia

### Messaggistica separata

Il servizio distinto conserva e consegna i messaggi e ne definisce la conservazione. Un messaggio allegato all’annuncio prima della prima risposta usa l’ID dell’annuncio; i messaggi della trattativa usano l’ID della negoziazione. Le cinque tabelle F06 non contengono il testo dei messaggi. Le notifiche di un risultato diventano inviabili solo dopo il commit; consegna affidabile e recupero dei guasti del servizio sono **DA DEFINIRE**.

### Statistiche e pulizia

Gli annunci terminali vengono aggregati prima della rimozione, per lotti e con un criterio contro conteggi duplicati dopo un retry. Il DBML non include un registro di aggregazione: chiave di deduplicazione, persistenza degli aggregati, frequenza e dimensione dei lotti sono **DA DEFINIRE**. Non si dichiara già garantita un’aggregazione eseguita una sola volta.

`created_at` e `expires_at` permettono statistiche su pubblicazione e scadenza; `TRADES_COMPLETED.completed_at` registra il completamento. Manca una data di annullamento: `status = CANCELLED` permette di contare gli annullamenti rispetto alla data di creazione, non di conoscerne il momento esatto. Non viene inventato un campo temporale assente.

Prima di eliminare un annuncio occorre chiudere le eventuali trattative e rilasciare tutti gli hold associati, comprese le prenotazioni di pubblicazione senza trattativa, usando i guard comuni. La FK `CURRENT_NEGOTIATIONS.announce_id` impedisce di cancellare l’annuncio lasciando una trattativa dipendente. Le attuali FK delle prenotazioni verso la trattativa non bastano al nuovo flusso: il loro adeguamento deve consentire sia il rilascio alla chiusura sia il mantenimento degli hold di A alla ripubblicazione, senza affidarsi alla cancellazione della trattativa. `TRADES_COMPLETED.announce_id` è invece storico e privo di FK, quindi lo scambio concluso sopravvive. La politica di conservazione degli scambi è distinta da quella degli annunci.

Quando una trattativa privata rifiutata o scaduta viene eliminata, lo schema non conserva più B nell’annuncio: non può garantirgli una consultazione storica della richiesta. B può consultare gli scambi completati attraverso `TRADES_COMPLETED`. L’eventuale conservazione indipendente dei messaggi non crea automaticamente un’autorizzazione API su un annuncio senza destinatario persistito.

## Endpoint necessari

Le rotte seguenti sono una **proposta da concordare**, adattata al prefisso `/api`, alle risorse nominali e ai segnaposto camelCase del progetto; non sono API implementate. I percorsi con `/accept`, `/decline` e `/cancel` della baseline sono resi come creazione di accettazioni, rifiuti e annullamenti. Status, payload definitivi, limiti, serializzazione degli interi, paginazione e OpenAPI restano **DA DEFINIRE**.

| Metodo e percorso proposto | Responsabilità |
| --- | --- |
| `POST /api/announcements` | Crea pubblico o privato. Valida offerte carta/arte, token e negoziabilità e prenota sempre il lato uscente di A nello stesso commit della creazione; se insufficiente, rollback completo. Per il privato risolve il `public_id`, verifica l’amicizia e crea anche trattativa e consenso iniziale. |
| `GET /api/announcements?scope=marketplace` | Lista paginata dei soli pubblici `PUBLISHED` non scaduti. Non acquisisce annunci e non prenota beni. |
| `GET /api/announcements?scope=mine` | Lista gli annunci di A con stato e scadenza, compresi quelli chiusi finché conservati. |
| `GET /api/announcements/{announceId}` | Restituisce proposta originale e stato. Il pubblico pubblicato è consultabile nel marketplace; l’annuncio `PENDING` è limitato ad A e B. Dopo la chiusura resta consultabile da A finché conservato; B usa lo storico concluso, se esiste. |
| `POST /api/announcements/{announceId}/negotiations` | Prima risposta pubblica: acquisizione atomica. Distingue accettazione originale, avvio senza modifica e controproposta iniziale se negoziabile; applica versioni, consensi e hold del ramo scelto. L’accettazione diretta può restituire già lo scambio concluso. |
| `GET /api/negotiations?scope=mine` | Lista le trattative attive cui partecipa l’utente, incluse le richieste private ricevute. |
| `GET /api/negotiations/{negotiationId}` | Restituisce ai soli partecipanti proposta corrente, versione, consensi e scadenza; informazioni di disponibilità private solo al rispettivo proprietario. |
| `PUT /api/negotiations/{negotiationId}/offers/current` | Sostituisce l’intera proposta se negoziabile, richiede la versione attesa, adegua gli hold dei lati già prenotati, incrementa versione e azzera i due consensi nello stesso commit; rollback completo se le nuove offerte non sono coperte. |
| `POST /api/negotiations/{negotiationId}/acceptances` | Accetta la versione attesa per l’utente autenticato, verifica e riusa la prenotazione del suo lato o la crea se non ancora prenotato e, se è il secondo consenso valido, completa atomicamente. Retry della stessa accettazione non duplicano hold. |
| `POST /api/negotiations/{negotiationId}/rejections` | Nel pubblico valido ripristina atomicamente offerta originale e hold di A, rilascia B e ripubblica; recupero se l’originale non è copribile **DA DEFINIRE**. Nel privato o se scaduto rilascia tutto e chiude con `CANCELLED` o `EXPIRED`. |
| `POST /api/announcements/{announceId}/cancellations` | Solo A annulla un annuncio pubblico aperto, rilasciando gli hold e rimuovendo l’eventuale trattativa. Nel privato A o B chiudono tramite il rifiuto della trattativa. |
| `GET /api/trades?scope=mine` e `GET /api/trades/{tradeId}` | Espongono gli scambi conclusi ai soli partecipanti, anche dopo la rimozione dell’annuncio originale. |
| `GET /api/users/{userPublicId}/tradable-cards` | Espone le varianti scambiabili visibili secondo le regole di amicizia, tenendo conto delle prenotazioni vive; non rivela saldo o hold privati della controparte. Può appartenere al servizio collezioni. |

Gli input distinguono esplicitamente l’intento di accettare l’originale da quello di proporre modifiche; il contenuto esatto della richiesta è da concordare. Il backend ricava sempre identità, ruolo del partecipante e proprietari dalla sessione e dai dati persistiti. Il client non invia un flag di accettazione autorevole o una quantità riservata da applicare direttamente.

Il servizio di messaggistica espone le proprie operazioni; nessun endpoint di messaggistica viene aggiunto automaticamente all’API degli annunci. Scadenza, aggregazione e pulizia sono processi interni, non endpoint pubblici. Nemmeno le tabelle di prenotazione hanno CRUD pubblico autonomo.

## Errori e comportamento del client

Il client mostra saldo totale, riservato e disponibile solo quando restituiti dal server; per le carte distingue la variante grafica. Non calcola un’autorizzazione a spendere da dati memorizzati localmente. Dopo conflitti, scadenza o esito incerto rilegge lo stato. Le API di prenotazione non sono esposte direttamente agli utenti.

Categorie da distinguere: autenticazione/proprietà, arte incoerente, dati d’offerta invalidi, versione superata, annuncio occupato o terminale, risorse disponibili insufficienti, prenotazioni incoerenti ed errore transazionale. Codici, status e payload definitivi sono **DA DEFINIRE**. Un errore deve lasciare gli effetti atomici della richiesta tutti applicati o tutti annullati; non deve esporre beni di utenti estranei.

## Criteri di accettazione

Scenari previsti, **non eseguiti**; gli ID sono locali a F06 e non identificano issue assegnate.

| ID | Dato / quando | Risultato previsto |
| --- | --- | --- |
| F06-01 | Pubblicazione pubblica, poi avvio sulla proposta originale | Token e carte/arti di A prenotati al commit della pubblicazione; nessun hold di B; all’avvio si riusano gli hold di A senza duplicarli |
| F06-02 | Due rispondenti acquisiscono lo stesso annuncio | Una sola trattativa; nessun hold o stato parziale per il perdente |
| F06-03 | Due trattative prenotano contemporaneamente gli stessi token o la stessa arte | Esito seriale; totale riservato entro la disponibilità |
| F06-04 | Stessa carta con due arti, ma l’arte richiesta è insufficiente | Nessuna sostituzione implicita; accettazione rifiutata senza hold parziali |
| F06-05 | Retry dell’accettazione della stessa versione | Nessun raddoppio di hold o trasferimento |
| F06-06 | Modifica e accettazione concorrenti | Versione precedente rifiutata oppure consenso precedente invalidato; modifica aggiorna gli hold alla nuova offerta e resetta entrambi i flag, senza prenotazioni parziali |
| F06-07 | Saldo o copie insufficienti alla pubblicazione, all’invio privato o all’accettazione | Rollback dell’intera operazione; nessun nuovo annuncio o hold parziale; gli hold precedentemente committati restano coerenti |
| F06-08 | Scadenza raggiunta mentre si attendono i lock; worker in ritardo | Cutoff dopo i lock; niente consenso o completamento; beni scaduti già disponibili |
| F06-09 | Acquisto o altro scambio tenta di spendere beni prenotati | Operazione rifiutata senza intaccare gli hold vivi |
| F06-10 | Secondo consenso valido con entrambi gli hold esatti | Trasferimenti, consumo hold, storico e chiusura atomici; niente doppio addebito del riservato |
| F06-11 | Guasto dopo consumo hold o prima di registrare lo storico | Rollback di trasferimenti, hold e stato |
| F06-12 | Rifiuto, annullamento, scadenza o richiesta ripetuta dopo completamento, anche senza trattativa | Ripubblicazione solo con hold originali di A; chiusura terminale con rilascio di tutti gli hold; mai un secondo trasferimento |
| F06-13 | Cancellazione di carta/espansione con copie prenotate, anche in annunci senza trattativa | Annunci e trattative interessati annullati e tutti gli hold rilasciati prima della conversione |
| F06-14 | Offerta privata ad amico, non amico o sé stesso | Solo l’amico accettato può essere destinatario; nessuna prenotazione nei casi rifiutati |
| F06-15 | Offerta con zero token e array vuoto su un lato | Consenso rappresentato dal flag, senza righe di prenotazione vuote |
| F06-16 | Ultima copia trasferita, con vecchi hold già scaduti presenti | Rimozione della tripla a zero senza FK alla riga di possesso; nessun hold scaduto conteggiato |
| F06-17 | Prima risposta pubblica con controproposta negoziabile | Versione 2, entrambi i consensi falsi, hold di A aggiornati alla nuova offerta, B non ancora prenotato; se A non ha copertura, rollback alla pubblicazione con hold originali intatti |
| F06-18 | Prima risposta pubblica accetta direttamente l’originale | Avvio, riuso degli hold di A, nuovi hold di B, trasferimenti, storico e chiusura nello stesso commit; rollback completo della risposta se manca una risorsa, senza perdere gli hold di pubblicazione di A |
| F06-19 | Rifiuto dopo una controproposta pubblica, prima o dopo la scadenza | Ripubblicazione valida solo con ripristino degli hold originali di A; scadenza rilascia tutto; nessun rinnovo del TTL; recupero se originale non copribile **DA DEFINIRE** |
| F06-20 | Messaggio prima della prima risposta e messaggio in trattativa | Correlazione rispettivamente ad annuncio e negoziazione nel servizio separato, senza testo nelle tabelle F06 |
| F06-21 | Pulizia ripetuta di annunci conclusi già aggregati | Nessun doppio conteggio con il meccanismo da definire; storico concluso conservato; nessuna data di annullamento dedotta |
| F06-22 | Modifica di carte, arti, quantità o token di un lato già prenotato | Hold adeguati alla nuova offerta senza sommare vecchio e nuovo; risorse rimosse liberate solo al commit; entrambi i consensi falsi |
| F06-23 | Modifica richiede risorse aggiuntive non disponibili | Proposta, versione, consensi e hold precedenti intatti; nessun rilascio parziale |
| F06-24 | Lato già prenotato con offerta vuota, poi modifica a un’offerta non vuota | Stato di prenotazione distinto dal consenso e dall’assenza di righe; nuove risorse prenotate nella modifica, oppure rollback completo |

## Verifiche, decisioni aperte e impatto

Questa modifica aggiorna il contratto F06: la pubblicazione prenota il lato di A e le modifiche adeguano atomicamente le prenotazioni alla proposta corrente, separando prenotazione e consenso. Lo schema descritto richiede ancora un adeguamento per prenotare senza trattativa e rappresentare i lati già prenotati anche con offerta vuota; il DBML non è stato modificato in questo intervento. Non sono state implementate API, migrazioni, processi di pulizia o servizi di prenotazione; i criteri sopra richiedono prove transazionali reali dopo l’implementazione.

Restano **DA DEFINIRE**: adeguamento di PK/FK e ciclo di vita delle prenotazioni per annuncio senza trattativa, persistenza dello stato di lato già prenotato indipendente dal consenso, chiusura/recupero del rifiuto pubblico quando l’originale non è più copribile dopo modifiche, approvazione delle rotte proposte, payload e serializzazione, errori e paginazione, deduplicazione della creazione, primitiva del guard catalogo, timeout/retry, frequenza della pulizia e deduplicazione degli aggregati, conversione in frammenti, giocabilità delle copie prenotate, selezione grafica nei mazzi, conservazione dei dati e consegna delle notifiche.

L’integrazione F03/F04/F05/F07/F10 e l’interfaccia devono adottare il nuovo contratto prima di abilitare le prenotazioni. Le vecchie indicazioni «prenotazioni da definire» nei documenti collegati non sostituiscono le decisioni F06 qui descritte; gli aspetti di gioco ancora aperti restano tali.

## Collegamenti

- [Interfaccia scambi](../FrontEnd/scambi-mercato.md)
- [Collezione](collezione-carte.md)
- [Pacchetti](acquisto-pacchetti.md)
- [Mazzi](gestione-mazzi.md)
- [Sfide e simulazione](sfide-simulazione.md)
- [Gestione amministrativa](gestione-amministrativa.md)
- [Indice BackEnd](README.md)
- [Indice e stato delle funzionalità](../README.md)
