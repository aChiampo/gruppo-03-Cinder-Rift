# Guida API per il Frontend

Guida autonoma da condividere con il team frontend. Riporta le route attualmente esposte dai controller Java `AuthController`, `UserController` e `DeckController`, i rispettivi DTO, i vincoli e le risposte HTTP. Il contratto può cambiare quando cambia il codice: per una verifica aggiornata consultare Swagger UI e la specifica OpenAPI live.

## 1. Panoramica e URL Base

- **Base URL:** `http://localhost:8081`
- **Swagger UI interattiva:** [http://localhost:8081/swagger-ui.html](http://localhost:8081/swagger-ui.html)
- **Specificazione OpenAPI JSON:** [http://localhost:8081/v3/api-docs](http://localhost:8081/v3/api-docs)

Per ogni richiesta che invia un body JSON, impostare:

```http
Content-Type: application/json
```

Le route GET e le risposte `204 No Content` non hanno un body JSON. Nessuna route documentata usa parametri query. Le route che mostrano `{userId}` o `{deckId}` usano un identificativo intero positivo nel percorso.

## 2. Come funziona l'Autenticazione (Leggere prima di iniziare)

Esistono due meccanismi distinti. Non scambiare il token CSRF con il JWT: servono a richieste diverse.

### Meccanismo A — CSRF + Cookie (solo per `/api/auth/*` in scrittura: registrazione, login, logout, reset password)

Usare questo flusso per le sei richieste `POST /api/auth/*`: registrazione, login, richiesta reset password, conferma reset password, rinnovo sessione e logout.

1. Chiamare `GET /api/auth/csrf-tokens` con le credenziali browser abilitate.
2. Leggere il campo `token` dal JSON ricevuto. La risposta imposta anche il cookie HttpOnly `__Host-cinder-csrf`; JavaScript non può leggerlo direttamente.
3. Nelle richieste POST inviare il valore letto nell'header `X-XSRF-TOKEN`.
4. Includere i cookie del browser in ogni chiamata: `credentials: 'include'` con `fetch` oppure `withCredentials: true` con Axios. È necessario anche per ricevere e rimandare `__Host-cinder-refresh`, usato da rinnovo e logout.
5. Non inviare un JWT per queste richieste. Login e registrazione impostano il cookie di rinnovo e restituiscono l'access token nel JSON.

Il cookie CSRF ha attributi `Path=/`, `Secure`, `HttpOnly` e `SameSite=Strict`. Il cookie refresh `__Host-cinder-refresh` usa gli stessi attributi. Il token CSRF è nel JSON, non nel cookie.

### Meccanismo B — Bearer Token JWT (per tutte le API protette `/api/users/**` e `/api/decks/**`)

Usare questo meccanismo per i profili, i mazzi e anche `GET /api/auth/users/me`. Inviare:

```http
Authorization: Bearer <accessToken>
```

Non servono cookie CSRF né `X-XSRF-TOKEN` per queste route. Il client riceve `accessToken` nel JSON della registrazione/login/rinnovo. Il cookie refresh è HttpOnly: non leggerlo né inserirlo manualmente negli header.

### Esempio JavaScript `fetch`

Esempio essenziale: ottiene il token CSRF, esegue il login, poi usa il JWT per leggere il profilo e salvare una bozza mazzo.

```javascript
const API = "http://localhost:8081";

async function getCsrfToken() {
  const response = await fetch(`${API}/api/auth/csrf-tokens`, {
    credentials: "include"
  });
  if (!response.ok) throw new Error(`CSRF bootstrap failed: ${response.status}`);
  const data = await response.json();
  return data.token;
}

async function login(email, password) {
  const csrfToken = await getCsrfToken();
  const response = await fetch(`${API}/api/auth/sessions`, {
    method: "POST",
    credentials: "include",
    headers: {
      "Content-Type": "application/json",
      "X-XSRF-TOKEN": csrfToken
    },
    body: JSON.stringify({ email, password })
  });
  if (!response.ok) throw new Error(`Login failed: ${response.status}`);
  return (await response.json()).accessToken;
}

async function getMyProfile(accessToken) {
  const response = await fetch(`${API}/api/auth/users/me`, {
    headers: { Authorization: `Bearer ${accessToken}` }
  });
  if (!response.ok) throw new Error(`Profile request failed: ${response.status}`);
  return response.json();
}

async function saveDraft(accessToken, deckId, entries) {
  const response = await fetch(`${API}/api/decks/${deckId}`, {
    method: "PUT",
    headers: {
      "Authorization": `Bearer ${accessToken}`,
      "Content-Type": "application/json"
    },
    body: JSON.stringify({ entries, isChallengeable: false })
  });
  if (!response.ok) {
    const problem = await response.json();
    throw new Error(problem.detail ?? `Deck update failed: ${response.status}`);
  }
  return response.json();
}
```

## 3. Tabella Riassuntiva di Tutti gli Endpoint

`CSRF + cookie` indica cookie `__Host-cinder-csrf` più header `X-XSRF-TOKEN`; il cookie refresh è richiesto solo quando indicato. `Bearer` indica l'header `Authorization`.

| Metodo | Endpoint | Autenticazione | Cosa fa | Codici HTTP |
| --- | --- | --- | --- | --- |
| `GET` | `/api/auth/csrf-tokens` | Nessuna | Fornisce il token per proteggere le scritture `/api/auth/*` | `200` |
| `POST` | `/api/auth/users` | CSRF + cookie | Registra un account e apre una sessione | `201`, `400`, `403`, `409`, `415` |
| `POST` | `/api/auth/sessions` | CSRF + cookie | Login con email e password | `200`, `400`, `401`, `403`, `415` |
| `POST` | `/api/auth/password-reset-requests` | CSRF + cookie | Accetta una richiesta di reset password | `202`, `400`, `403`, `415` |
| `POST` | `/api/auth/password-resets` | CSRF + cookie | Cambia password con token di reset | `204`, `400`, `403`, `415` |
| `GET` | `/api/auth/users/me` | Bearer | Restituisce il profilo privato corrente | `200`, `401` |
| `POST` | `/api/auth/session-renewals` | CSRF + cookie refresh | Ruota il token refresh e restituisce un nuovo access token | `200`, `401`, `403` |
| `POST` | `/api/auth/session-revocations` | CSRF + cookie facoltativo | Esegue logout e scade il cookie refresh | `204`, `403` |
| `GET` | `/api/users/{userId}` | Bearer | Legge il profilo pubblico di un utente attivo | `200`, `400`, `401`, `404` |
| `PATCH` | `/api/users/me` | Bearer | Modifica parzialmente il proprio profilo | `200`, `400`, `401`, `404`, `409` |
| `GET` | `/api/users/me/decks` | Bearer | Elenca i mazzi e la loro validazione corrente | `200`, `401` |
| `POST` | `/api/users/me/decks` | Bearer | Crea una bozza, con copie iniziali facoltative | `201`, `400`, `401`, `404` |
| `GET` | `/api/decks/{deckId}` | Bearer | Legge dettagli e validazione corrente di un mazzo di proprietà | `200`, `400`, `401`, `404` |
| `PUT` | `/api/decks/{deckId}` | Bearer | Sostituisce le copie e lo stato sfidabile | `200`, `400`, `401`, `404` |
| `DELETE` | `/api/decks/{deckId}` | Bearer | Elimina un mazzo di proprietà | `204`, `400`, `401`, `404` |

## 4. Dettaglio Completo per Ogni Endpoint

### Come leggere gli errori

Gli errori gestiti da Spring MVC usano `application/problem+json` e i campi RFC 7807:

```json
{
  "type": "about:blank",
  "title": "Bad Request",
  "status": 400,
  "detail": "Invalid request content.",
  "instance": "/api/auth/users"
}
```

- `type`: tipo di problema; il valore `about:blank` usa il titolo HTTP standard.
- `title`: breve descrizione dello status.
- `status`: status HTTP.
- `detail`: descrizione del problema; per errori di validazione generici può essere una descrizione standard, non il nome del campo.
- `instance`: percorso della richiesta che ha prodotto l'errore.

I filtri Spring Security rifiutano le richieste non autenticate con `401` e il bearer filter aggiunge `WWW-Authenticate`. Il rifiuto CSRF restituisce `403`. La configurazione non imposta un body RFC 7807 per i rifiuti prodotti direttamente dai filtri: in questi casi il frontend deve leggere lo status e non presumere che sia presente un JSON. Gli esempi MVC sotto mostrano il formato per gli status che il codice gestisce nel controller/servizio.

#### Esempi Problem Detail MVC

Credenziali errate (`401`, per esempio `POST /api/auth/sessions`):

```json
{
  "type": "about:blank",
  "title": "Unauthorized",
  "status": 401,
  "detail": "Invalid credentials",
  "instance": "/api/auth/sessions"
}
```

Risorsa mancante (`404`, esempio `GET /api/users/999`):

```json
{
  "type": "about:blank",
  "title": "Not Found",
  "status": 404,
  "detail": "User not found",
  "instance": "/api/users/999"
}
```

Conflitto account (`409`, esempio `PATCH /api/users/me`):

```json
{
  "type": "about:blank",
  "title": "Conflict",
  "status": 409,
  "detail": "Email or nickname is already in use",
  "instance": "/api/users/me"
}
```

Per `403` CSRF non è riportato un oggetto Problem Detail: la risposta effettiva del filtro non è garantita come JSON. La risposta `400` della validazione HTTP usa la struttura standard; il testo di `detail` può essere generico. Gli errori di validazione delle regole mazzo hanno inoltre i campi specifici mostrati nella sezione C.

### A. Autenticazione e Sessione

Tutti i body di richiesta qui sotto sono JSON. Le password e i token di reset sono `@JsonProperty(WRITE_ONLY)`: sono accettati nel body ma non vengono restituiti come proprietà dei DTO.

#### `GET /api/auth/csrf-tokens`

- **Header:** nessun header di autenticazione; il browser deve accettare il cookie.
- **Parametri:** nessuno.
- **Body:** nessuno.
- **Successo `200 OK`:**

```json
{
  "headerName": "X-XSRF-TOKEN",
  "token": "<csrf-token>"
}
```

`headerName` è il nome dell'header da usare nelle successive scritture; `token` è il valore da inviare. La risposta imposta il cookie `__Host-cinder-csrf`.
- **Errori:** `403` se una policy del browser/security rifiuta la richiesta; il filtro non garantisce un body Problem Detail. Questa route non ha un errore applicativo `400`, `401`, `404` o `409`.

#### `POST /api/auth/users` — registrazione

- **Header:** `Content-Type: application/json`, `X-XSRF-TOKEN: <token>`. Inviare i cookie con `credentials: 'include'`.
- **Parametri:** nessuno.
- **Body:** `nickname` stringa `@NotBlank`; `email` stringa `@NotBlank`, `@Email`, massimo 254 caratteri; `password` stringa `@NotBlank`. Il server rimuove gli spazi esterni da `nickname`, normalizza `email` con `strip().toLowerCase(Locale.ROOT)` e rifiuta password oltre 72 byte UTF-8. Non è dichiarato un limite massimo `@Size` per `nickname` in questo DTO.
- **Esempio:**

```json
{
  "nickname": "RiftPlayer",
  "email": "player@example.invalid",
  "password": "<password scelta dall'utente>"
}
```

- **Successo `201 Created`:** imposta il cookie HttpOnly `__Host-cinder-refresh` e restituisce:

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

`accessToken` è il JWT da usare come Bearer; `tokenType` vale `Bearer`; `expiresIn` è la durata in secondi; `user` contiene `id`, `nickname`, `email`, `role`, `rankingPoints`, `iconUrl`. `role` è `USER` o `ADMIN`; `iconUrl` può essere `null`.
- **Errori:** `400` body non valido o password oltre 72 byte; `403` CSRF mancante/errato (nessun Problem Detail garantito); `409` email o nickname già usati, con `detail: "Email or nickname is already in use"` nella struttura RFC qui sopra; `415` `Content-Type` non supportato. Non è richiesto Bearer, quindi un JWT non va inviato.

#### `POST /api/auth/sessions` — login

- **Header:** `Content-Type: application/json`, `X-XSRF-TOKEN: <token>`. Inviare i cookie con `credentials: 'include'`.
- **Parametri:** nessuno.
- **Body:** `email` stringa `@NotBlank`, `@Email`, massimo 254 caratteri; `password` stringa `@NotBlank`, massimo 72 byte UTF-8 applicato dal servizio.

```json
{
  "email": "player@example.invalid",
  "password": "<password>"
}
```

- **Successo `200 OK`:** stessa forma `SessionResponse` della registrazione; imposta/aggiorna `__Host-cinder-refresh`. Tutti i campi sono descritti in quella risposta.
- **Errori:** `400` body non valido o password oltre 72 byte; `401` credenziali non valide, con `detail: "Invalid credentials"`; `403` CSRF mancante/errato (nessun Problem Detail garantito); `415` tipo contenuto non supportato.

#### `POST /api/auth/password-reset-requests` — richiesta reset password

- **Header:** `Content-Type: application/json`, `X-XSRF-TOKEN: <token>`. Inviare i cookie con `credentials: 'include'`.
- **Parametri:** nessuno.
- **Body:** `email` stringa `@NotBlank`, `@Email`, massimo 254 caratteri; il servizio la normalizza.

```json
{"email":"player@example.invalid"}
```

- **Successo `202 Accepted`:**

```json
{
  "message": "If the account is eligible, the password reset request has been accepted."
}
```

`message` è uguale sia per account idonei sia non idonei. Il servizio crea token con scadenza a 10 minuti, ma invio email e consegna del token non sono implementati.
- **Errori:** `400` body email non valido; `403` CSRF mancante/errato, senza body Problem Detail garantito; `415` tipo contenuto non supportato. L'idoneità dell'account non causa un errore distinguibile.

#### `POST /api/auth/password-resets` — conferma reset password

- **Header:** `Content-Type: application/json`, `X-XSRF-TOKEN: <token>`. Inviare i cookie con `credentials: 'include'`.
- **Parametri:** nessuno.
- **Body:** `token` stringa `@NotBlank`; `newPassword` stringa `@NotBlank`, massimo 72 byte UTF-8 applicato dal servizio. Il token valido è una stringa Base64 URL-safe senza padding di 43 caratteri.

```json
{
  "token": "<reset-token di 43 caratteri>",
  "newPassword": "<nuova password>"
}
```

- **Successo `204 No Content`:** nessun body, nessuna nuova sessione. Le sessioni esistenti vengono revocate.
- **Errori:** `400` body non valido, password oltre 72 byte o token invalido/scaduto/usato; per il token il Problem Detail ha `title: "Bad Request"`, `status: 400`, `detail: "Invalid password reset token"` e `instance: "/api/auth/password-resets"`; `403` CSRF (nessun Problem Detail garantito); `415` tipo contenuto non supportato.

#### `GET /api/auth/users/me` — profilo privato corrente

- **Header:** `Authorization: Bearer <accessToken>`.
- **Parametri e body:** nessuno.
- **Successo `200 OK`:**

```json
{
  "id": 42,
  "nickname": "RiftPlayer",
  "email": "player@example.invalid",
  "role": "USER",
  "rankingPoints": 1000,
  "iconUrl": null
}
```

I campi sono quelli del record `UserProfile`; `getName()` è ignorato nella serializzazione.
- **Errori:** `401` access token mancante/invalido/scaduto/revocato; filtro Bearer, header `WWW-Authenticate`, nessun body RFC 7807 garantito. Non richiede CSRF.

#### `POST /api/auth/session-renewals` — rinnovo sessione

- **Header:** `X-XSRF-TOKEN: <token>`; inviare i cookie con `credentials: 'include'`. Il cookie `__Host-cinder-refresh` è richiesto; non inviare Bearer.
- **Parametri e body:** nessuno.
- **Successo `200 OK`:** `SessionResponse` con `accessToken`, `tokenType`, `expiresIn`, `user`, come mostrato per la registrazione. Il cookie `__Host-cinder-refresh` viene ruotato; il token precedente non è più valido.
- **Errori:** `401` cookie refresh mancante/non valido/scaduto/revocato, Problem Detail MVC con `detail: "Invalid refresh token"`; `403` CSRF mancante/errato, nessun body RFC garantito.

#### `POST /api/auth/session-revocations` — logout

- **Header:** `X-XSRF-TOKEN: <token>`; inviare i cookie con `credentials: 'include'`. Il cookie `__Host-cinder-refresh` è facoltativo; non inviare Bearer.
- **Parametri e body:** nessuno.
- **Successo `204 No Content`:** nessun body; la risposta imposta `__Host-cinder-refresh` con scadenza immediata. Un cookie mancante o inutilizzabile non impedisce la risposta `204`.
- **Errori:** `403` CSRF mancante/errato; risposta del filtro senza Problem Detail garantito. Non ci sono `401` o `404` per questa route.

### B. Profilo Utente

#### `GET /api/users/{userId}` — profilo pubblico

- **Header:** `Authorization: Bearer <accessToken>`.
- **Parametro di percorso:** `userId`, intero `@Positive`. Nessun parametro query e nessun body.
- **Successo `200 OK`:**

```json
{
  "id": 42,
  "nickname": "RiftPlayer",
  "rankingPoints": 1000,
  "iconUrl": null,
  "registrationDate": "2026-11-13T12:00:00Z"
}
```

Campi del record `PublicUserProfile`: `id` (intero), `nickname` (stringa), `rankingPoints` (intero), `iconUrl` (stringa o `null`), `registrationDate` (timestamp ISO-8601). Sono esposti solo utenti non logicamente cancellati.
- **Errori:** `400` `userId` non positivo/non convertibile, Problem Detail standard; `401` Bearer non valido, risposta del filtro e `WWW-Authenticate`; `404` utente assente o cancellato, `detail: "User not found"` nella struttura RFC comune.

#### `PATCH /api/users/me` — modifica profilo

- **Header:** `Authorization: Bearer <accessToken>`, `Content-Type: application/json`.
- **Parametri:** nessuno.
- **Body:** tutte le proprietà sono facoltative. `nickname` stringa, dopo `strip()` non blank e massimo 50 caratteri; `email` stringa, dopo `strip().toLowerCase(Locale.ROOT)` non blank, formato `@Email`, massimo 254 caratteri; `iconUrl` stringa senza vincoli Bean Validation. Un campo omesso o `null` mantiene il valore attuale; `iconUrl: ""` rimuove l'icona.

```json
{
  "nickname": " NewNickname ",
  "email": " PLAYER@EXAMPLE.INVALID ",
  "iconUrl": ""
}
```

- **Successo `200 OK`:** restituisce `UserProfile` aggiornato, con `id`, `nickname`, `email`, `role`, `rankingPoints`, `iconUrl`. Il server non restituisce `registrationDate` in questo record.

```json
{
  "id": 42,
  "nickname": "NewNickname",
  "email": "player@example.invalid",
  "role": "USER",
  "rankingPoints": 1000,
  "iconUrl": null
}
```

- **Errori:** `400` campi non validi, Problem Detail standard (il testo può non identificare il campo); `401` Bearer non valido, filtro e `WWW-Authenticate`; `404` profilo autenticato non più attivo, `detail: "User not found"`; `409` email o nickname duplicati, `detail: "Email or nickname is already in use"`.

### C. Gestione Mazzi

Ogni route richiede `Authorization: Bearer <accessToken>` e non richiede CSRF. L'identità e il proprietario sono ricavati dal principal: non inviare `userId` o `ownerId`. I mazzi altrui sono trattati come non esistenti e restituiscono `404`.

Gli elementi di input hanno la forma `DeckEntryRequest`:

```json
{"cardId":17,"cardArtId":41,"destination":"ROUND1"}
```

- `cardId`: intero obbligatorio, `@NotNull`, `@Positive`.
- `cardArtId`: intero obbligatorio, `@NotNull`, `@Positive`.
- `destination`: stringa obbligatoria non blank (`@NotBlank`). I valori seminati/configurati correnti sono `ROUND1`, `ROUND2`, `ROUND3`, `ROUND4`, `ROUND5`, `EQUIPMENT`. Una carta `CREATURE` è compatibile con `ROUND1`–`ROUND5`; `EQUIPMENT` è compatibile con `EQUIPMENT`. Il DTO verifica che non sia blank; esistenza della destinazione e compatibilità vengono controllate dalla validazione del mazzo.
- Ogni elemento è una copia singola. Non inviare `entryId`: l'ID viene assegnato dal server.

Una risposta di dettaglio contiene `id`, `ownerId`, `isChallengeable`, `entries` e `validation`. Ogni `entries[]` usa `entryId`, `cardId`, `cardArtId`, `destination`. `validation` contiene:

- `valid`: esito booleano della validazione corrente.
- `errors` e `warnings`: liste di `DeckViolation`, con campi `code`, `scope`, `message`, `cardId`, `cardArtId`, `destination`, `severity`. I campi identificativi/destinazione non pertinenti sono `null`; `severity` è `ERROR` o `WARNING`.
- `summary`: `totalCards`, `creatureCount`, `equipmentCount`, `cardCountByDestination` (mappa nome→intero), `energyByDestination` (mappa nome→numero).

I limiti per destinazione provengono da `round_configurations`: i valori seed iniziali configurano `max_cards: 5` per ogni destinazione e `max_energy` pari a `1`, `2`, `3`, `4`, `5` per `ROUND1`–`ROUND5` e `5` per `EQUIPMENT`. Il database può essere riconfigurato; la validazione usa i valori correnti.

#### `GET /api/users/me/decks` — elenco mazzi

- **Header:** `Authorization: Bearer <accessToken>`.
- **Parametri e body:** nessuno.
- **Successo `200 OK`:** lista (vuota se non ci sono mazzi); ogni elemento contiene `id`, `ownerId`, `isChallengeable`, `cardCount`, `validation`. Esempio:

```json
[
  {
    "id": 8,
    "ownerId": 42,
    "isChallengeable": false,
    "cardCount": 1,
    "validation": {
      "valid": true,
      "errors": [],
      "warnings": [
        {
          "code": "CREATURE_COUNT_INCOMPLETE",
          "scope": "creatures",
          "message": "Expected 25 creatures but found 0",
          "cardId": null,
          "cardArtId": null,
          "destination": null,
          "severity": "WARNING"
        },
        {
          "code": "EQUIPMENT_COUNT_INCOMPLETE",
          "scope": "equipment",
          "message": "Expected 5 equipment but found 0",
          "cardId": null,
          "cardArtId": null,
          "destination": "EQUIPMENT",
          "severity": "WARNING"
        },
        {
          "code": "CARD_COUNT_INCOMPLETE",
          "scope": "round",
          "message": "Expected 5 round but found 0",
          "cardId": null,
          "cardArtId": null,
          "destination": "ROUND1",
          "severity": "WARNING"
        },
        {
          "code": "CARD_COUNT_INCOMPLETE",
          "scope": "round",
          "message": "Expected 5 round but found 0",
          "cardId": null,
          "cardArtId": null,
          "destination": "ROUND2",
          "severity": "WARNING"
        },
        {
          "code": "CARD_COUNT_INCOMPLETE",
          "scope": "round",
          "message": "Expected 5 round but found 0",
          "cardId": null,
          "cardArtId": null,
          "destination": "ROUND3",
          "severity": "WARNING"
        },
        {
          "code": "CARD_COUNT_INCOMPLETE",
          "scope": "round",
          "message": "Expected 5 round but found 0",
          "cardId": null,
          "cardArtId": null,
          "destination": "ROUND4",
          "severity": "WARNING"
        },
        {
          "code": "CARD_COUNT_INCOMPLETE",
          "scope": "round",
          "message": "Expected 5 round but found 0",
          "cardId": null,
          "cardArtId": null,
          "destination": "ROUND5",
          "severity": "WARNING"
        },
        {
          "code": "DECK_COUNT_INCOMPLETE",
          "scope": "deck",
          "message": "Expected 30 deck but found 0",
          "cardId": null,
          "cardArtId": null,
          "destination": null,
          "severity": "WARNING"
        }
      ],
      "summary": {
        "totalCards": 0,
        "creatureCount": 0,
        "equipmentCount": 0,
        "cardCountByDestination": {},
        "energyByDestination": {}
      }
    }
  }
]
```

- **Errori:** `401` Bearer assente/non valido; filtro con `WWW-Authenticate`, senza body Problem Detail garantito. Questa route non dichiara `400`, `403`, `404` o `409`.

#### `POST /api/users/me/decks` — crea bozza

- **Header:** `Authorization: Bearer <accessToken>`. Se si invia un body, `Content-Type: application/json`.
- **Parametri:** nessuno.
- **Body:** interamente facoltativo. Se presente, `entries` è una lista facoltativa; può essere omessa, `null` o vuota. Ogni elemento segue `DeckEntryRequest`.

```json
{
  "entries": [
    {"cardId": 17, "cardArtId": 41, "destination": "ROUND1"}
  ]
}
```

- **Successo `201 Created`:** restituisce la forma completa di `DeckDetailsResponse` descritta nella route GET dettaglio. Il nuovo mazzo ha `isChallengeable: false`. Una bozza incompleta è consentita: la mancanza delle carte previste è in `validation.warnings`.
- **Errori:** `400` body/entry non valido o regole del mazzo non soddisfatte (con `application/problem+json` e `errors`, `warnings`, `summary`); `401` Bearer non valido; `404` utente non più attivo durante la creazione. Il rifiuto del filtro Bearer non garantisce Problem Detail.

#### `GET /api/decks/{deckId}` — dettaglio mazzo

- **Header:** `Authorization: Bearer <accessToken>`.
- **Parametro di percorso:** `deckId`, intero positivo. Nessun parametro query e nessun body.
- **Successo `200 OK`:** `DeckDetailsResponse`, con `id`, `ownerId`, `isChallengeable`, `entries` e `validation` come definiti sopra.

```json
{
  "id": 8,
  "ownerId": 42,
  "isChallengeable": false,
  "entries": [],
  "validation": {
    "valid": true,
    "errors": [],
    "warnings": [
      {
        "code": "CREATURE_COUNT_INCOMPLETE",
        "scope": "creatures",
        "message": "Expected 25 creatures but found 0",
        "cardId": null,
        "cardArtId": null,
        "destination": null,
        "severity": "WARNING"
      },
      {
        "code": "EQUIPMENT_COUNT_INCOMPLETE",
        "scope": "equipment",
        "message": "Expected 5 equipment but found 0",
        "cardId": null,
        "cardArtId": null,
        "destination": "EQUIPMENT",
        "severity": "WARNING"
      },
      {
        "code": "CARD_COUNT_INCOMPLETE",
        "scope": "round",
        "message": "Expected 5 round but found 0",
        "cardId": null,
        "cardArtId": null,
        "destination": "ROUND1",
        "severity": "WARNING"
      },
      {
        "code": "CARD_COUNT_INCOMPLETE",
        "scope": "round",
        "message": "Expected 5 round but found 0",
        "cardId": null,
        "cardArtId": null,
        "destination": "ROUND2",
        "severity": "WARNING"
      },
      {
        "code": "CARD_COUNT_INCOMPLETE",
        "scope": "round",
        "message": "Expected 5 round but found 0",
        "cardId": null,
        "cardArtId": null,
        "destination": "ROUND3",
        "severity": "WARNING"
      },
      {
        "code": "CARD_COUNT_INCOMPLETE",
        "scope": "round",
        "message": "Expected 5 round but found 0",
        "cardId": null,
        "cardArtId": null,
        "destination": "ROUND4",
        "severity": "WARNING"
      },
      {
        "code": "CARD_COUNT_INCOMPLETE",
        "scope": "round",
        "message": "Expected 5 round but found 0",
        "cardId": null,
        "cardArtId": null,
        "destination": "ROUND5",
        "severity": "WARNING"
      },
      {
        "code": "DECK_COUNT_INCOMPLETE",
        "scope": "deck",
        "message": "Expected 30 deck but found 0",
        "cardId": null,
        "cardArtId": null,
        "destination": null,
        "severity": "WARNING"
      }
    ],
    "summary": {
      "totalCards": 0,
      "creatureCount": 0,
      "equipmentCount": 0,
      "cardCountByDestination": {},
      "energyByDestination": {}
    }
  }
}
```

- **Errori:** `400` `deckId` non positivo/non convertibile; `401` Bearer non valido; `404` mazzo assente o di altro proprietario, Problem Detail con `detail: "Deck not found"`.

#### `PUT /api/decks/{deckId}` — sostituisce copie e stato sfidabile

- **Header:** `Authorization: Bearer <accessToken>`, `Content-Type: application/json`.
- **Parametro di percorso:** `deckId`, intero positivo.
- **Body:** `entries` è obbligatorio e non può essere `null`; può essere vuoto. Ogni elemento segue `DeckEntryRequest`. `isChallengeable` è facoltativo; se assente, `null` o `false`, viene interpretato come `false`.

```json
{
  "entries": [
    {"cardId": 17, "cardArtId": 41, "destination": "ROUND1"}
  ],
  "isChallengeable": false
}
```

- **Successo `200 OK`:** restituisce `DeckDetailsResponse`. Con `isChallengeable: false`, una composizione incompleta può essere salvata e le mancanze compaiono come warning. Per le altre violazioni (per esempio carta/arte inesistente, copie non possedute, destinazione non compatibile, limite di energia/carte superato) l'aggiornamento è respinto con `400`.
- **Per attivare un mazzo giocabile:** inviare `"isChallengeable": true`. La validazione completa richiede 30 copie: 25 creature e 5 equipaggiamenti, 5 creature per ciascuno dei round `ROUND1`–`ROUND5`, oltre a validità di carte, arti, possesso e limiti configurati. Se una regola fallisce, il server restituisce `400` e non salva la sostituzione.
- **Errore regole mazzo `400 Bad Request`:** il controller restituisce `ProblemDetail` RFC 7807 e aggiunge `errors`, `warnings`, `summary`. Esempio per una richiesta che prova a rendere sfidabile un mazzo vuoto:

```json
{
  "type": "about:blank",
  "title": "Deck validation failed",
  "status": 400,
  "detail": "Deck validation failed",
  "instance": "/api/decks/8",
  "errors": [
    {"code":"CREATURE_COUNT_INCOMPLETE","scope":"deck","message":"Expected 25 creatures but found 0","cardId":null,"cardArtId":null,"destination":null,"severity":"ERROR"},
    {"code":"EQUIPMENT_COUNT_INCOMPLETE","scope":"round","message":"Expected 5 equipment but found 0","cardId":null,"cardArtId":null,"destination":"EQUIPMENT","severity":"ERROR"},
    {"code":"CARD_COUNT_INCOMPLETE","scope":"round","message":"Expected 5 round but found 0","cardId":null,"cardArtId":null,"destination":"ROUND1","severity":"ERROR"},
    {"code":"CARD_COUNT_INCOMPLETE","scope":"round","message":"Expected 5 round but found 0","cardId":null,"cardArtId":null,"destination":"ROUND2","severity":"ERROR"},
    {"code":"CARD_COUNT_INCOMPLETE","scope":"round","message":"Expected 5 round but found 0","cardId":null,"cardArtId":null,"destination":"ROUND3","severity":"ERROR"},
    {"code":"CARD_COUNT_INCOMPLETE","scope":"round","message":"Expected 5 round but found 0","cardId":null,"cardArtId":null,"destination":"ROUND4","severity":"ERROR"},
    {"code":"CARD_COUNT_INCOMPLETE","scope":"round","message":"Expected 5 round but found 0","cardId":null,"cardArtId":null,"destination":"ROUND5","severity":"ERROR"},
    {"code":"DECK_COUNT_INCOMPLETE","scope":"deck","message":"Expected 30 deck but found 0","cardId":null,"cardArtId":null,"destination":null,"severity":"ERROR"}
  ],
  "warnings": [],
  "summary": {
    "totalCards": 0,
    "creatureCount": 0,
    "equipmentCount": 0,
    "cardCountByDestination": {},
    "energyByDestination": {}
  }
}
```

L'ordine delle violazioni non è garantito. Gli errori di binding/Bean Validation (per esempio `entries: null` o un ID non positivo) usano invece il Problem Detail standard MVC, senza le proprietà `errors`, `warnings`, `summary`. `401` token non valido; `404` mazzo mancante o non di proprietà (`detail: "Deck not found"`).

#### `DELETE /api/decks/{deckId}` — elimina mazzo

- **Header:** `Authorization: Bearer <accessToken>`.
- **Parametro di percorso:** `deckId`, intero positivo. Nessun parametro query e nessun body.
- **Successo `204 No Content`:** nessun body; elimina il mazzo e le righe copia associate.
- **Errori:** `400` `deckId` non positivo/non convertibile; `401` Bearer non valido; `404` mazzo assente o non di proprietà, `detail: "Deck not found"`.

#### Errori mazzo: codici di validazione

Il client può usare `errors[]` e `warnings[]` per evidenziare i campi/zone da correggere. I codici emessi dalle regole attuali includono:

| Code | Significato |
| --- | --- |
| `INVALID_CARD_REFERENCE` | `cardId` o `cardArtId` non positivo nel contesto validato |
| `INVALID_DESTINATION` | `destination` vuota nel contesto validato |
| `CARD_NOT_FOUND` | Carta assente o cancellata |
| `CARD_ART_NOT_FOUND` | Arte selezionata non associata a una carta attiva |
| `DESTINATION_NOT_FOUND` | Destinazione non configurata |
| `DESTINATION_TYPE_MISMATCH` | Tipo carta non compatibile con destinazione |
| `INSUFFICIENT_OWNERSHIP` | Il numero di copie usate supera quelle possedute per quella carta/arte |
| `INVALID_ENERGY_COST` | Costo energia mancante o negativo |
| `DESTINATION_CARD_LIMIT_EXCEEDED` | Numero carte oltre `max_cards` per destinazione |
| `DESTINATION_ENERGY_LIMIT_EXCEEDED` | Somma energia oltre `max_energy` per destinazione |
| `CREATURE_COUNT_INCOMPLETE` / `CREATURE_COUNT_EXCEEDED` | Creature sotto/sopra il totale previsto |
| `EQUIPMENT_COUNT_INCOMPLETE` / `EQUIPMENT_COUNT_EXCEEDED` | Equipaggiamenti sotto/sopra il totale previsto |
| `CARD_COUNT_INCOMPLETE` / `CARD_COUNT_EXCEEDED` | Copie per round sotto/sopra il totale previsto |
| `DECK_COUNT_INCOMPLETE` / `DECK_COUNT_EXCEEDED` | Copie totali sotto/sopra il totale previsto |

I codici di completezza sotto soglia sono `WARNING` per una bozza e `ERROR` quando si richiede `isChallengeable: true`; superare una soglia è un errore. Le regole di potenza, quote di rarità e limite copie della proposta V2.1 non sono attualmente implementate.

## 5. Guida Pratica ai Flussi Frontend (Step-by-Step)

### Flusso 1: Registrazione/Login e profilo

1. Richiedere `GET /api/auth/csrf-tokens` con cookie browser inclusi e conservare in memoria il suo `token`.
2. Inviare `POST /api/auth/users` per registrarsi oppure `POST /api/auth/sessions` per il login. In entrambi i casi inviare `X-XSRF-TOKEN`, `Content-Type: application/json` e `credentials: 'include'`.
3. Conservare `accessToken` in memoria. Il cookie `__Host-cinder-refresh` è gestito dal browser e non va letto dall'applicazione.
4. Leggere il profilo privato con `GET /api/auth/users/me` e `Authorization: Bearer <accessToken>`.
5. Modificare i campi consentiti con `PATCH /api/users/me`. La risposta è il profilo aggiornato.
6. Per logout chiamare `POST /api/auth/session-revocations` con token CSRF e cookie inclusi, poi rimuovere l'access token dalla memoria.

### Flusso 2: Deck Builder

1. Creare una bozza con `POST /api/users/me/decks` (body vuoto o `{}` consentito), oppure caricare l'elenco con `GET /api/users/me/decks`.
2. Caricare il dettaglio con `GET /api/decks/{deckId}` e mostrare `entries`, `validation.summary` e `validation.warnings`.
3. Salvare la composizione corrente con `PUT /api/decks/{deckId}` e `"isChallengeable": false`. La bozza può avere meno di 30 carte: la richiesta riesce se non ci sono altre violazioni bloccanti. Le mancanze sono elencate in `validation.warnings`.
4. Prima di rendere il mazzo giocabile, inviare lo stesso aggiornamento con `"isChallengeable": true`. Se una regola fallisce, il server risponde `400 Bad Request`; non salva le nuove copie e restituisce `errors`, `warnings` e `summary`.
5. Per mostrare gli errori usare `validation.errors[]` nella risposta positiva oppure `errors[]` nel Problem Detail `400`. Per le carte mancanti controllare i codici di completezza e `scope`/`destination`; per energia controllare `DESTINATION_ENERGY_LIMIT_EXCEEDED` e `summary.energyByDestination`. Un warning indica una mancanza non bloccante della bozza; `severity: "ERROR"` indica una violazione bloccante.

La validazione è eseguita sul server sui dati correnti di carte, arti, possesso e configurazioni. Il frontend può anticipare i controlli per l'interfaccia, ma deve usare la risposta server come esito autorevole.

## 6. Risoluzione Rapida Errori Comuni (Troubleshooting)

| HTTP status | Errore frequente nel frontend | Come risolvere |
| --- | --- | --- |
| `400` | Body JSON malformato, email/nickname non valido, password oltre 72 byte UTF-8, ID percorso non positivo, campi mancanti, oppure `isChallengeable: true` con regole non rispettate | Controllare nomi e tipi dei campi nella sezione endpoint. Per il mazzo leggere `errors[]`, `warnings[]` e `summary`; salvare come bozza con `false` finché non è completo. |
| `401` | JWT mancante/scaduto/revocato, password errata, oppure cookie refresh non presente/valido | Per `/api/users/**` e `/api/decks/**` inviare `Authorization: Bearer ${accessToken}` senza virgolette aggiuntive nel valore. Se scaduto, provare il rinnovo con cookie e CSRF; se il rinnovo fallisce, richiedere un nuovo login. |
| `403` | Dimenticato `credentials: 'include'`, cookie CSRF non inviato, oppure `X-XSRF-TOKEN` mancante o non corrispondente al token del bootstrap | Rifare `GET /api/auth/csrf-tokens` con `credentials: 'include'`; inviare il token JSON in `X-XSRF-TOKEN` e includere i cookie nella scrittura `/api/auth/*`. Le API Bearer non richiedono CSRF. |
| `404` | `userId` o `deckId` inesistente, account cancellato, oppure accesso a un mazzo di un altro utente | Verificare l'ID e il percorso. Il server nasconde la proprietà dei mazzi: mazzo mancante e non proprio hanno lo stesso esito. |
| `409` | Registrazione o PATCH con email/nickname già usati | Scegliere un valore diverso; il backend restituisce un messaggio generico per non rivelare quale campo coincide. |

**Nota:** inviare il JWT come `Authorization: Bearer <accessToken>`: non racchiudere il token tra virgolette e non usare il token CSRF come Bearer. `credentials: 'include'` è necessario per cookie e CSRF sulle rotte `/api/auth/*`; non sostituisce `Authorization` sulle rotte protette da JWT.
