# Accesso utenti

ID: F01 · Perimetro backend confermato per registrazione, accesso, profilo, rinnovo e uscita.
Implementazione: backend disponibile nel branch `feature/f01-accesso-utenti`; integrazione FrontEnd da avviare. Responsabile: DA DEFINIRE.
Ultimo aggiornamento significativo: 2026-11-12, su richiesta dell’utente: prefisso `/api/auth` e pulizia giornaliera delle sessioni tramite Spring alle 03:00 UTC, senza conservazione delle righe inutilizzabili. Flussi, durate, cookie e ruoli `ADMIN` e `USER` restano quelli confermati il 2026-11-11.
Fonte dati: [specifica canonica v1.1](Database/database-schema-v1.1.md#sistema-di-autenticazione-e-autorizzazione).

## Stack implementato

F01 usa Java 21, Spring Boot 4.1.2, Spring MVC, Bean Validation e Spring Security con il supporto OAuth2 resource server per verificare i bearer token. `AuthenticationService` emette i JWT nell’applicazione con `JwtEncoder`; non sono configurati un provider di identità esterno o un flusso di accesso OAuth2. Le password sono verificate con BCrypt.

`AccountRepository` accede a PostgreSQL con `JdbcClient` e SQL esplicito, senza entità JPA per F01. Lo schema v1.1 è predisposto esternamente: Flyway, DDL Hibernate e inizializzazione SQL automatica sono disabilitati. Compose configura PostgreSQL 18; le prove HTTP storiche indicate sotto usavano PostgreSQL 17.10. La catena di sicurezza è stateless rispetto a `HttpSession`, ma verifica nel database account e sessione a ogni richiesta Bearer. [Dipendenze e configurazione backend](../../springboot_app/app/README.md).

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
| `400` | Campi obbligatori mancanti, formato non valido o limite della password superato. |
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

1. Richiedere `GET /api/auth/csrf-tokens` all’avvio del flusso con `credentials: 'include'` quando l’origine del client è diversa. Inviare il valore `token` ricevuto nell’header `X-XSRF-TOKEN` e includere le credenziali nelle quattro richieste `POST` di sessione o registrazione.
2. Tenere il JWT di accesso in memoria e aggiungerlo alle richieste protette. Per ricostruire la sessione dopo un ricaricamento o una scadenza, chiamare l’endpoint di rinnovo con i cookie e il token CSRF. Omettere l’header `Authorization` sulle richieste di rinnovo e uscita. Sostituire il JWT in memoria dopo ogni rinnovo riuscito.
3. Scartare il JWT all’uscita. Se il rinnovo restituisce `401`, tornare alla schermata di accesso.

La configurazione CORS consente richieste con credenziali soltanto dalle origini configurate, con metodi `GET`, `HEAD`, `POST`, `PUT`, `PATCH`, `DELETE`, `OPTIONS` e header `Authorization`, `Content-Type`, `X-XSRF-TOKEN`. Un token Bearer presente viene comunque verificato anche sulle rotte pubbliche: il client evita di inviare un JWT scaduto o inutilizzabile nelle chiamate di bootstrap e accesso.

I cookie `__Host-cinder-csrf` e `__Host-cinder-refresh` usano `Secure`, `HttpOnly`, `SameSite=Strict` e `Path=/`. In distribuzione usare HTTPS e mantenere frontend e backend sullo stesso sito. Per lo sviluppo locale, usare lo stesso nome host nei due URL.

## Configurazione e operatività

- Configurare una chiave casuale di almeno 32 byte in Base64 tramite `APP_SECURITY_JWT_SECRET`. Fornire lo stesso valore alle istanze che devono verificare gli stessi JWT.
- Impostare `APP_SECURITY_ALLOWED_ORIGINS` come elenco di origini esatte separate da virgola. Il valore predefinito è `http://localhost:8082`. Le proprietà `app.security.access-token-lifetime` e `app.security.refresh-token-lifetime` controllano le durate, con default `15m` e `14d`: la prima deve essere almeno un secondo, la seconda almeno pari alla prima. Le relative variabili `APP_SECURITY_ACCESS_TOKEN_LIFETIME` e `APP_SECURITY_REFRESH_TOKEN_LIFETIME` sono lette da Spring, ma non sono inoltrate dal Compose corrente. Il backend richiede un database v1.1 già predisposto con il ruolo `USER`. [Configurazione backend](../../springboot_app/app/README.md).
- Il documento OpenAPI è disponibile su `GET /v3/api-docs`. Swagger UI è disponibile su `/swagger-ui/index.html`. Il controllo di salute usato da Compose è `GET /actuator/health`.
- `app.security.session-token-cleanup-cron` usa il formato cron Spring a sei campi e vale `0 0 3 * * *`; `app.security.session-token-cleanup-zone` vale `UTC`. Le corrispondenti variabili `APP_SECURITY_SESSION_TOKEN_CLEANUP_CRON` e `APP_SECURITY_SESSION_TOKEN_CLEANUP_ZONE` sono inoltrate da Compose. Impostare il cron a `-` per disabilitare il job su un’istanza. [Configurazione e verifiche](../../springboot_app/app/README.md#pulizia-delle-sessioni-f01).
- Il job opera mentre il backend è avviato; un’esecuzione persa durante un arresto non viene recuperata all’avvio. Il passaggio successivo elimina tutte le righe allora inutilizzabili. Ogni istanza abilitata esegue il job: con più repliche si può abilitarlo su una sola, senza necessità di coordinamento per la correttezza della cancellazione. Il log riporta solo il numero di righe eliminate; gli errori sono registrati dal scheduler e il tentativo successivo segue il cron.

## Verifiche ed evidenze

Il **2026-11-12** sono stati allineati al nuovo prefisso il controller, i matcher di sicurezza, i percorsi dei test e il contratto. Nella revisione documentale dello stesso giorno sono stati confrontati controller, servizio, repository JDBC, catena di sicurezza, scheduler, POM, proprietà e Compose. Le verifiche applicative del 2026-11-11 riportate sotto precedono la modifica del prefisso.

I report Surefire locali consultati il **2026-11-12** riportano **27 test**, tutti senza errori, fallimenti o casi saltati: 2 in `AppApplicationTests` (profilo e CSRF sui percorsi `/api/auth` nei sorgenti correnti), 21 in `SecurityConfigurationTests` (restrizioni amministrative), 3 in `SessionTokenCleanupTests` (cron, fuso e disabilitazione), 1 in `SessionTokenCleanupDatabaseTests` (cancellazione su tabella temporanea PostgreSQL e ripetibilità). Sono report già presenti in `springboot_app/app/target/surefire-reports/`, consultabili dopo una build locale e non versionati; non sono test rieseguiti in questa revisione e non attestano un commit identificato. [Sorgenti dei test](../../springboot_app/app/src/test/java/webapp).

Questa revisione non esegue una nuova build, test JUnit o prove HTTP. Restano da verificare l’intero flusso di autenticazione sui nuovi percorsi contro PostgreSQL 18 tramite Compose, l’integrazione React nel browser e l’esecuzione del job a orario reale. I test dello scheduler e della query di cancellazione non attestano questi scenari.

Il **2026-11-11** `mvnw.cmd verify` ha compilato e impacchettato il codice della copia di lavoro del branch `feature/f01-accesso-utenti`, con base `e1b4eeb`, su Windows e Java 21. I due test automatici hanno verificato il profilo protetto e il controllo CSRF sulle operazioni che scrivono il cookie. [Test di sicurezza](../../springboot_app/app/src/test/java/webapp/AppApplicationTests.java).

Le prove HTTP sul JAR hanno verificato registrazione e inventario, rollback su errore, duplicati, accesso, profilo, CORS, rotazione concorrente, scadenza della sessione, uscita, aggiornamento del ruolo e cancellazione logica. Il database di prova isolato usava PostgreSQL 17.10 con le tabelle F01 della v1.1. Restano da verificare PostgreSQL 18 tramite Compose e l’integrazione React in un browser.

Nella verifica del perimetro dei ruoli del **2026-11-11**, `mvnw.cmd verify` ha superato entrambi i test automatici. Le richieste HTTP al JAR con PostgreSQL 17.10 isolato hanno confermato accesso, profilo e rinnovo per `ADMIN` e `USER`, la risposta `401` anche con credenziali già emesse per un account con ruolo fuori perimetro e i due valori di `role` pubblicati in OpenAPI.

## Da definire

- Recupero della password e matrice dei permessi per le altre funzionalità.

## Collegamenti

- [Interfaccia di accesso](../FrontEnd/accesso-utenti.md)
- [Backoffice](gestione-amministrativa.md)
- [Indice BackEnd](README.md)
