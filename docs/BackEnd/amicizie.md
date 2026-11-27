# Amicizie

ID funzionalità: **F13**; dipendenze: **F01** e **F06**.
Perimetro: richieste e amicizie reciproche; contratto API definito e backend implementato, frontend e F06 da completare.
Implementazione: **backend F13 e cleanup amicizie F01 disponibili nella copia di lavoro** del branch `feature/f13-friendships-backend`; frontend e F06 esclusi dall'attività su scelta dell'utente. Responsabile: **DA DEFINIRE**.
Ultimo aggiornamento significativo: **2026-11-24**, migrazione amicizie V11. Il contratto API risale al **2026-11-20**; ruoli, errori, elenchi completi e requisiti F01/F06 al **2026-11-18**.
Fonti dati e invarianti: [specifica canonica v1.1](Database/database-schema-v1.1.md) e [DBML](Database/db_schema-v1.1.dbml). V11 realizza i vincoli F13 del modello esistente.
Aggiornamento implementativo: **2026-11-24**. Migrazione V11, API e cleanup atomico F01; verifiche e problemi dell'attività nel [report](../report/2026-11-22-implementazione-amicizie.md). Le evidenze precedenti restano storiche.

## Obiettivo, perimetro e dipendenze

L'amicizia esiste soltanto dopo l'accettazione della richiesta da parte del destinatario. Si prevedono invio, consultazione, accettazione, rifiuto e annullamento delle richieste, oltre alla rimozione delle amicizie accettate. `FRIENDSHIPS` rappresenta solo la relazione corrente; non conserva uno storico di rifiuti o rimozioni.

