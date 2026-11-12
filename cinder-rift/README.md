# Cinder-Rift — Frontend

Pagine: Login → Home · Collezione · Deck (deck builder) · Gioca · Negozio.

## Avvio
1. Installa Node.js 18+ (https://nodejs.org)
2. `npm install`
3. **Terminale 1** — backend di prova: `npm run mock-api` (http://localhost:3000/api)
4. **Terminale 2** — frontend: `npm run dev` → http://localhost:5173
5. Accedi con **mirko** (oppure mirko@cinder-rift.dev) e password **cinder123**

Senza backend acceso il login mostra "Server non raggiungibile". Per lavorare sulla grafica senza backend metti
`VITE_USE_MOCK=true` in `.env` (login finto, nessuna chiamata di rete).

## Collegare il backend vero
1. In `.env` imposta `VITE_API_URL` con l'indirizzo del backend e `VITE_USE_MOCK=false`.
2. Il backend deve rispettare il contratto qui sotto. Se i percorsi sono diversi, si cambia solo `src/services/endpoints.js`;
   se i campi dell'utente hanno altri nomi, solo `normalizeUser()` in `src/services/authService.js`.
3. Se frontend e backend sono su origini diverse, il backend deve abilitare il CORS
   (header `Content-Type` e `Authorization`) oppure si usa un proxy.
4. Elimina la cartella `server/` e lo script `mock-api` da `package.json`.

## Contratto API (autenticazione)
Base: `VITE_API_URL` — il token si invia come `Authorization: Bearer <token>`.

| Metodo | Endpoint        | Body                      | Risposta                                                     |
|--------|-----------------|---------------------------|--------------------------------------------------------------|
| POST   | `/auth/login`   | `{ identifier, password }`| `200 { token, user }` · `400 { message }` · `401 { message }` |
| GET    | `/auth/me`      | —                         | `200 { user }` · `401`                                       |
| POST   | `/auth/logout`  | —                         | `204`                                                        |

`user` = `{ id, name, level, credits, avatar }`. Gli errori devono avere il campo `message` (viene mostrato nel login).

Comportamento del frontend:
- Il token è salvato in `sessionStorage` (si perde chiudendo la scheda).
- Al ricaricamento la sessione viene verificata con `GET /auth/me`: se il token non è valido si torna al login.
- Qualsiasi richiesta autenticata che riceve `401` riporta al login.
- Il logout avvisa il backend (`POST /auth/logout`) e chiude comunque la sessione locale.
- Timeout richieste: 10 secondi.

## Pagine (id in `App.jsx` e `data/navigation.js`)
| Navbar      | id           | Stato                                      |
|-------------|--------------|--------------------------------------------|
| HOME        | `home`       | completa                                   |
| COLLEZIONE  | `collezione` | completa (`screens/CollectionScreen`)      |
| DECK        | `deck`       | deck builder (`screens/DeckScreen`)        |
| GIOCA       | `gioca`      | segnaposto                                 |
| NEGOZIO     | `negozio`    | segnaposto                                 |

Collezione, Deck e Home usano ancora dati di esempio (`data/`, `services/cardService.js`, `services/deckService.js`).

## Struttura
```
server/                 backend di prova (mock-api.js) — da eliminare con il backend vero
src/
├── main.jsx, App.jsx   ingresso e navigazione
├── context/            AuthContext (sessione)
├── services/           api.js (fetch), endpoints.js, authService.js, cardService.js, deckService.js
├── screens/            LoginScreen, HomeScreen, CollectionScreen, DeckScreen, PlaceholderScreen
├── components/         layout/ ui/ home/ collection/ cards/ deck-builder/
├── models/             regole di gioco (mazzo, carte, filtri)
├── data/               dati di esempio
├── hooks/              useToast, useCardCollection
└── styles/             CSS per area
```
