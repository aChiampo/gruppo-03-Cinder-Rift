# Cinder-Rift — Frontend React

Aggiornamento: **2026-11-21**, implementazione delle pagine React e rimozione della modalità dimostrativa su richiesta dell’utente.

Aggiornamento F04: **2026-11-22**, su richiesta dell'utente, catalogo e collezione collegati alle API backend esistenti; payload canonici conservati, filtri e test allineati.

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
| Catalogo e collezione | Letture F04 autenticate e distinte; filtri server, ordinamento crescente/decrescente, paginazione, varianti e copie persistite. |
| Mazzi | CRUD F05, copie per variante e destinazione, validazione del server. |
| Campagna e risultato | Selezione di un mazzo del server e codice stage; simulazione PvE F07; pagina risultato con round, scambi, replay e log originale. Ultimo risultato riapribile durante la sessione. |
| Negozio e dettaglio pacchetto | Sezioni, risorse distinte e dettaglio modale; acquisto e apertura disabilitati finché F03 non definisce le API. |
| Profilo e impostazioni | Dati reali e aggiornamento nickname/email/icona tramite F01; logout. |

Navigazione tramite hash URL, con cronologia e ricaricamento. JWT solo in memoria, rinnovo tramite cookie dopo ricaricamento, prima della scadenza e dopo rifiuto Bearer; rinnovo e uscita serializzati. Un rinnovo rifiutato con `401` riporta all’accesso. Nessun saldo economico viene dedotto dai punti classifica.

Il catalogo e la collezione usano il [contratto F04](../docs/BackEnd/collezione-carte.md); i dettagli sono già inclusi nelle risposte degli elenchi. La UI conserva riferimenti e valori nulli del backend, mostra un segnaposto per immagini mancanti e distingue ogni variante posseduta. Richiede 15 righe per pagina e traduce la numerazione della UI in base zero. I filtri categoriali hanno un solo valore; i limiti numerici vuoti sono omessi, senza escludere implicitamente statistiche nulle. [Comportamento completo F04](../docs/FrontEnd/collezione-carte.md).

Aggiornamento F05 del **2026-11-22**, su richiesta dell'utente: il builder mostra le varianti possedute tramite F04, in formato compatto, filtrate per il gruppo attivo (creature con energia non superiore al numero del round, equipaggiamenti nel gruppo dedicato). Il selettore unico include Round 1–5 ed Equipaggiamenti. Un clic riempie il primo dei cinque slot vuoti; il trascinamento sceglie lo slot. Sono gestiti copie disponibili nell'intero mazzo, rimozione, ricerca, caricamento ed errori. I metadati delle copie salvate provengono da F04; la validazione completa resta al server. Il contratto F05 persiste copie e destinazioni, senza le posizioni locali degli slot. [Comportamento e limiti](../docs/FrontEnd/gestione-mazzi.md).

## Risultato della battaglia F07

Aggiornamento del **2026-11-22**, su richiesta dell'utente: pagina dedicata ispirata a `mock-up-pagine/battle-result.png`, con banner dell'esito, partecipanti, creature, equipaggiamenti, selezione round e navigazione degli scambi. Il risultato della partita rimane quello del campo `outcome`; i dettagli visuali interpretano esclusivamente i marcatori correnti del log del motore. Valori assenti o ambigui sono indicati come non disponibili. Le immagini facoltative dal catalogo F04 non sostituiscono le statistiche del report.

Dopo una simulazione la pagina si apre automaticamente. Home e Campagna riaprono l'ultimo risultato della sessione; `#risultato-battaglia?round=1&scambio=2` seleziona una posizione nel risultato presente in memoria. Il replay non richiama il POST. Ricaricando la pagina il report non è più disponibile: non è stato introdotto uno storico persistito.

- [Comportamento del visualizzatore e limiti del log testuale](../docs/FrontEnd/sfide-simulazione.md#risultato-della-sessione).
- [Verifica documentale e frontend del 2026-11-23](../docs/report/2026-11-23-allineamento-documentazione.md).

Verifica frontend: **54 test superati**, build Vite riuscita. Il browser non è disponibile nell'ambiente: confronto visivo, controlli interattivi e integrazione con Spring non verificati in questa attività. Payload, servizi API e backend invariati.

## Immagini delle carte CR01

Aggiornamento del **2026-11-22**, su richiesta dell'utente: le 30 illustrazioni centrali del set CR01 sono ritagliate dagli originali `Carte/` e salvate come WebP lossless in [public/img/cards](public/img/cards/), con nomi descrittivi distinti. Il retro comune è escluso. I file conservano i pixel del ritaglio originale senza ridimensionamento; non contengono cornici, pannelli dei nomi, statistiche o testi di gioco.

`npm run build` copia i file in `dist/img/cards/`. La migrazione backend V10 collega le arti esistenti agli URL della stessa origine `/img/cards/<nome-carta>.webp`; distribuire insieme backend aggiornato e frontend ricostruito. Il componente F04 legge già `imageUrl` e conserva il segnaposto per immagini assenti o non caricabili. [Associazione e aggiornamento dei dati](../docs/BackEnd/collezione-carte.md#immagini-del-set-cr01).

## Verifiche

```text
npm test
npm run build
```

I test verificano servizi e rendering statico con fixture isolate nei test. Non sostituiscono una verifica browser contro Spring/PostgreSQL. Il build aggiorna `dist/`, già versionata. Per servire il build localmente: `npm run preview`.

Verifica F05 del **2026-11-22**: 44 test frontend superati e build riuscita, incluse le verifiche del selettore, degli slot e dei limiti energetici inclusivi dei cinque round. Trascinamento nativo e flusso browser contro il backend non verificati in questa attività.

Verifica F04 del **2026-11-22**: 33 test frontend superati e build riuscita. Client sorgente verificato contro Spring locale: 30 carte nel catalogo e 30 righe nella collezione dimostrativa, filtro combinato, seconda pagina, `400` per intervallo invertito e `401` senza token. Nessuna modifica ai payload o al codice backend. Browser non verificato perché l'host di accesso non è disponibile.

## Avvio con Docker Compose

Usare la procedura Compose esistente dalla radice del repository. Il Dockerfile di questa cartella accetta solo `VITE_API_URL` e serve il build con Nginx sulla porta 8082; il proxy inoltra `/api/` a Spring mantenendo il prefisso.

Per applicare modifiche del frontend in locale: `docker compose --file compose.yaml up --detach --build --no-deps --wait react` dalla radice. Eventuali argomenti obsoleti di modalità dimostrativa non attivano modalità alternative: l'app usa il backend reale.

## Fonti e limiti

- [API frontend e stato degli endpoint](../docs/BackEnd/GUIDA-API-FRONTEND.md)
- [Autenticazione e profili F01](../docs/BackEnd/accesso-utenti.md)
- [Mazzi F05](../docs/BackEnd/gestione-mazzi.md)
- [Battaglie PvE F07](../docs/BackEnd/app-battaglie-api.md)
- [Collezione F04](../docs/BackEnd/collezione-carte.md)
- [Pacchetti F03](../docs/BackEnd/acquisto-pacchetti.md)
