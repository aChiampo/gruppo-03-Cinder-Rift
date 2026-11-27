# Applicazione backend

Rinumerazione F03 del **2026-11-27**: `V13__create_f03_packs_schema.sql` e `V14__normalize_f03_slot_pull_rates.sql` sostituiscono le precedenti V12/V13. V12 è riservata all’altra PR e non è inclusa in questo branch. Catena corrente di undici script: V1–V8, V11 e V13–V14. [Compatibilità e ordine di integrazione](../../docs/BackEnd/acquisto-pacchetti.md). Le verifiche precedenti, inclusa quella dell’acquisto, descrivono la numerazione precedente.

Verifica della rinumerazione del **2026-11-27**: **19 test mirati superati**, zero fallimenti, errori o casi saltati, con JDK 21.0.11 e PostgreSQL 18.6 in un container temporaneo isolato, senza volumi applicativi. Comando dalla cartella backend: `mvnw.cmd -o "-Daether.syncContext.named.factory=rwlock-local" "-Dtest=PackMigrationDatabaseTests,RoundConfigurationMigrationDatabaseTests,BattleSeedMigrationDatabaseTests,CardArtworkMigrationDatabaseTests" clean test`, con connessione e chiave JWT di prova e scheduler/Compose disabilitati come descritto sotto. Verificati installazione, sequenza esatta V1–V8/V11/V13–V14, aggiornamenti, conservazione dei dati, rollback ed esecuzione successiva senza migrazioni. Controllati 468 collegamenti relativi della documentazione e assenza di vecchi script nelle risorse compilate. Suite completa, JAR e catena combinata con la V12 dell’altra PR non verificati; database applicativo non modificato.