[F01](accesso-utenti.md) fornisce identità e autenticazione. [F06](scambi-mercato.md#richiesta-privata) richiede un'amicizia `accepted` per avviare uno scambio privato. Anche F07 usa la relazione accettata come precondizione per creare una Sfida Amichevole diretta; il controllo è eseguito dall'API PvP tramite il servizio amicizie. Il contratto PvP canonico è in [Sfide e simulazione](sfide-simulazione.md#sfide-amichevoli-dirette), senza duplicare qui endpoint o payload. La rimozione successiva dell'amicizia non è descritta come annullamento automatico delle sfide già create.

Per decisione dell'utente, la rimozione dell'amicizia da parte di un partecipante `USER` o `ADMIN` non elimina gli scambi pubblici o privati già esistenti e non ne modifica il TTL: restano soggetti alle normali regole F06, con scadenza originaria di due giorni dalla creazione dell'annuncio e possibilità di chiusura anticipata secondo quel contratto. Non si possono avviare nuovi scambi privati senza una nuova amicizia `accepted`.

Questa pagina contiene il contratto canonico F13 e descrive il backend disponibile. Cleanup amicizie: [F01](accesso-utenti.md#integrazione-prevista-con-f13-e-f06). Effetti futuri sugli scambi: [F06](scambi-mercato.md#amicizie-e-cancellazione-degli-account). La [pagina FrontEnd](../FrontEnd/amicizie.md) resta una specifica da implementare; nessun file frontend viene modificato in questa attività.

## Dati, vincoli e persistenza

### Identificativi degli utenti

`USERS.id` è l'unico identificativo dell'utente, un `int` auto-incrementale usato anche nei riferimenti pubblici, nelle notifiche e per i destinatari degli scambi privati. Il client invia questo intero; non esistono `user_public_id` o un UUID pubblico separato. Il backend identifica l'attore dalla sessione autenticata, senza accettare dal client il suo ID o `requested_by` come autorità.

Un account non eliminato ha `USERS.is_deleted = false`; lo schema non definisce un ulteriore stato di account attivo o una tabella di blocchi tra utenti. Conoscere un ID non autorizza l'accesso alle sue relazioni.

### `FRIENDSHIPS`

| Campo | Tipo e nullabilità | Default | Significato |
| --- | --- | --- | --- |
| `user_a_id` | `int NOT NULL` | Nessuno | Minore dei due `USERS.id`; FK e prima parte della PK. |
| `user_b_id` | `int NOT NULL` | Nessuno | Maggiore dei due `USERS.id`; FK e seconda parte della PK. |
| `requested_by` | `int NOT NULL` | Nessuno | FK a `USERS.id` del mittente; appartiene alla coppia. |
| `status` | `text NOT NULL` | Nessuno | `pending` oppure `accepted`; va impostato esplicitamente. |
| `created_at` | `timestamptz NOT NULL` | `now()` | Creazione della richiesta corrente. |
| `accepted_at` | `timestamptz`, nullable | Nessuno | Nullo in attesa, valorizzato dopo l'accettazione. |

La PK composta è `(user_a_id, user_b_id)`. Non esiste un ID autonomo dell'amicizia e nessuna delle sue colonne genera identificativi. Il mittente può essere il minore o il maggiore degli utenti: l'ordinamento numerico della coppia non esprime la direzione della richiesta.

| Vincolo dichiarato | Espressione |
| --- | --- |
| `friendships_ordered_pair_check` | `user_a_id < user_b_id` |
| `friendships_requester_check` | `requested_by IN (user_a_id, user_b_id)` |
| `friendships_status_check` | `status IN ('pending', 'accepted')` |
| `friendships_acceptance_check` | `(status = 'pending' AND accepted_at IS NULL) OR (status = 'accepted' AND accepted_at IS NOT NULL)` |

PK e ordinamento impediscono coppie duplicate, coppie inverse e autoamicizie. Le tre FK (`user_a_id`, `user_b_id`, `requested_by`) riferiscono `USERS.id` con `ON DELETE CASCADE` alla cancellazione fisica. La cancellazione logica non attiva la cascata.

La PK indicizza la coppia a partire da `user_a_id`. La v1.1 non dichiara un indice secondario su `user_b_id`; la sua eventuale introduzione richiede una successiva modifica dello schema. La lettura dell'elenco deve comunque considerare entrambi i lati.

Il database verifica integrità della riga ed esistenza degli utenti; autenticazione, autorizzazione, account non eliminati e transizioni consentite sono responsabilità del backend. Non sono modellati scadenza delle richieste, periodi di attesa, storico o blocchi tra utenti.

## Attori, autorizzazioni e precondizioni

Tutte le operazioni richiedono `Authorization: Bearer <accessToken>`, con JWT e controlli di account/sessione F01; i cookie da soli non autenticano F13 e non è richiesto CSRF per queste rotte Bearer. `USER` e `ADMIN` hanno gli stessi permessi e possono operare soltanto sulle proprie richieste e amicizie. Il ruolo `ADMIN` non autorizza a leggere, accettare o eliminare relazioni tra altri utenti. Il backend verifica che attore e altro utente esistano, siano distinti e non siano eliminati logicamente. Le risposte includono nickname e icona pubblici, senza email, hash delle password o altri dati riservati.

| Operazione | Autorizzazione confermata |
| --- | --- |
| Invio | `USER` o `ADMIN`, per il proprio account. |
| Elenco e consultazione | `USER` o `ADMIN`, limitatamente alle proprie relazioni. |
| Accettazione | `USER` o `ADMIN`, solo se destinatario della richiesta `pending`. Essere mittente non autorizza ad accettarla. |
| Rifiuto personale | `USER` o `ADMIN`, solo se destinatario della richiesta `pending`. |
| Annullamento personale | `USER` o `ADMIN`, solo se mittente della richiesta `pending`. |
| Rimozione personale | `USER` o `ADMIN`, se partecipante alla relazione `accepted`. |

Per «richiesta propria» si intende una richiesta che coinvolge l'attore: soltanto il destinatario la accetta o rifiuta, soltanto il mittente la annulla. Ciascun partecipante può rimuovere la propria amicizia accettata. Questi vincoli valgono anche per `ADMIN`, senza eccezioni o gerarchia tra ruoli in F13.

## Flussi e transizioni

### Invio

1. A seleziona B tramite il suo intero `USERS.id`, rappresentato come `recipientId` nel payload.
2. Il backend ricava A dall'autenticazione e verifica le precondizioni.
3. Calcola `user_a_id = min(A.id, B.id)` e `user_b_id = max(A.id, B.id)` con confronto numerico.
4. Inserisce `requested_by = A.id`, `status = 'pending'` e `accepted_at = NULL`; `created_at` usa il default del database.
5. Dopo il commit, B vede una richiesta ricevuta e A una richiesta inviata.

La PK composta impedisce di inserire una seconda riga per la stessa coppia, anche in direzione inversa. Il backend traduce il conflitto in un errore `409` con codice `FRIEND_REQUEST_ALREADY_EXISTS` per un invio già pendente dello stesso mittente, `INCOMING_REQUEST_EXISTS` per la direzione opposta e `ALREADY_FRIENDS` per una relazione già accettata. La mappatura `409` è confermata dall'utente. Nessun invio duplicato modifica la riga esistente o accetta implicitamente una richiesta ricevuta.

### Accettazione

1. B apre la richiesta ricevuta da A.
2. Il backend aggiorna soltanto la coppia ancora `pending` con `requested_by = A.id`, verificando che B sia l'altro partecipante.
3. Imposta atomicamente `status = 'accepted'` e `accepted_at = now()`, mantenendo `created_at` e `requested_by`.
4. Il successo richiede una riga aggiornata; lo stato mostrato in precedenza dal client non prova che la richiesta sia ancora accettabile.

Per decisione dell'utente, un'accettazione ripetuta produce un errore HTTP con un messaggio nel payload, senza modificare le date: `409` con codice `ALREADY_FRIENDS` se la coppia è già `accepted`, oppure `404` con codice `FRIEND_REQUEST_NOT_FOUND` se la riga è assente. La mappatura è confermata.

### Rifiuto, annullamento e rimozione

Il destinatario rifiuta e il mittente annulla eliminando la riga `pending`. Ciascun partecipante rimuove l'amicizia eliminando la riga `accepted`. Il backend distingue i ruoli tramite `requested_by` e applica la condizione sullo stato, evitando che un comando rivolto a una richiesta elimini un'amicizia già accettata.

`USER` e `ADMIN` usano le stesse rotte personali e devono partecipare alla relazione. Non è prevista una rotta amministrativa per eliminare richieste o amicizie altrui.

Per decisione dell'utente, una cancellazione ripetuta restituisce un errore HTTP con un messaggio nel payload: l'assenza della riga non viene più trattata come successo `204`. Si restituisce `404` per riga assente e `409` se una rotta personale richiede `pending` ma trova `accepted`, o viceversa. Non essendoci uno storico, la sola assenza non permette di distinguere una cancellazione ripetuta da una relazione mai esistita.

Dopo l'eliminazione si può inviare una nuova richiesta, con una nuova `created_at`. Non vengono persistiti stati `rejected`, `cancelled` o `removed`.

| Origine | Azione e attore | Risultato persistito |
| --- | --- | --- |
| Nessuna riga | Invio da A | `pending`, `requested_by = A.id`, `accepted_at = NULL`. |
| `pending` | Accettazione del destinatario | `accepted`, `accepted_at` valorizzato. |
| `pending` | Rifiuto del destinatario o annullamento del mittente | Nessuna riga. |
| `accepted` | Rimozione di un partecipante | Nessuna riga. |

`none`, `incoming`, `outgoing`, `friends` sono stati di presentazione derivati rispetto all'attore; non sono valori della colonna `status`. I timestamp sono `timestamptz`, con connessioni e serializzazione UTC come previsto dalla v1.1; il JSON usa ISO 8601, per esempio `2026-11-05T12:00:00Z`.

## Contratto tecnico condiviso

Le rotte seguenti sono **implementate nel backend**. Si mantengono gli otto percorsi del contratto, con invio tramite `recipientId`, accettazione tramite `status: "accepted"` e un'unica eliminazione delle richieste per rifiuto o annullamento. `USER` e `ADMIN` operano solo sulle proprie relazioni. Le rotte seguono il prefisso `/api`, sostantivi plurali e parametri camelCase delle [convenzioni](../../Linee-Guida-e-Regole.md#convenzioni-per-gli-endpoint). Gli ID sono numeri interi positivi `USERS.id` nel dominio Java `int`, anche nei percorsi. Il corpo delle richieste `POST` e `PATCH` è un oggetto JSON con `Content-Type: application/json`; le altre rotte non richiedono corpo.

| Metodo | Endpoint | Autenticazione | Cosa Fa | Codici HTTP | Ruoli Autorizzati | Payload Richiesta | Payload Risposta |
| --- | --- | --- | --- | --- | --- | --- | --- |
| `POST` | `/api/friend-requests` | Bearer JWT | Invia una richiesta per conto del chiamante. | `201`; errori `400`, `401`, `403`, `404`, `409`, `415`, `422` | `USER`, `ADMIN`; solo per sé | `{"recipientId":42}` | `Relazione`; errore applicativo `Errore` |
| `GET` | `/api/me/friend-requests?direction=incoming` | Bearer JWT | Elenca tutte le richieste pendenti ricevute, senza paginazione. | `200`; errori `400`, `401`, `403`, `422` | `USER`, `ADMIN`; solo proprie | Nessun corpo; query `direction` | `ElencoRelazioni`; errore applicativo `Errore` |
| `GET` | `/api/me/friend-requests?direction=outgoing` | Bearer JWT | Elenca tutte le richieste pendenti inviate, senza paginazione. | `200`; errori `400`, `401`, `403`, `422` | `USER`, `ADMIN`; solo proprie | Nessun corpo; query `direction` | `ElencoRelazioni`; errore applicativo `Errore` |
| `PATCH` | `/api/friend-requests/{otherUserId}` | Bearer JWT | Accetta la richiesta pendente ricevuta dall'altro utente. | `200`; errori `400`, `401`, `403`, `404`, `409`, `415`, `422` | `USER`, `ADMIN`; solo destinatario | `{"status":"accepted"}` | `Relazione`; errore applicativo `Errore` |
| `DELETE` | `/api/friend-requests/{otherUserId}` | Bearer JWT | Rifiuta una richiesta ricevuta oppure annulla una richiesta inviata; solo stato `pending`. | `204`; errori `400`, `401`, `403`, `404`, `409`, `422` | `USER`, `ADMIN`; destinatario rifiuta, mittente annulla | Nessun corpo | Nessun corpo al successo; errore applicativo `Errore` |
| `GET` | `/api/me/friends` | Bearer JWT | Elenca tutte le amicizie accettate del chiamante, senza paginazione. | `200`; errori `401`, `403` | `USER`, `ADMIN`; solo proprie | Nessun corpo o parametro query previsto | `ElencoRelazioni`; errore applicativo `Errore` |
| `DELETE` | `/api/me/friends/{otherUserId}` | Bearer JWT | Rimuove un'amicizia accettata del chiamante. | `204`; errori `400`, `401`, `403`, `404`, `409`, `422` | `USER`, `ADMIN`; uno dei partecipanti | Nessun corpo | Nessun corpo al successo; errore applicativo `Errore` |
| `GET` | `/api/users/{otherUserId}/friendship` | Bearer JWT | Consulta la relazione del chiamante con l'altro utente. | `200`; errori `400`, `401`, `403`, `404`, `422` | `USER`, `ADMIN`; solo propria | Nessun corpo | `Relazione`; errore applicativo `Errore` |

I nomi `Relazione`, `ElencoRelazioni` ed `Errore` sono etichette descrittive della documentazione. I campi JSON restano in inglese; risposte di successo con corpo in `application/json`, errori in `application/problem+json` e risposte `204` senza corpo.

### Richieste e risposte

- Invio: `recipientId` è obbligatorio, non nullo e intero positivo nel dominio `int`. Il mittente è sempre ricavato dal JWT. Non si accetta una nuova relazione verso sé stessi.
- Accettazione: `status` è obbligatorio, non nullo e deve essere esattamente la stringa `accepted`; solo il destinatario della richiesta può accettarla.
- Elenco richieste: `direction` è obbligatorio e deve essere esattamente `incoming` oppure `outgoing`; non esiste un valore predefinito. Nessun altro filtro o ordinamento configurabile è previsto.
- `Relazione` ha sempre i campi della tabella seguente, compresi quelli il cui valore è `null`. Invio riuscito restituisce `relationship = "outgoing"`; accettazione riuscita restituisce `"friends"`. Le liste includono lo stesso oggetto per ciascun altro utente.

| Campo | Tipo JSON | Significato |
| --- | --- | --- |
| `otherUserId` | Numero intero positivo | ID dell'altro utente. |
| `nickname` | Stringa | Nickname pubblico corrente dell'altro utente, come in F01. |
| `iconUrl` | Stringa oppure `null` | Icona pubblica corrente dell'altro utente; campo presente anche quando l'icona non è impostata. |
| `relationship` | Stringa | `none`, `incoming`, `outgoing` oppure `friends`, rispetto al chiamante. |
| `createdAt` | Stringa ISO 8601 UTC oppure `null` | Data della richiesta corrente; conservata all'accettazione. `null` se non esiste una relazione. |
| `acceptedAt` | Stringa ISO 8601 UTC oppure `null` | Data dell'accettazione; `null` per richieste pendenti o relazione assente. |

Nickname e icona si leggono dal profilo corrente: non sono nuove colonne di `FRIENDSHIPS` né fotografie storiche del profilo. Il timestamp `createdAt` resta la creazione della richiesta anche per le amicizie accettate. Non si espongono altri dati del profilo in F13.

`ElencoRelazioni` è `{"items": [...]}`, con tutti gli elementi corrispondenti al filtro e senza metadati di paginazione. Un elenco vuoto restituisce `{"items": []}` con `200`. `GET /api/users/{otherUserId}/friendship` restituisce anche nickname e icona quando la relazione è `none`, purché l'altro utente esista e sia accessibile; entrambi i timestamp sono allora `null`.

`Errore` usa i campi Problem Details `type` (stringa `about:blank`), `title` (stringa), `status` (numero HTTP), `detail` (messaggio stringa), `instance` (percorso della richiesta) e `code` (codice applicativo stringa). Il record esplicito `FriendshipError`, scelto dall'utente il **2026-11-22**, mantiene sempre i sei campi nelle risposte MVC e nei rifiuti dei filtri F13; non modifica il formato F01. Le risposte `401` includono `WWW-Authenticate: Bearer`. Il client usa `status` e `code` per la logica e `detail` per il messaggio; il testo può cambiare senza cambiare il codice.

Esempio illustrativo di creazione; gli ID non identificano account reali:

```http
POST /api/friend-requests
Authorization: Bearer <accessToken>
Content-Type: application/json

{"recipientId": 42}
```

Esempio illustrativo di risposta alla consultazione dopo l'accettazione:

```json
{
  "otherUserId": 42,
  "nickname": "RiftPlayer",
  "iconUrl": null,
  "relationship": "friends",
  "createdAt": "2026-11-05T12:00:00Z",
  "acceptedAt": "2026-11-05T12:05:00Z"
}
```

Si restituisce `201` per una nuova richiesta, `200` per consultazioni e accettazione riuscita, `204` esclusivamente per un'eliminazione effettivamente riuscita. Sono confermati gli errori con messaggio per accettazioni e cancellazioni ripetute e la seguente mappatura HTTP.

| Condizione | HTTP confermato | Codice applicativo |
| --- | --- | --- |
| Invio ripetuto della stessa richiesta pendente | `409` | `FRIEND_REQUEST_ALREADY_EXISTS` |
| Invio quando esiste una richiesta ricevuta dall'altro utente | `409` | `INCOMING_REQUEST_EXISTS` |
| Invio, oppure accettazione del destinatario originario, quando la coppia è già amica | `409` | `ALREADY_FRIENDS` |
| Accettazione o eliminazione di una richiesta assente | `404` | `FRIEND_REQUEST_NOT_FOUND` |
| Rimozione di un'amicizia assente | `404` | `FRIENDSHIP_NOT_FOUND` |
| Eliminazione personale di richiesta con coppia già `accepted` | `409` | `FRIEND_REQUEST_NOT_PENDING` |
| Rimozione personale di amicizia con coppia ancora `pending` | `409` | `FRIENDSHIP_NOT_ACCEPTED` |

Si restituisce inoltre `401` per autenticazione mancante o non valida (`UNAUTHORIZED`), `403` per operazione non autorizzata (`FORBIDDEN`), `404` per utente sconosciuto o eliminato logicamente (`USER_NOT_FOUND`) e `415` per tipo di contenuto non supportato (`UNSUPPORTED_MEDIA_TYPE`). È confermata la distinzione tra input malformato (`400`, `INVALID_REQUEST`) e semanticamente non ammissibile (`422`, `VALIDATION_ERROR`); `409` riguarda invece il conflitto con lo stato corrente della relazione:

| Tipo di errore | HTTP | Esempi |
| --- | --- | --- |
| Sintassi o rappresentazione non valida | `400` | JSON malformato o corpo non rappresentato da un oggetto JSON; ID non interpretabile nel dominio `int`; tipo JSON errato per un campo. |
| Contenuto o parametri leggibili ma non ammissibili | `422` | ID intero non positivo; auto-richiesta; campo obbligatorio assente in un oggetto JSON valido; `status` diverso da `accepted`; `direction` non ammessa. |
| Stato corrente incompatibile con il comando | `409` | Richiesta duplicata, accettazione ripetuta, eliminazione personale sullo stato sbagliato. |

Riferimento: [semantica HTTP, RFC 9110](https://www.rfc-editor.org/rfc/rfc9110.html#name-client-error-4xx). Un campo obbligatorio nullo equivale a un campo mancante e produce `422`. I campi aggiuntivi nel corpo non attribuiscono autorità: attore, mittente e date sono sempre determinati dal server.

Esempio illustrativo di errore per un'accettazione ripetuta, nel formato definito:

```json
{
  "type": "about:blank",
  "title": "Conflict",
  "status": 409,
  "detail": "Friendship is already accepted.",
  "instance": "/api/friend-requests/42",
  "code": "ALREADY_FRIENDS"
}
```

### Visibilità e ordine dei controlli

1. Verificare JWT, account e sessione prima di elaborare la relazione: credenziali mancanti o non valide producono `401`.
2. Validare sintassi, tipi e valori di input (`400`, `415` o `422`); un riferimento a sé stessi non è ammesso nelle operazioni di relazione.
3. Verificare che l'altro utente esista e non sia eliminato logicamente; in entrambi i casi di indisponibilità restituire `404` con `USER_NOT_FOUND`, senza distinguere la causa nel messaggio.
4. Leggere soltanto la coppia composta dal chiamante e dall'altro utente. Se la coppia è assente: il `GET` restituisce `200` con `relationship = "none"`, l'invio crea la richiesta, accettazione ed eliminazione restituiscono il `404` pertinente.
5. Se la riga esiste, verificare la partecipazione e, per l'accettazione, che il chiamante sia il destinatario: un mittente che prova ad accettare riceve `403`, anche se la coppia è già accettata. Solo dopo applicare gli errori di stato `409` della tabella. Rifiuto/annullamento e rimozione restano condizionati rispettivamente a `pending` e `accepted`.

Nessuna rotta permette di specificare una coppia di altri utenti. I messaggi non rivelano dettagli riservati dell'account. Eventuali errori interni producono `500` con `INTERNAL_ERROR` e un messaggio generico, senza errori SQL o dettagli di infrastruttura; prima del commit si annullano le scritture.

### Elenchi e ordinamento

Gli elenchi non sono paginati: ogni richiesta restituisce tutte le relazioni visibili al chiamante che corrispondono al filtro, senza troncamenti impliciti. Le richieste mantengono il filtro `direction` per distinguere ricevute e inviate; l'elenco amici include solo le relazioni `accepted`.

- Richieste ricevute o inviate: `createdAt` decrescente, quindi le richieste più recenti per prime.
- Amicizie: `acceptedAt` decrescente, quindi le amicizie accettate più di recente per prime, indipendentemente da quando fu inviata la richiesta.
- A parità di timestamp, `otherUserId` crescente in entrambi gli elenchi, per un ordine deterministico. La lettura considera entrambi i lati della coppia.

## Transazioni, concorrenza e richieste ripetute

La PK composta impedisce duplicati anche per invii simultanei. Il backend gestisce il conflitto di inserimento rileggendo la coppia e traducendolo nell'errore applicativo concordato, senza esporre errori SQL: non sovrascrive `requested_by`, non modifica la data originaria e non converte implicitamente una richiesta ricevuta in amicizia.

Accettazione e cancellazione usano scritture condizionali sulla coppia, lo stato e, quando pertinente, il mittente; il numero di righe modificate determina l'esito. Il vincolo di partecipazione si applica anche ad `ADMIN`. In caso di accettazione concorrente con rifiuto o annullamento, il comando che non trova più lo stato richiesto rilegge la relazione e restituisce un errore con messaggio, secondo la mappatura concordata. Una ripetizione non può essere segnalata come successo soltanto perché l'effetto desiderato è già presente. Timeout o deadlock richiedono rollback; retry limitati soltanto per errori transitori e operazioni sicure. Dopo un esito di commit incerto il client rilegge lo stato: un comando ripetuto può restituire l'errore confermato senza avere prodotto una seconda modifica.

Per scelta dell'utente del **2026-11-22**, le scritture F13 bloccano entrambe le righe stabili `USERS` in ordine numerico crescente e ricontrollano gli account dopo i lock. Il cleanup delle amicizie usa il lock del destinatario già acquisito da F01 e la stessa transazione della cancellazione logica: un errore annulla anche la pulizia delle relazioni. Non sono previsti retry automatici; gli errori imprevisti restituiscono `500` generico dopo rollback. L'avvio degli scambi privati resta da integrare con F06 secondo il [protocollo comune](Database/database-schema-v1.1.md#protocollo-transazionale-comune).

## Cancellazione degli account e notifiche

La cancellazione fisica di un utente elimina le amicizie tramite le tre cascate dichiarate, se l'operazione è consentita dalle altre FK. Impostare `USERS.is_deleted` non elimina alcuna riga automaticamente.

Decisioni confermate dall'utente il **2026-11-18**:

- Alla cancellazione logica dell'account devono essere eliminate tutte le sue righe `FRIENDSHIPS`, sia `pending` sia `accepted`, indipendentemente dal lato della coppia e da `requested_by`.
- Alla cancellazione logica si puliscono soltanto gli scambi ancora aperti dell'account, pubblici e privati: annunci `PUBLISHED` e trattative `PENDING`, con le rispettive prenotazioni. Gli scambi completati e il relativo storico si conservano; non vengono annullati i trasferimenti già conclusi. Il cleanup non è una cancellazione indiscriminata di tutte le righe F06 riferite all'account.
- Se l'account eliminato è il rispondente a un annuncio pubblico di un altro utente, la chiusura segue il rifiuto F06: si elimina la trattativa, si rilasciano tutti gli hold di entrambi i lati e si ripubblica l'annuncio originale senza hold, con le offerte originali e lo stesso `expires_at`. Nessuna controproposta sostituisce l'offerta originale ripubblicata. Se il TTL è già esaurito al controllo autorevole, l'annuncio passa a `EXPIRED` e non viene ripubblicato.
- Alla sola rimozione di una propria amicizia da parte di un partecipante `USER` o `ADMIN`, gli scambi pubblici e privati esistenti persistono con il TTL originario F06. La rimozione non li annulla, non li rinnova e non ne modifica consensi o prenotazioni; restano possibili le normali transizioni F06.

La pulizia delle amicizie al soft delete avviene nella transazione F01, con rollback comune. Il cleanup degli scambi, la conservazione dello storico F06 e la ripubblicazione degli annunci della controparte restano requisiti futuri F06; atomicità, recupero e rappresentazione dello storico F06 sono DA DEFINIRE.

Il cleanup delle sole amicizie è implementato con F01 nella stessa transazione dal **2026-11-22**. Gli effetti sugli scambi e sul relativo storico restano specifiche F06 da implementare; questa attività non introduce tabelle o servizi di scambio.

Per mantenere semplice la prima versione, F13 non introduce notifiche dedicate, blocchi tra utenti, scadenza delle richieste, attese o storico delle relazioni eliminate. Gli elenchi e la consultazione della relazione sono il riferimento per lo stato corrente; eventuali estensioni richiederanno un'attività separata.

## Criteri di accettazione e verifiche previste

La tabella mantiene i criteri di accettazione del contratto, inclusi gli scenari F06 futuri. Gli esiti effettivi dell'implementazione backend sono separati nel [report dell'attività](../report/2026-11-22-implementazione-amicizie.md).

| Scenario | Risultato atteso |
| --- | --- |
| ID numerici, incluso mittente con ID maggiore del destinatario | Coppia ordinata numericamente; direzione ricavata da `requested_by`. |
| JWT assente, scaduto o non valido | `401` nel formato di errore F13 con `WWW-Authenticate: Bearer`, senza esposizione di relazioni. |
| Input malformato o leggibile ma non ammissibile | Distinzione `400`/`422`, senza modifiche, secondo le regole del contratto. |
| Richiesta a sé stessi, utente inesistente o eliminato | Auto-richiesta `422`; utente inesistente o eliminato `404`; nessuna nuova relazione. |
| Richieste duplicate o simultanee, anche inverse | Al massimo una riga; nessuna accettazione implicita; `409` sul duplicato. |
| Accettazione dal destinatario | Una riga `accepted` con `accepted_at` valorizzato. |
| Accettazione dal mittente, anche `ADMIN` | `403` prima del controllo dello stato della coppia; nessuna modifica. Non è prevista una rotta per accettare richieste tra terzi. |
| Rifiuto, annullamento, rimozione | Partecipante e stato corretti; nessuno stato storico inventato. |
| Operazioni su richieste e amicizie proprie con ruolo `ADMIN` | Stesse autorizzazioni di `USER`: destinatario accetta o rifiuta, mittente annulla, ciascun amico rimuove la propria relazione. |
| Tentativo di eliminare una coppia altrui, anche con ruolo `ADMIN` | Operazione non autorizzata; nessuna modifica o esposizione di relazioni altrui. Nessuna rotta amministrativa F13 prevista. |
| Accettazione o cancellazione ripetuta | `409` se già accettata o nello stato sbagliato, `404` se assente; messaggio nel payload e nessuna nuova modifica. |
| Accettazione concorrente con eliminazione | Esito coerente con la scrittura condizionale effettiva. |
| Elenchi completi senza paginazione | Tutte le relazioni corrispondenti, da entrambi i lati della coppia; soli stati richiesti, utenti eliminati esclusi, nessun troncamento implicito e ordine stabile concordato. |
| Profilo dell'altro utente nelle risposte | `nickname`, `iconUrl`, `createdAt` e `acceptedAt` sempre presenti; icona non impostata e timestamp non applicabili rappresentati da `null`. Nessuna email o altro dato privato. |
| Ordinamento delle richieste e degli amici | Richieste per `createdAt` decrescente, amici per `acceptedAt` decrescente; a parità di data `otherUserId` crescente. |
| Consultazione di relazione assente verso utente accessibile | `200` con profilo pubblico minimo, `relationship = "none"` e timestamp nulli. |
| Cancellazione fisica e logica | Cascata soltanto nel primo caso; al soft delete pulizia applicativa di tutte le richieste e amicizie, su entrambi i lati. |
| Cancellazione logica con scambi pubblici e privati in corso | Pulizia delle operazioni aperte e rilascio degli hold di entrambi i lati; annuncio pubblico valido della controparte ripubblicato come da rifiuto F06. |
| Cancellazione logica con scambi completati | Scambi e storico completati conservati, senza inversione dei trasferimenti. |
| Account eliminato rispondente a un annuncio pubblico ancora valido | Trattativa e hold eliminati; annuncio originale `PUBLISHED`, offerte originali e `expires_at` invariati. |
| Account eliminato rispondente a un annuncio pubblico già scaduto | Nessuna ripubblicazione, stato `EXPIRED` e hold rilasciati secondo F06. |
| Rimozione della propria amicizia, da `USER` o `ADMIN`, con scambi esistenti | Scambi pubblici e privati conservati, scadenza originaria invariata e normali transizioni F06 applicabili. |
| Scambio privato verso non amico o relazione `pending` | Rifiuto senza creazione di trattativa o hold, secondo F06. |

I test F13 sono `FriendshipControllerDatabaseTests`, `FriendshipConcurrencyDatabaseTests` e `FriendshipMigrationDatabaseTests`; richiedono PostgreSQL e le variabili documentate nel [README applicativo](../../springboot_app/app/README.md). Il report distingue verifiche eseguite, problemi e limiti; non sono attestate verifiche del frontend, di F06 o di prestazioni.

## Decisioni aperte e collegamenti

- Concordare responsabili e attività delle aree, collegandole alla funzionalità F13.
- Il contratto API è definito: percorsi, Bearer JWT, payload, codici HTTP, formato degli errori, ordine dei controlli ed elenchi completi ordinati dal più recente. Non restano approvazioni API pendenti; la definizione non attesta implementazione.
- Dettagliare schermate, conferme e stati dell'interfaccia, accessibilità e integrazione con F01, mantenendo i campi e l'ordinamento fissati dal contratto.
- Integrare in futuro F06 con il cleanup F01/F13 disponibile: scambi aperti, hold, storico completato e ripubblicazione richiedono ancora implementazione e verifiche. Nessuna integrazione F06 viene attestata dai test F13.
- Valutare eventuali indici solo con evidenze pertinenti; le estensioni escluse dalla prima versione non bloccano l'API semplice descritta qui.

[Indice BackEnd](README.md) · [Indice generale](../README.md) · [Accesso utenti F01](accesso-utenti.md) · [Scambi F06](scambi-mercato.md) · [Interfaccia amicizie](../FrontEnd/amicizie.md) · [Interfaccia scambi](../FrontEnd/scambi-mercato.md)
