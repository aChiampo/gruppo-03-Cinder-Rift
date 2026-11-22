# Catalogo e collezione delle carte

ID: F04 · Perimetro: confermato; contratto HTTP implementato.
Implementazione: controller, servizi e repository JDBC disponibili; catalogo e collezione React collegati. Otto test API superati il 2026-11-21; verifiche frontend e client contro Spring del 2026-11-22 descritte sotto.
Ultimo aggiornamento significativo: 2026-11-22, su richiesta dell'utente di collegare React alle API esistenti e pubblicare le illustrazioni delle 30 carte CR01 tramite V10, allineando documentazione e verifiche. Il contratto backend resta invariato. Contratto iniziale: 2026-11-15; importazione del branch `feature/api-collezione-catalogo`: 2026-11-21. Schema persistente: [database v1.1](Database/database-schema-v1.1.md).

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
| `GET /api/users/me/collections/{cardId}/{cardArtId}` | Bearer richiesto | Singola variante posseduta dal chiamante; `404` se assente. |

Le risposte degli elenchi hanno forma `CardPage<T>`: `items`, `page`, `size`, `totalElements`, `totalPages`. Gli ID sono interi. Nessun parametro di percorso o query seleziona il proprietario della collezione.

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
      "faction": {"id": 1, "code": "SAP", "name": "Sapiens"},
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
      "faction": {"id": 1, "code": "SAP", "name": "Sapiens"},
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

