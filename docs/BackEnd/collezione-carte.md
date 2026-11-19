# Catalogo e collezione delle carte

ID: F04 · Perimetro: confermato; contratto API in approfondimento.
Implementazione: controller, servizi e repository JDBC aggiunti; verifica applicativa non completata.
Ultimo aggiornamento significativo: 2026-11-15. Fonte: richiesta F04 e scelte dell'utente su filtri, ordinamenti e varianti grafiche. Schema persistente: [database v1.1](Database/database-schema-v1.1.md).

## Obiettivo e decisioni note

- Fornire il catalogo attivo delle carte e la collezione dell'utente autenticato.
- Esporre identificativo, nome, fazione, rarità, tipo, espansione, energia, attacco, difesa, punti potenza e immagini quando i dati sono presenti.
- Restituire tutte le varianti grafiche del catalogo. Nella collezione ogni riga corrisponde a una carta e alla sua variante, con i contatori persistiti delle copie.
- La collezione è privata: il backend usa l'ID del principal autenticato; il client non può specificare l'ID di un altro proprietario.
- Filtri concordati: nome, fazione, rarità, tipo, espansione e intervalli inclusivi di energia, attacco, difesa e punti potenza.
- Ordinamenti concordati: nome, fazione, rarità, energia, attacco, difesa e punti potenza, con direzione crescente o decrescente.
- Le rotte di consultazione richiedono un bearer token valido secondo F01. La scrittura del catalogo è amministrativa e descritta nel contratto [F10](gestione-amministrativa.md).
- La collezione non espone scritture dirette: le copie sono gestite dai flussi di dominio, non dal client.

## Contratto HTTP condiviso

| Metodo e percorso | Accesso | Esito |
| --- | --- | --- |
| `GET /api/cards` | Bearer richiesto | Carte non cancellate logicamente, filtrate e paginate; ogni carta include l'elenco delle varianti grafiche. |
| `GET /api/cards/{cardId}` | Bearer richiesto | Dettaglio di una carta attiva con tutte le varianti grafiche; `404` se non disponibile. |
| `GET /api/users/me/collections` | Bearer richiesto | Collezione dell'utente autenticato, filtrata e paginata per carta e variante grafica. |

Entrambe le risposte hanno forma `CardPage<T>`: `items`, `page`, `size`, `totalElements`, `totalPages`. Gli ID sono interi. Nessun parametro di percorso o query seleziona il proprietario della collezione.

### Parametri di ricerca

I parametri sono facoltativi e combinabili su entrambe le rotte.

| Parametro | Significato |
| --- | --- |
| `name` | Ricerca parziale senza distinzione tra maiuscole e minuscole; massimo 100 caratteri. |
| `faction`, `rarity`, `type`, `expansion` | Codice esatto del relativo catalogo; confronto senza distinzione tra maiuscole e minuscole; massimo 100 caratteri. |
| `energyMin`, `energyMax` | Limiti inclusivi del costo energia. |
| `attackMin`, `attackMax` | Limiti inclusivi dell'attacco. |
| `defenseMin`, `defenseMax` | Limiti inclusivi della difesa. |
| `powerMin`, `powerMax` | Limiti inclusivi dei punti potenza. |
| `sort` | `name`, `faction`, `rarity`, `energy`, `attack`, `defense` o `power`; predefinito `name`. |
| `direction` | `asc` o `desc`; predefinito `asc`. |
| `page` | Indice pagina a base zero; predefinito `0`. |
| `size` | Elementi per pagina da 1 a 100; predefinito `20`. |

Gli intervalli devono avere limiti non negativi e il minimo non può superare il massimo. L'ordinamento tratta i valori nulli come ultimi e usa l'ID carta come spareggio stabile; nella collezione usa anche l'ID della variante.

### Risposta catalogo

Ogni elemento contiene `id`, `name`, `faction`, `rarity`, `energyCost`, `attack`, `defense`, `power`, `type`, `expansion` e `artworks`. Un riferimento ha `id`, `code` e `name`; statistiche e riferimenti opzionali possono essere `null`. Ogni elemento di `artworks` contiene `cardArtId`, `artCode` e `imageUrl`, che può essere `null`.

Esempio illustrativo:

```json
{
  "items": [
    {
      "id": 101,
      "name": "Creatura dimostrativa",
      "faction": {"id": 1, "code": "SAPIENS", "name": "Sapiens"},
      "rarity": {"id": 1, "code": "COMMON", "name": "Common"},
      "energyCost": 1,
      "attack": null,
      "defense": null,
      "power": 1,
      "type": {"id": 1, "code": "CREATURE", "name": null},
      "expansion": null,
      "artworks": [
        {"cardArtId": 201, "artCode": "STANDARD", "imageUrl": null}
      ]
    }
  ],
  "page": 0,
  "size": 20,
  "totalElements": 1,
  "totalPages": 1
}
```

