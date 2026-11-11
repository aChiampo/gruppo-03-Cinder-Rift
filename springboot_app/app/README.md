# Applicazione backend

Ultimo aggiornamento significativo il **2026-11-11** con l'aggiunta del Dockerfile del backend. Le dipendenze sono dichiarate nel [POM](pom.xml).

Il backend utilizza Java 21 e Spring Boot 4.1.2; il database previsto è PostgreSQL 18. Il frontend è realizzato con React.

Questo documento descrive le dipendenze dell'applicazione. La loro presenza non attesta l'implementazione delle funzionalità, il corretto avvio o il funzionamento delle integrazioni.

## Dipendenze

### Gestione delle versioni

Le versioni sono gestite da Spring Boot tramite `spring-boot-starter-parent`. Fa eccezione springdoc, la cui versione è impostata esplicitamente a `3.2.0`.

Nelle tabelle seguenti, `starter-*` indica `org.springframework.boot:spring-boot-starter-*`. Lo scope predefinito di Maven è `compile`.

### Dipendenze applicative attive

| Dipendenza | Scope Maven | Scopo |
| --- | --- | --- |
| `starter-data-jpa` | `compile` | Persistenza tramite JPA e repository per utenti, inventari, collezioni, mazzi, scambi, amicizie e storico delle partite. |
| `org.postgresql:postgresql` | `runtime` | Driver JDBC per la connessione a PostgreSQL. |
| `starter-flyway` | `compile` | Integrazione Spring Boot per la gestione delle migrazioni versionate del database. |
| `org.flywaydb:flyway-database-postgresql` | `runtime` | Supporto specifico di Flyway per PostgreSQL. Affianca il driver JDBC senza sostituirlo. |
| `starter-security-oauth2-resource-server` | `compile` | Infrastruttura di sicurezza per la validazione dei bearer token JWT e la protezione delle risorse API. |
| `org.springdoc:springdoc-openapi-starter-webmvc-ui` | `compile` | Generazione della documentazione OpenAPI e disponibilità di Swagger UI per consultare e richiamare manualmente gli endpoint HTTP. |
| `starter-validation` | `compile` | Validazione dei campi delle richieste e degli altri dati annotati tramite Bean Validation. |
| `starter-mail` | `compile` | Supporto all'invio di email, utilizzabile per il recupero della password se viene confermato questo canale. |
| `starter-thymeleaf` | `compile` | Generazione di HTML tramite template lato server, potenzialmente per le email. Il frontend rimane affidato a React. |
| `starter-actuator` | `compile` | Endpoint operativi per il controllo dello stato e il monitoraggio dell'applicazione. |
| `org.springframework.boot:spring-boot-docker-compose` | `runtime` | Integrazione facoltativa per la gestione dei servizi Compose configurati nell'ambiente di sviluppo. |
| `org.projectlombok:lombok` | `compile` | Riduzione del codice ripetitivo tramite elaborazione delle annotazioni. Dichiarata con `optional=true`, con annotation processor configurato per sorgenti applicativi e test. |

Mail, Thymeleaf, Actuator, integrazione Docker Compose e Lombok rispondono a esigenze facoltative, operative o di sviluppo. I relativi flussi e configurazioni devono comunque essere implementati e verificati.

### Stato di WebSocket e Spring MVC

La dichiarazione di `starter-websocket` è attualmente commentata nel POM e non è una dipendenza applicativa attiva. Il suo impiego per notifiche in tempo reale rimane da decidere; la modalità base del gioco prevede una simulazione automatica lato server.

Il POM non dichiara direttamente `starter-webmvc`. Poiché il backend previsto espone API REST, prima dell'avvio occorre verificare la composizione delle dipendenze applicative e rendere esplicita la scelta dello starter MVC. La presenza di librerie nello scope `test` non ne garantisce la disponibilità nell'applicazione distribuita.

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

I test devono essere implementati. Swagger UI permette di esplorare manualmente le API, ma non sostituisce i test automatici.

Il motore di gioco deve restare un componente Java isolato e coperto da test JUnit, come richiesto dalla [traccia](../../Laboratorio-Integrato-TRACCIA.md). I suoi test unitari devono verificare le regole senza richiedere Spring o un database. Se organizzato come modulo separato, il motore deve dichiarare autonomamente le proprie dipendenze di test.

## Responsabilità implementative

Le dipendenze forniscono l'infrastruttura. L'applicazione deve comunque implementare:

- Registrazione, accesso, emissione, rinnovo e revoca dei token e controllo dei permessi.
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

## Verifiche e limiti

La revisione documentale del **2026-11-06** riguarda corrispondenza con il POM, collegamenti relativi e formattazione Markdown. Non attesta risoluzione delle dipendenze Maven, compilazione, avvio, integrazioni o test applicativi.

Il **2026-11-11** Compose ha validato la configurazione e risolto il Dockerfile e il contesto di build di `spring_app`. Il motore Linux di Docker Desktop era indisponibile durante il tentativo di build. Restano da verificare la build dell'immagine, l'avvio del servizio e l'accesso all'endpoint di salute con la configurazione effettiva del database e della sicurezza.

## Collegamenti

- [POM dell'applicazione](pom.xml)
- [Indice della documentazione BackEnd](../../docs/BackEnd/README.md)
- [Accesso utenti e autenticazione — F01](../../docs/BackEnd/accesso-utenti.md)
- [Specifica della base di dati v1.0](../../docs/BackEnd/Database/database-schema-v1.0.md)
- [Sfide e simulazione — F07](../../docs/BackEnd/sfide-simulazione.md)
- [README del progetto](../../README.md)
