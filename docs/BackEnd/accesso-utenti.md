# Accesso utenti

ID: F01 · Perimetro backend confermato per registrazione, accesso, profilo, rinnovo, uscita e base del recupero password.
Implementazione: backend disponibile nel branch `feature/f01-accesso-utenti`; integrazione FrontEnd da avviare. Responsabile: DA DEFINIRE.
Ultimo aggiornamento significativo il 2026-11-12 con le migrazioni Flyway F01 per PostgreSQL 18. Il backend del recupero password usa token di 10 minuti, preserva i token emessi nelle richieste precedenti e revoca le sessioni al completamento. Invio email e interfaccia sono rinviati. Restano il prefisso `/api/auth` e la pulizia giornaliera delle sessioni alle 03:00 UTC.
Fonte dati: [specifica canonica v1.1](Database/database-schema-v1.1.md#sistema-di-autenticazione-e-autorizzazione).

## Stack implementato

F01 usa Java 21, Spring Boot 4.1.2, Spring MVC, Bean Validation e Spring Security con il supporto OAuth2 resource server per verificare i bearer token. `AuthenticationService` emette i JWT nell’applicazione con `JwtEncoder`; non sono configurati un provider di identità esterno o un flusso di accesso OAuth2. Le password sono verificate con BCrypt.

`AccountRepository` accede a PostgreSQL con `JdbcClient` e SQL esplicito, senza entità JPA per F01. Flyway gestisce il perimetro F01 dello schema v1.1 su PostgreSQL 18. La catena di sicurezza è stateless rispetto a `HttpSession`, ma verifica nel database account e sessione a ogni richiesta Bearer. [Dipendenze e configurazione backend](../../springboot_app/app/README.md).

## Attori, autorizzazioni e precondizioni

- Chi si registra ottiene il ruolo `USER` e accede subito. Il ruolo deriva dalla riga `ROLES.code = 'USER'`.
- Gli account attivi `ADMIN` e `USER` possono accedere e ottenere il proprio profilo. Spring Security assegna esclusivamente l’autorità `ROLE_` corrispondente al ruolo corrente nel database. I privilegi per le altre funzionalità si definiscono nei rispettivi contratti.
- Gli account con `is_deleted = true` o con un codice di ruolo diverso da `ADMIN` e `USER` ricevono `401` su accesso, rinnovo e richieste protette. L’ID dell’account da solo non conferisce permessi.
- Non esiste una gerarchia tra ruoli. Il namespace `/api/admin/**` richiede `ADMIN` per ogni metodo HTTP; `USER` riceve `403` e un chiamante privo di autenticazione riceve `401`. Le singole operazioni di backoffice non sono ancora implementate e i relativi permessi di dettaglio restano da definire nel [contratto F10](gestione-amministrativa.md).

## Contratto tecnico condiviso

Le richieste di registrazione e accesso inviano `Content-Type: application/json`. Tutte le risposte che contengono credenziali o profilo usano `Cache-Control` con `no-store`.

Tutti gli endpoint F01 del controller usano il prefisso `/api/auth`. La modifica sostituisce i precedenti percorsi sotto `/api`, senza alias di compatibilità: i client devono aggiornare le chiamate al nuovo prefisso. I percorsi delle altre funzionalità non cambiano.

| Metodo e percorso | Credenziali richieste | Corpo della richiesta | Risposta di successo |
| --- | --- | --- | --- |
| `GET /api/auth/csrf-tokens` | Nessuna | Assente | `200` con `headerName` e `token`, entrambi stringhe. Imposta il cookie CSRF quando serve. |
| `POST /api/auth/users` | Cookie CSRF e header `X-XSRF-TOKEN` | `nickname`, `email`, `password` | `201` con la risposta di sessione e il cookie di rinnovo. |
| `POST /api/auth/sessions` | Cookie CSRF e header `X-XSRF-TOKEN` | `email`, `password` | `200` con la risposta di sessione e il cookie di rinnovo. |
| `GET /api/auth/users/me` | `Authorization: Bearer <accessToken>` | Assente | `200` con il profilo. |
| `POST /api/auth/session-renewals` | Cookie di rinnovo, cookie CSRF e header `X-XSRF-TOKEN` | Assente | `200` con una nuova risposta di sessione e il cookie di rinnovo sostitutivo. |
| `POST /api/auth/session-revocations` | Cookie CSRF e header `X-XSRF-TOKEN`. Cookie di rinnovo, se presente. | Assente | `204` con scadenza immediata del cookie di rinnovo. Ripetere la richiesta produce lo stesso esito. |
| `POST /api/auth/password-reset-requests` | Cookie CSRF e header `X-XSRF-TOKEN`; nessun JWT richiesto | `email` | `202` con `message` generico; nessun token o cookie di rinnovo. Invio email non implementato. |
| `POST /api/auth/password-resets` | Cookie CSRF e header `X-XSRF-TOKEN`; nessun JWT richiesto | `token`, `newPassword` | `204`, senza credenziali o cookie di rinnovo. |

- `nickname` è una stringa non vuota e non composta soltanto da spazi bianchi (`@NotBlank`). Il server rimuove gli spazi iniziali e finali e confronta il valore conservando maiuscole e minuscole.
- `email` è una stringa in formato email di massimo 254 caratteri. Dopo la validazione il server rimuove gli spazi esterni e usa la forma minuscola per il salvataggio e la ricerca.
- `password` è una stringa non vuota e non composta soltanto da spazi bianchi (`@NotBlank`), di massimo 72 byte UTF-8. Il server conserva gli spazi e salva l’hash BCrypt.
- La risposta di sessione include `accessToken` (stringa JWT), `tokenType` (stringa `Bearer`), `expiresIn` (secondi interi) e `user` (profilo). Il valore segreto di rinnovo viaggia nel cookie `__Host-cinder-refresh`.
- Il profilo include `id` e `rankingPoints` come interi e `nickname`, `email` e `role` come stringhe. `role` vale `ADMIN` o `USER`. `iconUrl` è una stringa o `null`.

Esempio illustrativo del corpo di registrazione.

```json
{
  "nickname": "RiftPlayer",
  "email": "player@example.invalid",
  "password": "<password scelta dall'utente>"
}
```

Esempio illustrativo di una risposta di sessione.

```json
{
  "accessToken": "<jwt>",
  "tokenType": "Bearer",
  "expiresIn": 900,
  "user": {
    "id": 42,
    "nickname": "RiftPlayer",
    "email": "player@example.invalid",
    "role": "USER",
    "rankingPoints": 1000,
    "iconUrl": null
  }
}
```

| Stato | Condizione |
| --- | --- |
| `400` | Campi obbligatori mancanti, formato non valido o limite della password superato. Anche token di recupero malformato, sconosciuto, scaduto, usato o associato a un account non ammesso: `Invalid password reset token`. |
| `401` | Credenziali errate, cookie di rinnovo mancante o inutilizzabile, oppure JWT mancante o inutilizzabile su una risorsa protetta. L’accesso con password usa lo stesso messaggio per email sconosciuta, account cancellato, ruolo non ammesso e password errata. |
| `403` | Token CSRF mancante o errato oppure origine CORS non ammessa. |
| `409` | Email o nickname già in uso. |
| `415` | Tipo di contenuto non supportato per una richiesta con corpo JSON. |
| `500` | Errore interno di persistenza o configurazione. Una registrazione o un rinnovo falliti prima del commit annullano le proprie scritture. |

Gli errori applicativi di validazione, credenziali e conflitto usano `application/problem+json`. Per i rifiuti della catena di sicurezza, il client usa lo stato HTTP. Le risposte Bearer `401` includono `WWW-Authenticate`.

```json
{
  "type": "about:blank",
  "title": "Unauthorized",
  "status": 401,
  "detail": "Invalid credentials",
  "instance": "/api/auth/sessions"
}
```

## Ciclo di vita, dati e concorrenza

- `USERS.id` e `ROLES.id` sono interi generati dal database. La registrazione salva l’account, l’inventario e la prima sessione nello stesso commit. Il database fornisce `ranking_points = 1000`, `is_deleted = false` e `0` per `cobalt_shards`, `rift_cores` e `scraps`.
- Ogni accesso crea una sessione indipendente in `REFRESH_TOKENS`. Il token di rinnovo contiene 32 byte casuali in Base64 URL senza padding. Il database conserva il suo SHA-256 in esadecimale, l’utente, la creazione, la scadenza e l’eventuale revoca.
- Il JWT è firmato con HS256 e scade dopo 15 minuti per impostazione predefinita. L’emittente (`iss`) è `cinder-rift`; `sub` contiene l’ID utente e `sid` l’ID della riga `REFRESH_TOKENS`, entrambi come stringhe di interi positivi. `iat` ed `exp` indicano emissione e scadenza. Il ruolo proviene dal database, non da un claim del JWT. Il backend verifica firma, emittente, scadenza e associazione tra utente attivo e sessione attiva a ogni richiesta protetta.
- La sessione dura 14 giorni dalla sua emissione o dall’ultimo rinnovo riuscito. Il rinnovo acquisisce il lock dell’utente, revoca il token precedente e salva quello nuovo nella stessa transazione. Una sola richiesta concorrente con lo stesso token può riuscire. Il JWT precedente perde validità con la rotazione.
- L’uscita revoca la sessione corrispondente al cookie e invalida il suo JWT. Le altre sessioni dello stesso utente continuano a funzionare. Un rinnovo con un token già revocato risponde `401`. Il client serializza rinnovo e uscita per evitare l’uso di un cookie appena sostituito. Se si perde la risposta di un rinnovo completato e il cookie non è stato aggiornato, serve un nuovo accesso.
- Un token è scaduto quando la scadenza è minore o uguale all’istante corrente. I confronti di persistenza usano istanti UTC. La v1.1 non collega le righe che precedono e seguono una rotazione.
- Ogni giorno alle 03:00 UTC il job Spring elimina da `REFRESH_TOKENS` le righe con `expires_at <= cutoff` **oppure** `revoked_at IS NOT NULL`. Il cutoff è acquisito una sola volta per esecuzione; non è previsto un periodo di conservazione. Le sessioni attive restano presenti. La cancellazione è una singola istruzione atomica e ripeterla è sicuro. Non comprende i token di recupero password.
- La pulizia non determina la validità delle credenziali: i controlli di autenticazione rifiutano subito le sessioni scadute, revocate o mancanti, anche se il job è disabilitato o in ritardo. Non sono modellate catene di rotazione o evidenze di audit da conservare nelle righe eliminate.

## Comportamento del client

1. Richiedere `GET /api/auth/csrf-tokens` all’avvio del flusso con `credentials: 'include'` quando l’origine del client è diversa. Inviare il valore `token` ricevuto nell’header `X-XSRF-TOKEN` e includere le credenziali nelle sei richieste `POST` F01, comprese richiesta e conferma del recupero password.
2. Tenere il JWT di accesso in memoria e aggiungerlo alle richieste protette. Per ricostruire la sessione dopo un ricaricamento o una scadenza, chiamare l’endpoint di rinnovo con i cookie e il token CSRF. Omettere l’header `Authorization` sulle richieste di rinnovo e uscita. Sostituire il JWT in memoria dopo ogni rinnovo riuscito.
3. Scartare il JWT all’uscita. Se il rinnovo restituisce `401`, tornare alla schermata di accesso.

La configurazione CORS consente richieste con credenziali soltanto dalle origini configurate, con metodi `GET`, `HEAD`, `POST`, `PUT`, `PATCH`, `DELETE`, `OPTIONS` e header `Authorization`, `Content-Type`, `X-XSRF-TOKEN`. Un token Bearer presente viene comunque verificato anche sulle rotte pubbliche: il client evita di inviare un JWT scaduto o inutilizzabile nelle chiamate di bootstrap e accesso.

I cookie `__Host-cinder-csrf` e `__Host-cinder-refresh` usano `Secure`, `HttpOnly`, `SameSite=Strict` e `Path=/`. In distribuzione usare HTTPS e mantenere frontend e backend sullo stesso sito. Per lo sviluppo locale, usare lo stesso nome host nei due URL.

## Configurazione e operatività

- Configurare una chiave casuale di almeno 32 byte in Base64 tramite `APP_SECURITY_JWT_SECRET`. Fornire lo stesso valore alle istanze che devono verificare gli stessi JWT.
- Impostare `APP_SECURITY_ALLOWED_ORIGINS` come elenco di origini esatte separate da virgola. Il valore predefinito è `http://localhost:8082`. Le proprietà `app.security.access-token-lifetime` e `app.security.refresh-token-lifetime` controllano le durate, con default `15m` e `14d`: la prima deve essere almeno un secondo, la seconda almeno pari alla prima. Le relative variabili `APP_SECURITY_ACCESS_TOKEN_LIFETIME` e `APP_SECURITY_REFRESH_TOKEN_LIFETIME` sono lette da Spring, ma non sono inoltrate dal Compose corrente. [Configurazione backend](../../springboot_app/app/README.md).
- Su un database vuoto, all’avvio V1 crea `roles`, `users`, `inventory`, `refresh_tokens` e `password_reset_tokens`. V2 inserisce i codici `USER` e `ADMIN` con ID generati dal database e conserva le righe dei ruoli già presenti.
- Per adottare un database F01 esistente, eseguire un backup e confrontare lo schema con V1 su un database di riferimento isolato. Riconciliare le differenze prima di registrare esplicitamente la baseline a 1. Applicare V2 e verificare i dati e gli ID esistenti. Baseline automatica, DDL Hibernate e inizializzazione SQL Spring sono disabilitati. [Procedura operativa](../../springboot_app/app/README.md#migrazioni-flyway-f01).
- Il documento OpenAPI è disponibile su `GET /v3/api-docs`. Swagger UI è disponibile su `/swagger-ui/index.html`. Il controllo di salute usato da Compose è `GET /actuator/health`.
- `app.security.session-token-cleanup-cron` usa il formato cron Spring a sei campi e vale `0 0 3 * * *`; `app.security.session-token-cleanup-zone` vale `UTC`. Le corrispondenti variabili `APP_SECURITY_SESSION_TOKEN_CLEANUP_CRON` e `APP_SECURITY_SESSION_TOKEN_CLEANUP_ZONE` sono inoltrate da Compose. Impostare il cron a `-` per disabilitare il job su un’istanza. [Configurazione e verifiche](../../springboot_app/app/README.md#pulizia-delle-sessioni-f01).
- Il job opera mentre il backend è avviato; un’esecuzione persa durante un arresto non viene recuperata all’avvio. Il passaggio successivo elimina tutte le righe allora inutilizzabili. Ogni istanza abilitata esegue il job: con più repliche si può abilitarlo su una sola, senza necessità di coordinamento per la correttezza della cancellazione. Il log riporta solo il numero di righe eliminate; gli errori sono registrati dal scheduler e il tentativo successivo segue il cron.

## Recupero password: base backend

Decisioni confermate dall’utente il **2026-11-12**. Gli endpoint sono implementati; l’invio email ha soltanto commenti `TODO [issue-pending]` nel servizio e l’interfaccia React non è inclusa nell’attività. Il recupero non è ancora utilizzabile dall’utente finale: il segreto generato non viene consegnato né restituito dall’API e non esiste una rotta di debug per leggerlo.

### Richiesta e contratto

`POST /api/auth/password-reset-requests` accetta JSON con `email`, validata e normalizzata come per l’accesso. Per account attivi `ADMIN` e `USER` genera 32 byte con `SecureRandom`, codificati Base64 URL senza padding (43 caratteri); salva soltanto SHA-256 esadecimale in `PASSWORD_RESET_TOKENS`, con creazione UTC, scadenza dopo esattamente **10 minuti** e `used_at = NULL`. Una richiesta successiva aggiunge un token senza eliminare o invalidare i precedenti; non cambia password o sessioni. Email sconosciute, account eliminati e ruoli non ammessi non ricevono token.

Per un corpo valido, la risposta è sempre `202` con questo JSON, che conferma la sola accettazione della richiesta e non la consegna di un messaggio:

```json
{"message":"If the account is eligible, the password reset request has been accepted."}
```

`POST /api/auth/password-resets` accetta `token` e `newPassword`, stringhe obbligatorie non vuote e non composte soltanto da spazi. Il token deve avere il formato opaco indicato sopra; `newPassword` segue gli stessi vincoli di registrazione: massimo 72 byte UTF-8, spazi conservati e hash BCrypt. Esempio illustrativo:

```json
{"token":"<segreto di recupero>","newPassword":"<nuova password scelta dall'utente>"}
```

Entrambe le rotte sono accessibili senza autenticazione, con cookie CSRF e header `X-XSRF-TOKEN`. Il client omette `Authorization`, compreso un eventuale JWT scaduto. Le risposte usano le intestazioni di cache della catena di sicurezza. Gli errori applicativi seguono `application/problem+json`; la risposta di completamento è `204`, senza accesso automatico né modifica dei cookie.

### Transazione e concorrenza

Il servizio usa `@Transactional` e SQL JDBC: PostgreSQL esegue un’unica transazione, senza funzioni o procedure memorizzate. Mantiene la codifica BCrypt nel servizio Java e usa lo stesso ordine dei lock dei flussi esistenti.

1. Individua il proprietario mediante hash del token e blocca la riga `USERS` attiva con `SELECT ... FOR UPDATE`.
2. Acquisisce il cutoff UTC dopo l’eventuale attesa del lock; consuma il token con un aggiornamento condizionale che richiede proprietario coerente, `used_at IS NULL` e `expires_at > cutoff`. All’uguaglianza il token è scaduto.
3. Aggiorna l’hash BCrypt della password e mantiene `used_at` sul token consumato.
4. Elimina gli altri token di recupero non usati dello stesso utente e revoca tutte le sue righe `REFRESH_TOKENS` ancora non revocate. Gli altri utenti non sono coinvolti.
5. Esegue il commit; un errore precedente annulla consumo, password, cancellazioni e revoche. L’email di notifica futura è soltanto un commento segnaposto e dovrà essere inviata dopo il commit.

Una sola conferma concorrente può riuscire, anche usando link diversi dello stesso account. La conservazione dei token precedenti vale alla **richiesta**, mentre un **reset riuscito** rende inutilizzabili gli altri link. Richiesta di recupero, accesso con password e rinnovo acquisiscono lo stesso lock utente: l’accesso controlla l’hash dopo il lock, evitando emissioni con la vecchia password dopo il reset. I JWT delle sessioni revocate falliscono alla successiva autenticazione perché il controllo Bearer consulta il database. Le richieste già autenticate prima del commit non vengono interrotte retroattivamente.

### Pulizia dei token di recupero

Il job separato `PasswordResetTokenCleanup` elimina i record con `expires_at <= cutoff` oppure `used_at IS NOT NULL`, senza conservazione. Usa un cutoff UTC per esecuzione e, come scelta operativa di questa base, viene eseguito ogni giorno alle 03:00 UTC. La ripetizione è sicura e i token ancora utilizzabili rimangono presenti. La scadenza è verificata durante il reset anche se il job è disabilitato o in ritardo.

| Proprietà | Variabile inoltrata da Compose | Default |
| --- | --- | --- |
| `app.security.password-reset-token-cleanup-cron` | `APP_SECURITY_PASSWORD_RESET_TOKEN_CLEANUP_CRON` | `0 0 3 * * *` |
| `app.security.password-reset-token-cleanup-zone` | `APP_SECURITY_PASSWORD_RESET_TOKEN_CLEANUP_ZONE` | `UTC` |

Il cron `-` disabilita solo questa pulizia. Valgono gli stessi limiti operativi del job sessioni: backend avviato, nessun recupero immediato delle esecuzioni perse e possibili esecuzioni su più repliche. Il job usa la tabella `PASSWORD_RESET_TOKENS` inclusa in V1.

## Controlli esclusi su richiesta esplicita dell’utente

Il **2026-11-12** l’utente ha chiesto esplicitamente di omettere i controlli proposti nell’annotazione alla risposta precedente. Questa base **non implementa limiti di richieste per IP/account, equalizzazione dei tempi delle risposte o protezioni dedicate contro la diffusione dei link di recupero tramite log, analytics e referrer**. Si tratta di esclusioni richieste, non di controlli completati o verificati. Non vengono aggiunti componenti per questi scopi; la regola generale del progetto di non inserire segreti nei log resta applicabile.

Costruzione e validazione dell’URL HTTPS di recupero sono rinviate insieme all’intero invio email in [Invio mail](invio-mail.md), senza attribuire loro un’implementazione corrente. Restano le regole richieste nei punti 1, 2 e 4: risposta generica, token casuali memorizzati come hash, scadenza di 10 minuti, uso singolo, transazione atomica, revoca delle sessioni e nessuna emissione automatica di una nuova sessione. L’attività non implementa il frontend.

## Verifiche ed evidenze

### Migrazioni F01

Il **2026-11-12**, su PostgreSQL 18.1 isolato e Java 21.0.11, sono passati tutti i **44 test** di `mvnw.cmd verify` e le prove di avvio del JAR. Le verifiche sono state ripetute dopo l’attivazione predefinita di Flyway, con nuovi database isolati.

- **Avvio da zero.** Flyway ha applicato V1 e V2. Le prove HTTP hanno verificato registrazione, creazione dell’inventario con saldi a zero e della prima sessione, accesso, rinnovo e revoca del token precedente. Sono stati verificati richiesta di recupero, completamento con un token di prova e revoca delle sessioni. Il riavvio ha mantenuto una riga per ruolo e la stessa cronologia Flyway.
- **Adozione.** Lo schema F01 predisposto separatamente coincideva con V1 nel confronto di `pg_dump --schema-only`. Il backend ha rifiutato l’avvio prima della baseline. Dopo la baseline esplicita a 1, l’avvio ha applicato V2 e preservato i dati di tutte e cinque le tabelle. Nelle due prove è stato conservato prima l’ID di un ruolo `USER` preesistente e poi l’ID di un ruolo `ADMIN` preesistente.
- **Compose.** `docker compose config --quiet` è passato. Il motore Linux di Docker Desktop era indisponibile durante le prove di avvio. [Verifiche e limiti del backend](../../springboot_app/app/README.md#verifiche-e-limiti).

### Verifica precedente della base del recupero password

Per la base del recupero del **2026-11-12**, sulla copia di lavoro di `feature/f01-accesso-utenti`, Java 21.0.11 e Maven Wrapper hanno compilato sorgenti e test con `mvnw.cmd -o -DskipTests test-compile`. Il comando `mvnw.cmd -o "-Dtest=SecurityConfigurationTests,SessionTokenCleanupTests,PasswordResetTokenCleanupTests" test` ha superato **31 test**, senza fallimenti, errori o casi saltati: 25 su catena di sicurezza/contratto HTTP con servizio simulato e 6 sugli scheduler. Questi controlli non attestano le query o la transazione completa.

Sono aggiunti e compilati test PostgreSQL con schema isolato per durata, richieste ripetute, account non ammessi, validazione, consumo, revoche, rollback, concorrenza e pulizia. **Non sono stati eseguiti**: il server non è stato avviato in sandbox e l’autorizzazione ad avviare un’istanza locale isolata è stata rifiutata. Non è stata eseguita l’intera suite. Sono passati `git diff --check`, `docker compose config --quiet` e il controllo di 182 collegamenti locali e relativi titoli in 11 documenti modificati. La validazione Compose riguarda la configurazione, non l’avvio dei servizi. Invio email, rendering Thymeleaf, frontend e Compose PostgreSQL 18 restano non verificati. I report storici sotto non attestano il nuovo recupero password.

### Evidenze storiche precedenti al recupero password

Il **2026-11-12** sono stati allineati al nuovo prefisso il controller, i matcher di sicurezza, i percorsi dei test e il contratto. Nella revisione documentale precedente al recupero sono stati confrontati controller, servizio, repository JDBC, catena di sicurezza, scheduler, POM, proprietà e Compose. Le verifiche applicative del 2026-11-11 riportate sotto precedono la modifica del prefisso.

I report Surefire locali consultati nella revisione precedente del **2026-11-12** riportavano **27 test**, tutti senza errori, fallimenti o casi saltati: 2 in `AppApplicationTests`, 21 in `SecurityConfigurationTests`, 3 in `SessionTokenCleanupTests`, 1 in `SessionTokenCleanupDatabaseTests`. Erano report già presenti in `springboot_app/app/target/surefire-reports/`, non versionati e privi di un commit identificato; le successive esecuzioni possono sostituire i singoli file. Questi numeri descrivono quella consultazione storica, non il risultato corrente dei test del recupero. [Sorgenti dei test](../../springboot_app/app/src/test/java/webapp).

Quella revisione documentale non aveva eseguito una nuova build, test JUnit o prove HTTP. Restano da verificare l’intero flusso di autenticazione sui nuovi percorsi contro PostgreSQL 18 tramite Compose, l’integrazione React nel browser e l’esecuzione dei job a orario reale. I test dello scheduler e della query di cancellazione non attestano questi scenari.

Il **2026-11-11** `mvnw.cmd verify` ha compilato e impacchettato il codice della copia di lavoro del branch `feature/f01-accesso-utenti`, con base `e1b4eeb`, su Windows e Java 21. I due test automatici hanno verificato il profilo protetto e il controllo CSRF sulle operazioni che scrivono il cookie. [Test di sicurezza](../../springboot_app/app/src/test/java/webapp/AppApplicationTests.java).

Le prove HTTP sul JAR hanno verificato registrazione e inventario, rollback su errore, duplicati, accesso, profilo, CORS, rotazione concorrente, scadenza della sessione, uscita, aggiornamento del ruolo e cancellazione logica. Il database di prova isolato usava PostgreSQL 17.10 con le tabelle F01 della v1.1. Restano da verificare PostgreSQL 18 tramite Compose e l’integrazione React in un browser.

Nella verifica del perimetro dei ruoli del **2026-11-11**, `mvnw.cmd verify` ha superato entrambi i test automatici. Le richieste HTTP al JAR con PostgreSQL 17.10 isolato hanno confermato accesso, profilo e rinnovo per `ADMIN` e `USER`, la risposta `401` anche con credenziali già emesse per un account con ruolo fuori perimetro e i due valori di `role` pubblicati in OpenAPI.

## Da definire

- Invio delle email di recupero e notifica con template Thymeleaf: [dipendenza rinviata](invio-mail.md). Interfaccia e verifica integrata del recupero restano fuori dall’attività corrente.
- Matrice dei permessi per le altre funzionalità.

## Collegamenti

- [Interfaccia di accesso](../FrontEnd/accesso-utenti.md)
- [Invio mail e template Thymeleaf](invio-mail.md)
- [Backoffice](gestione-amministrativa.md)
- [Indice BackEnd](README.md)