### Risposta collezione

Ogni elemento contiene `cardId`, `cardArtId`, `artCode`, `imageUrl`, i campi descrittivi del catalogo e i contatori `ownedReplica` e `tradableReplica`. La chiave persistente identifica la terna `(id_user, id_card, id_card_art)`; copie della stessa variante sono aggregate nella riga e non hanno un `copyId` individuale.

Esempio illustrativo:

```json
{
  "items": [
    {
      "cardId": 101,
      "cardArtId": 201,
      "artCode": "STANDARD",
      "imageUrl": null,
      "name": "Creatura dimostrativa",
      "faction": {"id": 1, "code": "SAPIENS", "name": "Sapiens"},
      "rarity": {"id": 1, "code": "COMMON", "name": "Common"},
      "energyCost": 1,
      "attack": null,
      "defense": null,
      "power": 1,
      "type": {"id": 1, "code": "CREATURE", "name": null},
      "expansion": null,
      "ownedReplica": 2,
      "tradableReplica": 1
    }
  ],
  "page": 0,
  "size": 20,
  "totalElements": 1,
  "totalPages": 1
}
```

`tradableReplica` è il contatore salvato in `COLLECTIONS`; non sottrae gli hold attivi di F06 e non rappresenta, da solo, la disponibilità netta agli scambi. Una collezione vuota restituisce `200` con `items: []` e conteggi a zero.

## Errori e persistenza

| Stato | Condizione |
| --- | --- |
| `200` | Lettura riuscita, anche senza risultati. |
| `400` | Parametri non validi, intervalli invertiti, ordinamento o direzione non supportati, pagina o dimensione fuori limite. |
| `401` | Bearer assente o non valido, secondo F01. |
| `404` | Carta non attiva o variante non appartenente alla collezione del chiamante. |
| `500` | Errore interno, compreso un errore di persistenza. |

Il catalogo legge `CARDS` non cancellate logicamente, i riferimenti dei cataloghi e tutte le righe `CARDS_ARTS`. `EXPANSIONS.is_active` non è usato come filtro: l'inattività non equivale alla cancellazione logica secondo lo schema v1.1. La collezione legge solo le righe `COLLECTIONS` del principal e la variante corrispondente in `CARDS_ARTS`; anche le carte cancellate logicamente sono escluse. Le letture sono JDBC e paginano lato database. `CARDS` ammette campi descrittivi nullabili nello schema: il contratto conserva tale nullabilità.

La composizione e il bilanciamento delle carte non sono approvati definitivamente. Le statistiche dimostrative non attestano valori di gioco definitivi. Le operazioni di scrittura amministrative del catalogo appartengono al contratto F10.

## Dati dimostrativi e verifica

I test database usano fixture isolate: 25 creature ripartite in cinque gruppi da cinque e cinque equipaggiamenti. La proposta V2.1 è rispettata per distribuzione di rarità, energia dei gruppi e budget punti potenza; attacco e difesa sono solo valori di prova perché le statistiche non sono bilanciate. Le fixture creano due varianti grafiche e conteggi di collezione, lasciando intenzionalmente una carta solo nel catalogo per verificare che non compaia nella collezione del chiamante. Non sono account o righe demo inseriti nelle migrazioni di produzione; le URL usano il dominio riservato `example.invalid`.

La prima esecuzione mirata del **2026-11-15** si è fermata al caricamento del contesto Spring per un conflitto nel nome del bean `cardCatalogRepository`; il nome del bean del repository F04 è stato reso esplicito. La ripetizione dei test non è stata eseguita. La compilazione di test è arrivata all'avvio del contesto, ma non costituisce esito positivo dei test. Restano da verificare query, risposte HTTP, filtri, proprietà della collezione e dati su PostgreSQL.

## Da definire

- Conferma definitiva del regolamento V2.1, dei valori numerici e dei contenuti delle carte.
- Modalità di caricamento di un catalogo dimostrativo consultabile nell'ambiente locale; al momento i dati demo sono fixture di test.
- Comportamento UI per immagini mancanti, filtri attivi, paginazione e ordinamento; integrazione FrontEnd non implementata.
- Compatibilità delle modifiche amministrative con flussi e dati di gioco non ancora presenti, da coordinare nelle funzionalità F03, F05 e F06.

## Collegamenti

- [Interfaccia catalogo e collezione](../FrontEnd/collezione-carte.md)
- [Schema dati v1.1](Database/database-schema-v1.1.md)
- [Catalogo e backoffice F10](gestione-amministrativa.md)
- [Accesso utenti F01](accesso-utenti.md)
- [Apertura pacchetti F03](acquisto-pacchetti.md)
- [Scambi F06](scambi-mercato.md)
- [Regolamento Versione 2.1](../Digital/regolamento-base.md)
- [Indice BackEnd](README.md)