Il set CR01 locale e la collezione dimostrativa sono inseriti dalle migrazioni V5/V6. Le fixture API descritte sotto restano isolate dai dati applicativi. [Seed corrente](../../springboot_app/app/README.md#set-iniziale-e-utente-dimostrativo-f01f05).

I test database usano fixture isolate: 25 creature ripartite in cinque gruppi da cinque e cinque equipaggiamenti. La proposta V2.1 è rispettata per distribuzione di rarità, energia dei gruppi e budget punti potenza; attacco e difesa sono solo valori di prova perché le statistiche non sono bilanciate. Le fixture creano due varianti grafiche e conteggi di collezione, lasciando intenzionalmente una carta solo nel catalogo per verificare che non compaia nella collezione del chiamante. Non sono account o righe demo inseriti nelle migrazioni di produzione; le URL usano il dominio riservato `example.invalid`.

Evidenza storica del branch sorgente: la prima esecuzione mirata del **2026-11-15** si è fermata al caricamento del contesto Spring per un conflitto nel nome del bean `cardCatalogRepository`; il nome del bean del repository F04 è stato reso esplicito. La ripetizione dei test non è stata eseguita. La compilazione di test è arrivata all'avvio del contesto, ma non costituisce esito positivo dei test. Restano da verificare query, risposte HTTP, filtri, proprietà della collezione e dati su PostgreSQL.

## Verifica dell’integrazione del 2026-11-21

Su richiesta dell’utente, integrate le modifiche di `feature/api-collezione-catalogo` (`3f01878`) in `test-connessione`, con base `8b358bd`. Risolti sei conflitti documentali conservando i contratti e le evidenze F01/F05/F07 più recenti. Adeguati i test importati a `/api/auth/signin`, Jackson 3, codici fazione `SAP`/`CYB`/`ALN`/`CLT`/`RAD` e filtro sull’espansione della fixture per non contare il seed CR01.

Ambiente: Windows, JDK 21.0.11 e PostgreSQL 18.6 in un container temporaneo dedicato, con database e chiave JWT di prova separati dall’applicazione. Il tentativo iniziale della suite si è fermato per una chiave JWT di prova non Base64; corretto il formato prima delle prove successive.

- `mvnw.cmd -o verify`: 212 test esistenti superati; gli otto test importati inizialmente non eseguibili per la dipendenza Jackson 2. Dopo l’adeguamento, due asserzioni hanno evidenziato i vecchi codici fazione nelle fixture, poi corretti.
- `mvnw.cmd -o "-Dtest=CardCollectionControllerDatabaseTests" verify`: otto test superati, senza fallimenti, errori o casi saltati; prodotto il JAR. Coperti letture catalogo e collezione, autenticazione, proprietà, varianti, filtri, paginazione, input non valido e CRUD amministrativo.
- `git diff --check` e controllo dei collegamenti relativi nei documenti modificati superati; nessun marcatore di conflitto residuo.

I 220 test sono passati in esecuzioni distinte: la suite completa non è stata ripetuta dopo le sole correzioni ai test importati. Browser, integrazione React, immagini Compose e distribuzione sul database applicativo non verificati da questa attività.

## Integrazione React e verifica del 2026-11-22

Il client usa le due rotte di elenco, senza body nelle richieste GET né scelta del proprietario. Conserva `CardPage<T>` e i DTO canonici; la sola conversione della paginazione avviene tra interfaccia a base uno e query a base zero. La UI richiede `size=15`, nome crescente come default e filtri singoli per i codici categoriali. Nessun limite numerico è implicito; i campi vuoti sono omessi e zero viene conservato.

Catalogo e collezione hanno sorgenti distinte: il catalogo conserva tutte le arti nella stessa carta e consente di selezionarle nel dettaglio; la collezione conserva una riga per variante e i due contatori persistiti, senza dedurre gli hold F06. Gli elenchi contengono già i dettagli necessari: le rotte di dettaglio rimangono implementate solo nel backend. [Comportamento dell'interfaccia, verifiche e limiti](../FrontEnd/collezione-carte.md).

Nella copia di lavoro di `test-connessione` con base `9e0dd83` e modifiche non committate, `npm test` ha superato 33 test frontend e `npm run build` ha prodotto il bundle. Il client sorgente è stato verificato anche contro Spring locale: catalogo e collezione da 30 righe, filtro combinato, seconda pagina, intervallo invertito `400` e accesso senza Bearer `401`. Dopo la ricostruzione del solo frontend con Compose, gli stessi controlli sono passati attraverso Nginx sulla porta 8082, insieme alla lettura dei cinque equipaggiamenti, alla conservazione dei payload e al controllo del bundle e della salute. Usato l'account dimostrativo già documentato; sessioni di prova chiuse, nessuna scrittura su carte o copie. Nessuna modifica al codice Java o al contratto HTTP; suite backend non ripetuta. Browser non verificato: host di accesso non disponibile.

## Immagini del set CR01

Aggiornamento del **2026-11-22**, su richiesta dell'utente. La migrazione [V10](../../springboot_app/app/src/main/resources/db/migration/V10__set_initial_card_art_urls.sql) assegna alle arti `default` delle 30 carte CR01 gli URL `/img/cards/<nome-carta>.webp`, risolvendo la carta tramite nome esatto e codice editoriale. Sostituisce anche eventuali URL precedenti su queste arti; varianti diverse, carte con nome/codice non corrispondenti e carte esterne al set restano escluse. Conserva ID, `source_image` e riferimenti di collezioni e mazzi; non modifica lo schema o i DTO.

I 30 file in [public/img/cards](../../cinder-rift/public/img/cards/) contengono soltanto l'illustrazione centrale ritagliata dagli originali forniti nella cartella locale `Carte/`, con nomi stampati verificati contro il seed V5. Sono WebP lossless, senza ridimensionamento o rigenerazione dell'illustrazione; cornici, pannelli del titolo, statistiche e testi sono esclusi. Il retro comune non rappresenta una carta e non entra nella migrazione. Gli originali restano esterni al repository; `source_image` ne conserva il nome originale.

Flyway applica V10 al normale avvio del backend su un database a V9, senza reset o modifica delle migrazioni precedenti. Gli URL sono relativi all'origine del frontend: Vite e Nginx servono gli asset da `/img/cards/`. Ricostruire e distribuire anche il frontend con i nuovi file; il backend non serve queste immagini. Per controlli ed esiti correnti vedere la [verifica degli asset](../FrontEnd/collezione-carte.md#immagini-del-set-cr01).

Verifica locale del **2026-11-22**, copia di lavoro di `test-connessione`: il test `RoundConfigurationMigrationDatabaseTests` passa con JDK 21.0.11 e PostgreSQL 18.6 in un container temporaneo dedicato. Flyway applica V1–V10 su schema vuoto e non applica ulteriori migrazioni al secondo avvio. Comando dalla cartella backend, con le variabili di prova documentate: `mvnw.cmd -o "-Daether.syncContext.named.factory=rwlock-local" "-Dtest=RoundConfigurationMigrationDatabaseTests" test`.

Controlli SQL separati, nello stesso database isolato, verificano tutte le 30 associazioni dopo l'installazione e l'aggiornamento di uno schema a V9: sostituzione di un URL precedente, conservazione di ID e metadati delle arti, carte, collezioni, mazzi e relative copie, esclusione di un'arte alternativa e di una carta esterna con lo stesso nome, rispetto del nome esatto e seconda esecuzione senza aggiornamenti. Le fixture di aggiornamento sono annullate con rollback; il database applicativo non è modificato. Suite backend completa, distribuzione Compose e browser non verificati da questa attività.

## Dati, copie e varianti grafiche

- Gli ID generati di utenti, carte e arti sono `int` auto-incrementali. `COLLECTIONS` eredita gli interi nella PK `(id_user, id_card, id_card_art)` e non genera nuovi identificativi.
- `CARDS_ARTS` ha PK `(id_card, art_code)`, `id_art` auto-incrementale univoco e unicità `(id_card, id_art)`. La FK composta della collezione garantisce che l'arte appartenga alla carta; l'URL dell'arte rimane nullable.
- `owned_replica` è obbligatorio e maggiore di zero; `tradable_replica` è obbligatorio e compreso fra zero e il possesso. Una tripla assente significa zero solo dopo una lettura riuscita. Il trasferimento dell'ultima copia elimina la riga.
- Le copie scambiabili disponibili sono `tradable_replica` meno la somma degli hold non scaduti della stessa tripla. Prenotare non modifica i due contatori. Prima di eliminare una riga occorre consumare o rilasciare gli hold attivi; non esiste una FK degli hold alla riga volatile di possesso.
- I mazzi scelgono esplicitamente `id_card_art`. Possesso per variante, effetti dei trasferimenti sui mazzi e disponibilità per il gioco richiedono validazione backend; le prenotazioni economiche non decidono da sole la giocabilità.
- Il modello completo prevede il [protocollo transazionale comune](Database/database-schema-v1.1.md#protocollo-transazionale-comune). Nel modello completo, la cancellazione logica di carte o espansioni invalida prima le trattative e rilascia entrambi i lati, poi converte le copie e ripulisce collezione e mazzi; tassi e frammenti restano da definire. Il CRUD F10 corrente rifiuta invece la modifica o cancellazione di carte presenti in collezioni o mazzi; non implementa ancora questi flussi futuri.

## Da definire

- Conferma definitiva del regolamento V2.1, dei valori numerici e dei contenuti delle carte.
- Verifica browser di immagini mancanti, filtri, varianti e paginazione; comportamento implementato e descritto nella pagina FrontEnd, verifica browser ancora da eseguire.
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