Acquisto F03 implementato il **2026-11-27**, su richiesta dell’utente: `POST /api/users/me/pack-purchases` addebita Scaglie disponibili e accredita pacchetti chiusi nello stesso commit. Nessuna migrazione aggiunta o modificata, deduplicazione o cronologia acquisti. [Contratto corrente](../../docs/BackEnd/acquisto-pacchetti.md#acquisto-pacchetti-contratto-http).

I servizi [DebitService](src/main/java/webapp/economy/DebitService.java) e [ReservationReader](src/main/java/webapp/economy/ReservationReader.java) sono riutilizzabili da F03 e dai futuri servizi F06. Richiedono una transazione del chiamante; il lettore richiede il lock esclusivo dell’utente già acquisito. Il servizio di addebito acquisisce utente e inventario e preserva gli hold monetari, confermati in Scaglie. Futuri scrittori F06 devono usare il medesimo guard utente. In assenza completa delle tre tabelle F06 le prenotazioni valgono zero; storage parziale restituisce `503`, hold vivi incoerenti impediscono la spesa. Nessun errore di lettura diventa disponibilità libera. Backend di mercato, frontend e seed commerciale esclusi.

Le verifiche datate dell’apertura e dell’integrazione sotto sono precedenti all’acquisto.

## Verifica acquisto F03 del 2026-11-27

Verifica finale sulla copia di lavoro con JDK **21.0.11**, Maven Wrapper offline e PostgreSQL **18.6** in un container temporaneo dedicato, senza volumi applicativi. Completato `verify`: **412 test backend superati**, zero fallimenti, errori o casi saltati; prodotto `target/app-0.0.1-SNAPSHOT.jar`. Database applicativo non modificato; nessuna nuova migrazione. Frontend, browser, motore separato e flussi di mercato F06 non rieseguiti.

```powershell
.\mvnw.cmd -o "-Daether.syncContext.named.factory=rwlock-local" verify
```

Configurare le variabili di connessione e sicurezza descritte sotto su un database di prova dedicato, con entrambi gli scheduler e Docker Compose disabilitati per i test. Il database temporaneo usato per questa verifica esponeva soltanto il loopback sulla porta `55439`.

- [PackPurchaseDatabaseTests](src/test/java/webapp/pack/PackPurchaseDatabaseTests.java): **49 test**, con JWT e ruoli reali, contratto OpenAPI, input rigoroso, prezzo corrente, creazione/incremento del possesso, richieste ripetute, prezzo zero, limiti interi, rollback dopo credito, inventario mancante, risorse e mercy invariati, prenotazioni e disponibilità, hold mancanti/incoerenti, storage parziale e concorrenza con altri acquisti, scrittori di hold, aperture, PUT/DELETE amministrativi e cancellazione account.
- [ReservationReaderDatabaseTests](src/test/java/webapp/economy/ReservationReaderDatabaseTests.java): **5 test**, con cutoff esatto, hold del rispondente, overflow della somma, SQL malformato e introduzione successiva dello storage senza cache. Gli schemi e le tabelle F06 sono fixture esclusivamente di test; non sono migrazioni o inizializzazioni applicative.
- La suite completa comprende anche i **47 test API/transazionali di apertura** e i test esistenti di autenticazione, profili, amicizie, catalogo, mazzi e migrazioni.

Apertura F03 implementata il **2026-11-27**, su richiesta dell’utente: letture dell’inventario e apertura atomica di 1–10 pacchetti dello stesso tipo, filtri cumulativi, carte equiprobabili, artwork indipendente con fallback Standard e mercy sull’ultimo slot. Il servizio di validazione verifica i flussi amministrativi POST/PUT e ogni apertura. Nessuna nuova migrazione o seed commerciale; acquisto e frontend F03 restano esclusi. [Contratto e limiti](../../docs/BackEnd/acquisto-pacchetti.md).

Aggiornamento F03 del **2026-11-27**, su decisione dell’utente: V13 crea direttamente `mercy_rarity` in `packs` come FK nullable; `user_mercy` conserva soltanto utente, pacchetto e contatore. CRUD e apertura usano questo campo; la garanzia sostituisce l’ultimo slot con la rarità esatta configurata quando necessaria. Acquisto non implementato. [Contratto corrente](../../docs/BackEnd/acquisto-pacchetti.md).

Integrazione F03 con `dev` del **2026-11-27**, su richiesta dell’utente: preservati schema V3 e seed V5 ripuliti, artwork `STANDARD`, ID generati e amicizie V11. Pacchetti e slot sono ora V13/V14; catena completa di undici script, V1–V8, V11 e V13–V14. Catalogo, CRUD, inventario e apertura disponibili; acquisto e seed commerciale rinviati. [Contratto e compatibilità](../../docs/BackEnd/acquisto-pacchetti.md).

Verifica storica del cleanup di `dev` del **2026-11-27**, prima dell’integrazione F03: **263 test backend** e **51 del motore** superati; JAR backend prodotto e catena V1–V8 e V11 verificata su PostgreSQL 18.6 isolato. [Comandi, copertura e limiti](#verifica-della-riscrittura-del-2026-11-27). Frontend, browser e distribuzione Compose non rieseguiti.

Verifica storica del **2026-11-24**: **262 test backend**, **51 del motore** e **54 frontend** superati; build e stack Compose verificati con artwork `STANDARD` e amicizie V11. [Ambiente, controlli HTTP e limiti](../../docs/README.md).

Aggiornamento F13 del **2026-11-24**: backend richieste e amicizie, migrazione V11 e cleanup amicizie nella transazione F01 disponibili. Frontend F13 e scambi F06 restano da completare. [Verifiche storiche F13](../../docs/report/2026-11-22-implementazione-amicizie.md).

Evidenza storica dell'importazione parziale del **2026-11-22** in `dev`: **220 test applicativi** e **51 test JUnit del motore** superati; a quella data frontend e V10 immagini erano esclusi. [Ambito e limiti](../../docs/BackEnd/collezione-carte.md#verifica-dellimportazione-parziale-del-2026-11-22).

Ultimo aggiornamento significativo il **2026-11-27**: V3 definisce lo schema completo di carte e arti, V5 inserisce direttamente i dati definitivi e V8 usa gli ID carta generati. Rimosse V9/V10; la catena corrente è V1–V8, V11 e V13–V14. Endpoint e payload restano invariati. Le immagini delle 30 carte CR01 sono disponibili dal 2026-11-22. Aggiornamento del **2026-11-18**, con i limiti F05 di energia per carta e potenza per destinazione approvati dall’utente nelle migrazioni V3/V4. La rinomina degli endpoint F01 e la cancellazione logica degli account risalgono al 2026-11-13. Il backend include la base del recupero password, il prefisso `/api/auth` e la pulizia programmata delle sessioni. Invio email e interfaccia di recupero sono rinviati. La configurazione iniziale del backend per F01 risale al 2026-11-11. Le dipendenze sono dichiarate nel [POM](pom.xml).

Aggiornamento del **2026-11-21**: importate le API [F04 di catalogo e collezione](../../docs/BackEnd/collezione-carte.md) e [F10 di gestione carte](../../docs/BackEnd/gestione-amministrativa.md), inclusi fixture e test database; 8 test API superati il 2026-11-21 su JDK 21.0.11/PostgreSQL 18.6 isolato; JAR prodotto. Le prove non verificano il browser o le nuove immagini Compose.

Il backend utilizza Java 21 e Spring Boot 4.1.2; il database previsto è PostgreSQL 18. Dal **2026-11-16**, Compose include i Dockerfile del [frontend React](../../cinder-rift/README.md#avvio-con-docker-compose) in `cinder-rift/` e del [motore dimostrativo](../game_engine/README.md#avvio-con-docker-compose) in `game_engine/`. Allineamento ai sorgenti del **2026-11-23**: React usa F01, F04, F05 e PvE F07; storico persistito, PvP, negozio e scambi restano da realizzare. [Avvio e configurazione comune](../../guida-avvio.md).

Il codice corrente include anche profili pubblici e aggiornamento profilo F01, CRUD e validazione dei mazzi F05 e le migrazioni V1–V8, V11 e V13–V14. [Contratto F05](../../docs/BackEnd/gestione-mazzi.md).

Questo documento descrive dipendenze, configurazione e stato del backend. F01 è implementato con JWT emessi localmente e verificati da Spring Security; il [contratto F01](../../docs/BackEnd/accesso-utenti.md) riporta comportamento ed evidenze. La sola presenza di una dipendenza non attesta l’implementazione delle altre funzionalità o il funzionamento delle integrazioni.

## Dipendenze

### Gestione delle versioni

Le versioni sono gestite da Spring Boot tramite `spring-boot-starter-parent`. Fa eccezione springdoc, la cui versione è impostata esplicitamente a `3.2.0`.

Nelle tabelle seguenti, `starter-*` indica `org.springframework.boot:spring-boot-starter-*`. Lo scope predefinito di Maven è `compile`.

### Dipendenze applicative attive

| Dipendenza | Scope Maven | Scopo |
| --- | --- | --- |
| `starter-webmvc` | `compile` | API REST tramite Spring MVC e server HTTP integrato. |
| `starter-data-jpa` | `compile` | Infrastruttura di persistenza JPA e accesso JDBC transitivo. F01 usa `JdbcClient` con SQL esplicito in `AccountRepository`; non usa entità o repository JPA. |
| `org.postgresql:postgresql` | `runtime` | Driver JDBC per la connessione a PostgreSQL. |
| `starter-flyway` | `compile` | Integrazione Spring Boot per la gestione delle migrazioni versionate del database. |
| `org.flywaydb:flyway-database-postgresql` | `runtime` | Supporto specifico di Flyway per PostgreSQL. Affianca il driver JDBC senza sostituirlo. |
| `starter-security-oauth2-resource-server` | `compile` | Infrastruttura di sicurezza per la validazione dei bearer token JWT e la protezione delle risorse API. |
| `org.springdoc:springdoc-openapi-starter-webmvc-ui` | `compile` | Generazione della documentazione OpenAPI e disponibilità di Swagger UI per consultare e richiamare manualmente gli endpoint HTTP. |
| `starter-validation` | `compile` | Validazione dei campi delle richieste e degli altri dati annotati tramite Bean Validation. |
| `starter-mail` | `compile` | Supporto all'invio di email per il recupero password; implementazione rinviata, con soli commenti segnaposto nel servizio. |
| `starter-thymeleaf` | `compile` | Rendering dei futuri template email, confermato dall’utente il 2026-11-12; template e integrazione non implementati. Il frontend rimane affidato a React. |
| `starter-actuator` | `compile` | Endpoint operativi per il controllo dello stato e il monitoraggio dell'applicazione. |
| `org.springframework.boot:spring-boot-docker-compose` | `runtime` | Integrazione facoltativa per la gestione dei servizi Compose configurati nell'ambiente di sviluppo. |
| `org.projectlombok:lombok` | `compile` | Riduzione del codice ripetitivo tramite elaborazione delle annotazioni. Dichiarata con `optional=true`, con annotation processor configurato per sorgenti applicativi e test. |

Mail, Thymeleaf, Actuator, integrazione Docker Compose e Lombok rispondono a esigenze facoltative, operative o di sviluppo. I relativi flussi e configurazioni devono comunque essere implementati e verificati.

### Stato di WebSocket e Spring MVC

La dichiarazione di `starter-websocket` è attualmente commentata nel POM e non è una dipendenza applicativa attiva. Il suo impiego per notifiche in tempo reale rimane da decidere; la modalità base del gioco prevede una simulazione automatica lato server.

Il POM dichiara `starter-webmvc` direttamente per le API REST del backend.

### Dipendenze transitive

- `starter-security-oauth2-resource-server` include `starter-security`: non è necessaria una dichiarazione diretta separata.
- `starter-security-test` include `starter-test`, rendendo disponibili ai test le librerie generali di verifica, incluso JUnit Jupiter.
- Se riattivato, `starter-websocket` include `starter-webmvc`.

### Dipendenze di test

Tutte le dipendenze seguenti utilizzano lo scope Maven `test`.

| Dipendenza | Utilizzo previsto |
| --- | --- |
| `starter-data-jpa-test` | Test dei repository e della persistenza. |
| `starter-flyway-test` | Test che coinvolgono le migrazioni del database. |
| `starter-mail-test` | Test del comportamento applicativo relativo alle email. |
| `starter-security-test` | Test di autenticazione e autorizzazione. |
| `starter-thymeleaf-test` | Test relativi ai template. |
| `starter-validation-test` | Test dei comportamenti di validazione. |
| `starter-websocket-test` | Supporto ai test WebSocket; resta dichiarato anche se lo starter applicativo è commentato. La coerenza di questa scelta va rivalutata quando viene deciso il canale di comunicazione. |

Sono presenti test F01 per profilo protetto e CSRF, restrizioni del namespace amministrativo, scheduler e cancellazione delle sessioni. I report esistenti e i limiti sono descritti nel [contratto F01](../../docs/BackEnd/accesso-utenti.md#verifiche-ed-evidenze). Sono presenti anche test di profili utente, validatore, servizio e controller mazzi F05; Swagger UI non sostituisce i test automatici.

Il motore di gioco deve restare un componente Java isolato e coperto da test JUnit, come richiesto dalla [traccia](../../Laboratorio-Integrato-TRACCIA.md). I suoi test unitari devono verificare le regole senza richiedere Spring o un database. Se organizzato come modulo separato, il motore deve dichiarare autonomamente le proprie dipendenze di test.

## Responsabilità implementative

Le dipendenze forniscono l'infrastruttura. Rispetto ai sorgenti correnti restano da completare:

- Interfacce di cancellazione account e recupero password, invio email e cleanup F13/F06. Registrazione, accesso, profilo, rinnovo e uscita React sono già collegati al [backend F01](../../docs/BackEnd/accesso-utenti.md).
- Bilanciamento definitivo V2.1 e coordinamento con i futuri scrittori del catalogo: validazione F05 e riuso nel PvE sono già implementati.
- Scambi atomici e loro migrazione F06; acquisto F03 disponibile con addebito e lettore prenotazioni condivisi, senza nuova migrazione.
- Migrazioni per le funzionalità successive: V1–V8, V11 e V13–V14 coprono la base implementata, seed, effetti, URL immagini, amicizie e catalogo pacchetti; le altre tabelle del modello v1.1 restano da implementare.
- Sfide PvP e storico persistito. Simulazione PvE e snapshot per la singola esecuzione sono già implementati.
- Configurazione SMTP; database, JWT e servizi Compose richiedono già le impostazioni di avvio descritte sotto.

La selezione di PostgreSQL differisce dal database MySQL indicato nella traccia originale. Il punto è riportato nelle [differenze rispetto alla traccia](../../docs/README.md#differenze-rispetto-alla-traccia).

## Build dell'immagine Docker

Il servizio `spring_app` usa Java 21 su Alpine e il Maven Wrapper del progetto. BuildKit riutilizza i download di Maven e delle dipendenze. Il comando di packaging salta l'esecuzione dei test, da eseguire a parte con l'infrastruttura richiesta. [Dockerfile](Dockerfile) · [Configurazione del Maven Wrapper](.mvn/wrapper/maven-wrapper.properties).

L'immagine esegue `/app/app.jar` con UID e GID `10001`. Il Dockerfile seleziona `target/app-0.0.1-SNAPSHOT.jar`. Se cambiano l'identificativo o la versione dell'artefatto nel POM, aggiornare il percorso del JAR nel Dockerfile.

Spring Boot legge la porta `8081` da [application.properties](src/main/resources/application.properties). Il Dockerfile include `curl` e verifica `/actuator/health` ogni 10 secondi per l'avvio diretto con Docker. Compose usa il proprio controllo ogni 5 secondi. [Configurazione Compose](../../compose.yaml).

Con Docker avviato, eseguire dalla radice del repository:

```sh
docker compose build spring_app
```

## Configurazione F01

Gli endpoint di autenticazione usano il prefisso `/api/auth`, introdotto su richiesta dell’utente del **2026-11-12**. Dal **2026-11-13**, su ulteriore richiesta dell’utente, registrazione, accesso, profilo e uscita usano i percorsi rinominati nel [contratto F01](../../docs/BackEnd/accesso-utenti.md#contratto-tecnico-condiviso); i client devono aggiornare le chiamate secondo quel riferimento.

`AuthenticationService` verifica le password con BCrypt ed emette JWT HS256 tramite `JwtEncoder`. Il resource server Spring Security verifica questi JWT e carica a ogni richiesta profilo, ruolo e sessione dal database. La dipendenza OAuth2 resource server non introduce un provider esterno o un flusso di accesso OAuth2. Non vengono create sessioni HTTP servlet.

Impostare `APP_SECURITY_JWT_SECRET` con una chiave casuale in Base64 di almeno 32 byte. Compose passa la variabile al servizio `spring_app`. L’applicazione verifica il formato e la lunghezza della chiave all’avvio. Per l’accesso da React, `APP_SECURITY_ALLOWED_ORIGINS` accetta le origini esatte separate da virgola e usa `http://localhost:8082` come valore predefinito. [Contratto di accesso e configurazione dei cookie](../../docs/BackEnd/accesso-utenti.md#configurazione-e-operatività).

Le proprietà `app.security.access-token-lifetime` e `app.security.refresh-token-lifetime` hanno default `15m` e `14d`. Spring le legge anche da `APP_SECURITY_ACCESS_TOKEN_LIFETIME` e `APP_SECURITY_REFRESH_TOKEN_LIFETIME`; queste due variabili non sono inoltrate dal file Compose corrente e, per modificarle nel container, vanno aggiunte alla configurazione del servizio. La durata di accesso deve essere almeno un secondo e quella di rinnovo almeno pari alla durata di accesso.

### Migrazioni Flyway F01

Usare PostgreSQL 18 e configurare la connessione con `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME` e `SPRING_DATASOURCE_PASSWORD`. Con Compose, la connessione è configurata a partire dalle variabili `POSTGRES_DB`, `POSTGRES_USER` e `POSTGRES_PASSWORD`. L’utente che esegue Flyway deve poter creare oggetti nello schema e inserire righe in `roles` usando il relativo generatore di ID. DDL Hibernate, inizializzazione SQL Spring e baseline automatica di Flyway sono disabilitati. [Specifica v1.1](../../docs/BackEnd/Database/database-schema-v1.1.md) · [Configurazione applicativa](src/main/resources/application.properties).

**Database nuovo.** Creare un database vuoto e avviare normalmente il backend. Flyway applica V1 per creare le cinque tabelle F01 e V2 per inserire i ruoli `USER` e `ADMIN`. V3 crea le tabelle di catalogo, arti, collezione, configurazioni dei round e mazzi; V4 inserisce tipi, rarità, fazioni e configurazioni dei round. V5 importa le 30 carte del set iniziale CR01 e le relative arti; V6 crea l’utente dimostrativo e la sua collezione, come descritto sotto. V11 crea le amicizie; V13/V14 aggiungono pacchetti, possesso, mercy e slot relazionali. Le migrazioni non realizzano tutto il modello v1.1. Con Compose, dalla radice del repository eseguire `docker compose up --build -d spring_app` con un database vuoto. Verificare V1–V8, V11 e V13–V14 in `flyway_schema_history` e lo stato di `GET /actuator/health`. [V1](src/main/resources/db/migration/V1__create_f01_schema.sql) · [V2](src/main/resources/db/migration/V2__seed_f01_roles.sql) · [V3](src/main/resources/db/migration/V3__create_f05_cards_and_decks_schema.sql) · [V4](src/main/resources/db/migration/V4__seed_f05_reference_data.sql).

**Database con la vecchia cronologia.** La revisione del 2026-11-27 riscrive V3/V5/V6/V8 e rimuove V9/V10; il normale avvio non aggiorna uno schema installato con quei file precedenti. Le istruzioni seguenti riguardano soltanto l’adozione di una base F01 compatibile, non la conversione di un vecchio catalogo.

**Database esistente senza cronologia Flyway.**

1. Identificare il database e lo schema effettivamente usati dall’applicazione. Impostare `PGHOST`, `PGPORT`, `PGDATABASE` e `PGUSER` per la destinazione ed eseguire un backup prima di apportare modifiche, ad esempio con `pg_dump --format=custom --file=f01-before-flyway.dump`.
2. Applicare V1 in un database di riferimento vuoto e isolato, ad esempio con `psql -d "$REFERENCE_DATABASE" -v ON_ERROR_STOP=1 -f src/main/resources/db/migration/V1__create_f01_schema.sql` dalla cartella del backend. Impostare `REFERENCE_DATABASE` al nome del database isolato, oppure a una stringa di connessione se usa un’altra istanza. Verificare anche l’assenza di tabelle F05 preesistenti: V3 le crea senza `IF NOT EXISTS`, quindi eventuali oggetti già presenti richiedono riconciliazione prima della migrazione. Confrontare le definizioni delle cinque tabelle F01 nello schema effettivo con quelle del riferimento, ad esempio usando `pg_dump --schema-only --schema=public --no-owner --no-privileges` su entrambi. Verificare tipi, nullabilità, valori predefiniti, generatori di ID, vincoli, azioni delle chiavi esterne e indici. Controllare che i generatori siano allineati agli ID esistenti e che i dati rispettino il contratto F01. Riconciliare qualsiasi differenza e ripetere il confronto prima di procedere alla baseline.
3. Dalla cartella del backend, impostare `FLYWAY_URL`, `FLYWAY_USER` e `FLYWAY_PASSWORD` per la destinazione dell’adozione. Con una CLI Flyway compatibile con la versione risolta dal POM, eseguire la baseline esplicita a 1 e poi la migrazione. Adattare lo schema `public` dell’esempio a quello effettivo dell’applicazione. [Comando baseline di Flyway](https://documentation.red-gate.com/flyway/reference/commands/baseline).

   ```sh
   flyway -defaultSchema=public -schemas=public -baselineVersion=1 -baselineOnMigrate=false -locations=filesystem:src/main/resources/db/migration baseline
   flyway -defaultSchema=public -schemas=public -baselineOnMigrate=false -locations=filesystem:src/main/resources/db/migration migrate
   ```

4. Verificare in `flyway_schema_history` la baseline a 1 e l’applicazione di V2–V8 e V11 e V13–V14. Verificare i dati esistenti, gli ID dei ruoli già presenti e la presenza dei due ruoli richiesti, quindi avviare il backend e verificare `GET /actuator/health`.

### Set iniziale e utente dimostrativo F01/F05

Aggiornamento del **2026-11-16**, su richiesta dell’utente. [V5](src/main/resources/db/migration/V5__seed_initial_card_set.sql) importa le **30 carte** di `cinder-rift-cards.csv`; i valori importati sono riportati direttamente nella migrazione. Sono importate soltanto le 30 carte originali, da `CR01-001` a `CR01-030`, associate a `CR01` / «Set Dimostrativo». Le 90 carte successive del CSV sono escluse, anche dalla collezione dimostrativa. Celle vuote e valori testuali `NULL` diventano SQL `NULL`.

Aggiornamento del **2026-11-27**, su richiesta dell’utente: V3 definisce lo schema completo di carte e arti; V5 contiene solo dati iniziali, senza `ALTER TABLE`. `cards` conserva `flavor_text` e non contiene `code`, `transfer_and_discard` o `duration`. Gli ID numerici sono generati da PostgreSQL, senza assegnazioni manuali o reset della sequenza; V5 usa `RETURNING` per collegare le arti alle carte inserite. Le creature usano le fazioni `CYB`, `SAP`, `RAD`, `ALN` e `CLT`; gli equipaggiamenti usano `NEUTRAL` ed energia 0. Le arti sono subito `STANDARD`, con `source_image` originale e URL `/img/cards/<nome-carta>.webp`. V9 e V10 sono rimosse; la catena comprende undici script, V1–V8, V11 e V13–V14.

[V6](src/main/resources/db/migration/V6__seed_test_user.sql) crea nickname ed email `test@email.com`, con la password dimostrativa richiesta `Megapassword1` salvata esclusivamente come hash BCrypt. Il ruolo è `USER`; punteggio iniziale 1000, data di registrazione corrente, account attivo e inventario con tutte le valute a zero seguono i default F01. La collezione contiene una copia posseduta e scambiabile di ciascuna delle 30 carte. Mazzi, sessioni e token di recupero sono creati dai rispettivi flussi quando richiesti.

V6 è una migrazione ordinaria applicata automaticamente: crea questo account dimostrativo in ogni database che applica V6. Un’email o un nickname già occupati fanno fallire la migrazione senza sovrascrivere account esistenti. Un nuovo avvio non duplica i dati grazie alla cronologia Flyway.

I [test correnti delle configurazioni dei round](src/test/java/webapp/deck/RoundConfigurationMigrationDatabaseTests.java) verificano l’installazione V1–V8, V11 e V13–V14 su uno schema vuoto, i valori iniziali e i vincoli positivi delle configurazioni dei round, seguiti da un’esecuzione senza ulteriori migrazioni. Richiedono PostgreSQL e le stesse variabili della suite. Comando mirato `mvnw.cmd -o "-Daether.syncContext.named.factory=rwlock-local" "-Dtest=RoundConfigurationMigrationDatabaseTests" test`. La verifica dettagliata del CSV e dell’accesso dimostrativo del 2026-11-16 è un’evidenza storica descritta sotto.

## Verifica dell’apertura F03 del 2026-11-27

Nota storica: le verifiche di apertura, rarità mercy e integrazione riportate sotto precedono la rinumerazione F03 da V12/V13 a V13/V14. Versioni e risultati citati descrivono la catena verificata allora.

Verifica sulla copia di lavoro con base `f6b5651` (`feature/f03-packages`). Database dedicato `f03_opening_verification` su porta locale `55438`, in un container PostgreSQL temporaneo senza volumi applicativi. I test degli endpoint usano MockMvc con la catena Spring Security e JWT reali; transazioni, connessioni concorrenti e migrazioni usano PostgreSQL reale. Nessuna modifica al database applicativo.

Prerequisiti: JDK 21, Docker disponibile e dipendenze Maven già in cache. Dalla radice del repository:

```powershell
docker run --detach --rm --name cinder-f03-opening-verification -e POSTGRES_HOST_AUTH_METHOD=trust -e POSTGRES_DB=f03_opening_verification -p 127.0.0.1:55438:5432 postgres:18-alpine
docker exec cinder-f03-opening-verification pg_isready -U postgres -d f03_opening_verification
```

Attendere che PostgreSQL sia pronto; dalla cartella `springboot_app/app/`:

```powershell
$env:JAVA_HOME = 'C:\Program Files\Java\jdk-21.0.11'
$env:SPRING_DATASOURCE_URL = 'jdbc:postgresql://localhost:55438/f03_opening_verification'
$env:SPRING_DATASOURCE_USERNAME = 'postgres'
$env:SPRING_DATASOURCE_PASSWORD = ''
$f03Key = New-Object byte[] 32
$f03Rng = [Security.Cryptography.RandomNumberGenerator]::Create()
$f03Rng.GetBytes($f03Key)
$env:APP_SECURITY_JWT_SECRET = [Convert]::ToBase64String($f03Key)
$f03Rng.Dispose()
$env:SPRING_DOCKER_COMPOSE_ENABLED = 'false'
$env:APP_SECURITY_SESSION_TOKEN_CLEANUP_CRON = '-'
$env:APP_SECURITY_PASSWORD_RESET_TOKEN_CLEANUP_CRON = '-'
.\mvnw.cmd -o '-Daether.syncContext.named.factory=rwlock-local' test-compile
.\mvnw.cmd -o '-Daether.syncContext.named.factory=rwlock-local' '-Dtest=PackDrawingServiceTests,PackOpeningDatabaseTests,PackControllerDatabaseTests,PackDeletionDatabaseTests,PackMigrationDatabaseTests' test
.\mvnw.cmd -o '-Daether.syncContext.named.factory=rwlock-local' clean verify
docker stop cinder-f03-opening-verification
```

Il factory `rwlock-local` evita scritture ai file di lock della cache Maven nell’ambiente ristretto. La chiave JWT è generata soltanto per la sessione di prova. Il container usa autenticazione di test e viene rimosso al termine; nessun servizio Compose applicativo viene ricreato.

La compilazione backend/test e i **93 test mirati** sono superati, senza fallimenti, errori o casi saltati: 7 di estrazione, 47 di apertura, 24 di catalogo/CRUD, 13 di migrazione e 2 di cancellazione con rollback.

`mvnw.cmd -o "-Daether.syncContext.named.factory=rwlock-local" clean verify` ha superato **358 test backend** in 33 suite, senza fallimenti, errori o casi saltati, e prodotto il JAR con JDK 21.0.11 e PostgreSQL 18.6. Controllati anche i report XML e il JAR eseguibile `target/app-0.0.1-SNAPSHOT.jar`: contiene i servizi di apertura e gli undici script correnti V1–V8/V11–V13. Confermata la stessa catena nella cronologia Flyway del database isolato; nessuna nuova migrazione o seed commerciale.

- [7 test deterministici](src/test/java/webapp/pack/PackDrawingServiceTests.java): frontiere delle probabilità a quattro decimali, equiprobabilità delle carte, duplicati e arti diverse nello stesso pacchetto, fallback Standard, avanzamento/reset mercy e sostituzione dell’ultimo slot con rarità esatta.
- [47 test di apertura](src/test/java/webapp/pack/PackOpeningDatabaseTests.java): Bearer JWT e ruolo USER esclusivo, inventario privato/vuoto/mancante, possesso non acquisibile, input senza coercizioni, limiti 1/10, schemi delle risposte e codici OpenAPI, consumo e accrediti dopo commit, scambiabilità invariata, mercy persistente a possesso zero e transizioni sequenziali nel lotto. Rollback dopo cancellazione dell’ultimo possesso o scrittura mercy, overflow, conflitto immediato NOWAIT senza pescate, ripresa dopo rilascio, PUT/DELETE concorrenti in entrambi gli ordini, incrementi atomici di tipi diversi e aggiornamenti condivisi della collezione; validazione cumulativa su POST/PUT e apertura, compreso il pool esatto mercy.
- Suite esistente di autenticazione, collezione, mazzi, amicizie, orchestrazione PvE, catalogo pacchetti, cancellazione e migrazioni inclusa nell’esecuzione completa. Le fixture amministrative sono state adeguate alle nuove verifiche di eleggibilità, senza modificare il seed commerciale.

Controllati 461 collegamenti locali e 213 riferimenti a sezioni in dieci documenti, senza destinazioni mancanti; `git diff --check` superato. Il database di prova temporaneo è stato rimosso al termine. Il database applicativo non è stato modificato. Frontend, browser, distribuzione Compose e motore separato non rieseguiti; acquisto, assegnazione iniziale dei pacchetti e seed commerciale restano da realizzare. Risultati delle pescate non persistiti: nessun registro, chiave di richiesta, deduplicazione o recupero introdotto.

## Verifica della rarità mercy del 2026-11-27

Eseguito `mvnw.cmd -o "-Dtest=PackControllerDatabaseTests,PackMigrationDatabaseTests,RoundConfigurationMigrationDatabaseTests" clean test` con JDK 21.0.11 e PostgreSQL 18.7 in un container temporaneo senza volumi applicativi: **38 test superati**, nessun fallimento, errore o caso saltato. Verificati installazione V1–V8/V11–V13, definizione diretta della FK nullable in V12, assenza della rarità nei progressi utente, conservazione di configurazione/contatori/possesso attraverso V13, API e configurazioni round. Build pulita senza migrazioni rimosse nelle risorse compilate. Suite completa e JAR non rieseguiti; frontend, acquisto e apertura non verificati. Database applicativo non modificato.

## Verifica dell’integrazione F03 del 2026-11-27

Su `feature/f03-packages`, durante la risoluzione del merge da `dev`, `mvnw.cmd -o "-Daether.syncContext.named.factory=rwlock-local" verify` supera **300 test backend**, senza fallimenti, errori o casi saltati, e produce il JAR con JDK 21.0.11 e PostgreSQL 18.6 in un container temporaneo dedicato. Il JAR contiene esattamente undici migrazioni: V1–V8 e V11–V13. I file V1–V8 e V11 coincidono con il genitore `dev` del merge (`f9a779c`); F03 usa V12/V13, senza collisioni.

Verificati installazione vuota, aggiornamento dalla base ripulita V11 fino a V13, conservazione di utenti, saldi, carte, arti complete, collezione, amicizie, effetti e mazzi; CRUD pacchetti, visibilità per ruolo, slot, precisione delle probabilità, mercy amministrativa e rollback dei rimborsi. Le fixture usano gli ID generati e non interrogano più `cards.code`.

Separatamente, `mvnw.cmd -o "-Daether.syncContext.named.factory=rwlock-local" test` nel motore supera **51 test JUnit**; `npm test` nel frontend supera **54 test** e `npm run build` riesce, senza differenze nel build distribuito da `dev`. Verificati JSON del diagramma, tabelle DBML, collegamenti relativi e sezioni della documentazione modificata e `git diff --check`. Browser e distribuzione Compose non eseguiti; database applicativo e volumi esistenti non modificati. La vecchia cronologia precedente al cleanup richiede un trattamento separato, descritto nel contratto F03.

### Verifica della riscrittura del 2026-11-27

Verifica storica prima dell’integrazione F03. Sul branch `feature/f04-card-migration-cleanup`, con JDK 21.0.11 e PostgreSQL 18.6 in un container temporaneo senza volumi applicativi, `mvnw.cmd -o "-Daether.syncContext.named.factory=rwlock-local" clean verify` supera **263 test backend**, senza fallimenti, errori o casi saltati, e produce il JAR. Il JAR contiene esattamente nove script SQL: V1–V8 e V11. Il comando parte dalla cartella `springboot_app/app/`, con connessione e chiave JWT di prova e scheduler disabilitati come descritto nelle sezioni di verifica.

Le prove verificano schema completo V3 e ID identity, dati definitivi già a V5, sette associazioni ai cinque equipaggiamenti, collezione dimostrativa e snapshot PvE. Il test degli effetti consuma un ID, elimina la riga e inserisce una carta omonima esterna al set prima di V5: le nuove carte e le relazioni usano gli ID generati effettivi. Verificato anche l’inserimento successivo di una carta senza conflitti di sequenza e il riavvio senza nuove migrazioni. Controllati la conservazione di nomi, statistiche, descrizioni, flavor text e nomi immagine delle 30 carte, l’esistenza dei 30 asset WebP e l’assenza di nuovi collegamenti relativi mancanti nei documenti modificati.

Separatamente, dalla cartella `springboot_app/game_engine/`, `mvnw.cmd -o "-Daether.syncContext.named.factory=rwlock-local" test` supera **51 test JUnit**, senza fallimenti, errori o casi saltati. Frontend, browser e distribuzione Compose non rieseguiti. Il database applicativo non viene modificato; le verifiche storiche sotto descrivono le migrazioni precedenti alla riscrittura.

### Immagini del set iniziale F04

[V5](src/main/resources/db/migration/V5__seed_initial_card_set.sql) crea direttamente le 30 arti `STANDARD` con gli URL delle illustrazioni estratte dagli originali `Carte/` e salvate in [public/img/cards](../../cinder-rift/public/img/cards/) come WebP lossless. I riferimenti usano gli ID generati delle carte; `source_image` conserva i nomi originali. [Contratto e limiti F04](../../docs/BackEnd/collezione-carte.md#immagini-del-set-cr01).

Queste migrazioni riscritte richiedono un database nuovo: una vecchia cronologia Flyway non è compatibile con i checksum e i file attuali. La ricreazione del database applicativo è un’operazione separata e non viene eseguita da questa revisione. Distribuire anche il frontend ricostruito: le immagini sono servite dalla sua origine, non dal backend.

### Effetti delle carte F07

Aggiornamento richiesto dall'utente il **2026-11-20**. [V7](src/main/resources/db/migration/V7__create_card_effects_schema.sql) aggiunge `effects` e `card_effects` con FK, parametri JSONB a oggetto e ordine positivo univoco per carta. [V8](src/main/resources/db/migration/V8__seed_initial_card_effects.sql) inizializza tre comportamenti e sette associazioni ai cinque equipaggiamenti CR01, risolvendo i `cards.id` generati per nome esatto ed espansione CR01 e gli ID effetto tramite `effects.code`. Non sono presupposti ID numerici delle carte. Il modello vale anche per le creature; non vengono aggiunte abilità non presenti nel seed originale.

Su un database vuoto Flyway applica V1–V8, V11 e V13–V14. Le descrizioni delle carte restano testo editoriale; il comportamento eseguibile deriva da codice effetto e parametri. Il backend carica le associazioni tramite `card_effects.card_id`, ordinate per `execution_order`, per costruire gli snapshot del [contratto interno del motore](../../docs/BackEnd/app-battaglie.md#attivazione-e-responsabilità-degli-effetti).

### Battaglie applicative PvE F07

Il package `webapp.battle` implementa il [contratto pubblico](../../docs/BackEnd/app-battaglie-api.md): Bearer F01, solo `deckId` nel body, controllo del proprietario, validazione completa F05 e snapshot in transazione breve `REPEATABLE READ`. `CompositionLimitsRule` completa le regole V2.1 del validatore condiviso. La simulazione remota avviene dopo il commit, senza mantenere lock utente/mazzo o una transazione aperta.

V5 inserisce direttamente le fazioni corrette delle 25 creature CR01 e l’energia 0 dei cinque equipaggiamenti iniziali. Non sono previste migrazioni successive di riparazione di questi dati.

| Configurazione | Locale | Compose |
| --- | --- | --- |
| `app.battle.engine-url` / `APP_BATTLE_ENGINE_URL` | `http://localhost:8083` | `http://game-engine:8083` |

Il budget cumulativo approvato è 8 secondi per la deadline cooperativa del motore, 9 per la risposta applicativa e 10 per il timeout frontend. Il guard L2 usa una connessione PostgreSQL riservata, diversa da quella della transazione breve: dimensionare il pool per queste due esigenze. Le richieste concorrenti dello stesso utente ricevono `409`; al termine HTTP il guard viene rilasciato anche se il lavoro remoto resta incerto. Nessun retry automatico, storico, premio o avanzamento della campagna. I limiti in caso di guasto e le verifiche sono nella [specifica applicativa](../../docs/BackEnd/app-battaglie.md).

Verifica corrente del **2026-11-20**: `clean verify` ha superato **212 test** e prodotto il JAR con JDK 21.0.11 e PostgreSQL 18.6 isolato; **49 test del motore** superati separatamente. Verificato anche il flusso HTTP tra i due JAR reali, con mazzo CR01 migrato, JWT F01 e risposte `200`/`409`/`404`. [Report e limiti](https://github.com/aChiampo/gruppo-03-Cinder-Rift/blob/73ad701a20b4fbd3e540689e92b5569a4c1e4da0/docs/report/2026-11-20-orchestrazione-pve.md). La verifica degli effetti riportata sotto precede l'orchestrazione.

Verifica del **2026-11-20** con `mvnw.cmd clean test -q`, JDK 21.0.11 e PostgreSQL 18.6 in un container di prova isolato: **139 test applicativi superati**, senza fallimenti, errori o casi saltati. I [test degli effetti](src/test/java/webapp/deck/CardEffectsMigrationDatabaseTests.java) coprono installazione vuota, aggiornamento da V6, ID effetto generati non fissi, associazioni e parametri del seed, FK, JSONB a oggetto, ordine univoco/positivo, uso sulle creature e cancellazione delle associazioni con la carta. Il motore ha superato separatamente 45 test. Non sono verificate l'orchestrazione PvE, il browser o una distribuzione sul database dell'utente.

## Pulizia delle sessioni F01

Il job [SessionTokenCleanup](src/main/java/webapp/auth/SessionTokenCleanup.java) usa lo scheduler Spring già incluso nelle dipendenze e il clock UTC condiviso con l’autenticazione. Il comportamento di cancellazione è definito nel [contratto F01](../../docs/BackEnd/accesso-utenti.md#ciclo-di-vita-dati-e-concorrenza).

| Proprietà | Variabile d’ambiente inoltrata da Compose | Predefinito |
| --- | --- | --- |
| `app.security.session-token-cleanup-cron` | `APP_SECURITY_SESSION_TOKEN_CLEANUP_CRON` | `0 0 3 * * *` (ogni giorno alle 03:00) |
| `app.security.session-token-cleanup-zone` | `APP_SECURITY_SESSION_TOKEN_CLEANUP_ZONE` | `UTC` |

Il formato cron Spring comprende i secondi: secondo, minuto, ora, giorno del mese, mese, giorno della settimana. Impostare `APP_SECURITY_SESSION_TOKEN_CLEANUP_CRON=-` per disabilitare il job. Con più repliche, questa impostazione permette di lasciarlo attivo su una sola istanza. Le modifiche alle variabili richiedono il riavvio o la ricreazione del servizio.

Il job usa la tabella `REFRESH_TOKENS` dello schema F01. Il backend deve rimanere avviato all’ora prevista; dopo un arresto la pulizia riprende al successivo orario del cron. Il log contiene il numero di righe eliminate, senza token, hash o identificativi degli utenti.

I test [dello scheduler](src/test/java/webapp/auth/SessionTokenCleanupTests.java) non richiedono PostgreSQL. Il [test di cancellazione](src/test/java/webapp/auth/SessionTokenCleanupDatabaseTests.java) richiede la stessa connessione PostgreSQL dei test applicativi e usa una tabella temporanea locale alla connessione, eliminata al termine, senza modificare le sessioni persistenti. Per eseguire l’intera suite, configurare `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD`, `APP_SECURITY_JWT_SECRET` e `SPRING_DOCKER_COMPOSE_ENABLED=false`, quindi eseguire `mvnw.cmd verify` dalla cartella dell’applicazione.

### Base del recupero password F01

Le due rotte pubbliche di recupero, i payload e il requisito CSRF sono nel [contratto canonico](../../docs/BackEnd/accesso-utenti.md#recupero-password-base-backend). `AuthenticationService` genera token opachi con validità fissa di **10 minuti**, ne persiste soltanto SHA-256 e preserva quelli precedenti alla richiesta di un nuovo token. Il completamento transazionale aggiorna la password BCrypt, consuma il token, elimina gli altri link non usati dello stesso utente e revoca tutte le sue sessioni. Login, richiesta di recupero e rinnovo condividono il lock utente.

V1 include `PASSWORD_RESET_TOKENS`. Il backend conserva soltanto l’hash del token; l’invio e il rendering hanno commenti `TODO` e sono documentati in [Invio mail](../../docs/BackEnd/invio-mail.md). Invio email e interfaccia React sono prerequisiti per rendere disponibile il flusso di recupero all’utente finale.

La pulizia separata [PasswordResetTokenCleanup](src/main/java/webapp/auth/PasswordResetTokenCleanup.java) elimina token scaduti o usati senza conservazione, con default giornaliero alle 03:00 UTC:

| Proprietà | Variabile inoltrata da Compose | Default |
| --- | --- | --- |
| `app.security.password-reset-token-cleanup-cron` | `APP_SECURITY_PASSWORD_RESET_TOKEN_CLEANUP_CRON` | `0 0 3 * * *` |
| `app.security.password-reset-token-cleanup-zone` | `APP_SECURITY_PASSWORD_RESET_TOKEN_CLEANUP_ZONE` | `UTC` |

Il cron `-` disabilita questa pulizia indipendentemente da quella delle sessioni. La validazione del token controlla comunque subito scadenza e uso. I [controlli esclusi su richiesta esplicita dell’utente](../../docs/BackEnd/accesso-utenti.md#controlli-esclusi-su-richiesta-esplicita-dellutente) sono documentati nel contratto.

I [test PostgreSQL del recupero](src/test/java/webapp/auth/PasswordResetDatabaseTests.java) usano uno schema casuale dedicato, che creano ed eliminano senza modificare i dati applicativi. L’utente di prova necessita quindi di `CREATE` sul database, oltre alla connessione. Le prove verificano anche rollback e lock con più connessioni. Il comando completo resta `mvnw.cmd verify` con le variabili di database e sicurezza indicate sopra; disabilitare entrambi gli scheduler durante le prove applicative.

Per le verifiche senza database (con JDK 21 e `JAVA_HOME` impostato alla sua cartella):

```sh
mvnw.cmd -o "-Dtest=AccountDeletionTests,RegistrationRequestTests,SecurityConfigurationTests,SessionTokenCleanupTests,PasswordResetTokenCleanupTests,UserServiceTests,UserProfileUpdateRequestTests,DeckValidatorTests,DestinationLimitsRuleTests" test
```

### Aggiornamento dei limiti F05 del 2026-11-18

Su approvazione dell’utente, V3 definisce `max_card_energy` e `max_round_power`. V4 configura energia individuale uguale a `round_number`, cinque carte per destinazione, potenza 14 per `ROUND1`–`ROUND5` e 10 per `EQUIPMENT`, la cui energia massima è 6. Il server applica `cards.energy_cost <= max_card_energy` per ogni copia e `sum(cards.power) <= max_round_power` per destinazione. La potenza è sommata con `long`; costi mancanti o negativi producono errori bloccanti. I limiti sono inclusivi e configurabili nel database. Il [contratto F05](../../docs/BackEnd/gestione-mazzi.md#implementazione-backend-disponibile) descrive i nuovi codici di errore.

La procedura scelta dall’utente per il prototipo ricrea il database tramite il reset dei volumi Docker Compose. Al successivo avvio Flyway applica V1–V8, V11 e V13–V14 su un database vuoto, usando lo schema V3 e i dati iniziali V4 aggiornati. Dalla radice del repository eseguire la procedura seguente.

```sh
docker compose down --volumes
docker compose up --build --wait
```

Verificare V1–V8, V11 e V13–V14 in `flyway_schema_history` e i limiti di `round_configurations`, inclusi energia 6 e potenza 10 per `EQUIPMENT`.

### Cancellazione account F01

Il [contratto di cancellazione account](../../docs/BackEnd/accesso-utenti.md#cancellazione-account) descrive API, permessi, transazione e comportamento del client. La cancellazione usa il flag `users.is_deleted` già presente, revoca tutte le sessioni e rimuove tutti i token di recupero dell’account nella stessa transazione.

Aggiornamento del **2026-11-22**: implementati gli endpoint del [contratto F13](../../docs/BackEnd/amicizie.md), con migrazione [V11](src/main/resources/db/migration/V11__create_friendships.sql), lock utenti ordinati e cleanup amicizie nella transazione F01. Nella catena riscritta V11 segue V8; non esiste più una V10. Questa progressione vale per installazioni che usano le migrazioni riscritte, mentre una vecchia cronologia richiede il trattamento separato descritto sopra. Frontend e cleanup scambi F06 esclusi. [Report, problemi e verifiche](../../docs/report/2026-11-22-implementazione-amicizie.md).

I [test senza database](src/test/java/webapp/auth/AccountDeletionTests.java) esercitano controller, servizio e catena di sicurezza con stati del repository simulati. I [test PostgreSQL](src/test/java/webapp/auth/AccountDeletionDatabaseTests.java) usano le migrazioni F01 in schemi casuali dedicati ed esercitano JWT reali, rollback e concorrenza. Per eseguirli, configurare le variabili di database e sicurezza indicate sopra su un database di prova dedicato, con permesso `CREATE`, ed eseguire `mvnw.cmd verify`. Le evidenze effettive sono nel [contratto F01](../../docs/BackEnd/accesso-utenti.md#verifiche-ed-evidenze).

## Verifiche e limiti

Verifica F05 del **2026-11-18** sulla copia di lavoro con gli obiettivi Maven `clean verify`, usando il POM corrente con directory di build isolata `target/f05-verification`, Java **21.0.11** e PostgreSQL **18.6** in un container di prova dedicato. Superati **135 test**, con zero fallimenti, errori o casi saltati, e prodotto il JAR. Verificati energia individuale, potenza aggregata, frontiere inclusive, metadati invalidi, overflow, errori HTTP e conservazione delle righe dopo rifiuto. Le prove Flyway coprono installazione V1–V6 su uno schema vuoto con V3/V4 aggiornate, limiti iniziali, vincoli positivi ed esecuzioni successive senza ulteriori migrazioni. Bilanciamento e integrazione React restano da verificare.

Verifica storica del **2026-11-16** per V5/V6 sulla copia di lavoro: `mvnw.cmd -o "-Daether.syncContext.named.factory=rwlock-local" verify` ha superato **241 test**, senza fallimenti, errori o casi saltati, e prodotto il JAR con Java **21.0.11** e PostgreSQL **18.6** in un container dedicato. I 123 nuovi casi verificano le 30 carte originali, l’assenza delle 90 successive, installazione vuota, aggiornamento da V4 con dati esistenti, secondo avvio senza duplicati, inventario, collezione e login dell’account dimostrativo. La fixture CSV coincide byte per byte con il file fornito; controllati 161 collegamenti relativi nei documenti modificati e `git diff --check`. Le prove non modificano il database applicativo dell’utente e non verificano browser o integrazione React.

Verifica storica del **2026-11-13**, copia di lavoro con base `97200d9`: `mvnw.cmd -o "-Daether.syncContext.named.factory=rwlock-local" verify` ha superato **118 test** su PostgreSQL **18.6** in un container dedicato e ha prodotto il JAR. Applicate V1–V4 su database vuoto; corretti CSRF con Bearer, limite nickname e isolamento dei test. `docker compose config --quiet` è passato. La build dell’immagine, l’avvio applicativo tramite Compose e il frontend non sono verificati. [Report con risultati e limiti](https://github.com/aChiampo/gruppo-03-Cinder-Rift/blob/6208de547e9c1d6d69a9021e0d0ef1a2add9beba/docs/report/2026-11-13-revisione-stato-sviluppo.md). Le evidenze seguenti sono precedenti.

Il **2026-11-12**, su PostgreSQL 18.1 isolato e Java 21.0.11, `mvnw.cmd -o "-Daether.syncContext.named.factory=rwlock-local" verify` ha superato **44 test** senza fallimenti, errori o casi saltati. La suite e le prove di avvio del JAR sono passate sia con l’attivazione Flyway di prova sia con la configurazione predefinita. Sono stati verificati installazione da zero, riavvio, autenticazione e adozione di uno schema F01 predisposto separatamente. [Evidenze F01](../../docs/BackEnd/accesso-utenti.md#verifiche-ed-evidenze).

`docker compose config --quiet` ha validato la configurazione. Il motore Linux di Docker Desktop era indisponibile, quindi le prove PostgreSQL 18.1 sono state eseguite con il JAR e un’istanza PostgreSQL locale isolata. Restano da verificare l’avvio tramite Compose e l’integrazione frontend. Invio email e template Thymeleaf restano da implementare.

La revisione documentale del **2026-11-06** riguarda corrispondenza con il POM, collegamenti relativi e formattazione Markdown. Non attesta risoluzione delle dipendenze Maven, compilazione, avvio, integrazioni o test applicativi.

Il **2026-11-11** Compose ha validato la configurazione e risolto il Dockerfile e il contesto di build di `spring_app`. Il motore Linux di Docker Desktop era indisponibile durante il tentativo di build. Restano da verificare la build dell'immagine, l'avvio del servizio e l'accesso all'endpoint di salute con la configurazione effettiva del database e della sicurezza.

## Collegamenti

- [POM dell'applicazione](pom.xml)
- [Indice della documentazione BackEnd](../../docs/BackEnd/README.md)
- [Accesso utenti e autenticazione — F01](../../docs/BackEnd/accesso-utenti.md)
- [Invio mail con template Thymeleaf — dipendenza F01](../../docs/BackEnd/invio-mail.md)
- [Specifica della base di dati v1.1](../../docs/BackEnd/Database/database-schema-v1.1.md)
- [Sfide e simulazione — F07](../../docs/BackEnd/sfide-simulazione.md)
- [README del progetto](../../README.md)
