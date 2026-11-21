# Cinder-Rift — Frontend React

Aggiornamento: **2026-11-21**, implementazione delle pagine React e rimozione della modalità dimostrativa su richiesta dell’utente.

## Avvio locale

Prerequisiti: Node.js 22+ e backend Spring disponibile. Dalla cartella `cinder-rift/`:

```text
npm ci
npm run dev
```

Aprire `http://localhost:5173`. Vite inoltra `/api` a `http://localhost:8081`. Per modificare la destinazione, copiare `.env.example` in `.env` e configurare `API_PROXY_TARGET`. `VITE_API_URL` è la base pubblica delle API, include `/api` e ha default `/api`. Non contiene segreti.

Accesso e registrazione richiedono il backend. Cookie Secure/HttpOnly e origini consentite seguono il [contratto F01](../docs/BackEnd/accesso-utenti.md#comportamento-del-client): mantenere lo stesso hostname; in distribuzione usare HTTPS nello stesso sito. Non sono disponibili un server di prova o credenziali simulate.

## Pagine e integrazione

| Pagina | Comportamento |
| --- | --- |
| Accesso e registrazione | Email/password; nickname e conferma password per la registrazione; CSRF e creazione sessione F01. |
| Home | Campagna, navigazione e mazzi reali; indisponibilità per novità, sfide e storico. |
| Catalogo e collezione | Componenti condivisi per filtri, carte, dettaglio e paginazione; indisponibilità finché manca il contratto HTTP F04. |
| Mazzi | CRUD F05, copie per variante e destinazione, validazione del server. |
| Campagna e risultato | Selezione di un mazzo del server e codice stage; simulazione PvE F07; esito e log restituiti dal server. |
| Negozio e dettaglio pacchetto | Sezioni, risorse distinte e dettaglio modale; acquisto e apertura disabilitati finché F03 non definisce le API. |
| Profilo e impostazioni | Dati reali e aggiornamento nickname/email/icona tramite F01; logout. |

Navigazione tramite hash URL, con cronologia e ricaricamento. JWT solo in memoria, rinnovo tramite cookie dopo ricaricamento, prima della scadenza e dopo rifiuto Bearer; rinnovo e uscita serializzati. Un rinnovo rifiutato con `401` riporta all’accesso. Nessun saldo economico viene dedotto dai punti classifica.

Il catalogo non ha ancora una rotta canonica. Il selettore visuale del builder segnala questa dipendenza; è disponibile un editor alternativo di ID carta/variante per copie note. Il contratto F05 non fornisce nomi, immagini o statistiche delle carte nei dettagli dei mazzi. Composizione e possesso restano verificati dal server.

## Verifiche

```text
npm test
npm run build
```

I test verificano servizi e rendering statico con fixture isolate nei test. Non sostituiscono una verifica browser contro Spring/PostgreSQL. Il build aggiorna `dist/`, già versionata. Per servire il build localmente: `npm run preview`.

## Docker

Usare la procedura Compose esistente dalla radice del repository. Il Dockerfile di questa cartella accetta solo `VITE_API_URL` e serve il build con Nginx sulla porta 8082; il proxy inoltra `/api/` a Spring mantenendo il prefisso.

Compose e documentazione nella radice non sono stati modificati per rispettare il confine richiesto. Eventuali argomenti obsoleti nella configurazione esterna non attivano modalità alternative.

## Fonti e limiti

- [API frontend e stato degli endpoint](../docs/BackEnd/GUIDA-API-FRONTEND.md)
- [Autenticazione e profili F01](../docs/BackEnd/accesso-utenti.md)
- [Mazzi F05](../docs/BackEnd/gestione-mazzi.md)
- [Battaglie PvE F07](../docs/BackEnd/app-battaglie-api.md)
- [Collezione F04](../docs/BackEnd/collezione-carte.md)
- [Pacchetti F03](../docs/BackEnd/acquisto-pacchetti.md)
- [Problemi, scelte e verifiche](frontend-report.md)
