# Gestione amministrativa

Aggiornamento F03 del **2026-11-26**: PUT e DELETE sono consentiti anche con possesso o mercy, secondo il [contratto canonico](acquisto-pacchetti.md#catalogo-implementato-migrazione-e-api). PUT conserva il possesso ed elimina i progressi relativi; DELETE rimborsa le quantità al prezzo corrente in `cobalt_shards` ed elimina i riferimenti nella stessa transazione. Il catalogo ADMIN include tutti i pacchetti, senza paginazione. Restano slot ordinati, flag di acquisizione e distribuzioni referenziate immutabili; i tipi e le probabilità si configurano tramite database, senza endpoint HTTP aggiuntivi.

ID: F10 · Perimetro: confermato; specifica in approfondimento.
Implementazione: API CRUD dei cataloghi carte e pacchetti disponibili; altre funzioni di backoffice non implementate.
Fonte: traccia, risposte e chiarimenti del gruppo; le decisioni note sono riportate sotto.
Fonte dell'approfondimento: regolamento Versione 2.1, proposta per il prototipo e le prove di bilanciamento; integrazione documentale approvata, regole definitive da confermare.
Ultimo aggiornamento significativo il **2026-11-26** per visibilità e riconciliazione del CRUD pacchetti F03, su richiesta dell’utente; acquisto, apertura e seed dei pacchetti esclusi; aggiunte configurazione artwork e validazione dei quattro tipi. La revisione del **2026-11-18** riguarda la propagazione delle decisioni dell'utente sui confini dei permessi F13 e sul futuro cleanup F01/F06. I permessi amministrativi di cancellazione account F01 risalgono al 2026-11-13; la protezione del namespace amministrativo al 2026-11-12. L'allineamento a inventario, mercy e regolamento Versione 2.1 risale al 2026-11-09. Verifica CRUD pacchetti/artwork del **2026-11-24**: 244 test backend e JAR superati su PostgreSQL 18.7 isolato; [ambito e limiti](acquisto-pacchetti.md#Tipi%20e%20probabilit%C3%A0%20degli%20artwork). Altre funzioni di backoffice da verificare.
Fonte dati: [specifica canonica v1.1](Database/database-schema-v1.1.md#dettaglio-struttura-delle-carte).

Evidenza storica di `test-connessione` del **2026-11-21**: importate le API CRUD del catalogo da `feature/api-collezione-catalogo`; 8 test API superati il 2026-11-21 su JDK 21.0.11/PostgreSQL 18.6 isolato; JAR prodotto. Le prove non verificano il browser o le nuove immagini Compose.

## Obiettivo e decisioni note

- Dal **2026-11-13**, su approvazione dell’utente, `ADMIN` può cancellare qualsiasi account, incluso se stesso e altri amministratori, attraverso il servizio F01. API, transazione e revoca dei token sono descritte nel [contratto di cancellazione account](accesso-utenti.md#cancellazione-account).
- In [F13](amicizie.md#attori-autorizzazioni-e-precondizioni), `ADMIN` gestisce soltanto le proprie richieste e amicizie, con gli stessi permessi di `USER`; nessuna rotta di eliminazione amministrativa delle relazioni altrui è prevista. Il potere distinto di cancellare account F01 resta invariato. La [pulizia F13/F06 al soft delete](accesso-utenti.md#integrazione-prevista-con-f13-e-f06) è un requisito futuro del servizio account, non un privilegio diretto sulle amicizie: non è implementata dalla presente revisione.
- Backoffice per il catalogo delle carte e dei pacchetti.
- Prezzo e configurazione del singolo pacchetto sono gestibili dal backoffice; ogni offerta contiene una sola unità, secondo il contratto canonico F03.
- L’accesso al namespace amministrativo è riservato ad `ADMIN`; `USER` non può accedervi. La matrice dei permessi per le singole operazioni di backoffice resta **DA DEFINIRE**. [Ruoli F01](accesso-utenti.md#attori-autorizzazioni-e-precondizioni).
- Il namespace `/api/admin/**` richiede il ruolo `ADMIN` per tutti i metodi HTTP. La regola di sicurezza è stata corretta il **2026-11-12**, su richiesta dell’utente di verificare le restrizioni amministrative in `SecurityConfiguration.java`. Sono disponibili le scritture carte F04/F10 e, dal 2026-11-24, il CRUD pacchetti F03; la matrice delle altre operazioni resta **DA DEFINIRE**.

I pacchetti richiedono la FK diretta `id_artwork_pull_rates` e restituiscono la configurazione in sola lettura; valori, campi e gestione delle configurazioni sono nel [contratto F03](acquisto-pacchetti.md#tipi-e-probabilità-degli-artwork). Non è introdotto un CRUD HTTP delle probabilità.

## Cataloghi, vincoli e cancellazione

- Gli ID autonomi di utenti, ruoli, carte, tipi, fazioni, rarità, espansioni, parole chiave, pacchetti e configurazioni sono `int` auto-incrementali. `CARDS_ARTS.id_art` è anch'esso generato e univoco; le FK sono intere senza sequenze autonome. Non si deducono permessi dal valore numerico del ruolo.
- `CARD_TYPES.code` è obbligatorio univoco; i filtri dei pacchetti usano i codici dei cataloghi. Carta e arte sono distinte e vincolate tramite `(id_card, id_art)`. Ogni carta ha al massimo una lista JSONB di parole chiave, con ID interi annidati validati dal backend e range nullable in `KEYWORDS`.
- `PACKS` conserva configurazione, filtri, descrizione JSONB e `pack_price int`; dal 2026-11-24 la migrazione V10 e il [CRUD F03](acquisto-pacchetti.md#catalogo-implementato-migrazione-e-api) sono disponibili, senza seed dei pacchetti; V10 inizializza la sola configurazione artwork approvata. La risorsa richiesta dal prezzo e l’acquisto restano da definire. `INVENTORY` distingue Scaglie (`cobalt_shards`), Nuclei Rift (`rift_cores`) e Rotami (`scraps`), tutti `int` obbligatori con default `0`. Questi saldi non autorizzano conversioni o trasferimenti impliciti, né nuovi permessi di accredito amministrativo. Ogni offerta contiene una sola unità di un pacchetto.
- `PACKS.mercy_threshold` disabilita mercy se `NULL`, altrimenti è positivo. `USER_MERCY` ha PK `(id_user, id_pack)`, FK a utente e pacchetto, rarità minima collegata a `RARITIES.id` e un contatore non negativo inizializzato dal backend dalla soglia. Il progresso avanza sulle aperture del pacchetto corrispondente e si resetta su una pesca qualificante. La configurazione deve consentire almeno una carta/arte valida della rarità richiesta o superiore nei filtri del pacchetto; la policy che assegna inizialmente la rarità resta da definire; PUT e DELETE amministrativi eliminano i progressi relativi secondo F03. Le modifiche del catalogo rispettano il guard esclusivo e non attribuiscono nuovi permessi di modifica diretta dei progressi ai ruoli.
- `ROUND_CONFIGURATIONS` definisce destinazioni univoche, parametri positivi e limiti di energia totale e numero di carte. L'immutabilità è una politica applicativa; valori iniziali e altre regole restano aperti. I cataloghi delle missioni hanno `is_deleted` e `progression_required > 0`; premi e comportamento dopo cancellazione logica restano da definire, senza attribuire nuovi permessi amministrativi.
- Cancellare logicamente carte o espansioni richiede il guard esclusivo del catalogo, invalidazione di annunci/trattative e rilascio di tutti gli hold prima della conversione e pulizia di collezione e mazzi. Applicare il [protocollo transazionale comune](Database/database-schema-v1.1.md#protocollo-transazionale-comune); non è un trigger. Tassi, frammenti e recupero per lotti restano da definire.
- `NOTIFICATIONS` usa FK intere per mittente e destinatario, messaggio obbligatorio e stato `new`, `read` o `archived`. Invii di massa e regali richiedono autorizzazione backend e aggiornamenti economici separati dalla notifica.

## Separazione dei servizi

F04 mantiene catalogo e collezione in package separati. `CardCatalogService` gestisce la consultazione; `CardManagementService` gestisce le scritture F10; `CollectionService` espone soltanto letture della collezione autenticata. Il controllo `ADMIN` è applicato dal namespace `/api/admin/**`.

## API del catalogo implementate

Le rotte richiedono Bearer JWT e ruolo `ADMIN`; senza autenticazione rispondono `401`, con ruolo `USER` rispondono `403`.

| Metodo e percorso | Comportamento | Esito |
| --- | --- | --- |
| `POST /api/admin/cards` | Crea una carta con le varianti indicate | `201` con dettaglio F04 e header `Location` |
| `PUT /api/admin/cards/{cardId}` | Sostituisce la definizione e le varianti di una carta attiva non utilizzata | `200` con dettaglio F04 |
| `DELETE /api/admin/cards/{cardId}` | Cancella logicamente una carta attiva non utilizzata | `204` |

POST e PUT ricevono `name`, `description`, `typeId`, `energyCost`, `power`, `rarityId`, `attack`, `defense`, `factionId`, `expansionId` e `artworks`. `name` è obbligatorio e non vuoto; `artworks` è obbligatorio e può essere vuoto. Ogni variante contiene `artCode` obbligatorio, con uno dei soli valori esatti `STANDARD`, `EXTENDED_ART`, `FULL_ART`, `ALTERNATE_ART`, e `imageUrl` facoltativo. Il backend e il `CHECK` SQL rifiutano gli altri codici; una carta ha al massimo una variante per tipo. Showcase ed effetti speciali sovrapposti sono esclusi. Gli ID di riferimento facoltativi devono essere positivi e riferirsi a voci esistenti non cancellate. Il payload non modifica gli effetti delle carte.

Parametri non validi, riferimenti sconosciuti e codici arte duplicati producono `400`; carta assente o cancellata produce `404` su PUT/DELETE; URL immagine duplicati e carte già presenti in collezioni o mazzi producono `409`. PUT/DELETE acquisiscono il lock della carta nella transazione. La conversione delle copie e la pulizia dei futuri scambi non sono implementate da queste rotte. Le risposte di dettaglio seguono [F04](collezione-carte.md#risposta-catalogo).

## Riferimenti alla proposta di regolamento

- Il catalogo delle carte dovrà considerare, dopo la conferma delle regole, i concetti descritti nelle sezioni su [energia e punti potenza](../Digital/regolamento-base.md#3-energia-e-punti-potenza), [caratteristiche delle creature](../Digital/regolamento-base.md#5-caratteristiche-delle-creature), [fazioni](../Digital/regolamento-base.md#6-fazioni) ed [equipaggiamenti](../Digital/regolamento-base.md#7-equipaggiamenti).
- Questi riferimenti sono requisiti di dominio proposti, non uno schema dati o un contratto amministrativo approvato. Statistiche, costi delle carte ed effetti precisi degli equipaggiamenti restano da definire nella documentazione Digital.

## Verifica della protezione del namespace

Il **2026-11-12**, `mvnw.cmd -o -Dtest=SecurityConfigurationTests test` ha superato 21 casi sulla copia di lavoro con Java 21: accesso consentito ad `ADMIN`, rifiuto con `403` per `USER` e con `401` senza autenticazione, per `GET`, `HEAD`, `POST`, `PUT`, `PATCH`, `DELETE` e `OPTIONS`. I [test della catena di sicurezza](../../springboot_app/app/src/test/java/webapp/auth/SecurityConfigurationTests.java) usano un controller di prova e identità simulate; non verificano l’emissione dei JWT, il database o funzionalità amministrative reali.

## Da definire

- Matrice dei permessi e operazioni consentite su cataloghi già utilizzati.
- Eventuali altre funzioni amministrative.
- Compatibilità e versionamento delle modifiche con mazzi, partite e flussi futuri, in coordinamento con F03, F05, F06 e F07.
- Altri endpoint amministrativi e compatibilità dei futuri acquisti/aperture con la riconciliazione del CRUD F03; modifica e rimozione con possesso o mercy sono ora consentite secondo il contratto canonico.

## Collegamenti

- [Interfaccia backoffice](../FrontEnd/gestione-amministrativa.md)
- [Accesso](accesso-utenti.md)
- [Offerte di pacchetti](acquisto-pacchetti.md)
- [Carte e identità](../Digital/lore-identita-carte.md)
- [Regolamento Versione 2.1](../Digital/regolamento-base.md)
- [Catalogo e collezione F04](collezione-carte.md)
- [Schema dati v1.1](Database/database-schema-v1.1.md)
- [Mazzi](gestione-mazzi.md)
- [Sfide](sfide-simulazione.md)
- [Indice BackEnd](README.md)
