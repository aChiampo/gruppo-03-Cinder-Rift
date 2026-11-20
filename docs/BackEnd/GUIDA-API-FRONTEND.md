# Riepilogo degli endpoint

Ultimo aggiornamento significativo: **2026-11-20**, su richiesta dell'utente di elencare gli endpoint documentati con percorso, funzione e stato. Fonte: contratti canonici collegati sotto e codice presente nel branch `docs/pve-battle-contract`.

Il primo campo include metodo HTTP e percorso, per distinguere operazioni diverse sulla stessa risorsa. I filtri di query già documentati restano espliciti. L'elenco comprende le rotte applicative documentate, incluse le proposte; non comprende pagine web, esempi generici di naming o rotte tecniche di salute/OpenAPI. L'API interna del motore, implementata dalla PR #36 ora integrata, non è una seconda rotta pubblica: il suo contratto è in [App battaglie](app-battaglie.md#api-interna-del-motore-implementata).

Gli stati usano esattamente queste etichette; BE significa Backend e FE significa Frontend:

- **Planned**: rotta documentata senza implementazione applicativa nel branch; include proposte ancora da concordare.
- **BE Implemented**: rotta presente nel backend, senza corrispondente chiamata nel frontend.
- **FE Implemented**: chiamata presente nel frontend. Per le quattro rotte qui indicate esiste anche il backend; lo stato non attesta una verifica integrata o il completamento di tutti i flussi della funzionalità.

Riepilogo: **39 voci**, di cui **23 Planned**, **12 BE Implemented** e **4 FE Implemented**. I dettagli di autenticazione, permessi, payload e codici HTTP restano nei contratti canonici.

| Endpoint path | Function | State |
| --- | --- | --- |
| `GET /api/auth/csrf-tokens` | Fornisce il token CSRF per le scritture F01 | FE Implemented |
| `POST /api/auth/signin` | Registra un account con ruolo `USER` e apre una sessione | BE Implemented |
| `POST /api/auth/login` | Esegue il login con email e password | FE Implemented |
| `POST /api/auth/password-reset-requests` | Accetta una richiesta di recupero password; invio email da implementare | BE Implemented |
| `POST /api/auth/password-resets` | Cambia la password e revoca le sessioni | BE Implemented |
| `GET /api/auth/user/me` | Restituisce il profilo privato corrente | FE Implemented |
| `POST /api/auth/session-renewals` | Ruota la sessione e restituisce un nuovo access token | BE Implemented |
| `POST /api/auth/logout` | Revoca la sessione corrente e scade il cookie refresh | FE Implemented |
| `DELETE /api/auth/users/{userId}` | Cancella logicamente un account e invalida i suoi token | BE Implemented |
| `GET /api/users/{userId}` | Legge il profilo pubblico di un utente attivo | BE Implemented |
| `PATCH /api/users/me` | Modifica parzialmente il proprio profilo | BE Implemented |
| `GET /api/users/me/decks` | Elenca i propri mazzi con validazione corrente | BE Implemented |
| `POST /api/users/me/decks` | Crea una propria bozza con copie iniziali facoltative | BE Implemented |
| `GET /api/decks/{deckId}` | Legge dettagli e validazione di un proprio mazzo | BE Implemented |
| `PUT /api/decks/{deckId}` | Sostituisce copie e stato sfidabile del proprio mazzo | BE Implemented |
| `DELETE /api/decks/{deckId}` | Elimina un proprio mazzo | BE Implemented |
| `POST /api/battle/pve/{stageId}` | Richiede una simulazione PvE con un proprio mazzo | Planned |
| `POST /api/announcements` | Propone un annuncio pubblico o uno scambio privato con un amico | Planned |
| `GET /api/announcements?scope=marketplace` | Elenca gli annunci pubblici pubblicati e non scaduti | Planned |
| `GET /api/announcements?scope=mine` | Elenca gli annunci del chiamante | Planned |
| `GET /api/announcements/{announceId}` | Legge proposta e stato secondo la visibilità; annuncio in trattativa riservato ai partecipanti | Planned |
| `POST /api/announcements/{announceId}/negotiations` | Avvia una trattativa, accetta l’originale o invia una prima controproposta ammessa | Planned |
| `GET /api/negotiations?scope=mine` | Elenca le trattative attive cui partecipa il chiamante | Planned |
| `GET /api/negotiations/{negotiationId}` | Mostra proposta, versione, consensi e scadenza ai partecipanti | Planned |
| `PUT /api/negotiations/{negotiationId}/offers/current` | Sostituisce la proposta negoziabile e rilascia le prenotazioni | Planned |
| `POST /api/negotiations/{negotiationId}/acceptances` | Accetta la versione attesa e prenota il proprio lato; il secondo consenso completa lo scambio | Planned |
| `POST /api/negotiations/{negotiationId}/rejections` | Rifiuta la trattativa e rilascia le prenotazioni | Planned |
| `POST /api/announcements/{announceId}/cancellations` | Consente al pubblicatore di annullare l’annuncio | Planned |
| `GET /api/trades?scope=mine` | Elenca gli scambi conclusi cui partecipa il chiamante | Planned |
| `GET /api/trades/{tradeId}` | Mostra uno scambio concluso ai soli partecipanti | Planned |
| `GET /api/users/{userId}/tradable-cards` | Mostra varianti scambiabili e disponibilità al netto delle prenotazioni | Planned |
| `POST /api/friend-requests` | Invia una richiesta di amicizia per il chiamante | Planned |
| `GET /api/me/friend-requests?direction=incoming` | Elenca le richieste pendenti ricevute dal chiamante | Planned |
| `GET /api/me/friend-requests?direction=outgoing` | Elenca le richieste pendenti inviate dal chiamante | Planned |
| `PATCH /api/friend-requests/{otherUserId}` | Consente al destinatario di accettare una richiesta pendente | Planned |
| `DELETE /api/friend-requests/{otherUserId}` | Consente al destinatario di rifiutare o al mittente di annullare una richiesta pendente | Planned |
| `GET /api/me/friends` | Elenca le amicizie accettate del chiamante | Planned |
| `DELETE /api/me/friends/{otherUserId}` | Rimuove un’amicizia accettata cui partecipa il chiamante | Planned |
| `GET /api/users/{otherUserId}/friendship` | Mostra la relazione del chiamante con l’altro utente | Planned |

