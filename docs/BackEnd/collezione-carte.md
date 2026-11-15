# Collezione delle carte

ID: F04 · Perimetro: confermato; specifica in approfondimento.
Implementazione: non iniziata. Responsabile: DA DEFINIRE (Software Developer).
Fonte: traccia, risposte e chiarimenti del gruppo; le decisioni note sono riportate sotto.
Ultimo aggiornamento significativo: 2026-11-13. Verifica applicativa: non eseguita.

## Obiettivo e decisioni note

- Consentire all'utente di consultare le carte possedute.
- L'apertura dei pacchetti e gli scambi aggiornano le proprietà delle carte.

## Dati, copie e varianti grafiche

- Gli ID generati di utenti, carte e arti sono `int` auto-incrementali. `COLLECTIONS` eredita gli interi nella PK `(id_user, id_card, id_card_art)` e non genera nuovi identificativi.
- `CARDS_ARTS` ha PK `(id_card, art_code)`, `id_art` auto-incrementale univoco e unicità `(id_card, id_art)`. La FK composta della collezione garantisce che l'arte appartenga alla carta; l'URL dell'arte rimane nullable.
- `owned_replica` è obbligatorio e maggiore di zero; `tradable_replica` è obbligatorio e compreso fra zero e il possesso. Una tripla assente significa zero solo dopo una lettura riuscita. Il trasferimento dell'ultima copia elimina la riga.
- Le copie scambiabili disponibili sono `tradable_replica` meno la somma degli hold non scaduti della stessa tripla. Prenotare non modifica i due contatori. Prima di eliminare una riga occorre consumare o rilasciare gli hold attivi; non esiste una FK degli hold alla riga volatile di possesso.
- I mazzi scelgono esplicitamente `id_card_art`. Possesso per variante, effetti dei trasferimenti sui mazzi e disponibilità per il gioco richiedono validazione backend; le prenotazioni economiche non decidono da sole la giocabilità.
- Ogni modifica applica il [protocollo transazionale comune](Database/database-schema-v1.1.md#protocollo-transazionale-comune). La cancellazione logica di carte o espansioni invalida prima le trattative e rilascia entrambi i lati, poi converte le copie e ripulisce collezione e mazzi; tassi e frammenti restano da definire.
## Bozza di classi di servizio e repository

Queste firme Java sono una proposta di progettazione, non codice compilabile né un contratto concordato. Non presuppongono Spring Data, JPA o un database specifico. L'implementazione resta sospesa finché stack e modello delle copie non saranno confermati.

### Modello concettuale

- `Card` rappresenta la definizione nel catalogo.
- `OwnedCopy` rappresenta una singola copia posseduta e ha un identificativo distinto da quello della carta catalogo. Due copie della stessa carta hanno quindi due `copyId`.
- `OwnedCardView` è una vista di lettura che combina i dati della copia con i dati descrittivi del catalogo, così la repository può restituire la collezione con una lettura aggregata invece di caricare carta per carta.

### Esempio di carte possedute

Esempio fittizio di risposta di lettura della collezione. `copyId` identifica la singola copia, mentre `cardId` identifica la definizione nel catalogo: due copie della stessa carta avrebbero `copyId` diversi. Nomi dei campi e forma dell'oggetto non sono ancora un contratto concordato.

```json
{
  "userId": "user-demo-01",
  "items": [
    {
      "copyId": "copy-demo-01",
      "cardId": "card-demo-01",
      "name": "Carta dimostrativa A",
      "rarity": "Comune"
    },
    {
      "copyId": "copy-demo-02",
      "cardId": "card-demo-02",
      "name": "Carta dimostrativa B",
      "rarity": "Rara"
    }
  ]
}
```

```java
public interface CardRepository {
    Optional<Card> findById(CardId cardId);

    List<CardSummary> search(CardQuery query);
}

public abstract class CardCatalogService {
    private final CardRepository cardRepository;

    public CardCatalogService(CardRepository cardRepository) {
        this.cardRepository = cardRepository;
    }

    public abstract Optional<CardDetails> findById(CardId cardId);

    public abstract List<CardSummary> search(CardQuery query);
}
```

```java
public interface OwnedCopyRepository {
    List<OwnedCardView> findCollectionByOwnerId(UserId ownerId);

    Optional<OwnedCardView> findCopyByOwnerIdAndCopyId(
            UserId ownerId,
            CopyId copyId);
}

public abstract class CollectionService {
    private final OwnedCopyRepository ownedCopyRepository;

    public CollectionService(OwnedCopyRepository ownedCopyRepository) {
        this.ownedCopyRepository = ownedCopyRepository;
    }

    public abstract List<OwnedCardView> listOwnCopies(UserId authenticatedUserId);

    public abstract Optional<OwnedCardView> findOwnCopy(
            UserId authenticatedUserId,
            CopyId copyId);
}
```

Le firme mostrano i metodi previsti, non implementazioni. `CardId`, `CopyId`, `UserId`, `CardQuery`, `Card`, `CardSummary`, `CardDetails` e `OwnedCardView` sono tipi concettuali da specificare; i tipi di ritorno e la politica per le risorse mancanti sono anch'essi da concordare. `CardQuery` potrà includere filtri e paginazione solo dopo averne definiti necessità e comportamento.

Il chiamante deve ricavare `authenticatedUserId` dal contesto autenticato lato server, non fidarsi di un identificativo proprietario inviato liberamente dal client. I permessi e le regole di visibilità restano da definire in coordinamento con F01. L'accesso a una copia altrui non deve essere reso possibile dal semplice metodo di lettura della collezione.

`CollectionService` è in sola lettura nella bozza. Le copie vengono create dall'apertura dei pacchetti F03 e trasferite dagli scambi F06; le relative operazioni di scrittura e i loro confini transazionali appartengono ai rispettivi servizi, non a un metodo generico per modificare la collezione. Il backoffice F10 potrà avere operazioni di gestione del catalogo, ma non sono incluse qui finché permessi e validazioni non saranno concordati.

## Da definire

- Dati esposti, filtri, paginazione, riutilizzo delle copie nei mazzi e giocabilità delle copie prenotate.
- Contratti e regole di visibilità della collezione.
- Tipi Java definitivi, gestione di liste grandi e forma della query di catalogo.
- Strategie di persistenza e test dopo la conferma dello stack.

## Collegamenti

- [Interfaccia collezione](../FrontEnd/collezione-carte.md)
- [Schema dati preliminare](schema-dati-preliminare.md)
- [Catalogo e backoffice](gestione-amministrativa.md)
- [Pacchetti](acquisto-pacchetti.md)
- [Scambi](scambi-mercato.md)
- [Accesso utenti](accesso-utenti.md)
- [Indice BackEnd](README.md)
