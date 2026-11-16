# Cinder-Rift — Frontend

Pagine: Login → Home · Collezione · Deck (deck builder) · Gioca · Negozio.

Ultimo aggiornamento significativo: **2026-11-16**, configurazione Docker richiesta dall’utente. Il frontend è un prototipo React 18/Vite 5; login e dati dimostrativi non attestano l’integrazione con il backend Spring. Il riferimento canonico per autenticazione, cookie e CSRF è il [contratto F01](../docs/BackEnd/accesso-utenti.md).

## Avvio con Docker Compose

Dalla radice del repository, configura la chiave JWT seguendo la [guida di avvio](../guida-avvio.md), poi esegui:

```text
docker compose up --build --wait
```

Apri `http://localhost:8082`. Il servizio `react` compila i sorgenti con Node.js 22 e `npm ci` dal lockfile; Nginx serve il risultato sulla porta interna `8082`, con utente non privilegiato, filesystem in sola lettura e file temporanei in `/tmp`. `dist/`, `node_modules/` e i `.env` locali sono esclusi dal contesto Docker. Il server Vite e il server Node di prova non vengono avviati nell’immagine.

Nel `.env` della **radice del repository**, le variabili `VITE_API_URL` (default `/api`) e `VITE_USE_MOCK` (default `false`) diventano argomenti di build. Nginx inoltra `/api/` a `spring_app:8081` senza rimuovere il prefisso e gestisce il fallback SPA su `index.html`. I nomi dei container non sono indirizzi raggiungibili dal browser. Il controllo di salute usa `/healthz` e verifica solo Nginx.

Il login reale non è ancora compatibile con F01: payload e risposta del login, percorso del profilo, cookie, CSRF e persistenza dei token devono essere allineati al contratto canonico. Per esplorare la grafica imposta `VITE_USE_MOCK=true` nella radice, quindi esegui `docker compose up --build --wait react`. Questo abilita il login simulato esistente senza avviare `mock-api`; non verifica autenticazione o dati server.

Le variabili Vite sono pubbliche e incorporate nel bundle durante il build: non inserirvi segreti. Dopo ogni modifica a `VITE_*` ricostruisci `react`; il riavvio non basta. Il `.env` di questa cartella riguarda soltanto lo sviluppo locale seguente. [Configurazione Compose e porte](../guida-avvio.md#configurazione-dei-servizi-compose).

Verifica del **2026-11-16**: immagine costruita, servizio sano nello stack Compose isolato e risposte HTTP controllate per pagina, asset, fallback SPA e proxy API. Login Spring e interfaccia nel browser non verificati. [Evidenze e limiti](../docs/report/2026-11-16-verifica-docker-compose.md).

## Avvio
1. Installa Node.js 18+ (https://nodejs.org)
2. `npm install`
3. **Terminale 1** — backend di prova: `npm run mock-api` (http://localhost:3000/api)
4. **Terminale 2** — frontend: `npm run dev` → http://localhost:5173
5. Accedi con **mirko** (oppure mirko@cinder-rift.dev) e password **cinder123**

Senza backend acceso il login mostra "Server non raggiungibile". Per lavorare sulla grafica senza backend metti
`VITE_USE_MOCK=true` in `.env` (login finto, nessuna chiamata di rete).

## Collegare il backend vero

L’integrazione deve adeguare il client al [contratto F01](../docs/BackEnd/accesso-utenti.md), già implementato dal backend Spring. Cambiare solo `VITE_API_URL` o i nomi dei campi non basta: servono l’allineamento dei payload e delle risposte, `/api/auth/user/me`, cookie con credenziali, gestione CSRF e access token in memoria. Queste modifiche applicative non fanno parte della configurazione Docker. Per lo sviluppo su origini diverse configurare anche `APP_SECURITY_ALLOWED_ORIGINS` nel backend. Conservare il mock finché utile alle verifiche isolate del prototipo.

## Contratto del server simulato (autenticazione)

Quanto segue descrive esclusivamente `server/mock-api.js` e il client attuale; non sostituisce il contratto canonico F01 e non richiede modifiche al backend Spring.
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