## Contratti e codice di riferimento

- [F01 — Autenticazione](accesso-utenti.md#guida-autenticazione-header-e-openapi)
- [Client React](../../cinder-rift/src/services/authService.js)
- [F01 — Contratto](accesso-utenti.md#contratto-tecnico-condiviso)
- [F01 — Recupero password](accesso-utenti.md#recupero-password-base-backend)
- [F01 — Cancellazione account](accesso-utenti.md#cancellazione-account)
- [F01 — Profili](accesso-utenti.md#profili-utente)
- [F05 — API mazzi](gestione-mazzi.md#api-http-implementate)
- [F07 — Endpoint e payload](app-battaglie-api.md)
- [F06 — Endpoint proposti](scambi-mercato.md#endpoint-necessari)
- [F13 — Contratto proposto](amicizie.md#contratto-tecnico-condiviso-proposto)
- [Controller F01](../../springboot_app/app/src/main/java/webapp/auth/AuthController.java), [profili](../../springboot_app/app/src/main/java/webapp/user/UserController.java) e [mazzi F05](../../springboot_app/app/src/main/java/webapp/deck/DeckController.java).
- [Specifica applicativa PvE e decisioni aperte](app-battaglie.md) · [Stato delle funzionalità](../README.md#stato-delle-funzionalità) · [Indice BackEnd](README.md).

## Ambito e verifiche

La funzionalità PvE applicativa e il suo endpoint pubblico sono **documentati**. Lo stato della rotta resta **Planned** nella tabella, perché orchestrazione applicativa e chiamata frontend sono da implementare; lo stato della specifica è distinto da quello del codice. Le API di pacchetti F03, collezione F04, storico F08 e backoffice F10 non hanno ancora percorsi definiti nei rispettivi contratti canonici e non vengono inventate in questo elenco. Il client della collezione contiene un percorso provvisorio in [endpoints.js](../../cinder-rift/src/services/endpoints.js), ma il [contratto F04](collezione-carte.md) lascia le API da definire.

Gli endpoint tecnici documentati separatamente sono [OpenAPI e salute del backend](accesso-utenti.md#guida-autenticazione-header-e-openapi) e [salute del motore](../../springboot_app/game_engine/README.md#avvio-con-docker-compose).

Gli stati derivano dall'ispezione dei contratti, dei controller e delle chiamate React nella copia di lavoro; nessuna build, prova runtime o verifica integrata è stata eseguita per questo aggiornamento documentale. Le evidenze storiche restano nelle pagine collegate.
