# Applicazione backend

Ultimo aggiornamento significativo il **2026-11-12** con le migrazioni Flyway F01 e la verifica su PostgreSQL 18.1. Il backend include la base del recupero password, il prefisso `/api/auth` e la pulizia programmata delle sessioni. Invio email e interfaccia di recupero sono rinviati. La configurazione iniziale del backend per F01 risale al 2026-11-11. Le dipendenze sono dichiarate nel [POM](pom.xml).

Il backend utilizza Java 21 e Spring Boot 4.1.2; il database previsto è PostgreSQL 18. Il frontend è realizzato con React.

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

Sono presenti test F01 per profilo protetto e CSRF, restrizioni del namespace amministrativo, scheduler e cancellazione delle sessioni. I report esistenti e i limiti sono descritti nel [contratto F01](../../docs/BackEnd/accesso-utenti.md#verifiche-ed-evidenze). I test delle altre funzionalità restano da implementare; Swagger UI non sostituisce i test automatici.

Il motore di gioco deve restare un componente Java isolato e coperto da test JUnit, come richiesto dalla [traccia](../../Laboratorio-Integrato-TRACCIA.md). I suoi test unitari devono verificare le regole senza richiedere Spring o un database. Se organizzato come modulo separato, il motore deve dichiarare autonomamente le proprie dipendenze di test.

## Responsabilità implementative

Le dipendenze forniscono l'infrastruttura. L'applicazione deve comunque implementare:

- Integrazione FrontEnd di registrazione, accesso, profilo, rinnovo e uscita. Il backend F01 è descritto nel [contratto di accesso](../../docs/BackEnd/accesso-utenti.md).
- Controlli sulla proprietà delle risorse e validazioni di dominio, inclusa la conformità dei mazzi.
- Acquisti e scambi atomici, rispettando il protocollo di lock documentato nella specifica del database.
- Migrazioni, mapping delle entità e mapping dei campi PostgreSQL JSONB.
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

Gli endpoint di autenticazione usano il prefisso `/api/auth`, allineato al controller su richiesta dell’utente del **2026-11-12**. Metodi, percorsi e requisiti di accesso sono definiti nel [contratto F01](../../docs/BackEnd/accesso-utenti.md#contratto-tecnico-condiviso); i client devono usare i nuovi percorsi.

`AuthenticationService` verifica le password con BCrypt ed emette JWT HS256 tramite `JwtEncoder`. Il resource server Spring Security verifica questi JWT e carica a ogni richiesta profilo, ruolo e sessione dal database. La dipendenza OAuth2 resource server non introduce un provider esterno o un flusso di accesso OAuth2. Non vengono create sessioni HTTP servlet.

Impostare `APP_SECURITY_JWT_SECRET` con una chiave casuale in Base64 di almeno 32 byte. Compose passa la variabile al servizio `spring_app`. L’applicazione verifica il formato e la lunghezza della chiave all’avvio. Per l’accesso da React, `APP_SECURITY_ALLOWED_ORIGINS` accetta le origini esatte separate da virgola e usa `http://localhost:8082` come valore predefinito. [Contratto di accesso e configurazione dei cookie](../../docs/BackEnd/accesso-utenti.md#configurazione-e-operatività).

Le proprietà `app.security.access-token-lifetime` e `app.security.refresh-token-lifetime` hanno default `15m` e `14d`. Spring le legge anche da `APP_SECURITY_ACCESS_TOKEN_LIFETIME` e `APP_SECURITY_REFRESH_TOKEN_LIFETIME`; queste due variabili non sono inoltrate dal file Compose corrente e, per modificarle nel container, vanno aggiunte alla configurazione del servizio. La durata di accesso deve essere almeno un secondo e quella di rinnovo almeno pari alla durata di accesso.

### Migrazioni Flyway F01

Usare PostgreSQL 18 e configurare la connessione con `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME` e `SPRING_DATASOURCE_PASSWORD`. Con Compose, la connessione è configurata a partire dalle variabili `POSTGRES_DB`, `POSTGRES_USER` e `POSTGRES_PASSWORD`. L’utente che esegue Flyway deve poter creare oggetti nello schema e inserire righe in `roles` usando il relativo generatore di ID. DDL Hibernate, inizializzazione SQL Spring e baseline automatica di Flyway sono disabilitati. [Specifica v1.1](../../docs/BackEnd/Database/database-schema-v1.1.md) · [Configurazione applicativa](src/main/resources/application.properties).

**Database nuovo.** Creare un database vuoto e avviare normalmente il backend. Flyway applica V1 per creare le cinque tabelle F01 e V2 per inserire i ruoli `USER` e `ADMIN`. I ruoli sono gli unici dati inseriti dalle migrazioni. Con Compose, dalla radice del repository eseguire `docker compose up --build -d spring_app` con un database vuoto. Verificare V1 e V2 in `flyway_schema_history` e lo stato di `GET /actuator/health`. [V1](src/main/resources/db/migration/V1__create_f01_schema.sql) · [V2](src/main/resources/db/migration/V2__seed_f01_roles.sql).

**Database esistente senza cronologia Flyway.**

1. Identificare il database e lo schema effettivamente usati dall’applicazione. Impostare `PGHOST`, `PGPORT`, `PGDATABASE` e `PGUSER` per la destinazione ed eseguire un backup prima di apportare modifiche, ad esempio con `pg_dump --format=custom --file=f01-before-flyway.dump`.
2. Applicare V1 in un database di riferimento vuoto e isolato, ad esempio con `psql -d "$REFERENCE_DATABASE" -v ON_ERROR_STOP=1 -f src/main/resources/db/migration/V1__create_f01_schema.sql` dalla cartella del backend. Impostare `REFERENCE_DATABASE` al nome del database isolato, oppure a una stringa di connessione se usa un’altra istanza. Confrontare le definizioni delle cinque tabelle nello schema effettivo con quelle del riferimento, ad esempio usando `pg_dump --schema-only --schema=public --no-owner --no-privileges` su entrambi. Verificare tipi, nullabilità, valori predefiniti, generatori di ID, vincoli, azioni delle chiavi esterne e indici. Controllare che i generatori siano allineati agli ID esistenti e che i dati rispettino il contratto F01. Riconciliare qualsiasi differenza e ripetere il confronto prima di procedere alla baseline.
3. Dalla cartella del backend, impostare `FLYWAY_URL`, `FLYWAY_USER` e `FLYWAY_PASSWORD` per la destinazione dell’adozione. Con una CLI Flyway compatibile con la versione risolta dal POM, eseguire la baseline esplicita a 1 e poi la migrazione. Adattare lo schema `public` dell’esempio a quello effettivo dell’applicazione. [Comando baseline di Flyway](https://documentation.red-gate.com/flyway/reference/commands/baseline).

   ```sh
   flyway -defaultSchema=public -schemas=public -baselineVersion=1 -baselineOnMigrate=false -locations=filesystem:src/main/resources/db/migration baseline
   flyway -defaultSchema=public -schemas=public -baselineOnMigrate=false -locations=filesystem:src/main/resources/db/migration migrate
   ```

4. Verificare in `flyway_schema_history` la baseline a 1 e l’applicazione di V2. Verificare i dati esistenti, gli ID dei ruoli già presenti e la presenza dei due ruoli richiesti, quindi avviare il backend e verificare `GET /actuator/health`.

### Pulizia delle sessioni F01

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

Per le verifiche senza database:

```sh
mvnw.cmd -o "-Dtest=SecurityConfigurationTests,SessionTokenCleanupTests,PasswordResetTokenCleanupTests" test
```

## Verifiche e limiti

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
