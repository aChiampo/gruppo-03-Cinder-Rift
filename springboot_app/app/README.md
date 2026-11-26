# Applicazione backend

Aggiornamento F03 del **2026-11-26**: catalogo per ruolo senza paginazione, PUT con possesso conservato e pulizia mercy, DELETE con rimborso in `cobalt_shards` al prezzo corrente. **257 test backend superati** e JAR prodotto; [contratto, verifiche e limiti](../../docs/BackEnd/acquisto-pacchetti.md#Catalogo%20implementato%3A%20migrazione%20e%20API).

Verifica F03 del **2026-11-25**: **251 test backend superati** e JAR prodotto su JDK 21.0.11/PostgreSQL 18.6 isolato. [Ambito e limiti](../../docs/BackEnd/acquisto-pacchetti.md#Slot%20e%20probabilit%C3%A0%20per%20rarit%C3%A0).

Aggiornamento F03 del **2026-11-25**: [V11](src/main/resources/db/migration/V11__normalize_f03_slot_pull_rates.sql) sostituisce gli slot JSON con `pack_slots`, `slot_pull_type` e `type_pull_rate`. Distribuzioni per rarità e artwork usano `decimal(7,4)`; `packs.is_acquirable` parte da false. [Contratto API, vincoli e aggiornamento dei dati legacy](../../docs/BackEnd/acquisto-pacchetti.md#slot-e-probabilità-per-rarità). V1–V10 restano immutate; nessun seed commerciale o dei tipi, acquisto/apertura rinviati.


Aggiornamento F03 del **2026-11-24**, branch `feature/f03-packages` da `dev` (`fa95b6c`): V10 crea `artwork_pull_rates`, `packs`, `inventory_packs` e `user_mercy`, inizializza la configurazione artwork 81/10/7,5/1,5 e normalizza `cards_arts.art_code`, senza seed dei pacchetti. Ogni pacchetto richiede `id_artwork_pull_rates`; codici ammessi e contratto sono in [F03](../../docs/BackEnd/acquisto-pacchetti.md#tipi-e-probabilità-degli-artwork). Disponibili [catalogo e CRUD amministrativo](../../docs/BackEnd/acquisto-pacchetti.md#catalogo-implementato-migrazione-e-api); apertura e acquisto rinviati. La V10 immagini del branch `test-connessione` non è inclusa. Verifica dell’allineamento artwork del **2026-11-24**: **244 test** e JAR superati con JDK 21.0.11/PostgreSQL 18.7 isolato; [dettagli e limiti](../../docs/BackEnd/acquisto-pacchetti.md#Tipi%20e%20probabilit%C3%A0%20degli%20artwork).
Importazione parziale del **2026-11-22** da `test-connessione`: API F04/F10, test e mescolamento ordinario del tutorial F07. A quella data migrazioni ferme a V9; V10 delle immagini, modifica al relativo test di migrazione e modifiche frontend escluse. Le verifiche del branch sorgente riportate sotto sono evidenze storiche, non nuove esecuzioni.

Verifica locale dell'importazione parziale del **2026-11-22**: `mvnw.cmd -o "-Daether.syncContext.named.factory=rwlock-local" clean verify` supera **220 test**, con zero fallimenti, errori o casi saltati; JAR prodotto su JDK 21.0.11/PostgreSQL 18.6 isolato. Verificate V1-V9 senza modificare il test di migrazione. [Ambito e limiti](../../docs/BackEnd/collezione-carte.md#verifica-dellimportazione-parziale-del-2026-11-22).

Ultimo aggiornamento significativo il **2026-11-18**, con i limiti F05 di energia per carta e potenza per destinazione approvati dall’utente nelle migrazioni V3/V4. La rinomina degli endpoint F01 e la cancellazione logica degli account risalgono al 2026-11-13. Il backend include la base del recupero password, il prefisso `/api/auth` e la pulizia programmata delle sessioni. Invio email e interfaccia di recupero sono rinviati. La configurazione iniziale del backend per F01 risale al 2026-11-11. Le dipendenze sono dichiarate nel [POM](pom.xml).

Aggiornamento del **2026-11-21**: importate le API [F04 di catalogo e collezione](../../docs/BackEnd/collezione-carte.md) e [F10 di gestione carte](../../docs/BackEnd/gestione-amministrativa.md), inclusi fixture e test database; 8 test API superati il 2026-11-21 su JDK 21.0.11/PostgreSQL 18.6 isolato; JAR prodotto. Le prove non verificano il browser o le nuove immagini Compose.

Il backend utilizza Java 21 e Spring Boot 4.1.2; il database previsto è PostgreSQL 18. Dal **2026-11-16**, Compose include i Dockerfile del [prototipo React](../../cinder-rift/README.md#avvio-con-docker-compose) in `cinder-rift/` e del [motore dimostrativo](../game_engine/README.md#avvio-con-docker-compose) in `game_engine/`. Autenticazione frontend e API di gioco restano da integrare. [Avvio e configurazione comune](../../guida-avvio.md).

Il codice corrente include anche profili pubblici e aggiornamento profilo F01, CRUD e validazione dei mazzi F05 e le migrazioni V1–V10, incluse le tabelle F03 senza seed. [Contratto F05](../../docs/BackEnd/gestione-mazzi.md).

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

Le dipendenze forniscono l'infrastruttura. L'applicazione deve comunque implementare:

- Integrazione FrontEnd di registrazione, accesso, profilo, rinnovo e uscita. Il backend F01 è descritto nel [contratto di accesso](../../docs/BackEnd/accesso-utenti.md).
- Completamento delle regole V2.1 e del coordinamento con il catalogo: proprietà e validazione iniziale dei mazzi sono già implementate in F05.
- Acquisti e scambi atomici, rispettando il protocollo di lock documentato nella specifica del database.
- Migrazioni per le funzionalità successive: V1–V4 coprono F01 e la base F05; le tabelle del resto del modello v1.1 e i relativi mapping JSONB restano da implementare.
- Simulazione delle partite, snapshot immutabili e storico.
- Configurazione di database, SMTP, JWT ed eventuali servizi Compose.

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

**Database nuovo.** Creare un database vuoto e avviare normalmente il backend. Flyway applica V1 per creare le cinque tabelle F01 e V2 per inserire i ruoli `USER` e `ADMIN`. V3 crea le tabelle di catalogo, arti, collezione, configurazioni dei round e mazzi; V4 inserisce tipi, rarità, fazioni e configurazioni dei round. V5 importa le 30 carte del set iniziale CR01 e le relative arti; V6 crea l’utente dimostrativo e la sua collezione, come descritto sotto. V7/V8 aggiungono gli effetti e il relativo seed; V9 ripara i dati dimostrativi delle battaglie; V10 crea pacchetti, possesso e progressi mercy senza seed dei pacchetti. Le migrazioni non realizzano tutto il modello v1.1. Con Compose, dalla radice del repository eseguire `docker compose up --build -d spring_app` con un database vuoto. Verificare V1–V10 in `flyway_schema_history` e lo stato di `GET /actuator/health`. [V1](src/main/resources/db/migration/V1__create_f01_schema.sql) · [V2](src/main/resources/db/migration/V2__seed_f01_roles.sql) · [V3](src/main/resources/db/migration/V3__create_f05_cards_and_decks_schema.sql) · [V4](src/main/resources/db/migration/V4__seed_f05_reference_data.sql).

**Database esistente senza cronologia Flyway.**

1. Identificare il database e lo schema effettivamente usati dall’applicazione. Impostare `PGHOST`, `PGPORT`, `PGDATABASE` e `PGUSER` per la destinazione ed eseguire un backup prima di apportare modifiche, ad esempio con `pg_dump --format=custom --file=f01-before-flyway.dump`.
2. Applicare V1 in un database di riferimento vuoto e isolato, ad esempio con `psql -d "$REFERENCE_DATABASE" -v ON_ERROR_STOP=1 -f src/main/resources/db/migration/V1__create_f01_schema.sql` dalla cartella del backend. Impostare `REFERENCE_DATABASE` al nome del database isolato, oppure a una stringa di connessione se usa un’altra istanza. Verificare anche l’assenza di tabelle F05 preesistenti: V3 le crea senza `IF NOT EXISTS`, quindi eventuali oggetti già presenti richiedono riconciliazione prima della migrazione. Confrontare le definizioni delle cinque tabelle F01 nello schema effettivo con quelle del riferimento, ad esempio usando `pg_dump --schema-only --schema=public --no-owner --no-privileges` su entrambi. Verificare tipi, nullabilità, valori predefiniti, generatori di ID, vincoli, azioni delle chiavi esterne e indici. Controllare che i generatori siano allineati agli ID esistenti e che i dati rispettino il contratto F01. Riconciliare qualsiasi differenza e ripetere il confronto prima di procedere alla baseline.
3. Dalla cartella del backend, impostare `FLYWAY_URL`, `FLYWAY_USER` e `FLYWAY_PASSWORD` per la destinazione dell’adozione. Con una CLI Flyway compatibile con la versione risolta dal POM, eseguire la baseline esplicita a 1 e poi la migrazione. Adattare lo schema `public` dell’esempio a quello effettivo dell’applicazione. [Comando baseline di Flyway](https://documentation.red-gate.com/flyway/reference/commands/baseline).

   ```sh
   flyway -defaultSchema=public -schemas=public -baselineVersion=1 -baselineOnMigrate=false -locations=filesystem:src/main/resources/db/migration baseline
   flyway -defaultSchema=public -schemas=public -baselineOnMigrate=false -locations=filesystem:src/main/resources/db/migration migrate
   ```

4. Verificare in `flyway_schema_history` la baseline a 1 e l’applicazione di V2–V9. Verificare i dati esistenti, gli ID dei ruoli già presenti e la presenza dei due ruoli richiesti, quindi avviare il backend e verificare `GET /actuator/health`.

### Set iniziale e utente dimostrativo F01/F05

Aggiornamento del **2026-11-16**, su richiesta dell’utente. [V5](src/main/resources/db/migration/V5__seed_initial_card_set.sql) importa le **30 carte** di `cinder-rift-cards.csv`; i valori importati sono riportati direttamente nella migrazione. Sono importate soltanto le 30 carte originali, da `CR01-001` a `CR01-030`, associate a `CR01` / «Set Dimostrativo». Le 90 carte successive del CSV sono escluse, anche dalla collezione dimostrativa. Celle vuote e valori testuali `NULL` diventano SQL `NULL`.

Gli ID numerici restano generati dal database. `cards.code` conserva l’ID editoriale; V5 aggiunge anche `transfer_and_discard`, `duration` e `flavor_text`. Le nuove colonne sono nullable per conservare la compatibilità con le carte esistenti. «Radioactive monsters» usa la fazione `RADIOACTIVE` già presente; gli equipaggiamenti usano la nuova fazione `NEUTRAL`. V5 assegna a ogni carta un’arte `default`, anche senza immagine; V10 la rinomina `STANDARD` senza cambiare `id_art` o il possesso. Gli altri codici ammessi dopo V10 sono `EXTENDED_ART`, `FULL_ART`, `ALTERNATE_ART`. `cards_arts.source_image` conserva il nome originale del file; `url_card_art` resta nullo finché gli asset non sono pubblicati.

[V6](src/main/resources/db/migration/V6__seed_test_user.sql) crea nickname ed email `test@email.com`, con la password dimostrativa richiesta `Megapassword1` salvata esclusivamente come hash BCrypt. Il ruolo è `USER`; punteggio iniziale 1000, data di registrazione corrente, account attivo e inventario con tutte le valute a zero seguono i default F01. La collezione contiene una copia posseduta e scambiabile di ciascuna delle 30 carte. Mazzi, sessioni e token di recupero sono creati dai rispettivi flussi quando richiesti.

V6 è una migrazione ordinaria applicata automaticamente: crea questo account dimostrativo in ogni database che applica V6. Un’email o un nickname già occupati fanno fallire la migrazione senza sovrascrivere account esistenti. Un nuovo avvio non duplica i dati grazie alla cronologia Flyway.

I [test correnti delle configurazioni dei round](src/test/java/webapp/deck/RoundConfigurationMigrationDatabaseTests.java) verificano l’installazione V1–V11 su uno schema vuoto, i valori iniziali e i vincoli positivi delle configurazioni dei round, seguiti da un’esecuzione senza ulteriori migrazioni. Richiedono PostgreSQL e le stesse variabili della suite. Comando mirato `mvnw.cmd -o "-Daether.syncContext.named.factory=rwlock-local" "-Dtest=RoundConfigurationMigrationDatabaseTests" test`. La verifica dettagliata del CSV e dell’accesso dimostrativo del 2026-11-16 è un’evidenza storica descritta sotto.

### Effetti delle carte F07

Aggiornamento richiesto dall'utente il **2026-11-20**. [V7](src/main/resources/db/migration/V7__create_card_effects_schema.sql) aggiunge `effects` e `card_effects` con FK, parametri JSONB a oggetto e ordine positivo univoco per carta. [V8](src/main/resources/db/migration/V8__seed_initial_card_effects.sql) inizializza tre comportamenti e sette associazioni ai cinque equipaggiamenti CR01, risolvendo gli ID tramite i codici delle carte e degli effetti. Il modello vale anche per le creature; non vengono aggiunte abilità non presenti nel seed originale.

Per un database già a V6 basta il normale avvio con Flyway abilitato: V7–V10 vengono applicate senza reset, modifiche a V1–V6 o riparazioni della cronologia. Su database vuoto si applicano V1–V10; V10 aggiunge soltanto le strutture F03, senza pacchetti iniziali. Le descrizioni delle carte restano testo editoriale; il comportamento eseguibile deriva da codice effetto e parametri. Il backend carica le associazioni ordinate per `execution_order` per costruire gli snapshot del [contratto interno del motore](../../docs/BackEnd/app-battaglie.md#attivazione-e-responsabilità-degli-effetti).

### Battaglie applicative PvE F07

Il package `webapp.battle` implementa il [contratto pubblico](../../docs/BackEnd/app-battaglie-api.md): Bearer F01, solo `deckId` nel body, controllo del proprietario, validazione completa F05 e snapshot in transazione breve `REPEATABLE READ`. `CompositionLimitsRule` completa le regole V2.1 del validatore condiviso. La simulazione remota avviene dopo il commit, senza mantenere lock utente/mazzo o una transazione aperta.

La migrazione [V9](src/main/resources/db/migration/V9__repair_initial_battle_card_snapshots.sql) ripara solo fazioni nulle delle 25 creature CR01 note usando i codici V4 e assegna energia 0 ai cinque equipaggiamenti iniziali quando nulla. Conserva ID, collezioni, effetti e valori non nulli; si applica anche a database già a V8 senza reset.

| Configurazione | Locale | Compose |
| --- | --- | --- |
| `app.battle.engine-url` / `APP_BATTLE_ENGINE_URL` | `http://localhost:8083` | `http://game-engine:8083` |

Il budget cumulativo approvato è 8 secondi per la deadline cooperativa del motore, 9 per la risposta applicativa e 10 per il timeout frontend. Il guard L2 usa una connessione PostgreSQL riservata, diversa da quella della transazione breve: dimensionare il pool per queste due esigenze. Le richieste concorrenti dello stesso utente ricevono `409`; al termine HTTP il guard viene rilasciato anche se il lavoro remoto resta incerto. Nessun retry automatico, storico, premio o avanzamento della campagna. I limiti in caso di guasto e le verifiche sono nella [specifica applicativa](../../docs/BackEnd/app-battaglie.md).

Verifica corrente del **2026-11-20**: `clean verify` ha superato **212 test** e prodotto il JAR con JDK 21.0.11 e PostgreSQL 18.6 isolato; **49 test del motore** superati separatamente. Verificato anche il flusso HTTP tra i due JAR reali, con mazzo CR01 migrato, JWT F01 e risposte `200`/`409`/`404`. [Report e limiti](../../docs/report/2026-11-20-orchestrazione-pve.md). La verifica degli effetti riportata sotto precede l'orchestrazione.

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

La procedura scelta dall’utente per il prototipo ricrea il database tramite il reset dei volumi Docker Compose. Al successivo avvio Flyway applica V1–V10 su un database vuoto, usando lo schema V3 e i dati iniziali V4 aggiornati. Dalla radice del repository eseguire la procedura seguente.

```sh
docker compose down --volumes
docker compose up --build --wait
```

Verificare V1–V10 in `flyway_schema_history` e i limiti di `round_configurations`, inclusi energia 6 e potenza 10 per `EQUIPMENT`.

### Cancellazione account F01

Il [contratto di cancellazione account](../../docs/BackEnd/accesso-utenti.md#cancellazione-account) descrive API, permessi, transazione e comportamento del client. La cancellazione usa il flag `users.is_deleted` già presente, revoca tutte le sessioni e rimuove tutti i token di recupero dell’account nella stessa transazione.

Aggiornamento documentale del **2026-11-20**: il [contratto API F13](../../docs/BackEnd/amicizie.md) è definito su decisioni e delega dell'utente, con Bearer JWT, profili pubblici minimi e ordinamento dal più recente; nessuna rotta F13 è disponibile. La [futura integrazione F13/F06](../../docs/BackEnd/accesso-utenti.md#integrazione-prevista-con-f13-e-f06), concordata il 2026-11-18, richiede anche la pulizia di richieste, amicizie e scambi aperti al soft delete, conservando gli scambi completati e gestendo l'annuncio pubblico della controparte come rifiuto secondo F06. Questa estensione **non è implementata** nel servizio o nelle migrazioni F01/F05 correnti e non è coperta dai test esistenti. La rimozione della sola amicizia conserva gli scambi e la scadenza originaria.

I [test senza database](src/test/java/webapp/auth/AccountDeletionTests.java) esercitano controller, servizio e catena di sicurezza con stati del repository simulati. I [test PostgreSQL](src/test/java/webapp/auth/AccountDeletionDatabaseTests.java) usano le migrazioni F01 in schemi casuali dedicati ed esercitano JWT reali, rollback e concorrenza. Per eseguirli, configurare le variabili di database e sicurezza indicate sopra su un database di prova dedicato, con permesso `CREATE`, ed eseguire `mvnw.cmd verify`. Le evidenze effettive sono nel [contratto F01](../../docs/BackEnd/accesso-utenti.md#verifiche-ed-evidenze).

## Verifiche e limiti

Verifica F05 del **2026-11-18** sulla copia di lavoro con gli obiettivi Maven `clean verify`, usando il POM corrente con directory di build isolata `target/f05-verification`, Java **21.0.11** e PostgreSQL **18.6** in un container di prova dedicato. Superati **135 test**, con zero fallimenti, errori o casi saltati, e prodotto il JAR. Verificati energia individuale, potenza aggregata, frontiere inclusive, metadati invalidi, overflow, errori HTTP e conservazione delle righe dopo rifiuto. Le prove Flyway coprono installazione V1–V6 su uno schema vuoto con V3/V4 aggiornate, limiti iniziali, vincoli positivi ed esecuzioni successive senza ulteriori migrazioni. Bilanciamento e integrazione React restano da verificare.

Verifica storica del **2026-11-16** per V5/V6 sulla copia di lavoro: `mvnw.cmd -o "-Daether.syncContext.named.factory=rwlock-local" verify` ha superato **241 test**, senza fallimenti, errori o casi saltati, e prodotto il JAR con Java **21.0.11** e PostgreSQL **18.6** in un container dedicato. I 123 nuovi casi verificano le 30 carte originali, l’assenza delle 90 successive, installazione vuota, aggiornamento da V4 con dati esistenti, secondo avvio senza duplicati, inventario, collezione e login dell’account dimostrativo. La fixture CSV coincide byte per byte con il file fornito; controllati 161 collegamenti relativi nei documenti modificati e `git diff --check`. Le prove non modificano il database applicativo dell’utente e non verificano browser o integrazione React.

Verifica storica del **2026-11-13**, copia di lavoro con base `97200d9`: `mvnw.cmd -o "-Daether.syncContext.named.factory=rwlock-local" verify` ha superato **118 test** su PostgreSQL **18.6** in un container dedicato e ha prodotto il JAR. Applicate V1–V4 su database vuoto; corretti CSRF con Bearer, limite nickname e isolamento dei test. `docker compose config --quiet` è passato. La build dell’immagine, l’avvio applicativo tramite Compose e il frontend non sono verificati. [Report con risultati e limiti](../../docs/report/2026-11-13-revisione-stato-sviluppo.md). Le evidenze seguenti sono precedenti.

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
