# Amicizie

ID funzionalità: **F13**; dipendenze: **F01** e **F06**.
Perimetro: amicizie reciproche e richieste; specifica **in approfondimento**.
Implementazione: **non iniziata**. Responsabile: **DA DEFINIRE**.
Versione iniziale della specifica F13. Ultimo aggiornamento significativo: **2026-11-06**.
Fonti dati e invarianti: [specifica canonica v1.1](Database/database-schema-v1.1.md) e [rappresentazione DBML](Database/db_schema-v1.1.dbml). La specifica del database prevale per dati e invarianti; i flussi e il contratto API sotto restano proposti dove non fissati dallo schema.
Verifiche applicative: **non eseguite**.

## Obiettivo, perimetro e dipendenze

L'amicizia esiste soltanto dopo l'accettazione della richiesta da parte del destinatario. Si prevedono invio, consultazione, accettazione, rifiuto e annullamento delle richieste, oltre alla rimozione delle amicizie accettate. `FRIENDSHIPS` rappresenta solo la relazione corrente; non conserva uno storico di rifiuti o rimozioni.

[F01](accesso-utenti.md) fornisce identità e autenticazione. [F06](scambi-mercato.md#richiesta-privata) richiede un'amicizia `accepted` per avviare uno scambio privato. La rimozione dell'amicizia non ha una cascata verso le trattative: il comportamento degli scambi privati già avviati resta **DA DEFINIRE** nel contratto F06.

Questa pagina è il riferimento BackEnd della proposta di contratto delle amicizie. Interfaccia, notifiche e matrice dei ruoli richiedono accordo tra aree; non è attestata alcuna integrazione.

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

Tutte le operazioni proposte richiedono autenticazione e consentono di leggere o modificare soltanto relazioni che coinvolgono l'attore. Il backend verifica che attore e destinatario esistano, siano distinti e non siano eliminati logicamente. Non espone email, hash delle password o altri dati riservati nelle risposte delle amicizie.

| Operazione proposta | Partecipante autorizzato |
| --- | --- |
| Invio | Utente autenticato per il proprio account. |
| Elenco e consultazione | Utente autenticato, limitatamente alle proprie relazioni. |
| Accettazione | Solo destinatario della richiesta `pending`. |
| Rifiuto | Solo destinatario della richiesta `pending`. |
| Annullamento | Solo mittente della richiesta `pending`. |
| Rimozione | Ciascuno dei partecipanti alla relazione `accepted`. |

La matrice per `ADMIN`, `MANAGER`, `USER` resta **DA DEFINIRE**, coerentemente con F01. Non si presume una gerarchia né un diritto amministrativo di agire sulle amicizie altrui. I permessi di ciascuna rotta devono essere concordati prima dell'implementazione.

## Flussi e transizioni proposte

### Invio

1. A seleziona B tramite il suo intero `USERS.id`, rappresentato come `recipientId` nel payload proposto.
2. Il backend ricava A dall'autenticazione e verifica le precondizioni.
3. Calcola `user_a_id = min(A.id, B.id)` e `user_b_id = max(A.id, B.id)` con confronto numerico.
4. Inserisce `requested_by = A.id`, `status = 'pending'` e `accepted_at = NULL`; `created_at` usa il default del database.
5. Dopo il commit, B vede una richiesta ricevuta e A una richiesta inviata.

Una richiesta già inviata restituisce lo stato corrente senza creare righe aggiuntive. Una richiesta in direzione opposta richiede accettazione esplicita: non viene accettata da un nuovo invio. Una relazione già accettata non genera una nuova richiesta.

### Accettazione

1. B apre la richiesta ricevuta da A.
2. Il backend aggiorna soltanto la coppia ancora `pending` con `requested_by = A.id`, verificando che B sia l'altro partecipante.
3. Imposta atomicamente `status = 'accepted'` e `accepted_at = now()`, mantenendo `created_at` e `requested_by`.
4. Il successo richiede una riga aggiornata; lo stato mostrato in precedenza dal client non prova che la richiesta sia ancora accettabile.

### Rifiuto, annullamento e rimozione

Il destinatario rifiuta e il mittente annulla eliminando la riga `pending`. Ciascun partecipante rimuove l'amicizia eliminando la riga `accepted`. Il backend distingue i ruoli tramite `requested_by` e applica la condizione sullo stato, evitando che un comando rivolto a una richiesta elimini un'amicizia già accettata.

Dopo l'eliminazione si può inviare una nuova richiesta, con una nuova `created_at`. Non vengono persistiti stati `rejected`, `cancelled` o `removed`.

| Origine | Azione e attore | Risultato persistito |
| --- | --- | --- |
| Nessuna riga | Invio da A | `pending`, `requested_by = A.id`, `accepted_at = NULL`. |
| `pending` | Accettazione del destinatario | `accepted`, `accepted_at` valorizzato. |
| `pending` | Rifiuto del destinatario o annullamento del mittente | Nessuna riga. |
| `accepted` | Rimozione di un partecipante | Nessuna riga. |

`none`, `incoming`, `outgoing`, `friends` sono stati di presentazione proposti, derivati rispetto all'attore; non sono valori della colonna `status`. I timestamp sono `timestamptz`, con connessioni e serializzazione UTC come previsto dalla v1.1; il JSON proposto usa ISO 8601, per esempio `2026-11-05T12:00:00Z`.

## Contratto tecnico condiviso proposto

Le rotte seguenti sono **proposte, non implementate né concordate**. Seguono il prefisso `/api`, sostantivi plurali e parametri camelCase delle [convenzioni](../../Linee-Guida-e-Regole.md#convenzioni-per-gli-endpoint). Gli ID sono numeri interi `USERS.id`, anche nei percorsi. Accesso per ruolo, payload definitivi e codici HTTP richiedono accordo.

| Metodo e percorso | Input e responsabilità |
| --- | --- |
| `POST /api/friend-requests` | Corpo con `recipientId`: invia una richiesta per conto dell'attore. |
| `GET /api/me/friend-requests?direction=incoming` | Elenca solo le richieste `pending` ricevute. |
| `GET /api/me/friend-requests?direction=outgoing` | Elenca solo le richieste `pending` inviate. |
| `PATCH /api/friend-requests/{otherUserId}` | Corpo con `status: "accepted"`: il destinatario accetta la richiesta pendente dell'altro utente. |
| `DELETE /api/friend-requests/{otherUserId}` | Il destinatario rifiuta o il mittente annulla; elimina solo `pending`. |
| `GET /api/me/friends` | Elenca solo le relazioni `accepted`, leggendo entrambi i lati della coppia. |
| `DELETE /api/me/friends/{otherUserId}` | Rimuove solo una relazione `accepted` dell'attore. |
| `GET /api/users/{otherUserId}/friendship` | Restituisce la relazione dell'attore con l'altro utente. |

Esempio illustrativo di creazione; gli ID non identificano account reali:

```http
POST /api/friend-requests
Content-Type: application/json

{"recipientId": 42}
```

Esempio illustrativo di risposta alla consultazione dopo l'accettazione:

```json
{
  "otherUserId": 42,
  "relationship": "friends",
  "createdAt": "2026-11-05T12:00:00Z",
  "acceptedAt": "2026-11-05T12:05:00Z"
}
```

Si propone `201` per una nuova richiesta, `200` per consultazioni, accettazione e ripetizione di un invio già pendente dello stesso mittente; `204` per eliminazioni riuscite o relazione già assente. L'eliminazione della risorsa nello stato sbagliato produce un conflitto senza modifiche. Un'accettazione senza aggiornamenti non viene segnalata come riuscita.

Si propongono `401` per autenticazione mancante, `403` per operazione non autorizzata, `404` per utente sconosciuto o non accessibile, `422` per ID invalido o auto-richiesta, `409` per conflitti di relazione. L'errore include un codice stabile, per esempio `INCOMING_REQUEST_EXISTS`, `ALREADY_FRIENDS` o `FRIEND_REQUEST_NOT_PENDING`; formato completo e politica di visibilità restano **DA DEFINIRE**. I messaggi non rivelano dettagli riservati dell'account.

Gli elenchi prevedono `limit` e `cursor`; dimensioni predefinita/massima, formato della pagina e del cursore restano **DA DEFINIRE**. Si propone un ordinamento stabile su `(created_at, other_user_id)` per le richieste e `(accepted_at, friend_id)` per gli amici, in cui gli ID sono derivati dall'altro lato della coppia. Direzione, semantica in presenza di modifiche concorrenti e payload delle voci devono essere concordati prima dell'integrazione.

## Transazioni, concorrenza e richieste ripetute

La PK composta impedisce duplicati anche per invii simultanei. Il backend gestisce il conflitto di inserimento rileggendo la coppia: non sovrascrive `requested_by`, non modifica la data originaria e non converte implicitamente una richiesta ricevuta in amicizia.

Accettazione e cancellazione usano scritture condizionali sulla coppia, lo stato e, quando pertinente, il mittente; il numero di righe modificate determina l'esito. In caso di accettazione concorrente con rifiuto o annullamento, il comando che non trova più lo stato richiesto rilegge la relazione e restituisce l'esito previsto dal contratto. Timeout o deadlock richiedono rollback; retry limitati soltanto per errori transitori e operazioni sicure.

Per coordinare controllo di `is_deleted`, modifiche delle amicizie e avvio di scambi privati, si propone di bloccare le righe stabili `USERS` in ordine numerico crescente e rileggere lo stato dopo i lock. L'integrazione deve rispettare l'ordine del [protocollo transazionale comune](Database/database-schema-v1.1.md#protocollo-transazionale-comune) e concordare lo stesso coordinamento nel flusso di cancellazione dell'account. Lo schema non implementa automaticamente questi controlli.

## Cancellazione degli account e notifiche

La cancellazione fisica di un utente elimina le amicizie tramite le tre cascate dichiarate, se l'operazione è consentita dalle altre FK. Impostare `USERS.is_deleted` non elimina alcuna riga automaticamente. Si propone di ripulire le amicizie nel flusso di cancellazione logica e di escludere sempre gli utenti eliminati da ricerca, elenchi e nuove operazioni; transazione, conservazione e coordinamento con F01/F06 restano **DA DEFINIRE**.

Le notifiche sono facoltative nella proposta. Se usate, `NOTIFICATIONS.id_sender` e `id_receiver` sono interi obbligatori riferiti a `USERS.id`; `message` è obbligatorio, `created_at` ha default `now()` e `status` ha default `new`, con soli valori `new`, `read`, `archived`. Non esiste un mittente testuale `system`. La consegna esterna segue il commit; trigger di creazione, atomicità della persistenza, canale, retry e recupero affidabile restano **DA DEFINIRE**. Una notifica non sostituisce la relazione autorevole in `FRIENDSHIPS`.

## Criteri di accettazione e verifiche previste

I seguenti scenari sono da verificare quando esisterà un'implementazione; non sono risultati di test.

| Scenario | Risultato atteso |
| --- | --- |
| ID numerici, incluso mittente con ID maggiore del destinatario | Coppia ordinata numericamente; direzione ricavata da `requested_by`. |
| Richiesta a sé stessi, utente inesistente o eliminato | Operazione rifiutata senza nuove relazioni. |
| Richieste duplicate o simultanee, anche inverse | Al massimo una riga; nessuna accettazione implicita. |
| Accettazione dal destinatario | Una riga `accepted` con `accepted_at` valorizzato. |
| Accettazione dal mittente o da terzi | Nessuna modifica e nessuna esposizione di relazioni altrui. |
| Rifiuto, annullamento, rimozione | Partecipante e stato corretti; nessuno stato storico inventato. |
| Accettazione concorrente con eliminazione | Esito coerente con la scrittura condizionale effettiva. |
| Elenchi e paginazione | Entrambi i lati, soli stati richiesti, utenti eliminati esclusi, ordine stabile concordato. |
| Cancellazione fisica e logica | Cascata soltanto nel primo caso; pulizia applicativa verificata nel secondo. |
| Scambio privato verso non amico o relazione `pending` | Rifiuto senza creazione di trattativa o hold, secondo F06. |

Questa specifica non attesta esecuzioni SQL, migrazioni o verifiche di prestazioni. Le applicazioni non sono inizializzate e non sono disponibili comandi di test della funzionalità.

## Decisioni aperte e collegamenti

- Concordare responsabili e attività delle aree, collegandole alla funzionalità F13.
- Concordare matrice dei ruoli, API, errori, visibilità, paginazione e comportamento dell'interfaccia.
- Definire eventuali blocchi, limiti antiabuso, attese o storico; nessuno è già modellato.
- Coordinare cancellazione logica e operazioni concorrenti con F01; definire con F06 gli effetti sulle trattative private già avviate.
- Definire notifiche e affidabilità della consegna; valutare gli indici solo con evidenze pertinenti.

[Indice BackEnd](README.md) · [Indice generale](../README.md) · [Accesso utenti F01](accesso-utenti.md) · [Scambi F06](scambi-mercato.md) · [Interfaccia scambi](../FrontEnd/scambi-mercato.md)
