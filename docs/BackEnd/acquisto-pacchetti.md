# Offerte e acquisto di pacchetti

ID: F03 · Perimetro: confermato; specifica in approfondimento.
Implementazione: migrazione e CRUD del catalogo disponibili; acquisto e apertura non implementati. Responsabile: DA DEFINIRE (Software Developer).
Fonte: traccia, risposte e chiarimenti del gruppo; le decisioni note sono riportate sotto.
Ultimo aggiornamento significativo: **2026-11-24**, su richiesta dell’utente: branch `feature/f03-packages` creato da `dev` (`fa95b6c`), ripristino dei quattro file di schema e implementazione iniziale di migrazione e CRUD. Su successiva indicazione dell’utente, regole ed endpoint di apertura restano rinviati. Nessun seed dei pacchetti. La successiva decisione dello stesso giorno introduce quattro tipi di artwork, la distribuzione 81/10/7,5/1,5 e una FK diretta dal pacchetto; Showcase ed effetti speciali esclusi. Stato delle verifiche nelle sezioni dedicate sotto.

## Catalogo implementato: migrazione e API

La migrazione [V10__create_f03_packs_schema.sql](../../springboot_app/app/src/main/resources/db/migration/V10__create_f03_packs_schema.sql) aggiunge `artwork_pull_rates`, `packs`, `inventory_packs` e `user_mercy` alla base V1–V9 di `dev`; riusa `inventory` e i cataloghi esistenti. Non modifica migrazioni precedenti; inizializza solo la configurazione artwork approvata e normalizza i codici delle arti, senza inserire pacchetti, possesso o progressi. Questa V10 appartiene al branch backend e non importa la V10 delle immagini presente su `test-connessione`: un’eventuale integrazione fra i due branch dovrà riconciliare le versioni prima dell’applicazione su database condivisi.

Tutte le rotte richiedono Bearer JWT F01. `USER` e `ADMIN` possono consultare il catalogo; soltanto `ADMIN` può scriverlo. Le operazioni amministrative configurano prodotti: non caricano file immagine, non assegnano pacchetti a utenti e non cambiano saldi, collezione o progressi.

| Metodo e percorso | Comportamento | Risposta |
| --- | --- | --- |
| `GET /api/packs?page=0&size=20` | Elenco per `id` crescente; `page >= 0`, `1 <= size <= 100` | `200`, `{items, page, size, totalElements, totalPages}`; catalogo vuoto restituisce `items: []` |
| `GET /api/packs/{packId}` | Dettaglio di una definizione | `200`, oggetto pacchetto |
| `POST /api/admin/packs` | Crea una definizione | `201`, oggetto pacchetto e `Location: /api/packs/{id}` |
| `PUT /api/admin/packs/{packId}` | Sostituisce integralmente una definizione non utilizzata | `200`, oggetto pacchetto |
| `DELETE /api/admin/packs/{packId}` | Elimina fisicamente una definizione non utilizzata | `204`, senza corpo |

POST e PUT ricevono gli stessi campi: `pack_name`, `pack_description`, `pack_price`, `pack_image_url`, `allowed_factions`, `allowed_types`, `allowed_expansions`, `allowed_rarities`, `mercy_threshold`, `slot_pulls`, `id_artwork_pull_rates`. Il dettaglio aggiunge `id` e l’oggetto di sola lettura `artwork_pull_rates` al medesimo oggetto, senza involucro `definition`. `pack_image_url` e `mercy_threshold` sono facoltativi e nullable; gli altri campi sono obbligatori. L’ID deve essere un intero positivo a 32 bit. Nome e testo descrittivo non possono essere vuoti. `pack_price` è un intero a 32 bit non negativo: questo vincolo tecnico non stabilisce una risorsa di pagamento né prezzi commerciali.

`pack_description` segue la struttura confermata sotto. `card_number > 0`, `0 <= granted_number <= card_number`; i due codici di rarità devono essere presenti nel catalogo non cancellato. `example_pull` contiene esattamente `card1`…`cardN`, con `N = card_number`. Ogni valore deve coincidere con il dettaglio completo corrente F04 della carta, inclusi campi nullable e tutte le arti: dati incompleti, inventati o proprietà aggiuntive sono rifiutati. Il client amministrativo può ottenere queste rappresentazioni da F04 e inserirle nella mappa. La creazione non accredita le carte di anteprima.

I quattro filtri sono array di codici distinti, esatti e non vuoti, esistenti nei rispettivi cataloghi non cancellati; è ammesso un array vuoto. `slot_pulls` viene conservato come oggetto o array JSON. Il CRUD non interpreta questi slot, non stabilisce il significato dei filtri vuoti e non certifica una composizione eseguibile: probabilità per rarità, carte garantite, applicazione degli artwork agli slot, duplicati e policy mercy restano da concordare prima dell’apertura. Tipi e percentuali artwork sono invece confermati nella sezione seguente. Anche i campi descrittivi `granted_*` e `best_pull_rarity` non rendono operativa una garanzia.

PUT e DELETE acquisiscono `SELECT ... FOR UPDATE` sulla definizione e rifiutano con `409` qualunque pacchetto riferito da `inventory_packs` o `user_mercy`, anche se non ci sono più pacchetti posseduti ma resta un progresso. Non si modificano o cancellano tali riferimenti e non si azzerano progressi. Questa prima politica preserva le configurazioni utilizzate finché non esiste una riconciliazione approvata. Il CRUD è transazionale; una validazione fallita lascia la definizione precedente intatta. Non vengono acquisiti guard economici o aggiornati inventari, perché questa porzione non effettua operazioni di possesso.

Errori: `400` per input, paginazione, codici o anteprime non validi; `401` per sessione assente o non valida; `403` per scritture senza `ADMIN`; `404` per ID assente; `409` per modifiche o rimozioni di pacchetti utilizzati. Gli errori applicativi usano le risposte Problem Details configurate in Spring. POST ripetuti creano definizioni distinte: nessuna chiave idempotente o unicità del nome è introdotta. Dopo una risposta incerta non ripetere automaticamente POST. PUT ripetuto con lo stesso corpo su un pacchetto non utilizzato converge allo stesso contenuto; DELETE ripetuto restituisce `404` dopo la prima eliminazione.

L’anteprima è una fotografia validata alla scrittura, non una copia aggiornata automaticamente dopo modifiche al catalogo F04. GET conserva la fotografia; un nuovo PUT deve usare i dati correnti. Il contratto di versionamento del catalogo e l’eventuale aggiornamento automatico delle anteprime restano aperti. Acquisto, accredito di pacchetti, consultazione del possesso e apertura non hanno nuove rotte in questa attività.

### Verifiche artwork

Verifica del **2026-11-24** su Windows/PowerShell, JDK **21.0.11** e PostgreSQL **18.7** in un container temporaneo dedicato: la suite completa `mvnw.cmd -o "-Daether.syncContext.named.factory=rwlock-local" verify` supera **244 test**, senza fallimenti, errori o casi saltati, e produce il JAR. Prima della suite completa, i **32 test mirati** di `PackMigrationDatabaseTests`, `PackControllerDatabaseTests` e `CardCollectionControllerDatabaseTests` sono superati, senza fallimenti, errori o casi saltati. Coprono installazione e aggiornamento V9→V10, ripetizione senza duplicati, valori approvati, range/somma delle percentuali, FK obbligatoria del pacchetto, conversione dei codici senza cambiare ID o possesso, accettazione dei quattro tipi, rifiuto di Showcase/alias legacy, risposta delle probabilità e cambio della FK senza modificare configurazioni condivise. Conservate le protezioni su pacchetti posseduti e progressi mercy.

Controllati anche nomi delle tabelle e campi delle relazioni fra DBML e layout dbdiagram, parsing degli esempi JSON F03, collegamenti della documentazione e `git diff --check`. V1–V9 non sono modificate. La verifica Java riuscita richiede esecuzione autorizzata fuori dal sandbox, che blocca le connessioni loopback. Il database applicativo non è stato avviato o migrato. Apertura, acquisto, rendering frontend e integrazione browser restano esclusi; le verifiche della prima implementazione sotto sono storiche e distinte da questo allineamento.

### Verifiche della prima implementazione

Verifica del **2026-11-24** su Windows/PowerShell, JDK **21.0.11** e PostgreSQL **18.6** in un container temporaneo dedicato: `mvnw.cmd -o "-Daether.syncContext.named.factory=rwlock-local" clean verify` supera **239 test**, zero fallimenti, errori o casi saltati, e produce il JAR. Inclusi **15 nuovi test [CRUD e sicurezza](../../springboot_app/app/src/test/java/webapp/pack/PackControllerDatabaseTests.java)** con JWT reali, permessi, validazione, JSONB, anteprime F04, paginazione, aggiornamento/rimozione, protezione del possesso e di mercy e pubblicazione OpenAPI; **4 nuovi test [della migrazione](../../springboot_app/app/src/test/java/webapp/pack/PackMigrationDatabaseTests.java)** verificano installazione vuota, aggiornamento V9, conservazione di utenti, saldi, carte e collezione esistenti, assenza di seed e vincoli. Aggiornata a V10 l’aspettativa del test delle configurazioni dei round. Controllati anche i collegamenti relativi della documentazione modificata e `git diff --check`.

Il primo tentativo nel sandbox non ha avviato il contesto per un blocco delle connessioni loopback Java; la verifica completa riuscita è stata eseguita con autorizzazione fuori dal sandbox. Il database applicativo dell’utente non è stato avviato o migrato. Non sono verificati apertura, acquisto, distribuzione Compose, interfaccia browser o integrazione FrontEnd. I dati commerciali restano assenti; le fixture dei test non sono seed applicativi.

## Tipi e probabilità degli artwork

Decisione confermata dall’utente il **2026-11-24**: quattro tipi di artwork, senza Showcase e senza progettare o introdurre un livello di effetti speciali. I codici sono esatti e distinguono maiuscole e minuscole:

| Tipo | `CARDS_ARTS.art_code` | Colonna in `ARTWORK_PULL_RATES` | Percentuale iniziale |
| --- | --- | --- | --- |
| Standard Art | `STANDARD` | `standard` | 81,00% |
| Extended Art | `EXTENDED_ART` | `extended_art` | 10,00% |
| Full Art | `FULL_ART` | `full_art` | 7,50% |
| Alternate Art | `ALTERNATE_ART` | `alternate_art` | 1,50% |

L’1% precedentemente proposto per Showcase passa a Standard. Sono valori di configurazione del gioco approvati dall’utente, non probabilità ufficiali comuni ai giochi di carte esistenti. Il [riferimento editoriale D01](../Digital/lore-identita-carte.md#tipi-di-artwork) descrive le quattro presentazioni.

`artwork_pull_rates` contiene ID intero generato e quattro percentuali esatte `numeric(5,2)`, obbligatorie, ciascuna fra 0 e 100 e con somma 100. V10 inizializza una configurazione con questi valori; non crea pacchetti commerciali. Ogni pacchetto deve valorizzare `id_artwork_pull_rates`, FK diretta obbligatoria senza default: nessuna tabella di relazione. Più pacchetti possono riutilizzare una configurazione. Eliminare un pacchetto non elimina la configurazione; una configurazione referenziata non può essere eliminata.

POST e PUT dei pacchetti richiedono `id_artwork_pull_rates` positivo ed esistente; riferimenti mancanti o sconosciuti restituiscono `400`. Non vengono create o modificate probabilità implicitamente. Per scegliere una configurazione, l’amministrazione legge gli ID da `SELECT id, standard, extended_art, full_art, alternate_art FROM artwork_pull_rates ORDER BY id`; non si assume che una sequenza sia consecutiva. La gestione delle righe avviene a livello database; non è introdotto un CRUD HTTP separato. Cambiare configurazione di un pacchetto non utilizzato significa sostituirne la FK, senza aggiornare una riga condivisa. Restano i `409` per pacchetti posseduti o con progressi mercy.

GET, POST e PUT restituiscono anche `artwork_pull_rates`, oggetto di sola lettura con `id`, `standard`, `extended_art`, `full_art`, `alternate_art`, oltre a `id_artwork_pull_rates` nel pacchetto. Esempio del solo frammento interessato, con ID illustrativo:

```json
{
  "id_artwork_pull_rates": 1,
  "artwork_pull_rates": {
    "id": 1,
    "standard": 81.00,
    "extended_art": 10.00,
    "full_art": 7.50,
    "alternate_art": 1.50
  }
}
```

Le percentuali descrivono una singola estrazione del tipo di artwork, distinta dalla rarità di gioco; non stabiliscono da sole una probabilità complessiva per pacchetto. Numero e posizione degli slot interessati, interazione con i filtri e comportamento quando manca una variante per la carta selezionata restano da definire. Non è implementata alcuna estrazione o apertura.

V10 converte gli alias `default`, `EXTENDED`, `FULL` e `ALTERNATE` nei quattro codici canonici e aggiunge un `CHECK` sui tipi. Conserva ID delle arti, immagini, collezione e riferimenti dei mazzi. Altri codici legacy o collisioni richiedono una mappatura esplicita prima della migrazione; non si perdono varianti automaticamente. Le migrazioni V1–V9 restano immutate. Questa revisione aggiorna la V10 ancora in sviluppo: se una precedente V10 F03 è già applicata a un database, occorre riconciliare la versione prima del rilascio con una migrazione successiva, senza mascherare il cambio di checksum con `repair`. Il database applicativo non viene aggiornato da questa attività.

## Obiettivo e decisioni note

- Ogni offerta acquistabile contiene esattamente una unità di un pacchetto, senza insiemi di pacchetti differenti.
- Scaglie, Nuclei Rift e Rotami sono risorse distinte secondo la [terminologia economica confermata](../Digital/economia-crediti-pacchetti.md#risorse-economiche), rappresentate rispettivamente da `INVENTORY.cobalt_shards`, `rift_cores` e `scraps`. Quali risorse siano ammesse nei prezzi resta da definire.
- Prezzo e configurazione sono gestibili dal backoffice; lo schema v1.1 include `PACKS.pack_price int` per il prezzo di un pacchetto. Importi e risorsa richiesta restano da definire; non è modellata una tabella delle offerte e il collegamento offerta/pacchetto resta aperto.
- Distinguere offerta, singolo pacchetto e carte risultanti dall'apertura.
- La traccia richiede carte casuali con quantità fissate per livello di rarità; valori e distribuzione non sono ancora definiti.
- Per i pacchetti con mercy abilitato, il conto alla rovescia avanza all'apertura del pacchetto corrispondente. Ogni pesca della rarità richiesta o superiore, naturale o garantita, lo riporta alla soglia del pacchetto.

## Dati, transazioni e disponibilità

- `PACKS.id` e `INVENTORY.id` sono `int` auto-incrementali. I riferimenti a utente e pacchetto sono interi assegnati dai record esistenti, senza auto-incremento autonomo.
- `PACKS` conserva nome obbligatorio, `pack_description jsonb` obbligatorio secondo il contratto sotto, `pack_price int` obbligatorio senza default, immagine facoltativa e JSONB obbligatori per fazioni, tipi, espansioni, rarità e `slot_pulls`. I filtri usano i codici dei cataloghi; gli ID delle carte negli slot sono interi. La validazione dei JSON è applicativa, senza FK annidate; gli array di codici dei filtri sono definiti nel CRUD sopra; formato operativo e semantica degli slot restano da completare.
- `INVENTORY_PACKS` usa PK `(id_inventory, id_pack)`. `item_quantity` è obbligatoria, senza default e con `CHECK > 0`; consumare l'ultimo pacchetto elimina la riga. Una coppia assente vale zero dopo una lettura riuscita.
- I tre saldi di `INVENTORY` sono `int`, obbligatori, con default `0` e senza nuovi `CHECK` di non negatività: il backend verifica quantità, disponibilità, saldo finale non negativo e risultati nel dominio `int`.
- L'acquisto deve leggere il prezzo autorevole da `PACKS.pack_price` e conoscere la risorsa richiesta dall'offerta, addebitare il relativo saldo disponibile e incrementare i pacchetti nello stesso commit. Il selettore della risorsa dell'offerta resta da definire. L'apertura decrementa i pacchetti e incrementa `COLLECTIONS` per l'esatta coppia carta/arte estratta; errori annullano tutta l'operazione.
- Le scritture rispettano il [protocollo transazionale comune](Database/database-schema-v1.1.md#protocollo-transazionale-comune), con guard utente e letture aggiornate dopo i lock. `TRADE_TOKEN_RESERVATIONS.token_amount` non distingue una delle tre risorse: il suo abbinamento a un saldo, oppure un'estensione del contratto di prenotazione, deve essere definito prima di proteggere addebiti monetari condivisi. L'apertura non addebita nuovamente il prezzo.

## Descrizione strutturata e prezzo del pacchetto

Contratto dati confermato su richiesta dell’utente il **2026-11-23**, usato dal CRUD F03/F10 implementato sopra. Questa sezione definisce i dati; percorsi e autorizzazioni HTTP sono mantenuti nella sezione del catalogo. `pack_price` è una colonna distinta dalla descrizione. `pack_description` passa da testo a oggetto JSON, non a una stringa contenente JSON: è una modifica incompatibile con un client che legga direttamente una stringa. Il testo va letto da `pack_description.pack_description`.

| Campo | Tipo | Contenuto |
| --- | --- | --- |
| `pack_price` | integer | Prezzo del singolo pacchetto, obbligatorio e senza default; il CRUD richiede un valore non negativo a 32 bit. Risorsa di pagamento e importi commerciali restano da definire. |
| `pack_description` | object, persistito come JSONB | Oggetto obbligatorio con tutte le proprietà riportate sotto. |
| `pack_description.pack_description` | string | Testo descrittivo. |
| `pack_description.card_number` | integer | Numero di carte contenute nel pacchetto. |
| `pack_description.granted_number` | integer | Numero di carte garantite della rarità indicata. |
| `pack_description.granted_rarity` | string | Codice del catalogo rarità per la garanzia del pacchetto. |
| `pack_description.best_pull_rarity` | string | Codice della migliore rarità ottenibile dalla configurazione. |
| `pack_description.example_pull` | object | Mappa `card1`, `card2`, …, `cardN`; ogni valore rispetta lo schema JSON completo della carta riportato sotto. |

I nomi `card_numder` e `best_pull_rariry` della bozza sono corretti in `card_number` e `best_pull_rarity`. Le proprietà sopra sono obbligatorie; le cinque carte dell’esempio non impongono una composizione fissa a tutti i pacchetti.

### Schema JSON completo di una carta

Lo schema seguente si applica a ogni valore `example_pull.card1`, `example_pull.card2`, …, `example_pull.cardN`. È autonomo: tutti i tipi annidati sono definiti localmente in `$defs`, senza rimandi ad altri endpoint. Ogni proprietà elencata in `required` deve essere presente anche quando il suo valore può essere `null`. Gli interi rispettano il dominio Java/PostgreSQL `int` a 32 bit; non si impongono nuovi limiti di bilanciamento; `artCode` usa i quattro tipi confermati sopra. `artworks` è sempre un array, anche vuoto, e conserva tutte le varianti grafiche disponibili.

```json
{
  "$schema": "https://json-schema.org/draft/2020-12/schema",
  "title": "PackPreviewCard",
  "type": "object",
  "additionalProperties": false,
  "required": [
    "id", "name", "faction", "rarity", "energyCost", "attack",
    "defense", "power", "type", "expansion", "artworks"
  ],
  "properties": {
    "id": {"$ref": "#/$defs/int32"},
    "name": {"type": ["string", "null"]},
    "faction": {"$ref": "#/$defs/catalogReference"},
    "rarity": {"$ref": "#/$defs/catalogReference"},
    "energyCost": {"$ref": "#/$defs/nullableInt32"},
    "attack": {"$ref": "#/$defs/nullableInt32"},
    "defense": {"$ref": "#/$defs/nullableInt32"},
    "power": {"$ref": "#/$defs/nullableInt32"},
    "type": {"$ref": "#/$defs/catalogReference"},
    "expansion": {"$ref": "#/$defs/catalogReference"},
    "artworks": {
      "type": "array",
      "items": {"$ref": "#/$defs/artwork"}
    }
  },
  "$defs": {
    "int32": {
      "type": "integer",
      "minimum": -2147483648,
      "maximum": 2147483647
    },
    "nullableInt32": {
      "type": ["integer", "null"],
      "minimum": -2147483648,
      "maximum": 2147483647
    },
    "catalogReference": {
      "type": ["object", "null"],
      "additionalProperties": false,
      "required": ["id", "code", "name"],
      "properties": {
        "id": {"$ref": "#/$defs/nullableInt32"},
        "code": {"type": ["string", "null"]},
        "name": {"type": ["string", "null"]}
      }
    },
    "artwork": {
      "type": "object",
      "additionalProperties": false,
      "required": ["cardArtId", "artCode", "imageUrl"],
      "properties": {
        "cardArtId": {"$ref": "#/$defs/int32"},
        "artCode": {"type": "string", "enum": ["STANDARD", "EXTENDED_ART", "FULL_ART", "ALTERNATE_ART"]},
        "imageUrl": {"type": ["string", "null"]}
      }
    }
  }
}
```

Ogni carta è quindi un oggetto completo, senza involucro di paginazione o contatori di possesso. Il frontend passa ciascun valore della mappa al componente carta esistente. Lo schema è confrontato con i record Java [CardCatalogItem](../../springboot_app/app/src/main/java/webapp/card/CardCatalogItem.java), [CardReference](../../springboot_app/app/src/main/java/webapp/card/CardReference.java) e [CardArtwork](../../springboot_app/app/src/main/java/webapp/card/CardArtwork.java); questi collegamenti sono riferimenti implementativi, non sostituiscono la definizione dei campi.

### Esempio di descrizione e prezzo

Esempio di descrizione e prezzo con cinque carte complete. Testi, identificativi, rarità, quantità e prezzo sono illustrativi e non stabiliscono valori commerciali o di bilanciamento approvati:

```json
{
  "pack_price": 300,
  "pack_description": {
    "pack_description": "Pacchetto dimostrativo con cinque carte.",
    "card_number": 5,
    "granted_number": 1,
    "granted_rarity": "RARE",
    "best_pull_rarity": "EPIC",
    "example_pull": {
      "card1": {
        "id": 101,
        "name": "Creatura dimostrativa A",
        "faction": {"id": 1, "code": "SAP", "name": "Sapiens"},
        "rarity": {"id": 1, "code": "COMMON", "name": "Common"},
        "energyCost": 1,
        "attack": null,
        "defense": null,
        "power": 1,
        "type": {"id": 1, "code": "CREATURE", "name": null},
        "expansion": null,
        "artworks": [
          {"cardArtId": 201, "artCode": "STANDARD", "imageUrl": null},
          {"cardArtId": 211, "artCode": "ALTERNATE_ART", "imageUrl": null}
        ]
      },
      "card2": {
        "id": 102,
        "name": "Creatura dimostrativa B",
        "faction": {"id": 1, "code": "SAP", "name": "Sapiens"},
        "rarity": {"id": 1, "code": "COMMON", "name": "Common"},
        "energyCost": 1,
        "attack": null,
        "defense": null,
        "power": 1,
        "type": {"id": 1, "code": "CREATURE", "name": null},
        "expansion": null,
        "artworks": [{"cardArtId": 202, "artCode": "STANDARD", "imageUrl": null}]
      },
      "card3": {
        "id": 103,
        "name": "Creatura dimostrativa C",
        "faction": {"id": 1, "code": "SAP", "name": "Sapiens"},
        "rarity": {"id": 1, "code": "COMMON", "name": "Common"},
        "energyCost": 1,
        "attack": null,
        "defense": null,
        "power": 1,
        "type": {"id": 1, "code": "CREATURE", "name": null},
        "expansion": null,
        "artworks": [{"cardArtId": 203, "artCode": "STANDARD", "imageUrl": null}]
      },
      "card4": {
        "id": 104,
        "name": "Creatura dimostrativa D",
        "faction": {"id": 1, "code": "SAP", "name": "Sapiens"},
        "rarity": {"id": 1, "code": "COMMON", "name": "Common"},
        "energyCost": 1,
        "attack": null,
        "defense": null,
        "power": 1,
        "type": {"id": 1, "code": "CREATURE", "name": null},
        "expansion": null,
        "artworks": [{"cardArtId": 204, "artCode": "STANDARD", "imageUrl": null}]
      },
      "card5": {
        "id": 105,
        "name": "Creatura dimostrativa rara",
        "faction": {"id": 1, "code": "SAP", "name": "Sapiens"},
        "rarity": {"id": 2, "code": "RARE", "name": "Rare"},
        "energyCost": 1,
        "attack": null,
        "defense": null,
        "power": 1,
        "type": {"id": 1, "code": "CREATURE", "name": null},
        "expansion": null,
        "artworks": [{"cardArtId": 205, "artCode": "STANDARD", "imageUrl": null}]
      }
    }
  }
}
```

`example_pull` è un’anteprima dimostrativa, non il risultato di un’apertura né un accredito alla collezione. Quantità e rarità descrittive devono essere coerenti con filtri e `slot_pulls`; il backend deve verificarle insieme a forma, tipi e riferimenti delle carte. I campi `granted_*` descrivono la garanzia del pacchetto e non sostituiscono soglia o progresso mercy. JSONB valida la sintassi JSON ma non questi controlli; non sono introdotti nuovi `CHECK` SQL. Regole di aggiornamento dell’anteprima dopo modifiche al catalogo e policy definitive di estrazione restano da concordare.

Il modello aggiornato è nella [specifica database](Database/database-schema-v1.1.md#inventario-e-pacchetti) e nel relativo DBML. La revisione documentale del 2026-11-23 non applicava SQL; dal 2026-11-24 la V10 di questo branch crea `PACKS`, come descritto sopra. Eventuali dati preesistenti in altre installazioni richiederanno una conversione esplicita della descrizione e la valorizzazione del prezzo, senza inventare default.

## Mercy sulle aperture

Le regole seguenti descrivono il comportamento futuro concordato: questa prima implementazione crea la tabella dei progressi, ma non esegue aperture o aggiornamenti mercy.

Regole confermate dall'utente il **2026-11-09**, rappresentate anche nella [specifica autonoma del database](Database/database-schema-v1.1.md#inventario-e-pacchetti).

- `PACKS.mercy_threshold` è un intero nullable: `NULL` disabilita mercy; una soglia attiva deve essere maggiore di zero. Non è aggiunto un default fisso di dieci.
- `USER_MERCY` contiene una riga per `(id_user, id_pack)`: questa coppia è l'unica PK composta e le colonne sono FK obbligatorie a `USERS.id` e `PACKS.id`, senza auto-incremento. `mercy_rarity` è una FK obbligatoria a `RARITIES.id`, interpretata come rarità minima della garanzia.
- Il progresso persiste anche quando l'ultimo pacchetto posseduto viene consumato: eliminare la riga a quantità zero da `INVENTORY_PACKS` non elimina `USER_MERCY`.
- Alla prima apertura con mercy abilitato, il backend inizializza `mercy_counter` dalla soglia del pacchetto. Non esiste un default SQL che imposti lo stesso numero a tutti i pacchetti. La rarità iniziale deve provenire da una policy backend concordata; il client non sceglie una rarità autorevole.
- Ogni apertura riuscita del corrispondente `id_pack` usa quel progresso: `remaining = max(mercy_counter - 1, 0)`. Se arriva a zero, almeno uno degli slot previsti deve produrre una carta di rarità pari o superiore a `mercy_rarity`, rispettando configurazione, filtri e arte. Slot e distribuzione della pesca garantita restano da precisare; non si aggiunge implicitamente una carta oltre la composizione del pacchetto.
- Una pesca qualificante, anche naturale prima dello zero, salva nuovamente `PACKS.mercy_threshold`. Se nessuna carta qualifica, salva `remaining`. Un contatore già a zero forza la garanzia nell'apertura corrente, senza scendere sotto zero.
- Con soglia `10`, nove aperture senza esito qualificante lasciano `9, 8, ... , 1`; la decima garantisce la rarità richiesta e salva `10`. Un esito qualificante alla terza apertura salva subito `10`. I progressi sono indipendenti per ciascun pacchetto; il conto avanza sulle aperture riuscite, comprese quelle di pacchetti ottenuti come ricompensa.
- Il `CHECK` di riga impone `mercy_counter >= 0`. Il backend verifica anche soglia abilitata, limite superiore rispetto alla soglia, rarità interpretabili e disponibilità di una carta/arte valida per la garanzia. Non confronta gli ID delle rarità per stabilirne il livello.

### Apertura atomica

Flusso descrittivo da implementare, non un endpoint o un trigger esistente:

```text
function openPack(authenticatedUser, packId):
    begin transaction
    acquireCommonGuards(authenticatedUser, packId)
    pack = readCurrentPackConfiguration(packId)
    requireOwnedPack(authenticatedUser, packId)

    if pack.mercy_threshold is null:
        cards = drawNormalComposition(pack)
    else:
        T = requirePositiveThreshold(pack.mercy_threshold)
        progress = loadOrInitializeMercy(authenticatedUser, packId,
                                         T, approvedMinimumRarity(packId))
        requireCounterBetween(progress.mercy_counter, 0, T)
        remaining = max(progress.mercy_counter - 1, 0)
        if remaining == 0:
            cards = drawCompositionWithMinimumRarity(pack, progress.mercy_rarity)
        else:
            cards = drawNormalComposition(pack)

    requireValidCompositionAndRarityMetadata(cards, pack)
    if pack.mercy_threshold is not null:
        qualifies = hasQualifyingCard(cards, progress.mercy_rarity)
        if remaining == 0:
            require(qualifies)
        progress.mercy_counter = T if qualifies else remaining
        saveMercy(progress)

    consumeOneOwnedPack(authenticatedUser, packId)
    grantCardVariants(authenticatedUser, cards)
    commit
    return {cards, mercy: progress if pack.mercy_threshold is not null else null}
```

Consumo del pacchetto, inizializzazione/decremento/reset del progresso e accredito delle carte condividono il commit. Un errore annulla tutti gli effetti; una garanzia incompatibile con i filtri non consuma il pacchetto o il progresso. Il guard utente serializza le aperture concorrenti e protegge anche la riga mercy ancora assente. Se la soglia è `NULL`, i progressi esistenti non vengono avanzati o eliminati automaticamente.

La chiave del progresso non deduplica richieste ripetute dopo un commit incerto. Chiave di richiesta, conservazione del risultato e recupero devono ancora essere definiti; il client rilegge lo stato invece di ripetere alla cieca. Modifiche di soglia/rarità e riattivazione richiedono una policy di riconciliazione dei progressi esistenti. La garanzia viene risolta nella composizione dell'apertura stessa, senza record di ricompensa differita.

## Percorso acquisto, addebito, estrazione e collezione

Il flusso seguente descrive il comportamento atteso a livello funzionale per F03 e F04. È una proposta da completare con le decisioni aperte riportate sotto: non definisce API, payload o una transazione già implementata.

1. **Scelta dell'offerta.** L'utente consulta un'offerta attiva relativa a una sola unità di un pacchetto, con prezzo `PACKS.pack_price` e risorsa di pagamento da definire. Prezzo e composizione sono quelli correnti del catalogo gestito dal backoffice, non valori autorevoli inviati dal client.
2. **Richiesta d'acquisto.** L'utente conferma l'offerta. Il server identifica l'utente autenticato e rilegge prezzo, disponibilità e composizione dell'offerta. Non considera attendibile un saldo o un prezzo calcolato dal client.
3. **Controllo e addebito.** Il server controlla la disponibilità del saldo e registra l'acquisto al prezzo applicato. L'addebito deve riferirsi all'acquisto e non superare il saldo disponibile; saldo e registrazione devono restare coerenti anche in presenza di richieste concorrenti.
4. **Accredito del pacchetto acquistato.** Il server incrementa di una unità il possesso del pacchetto associato all'offerta. La rappresentazione del riferimento all'offerta e del prezzo pagato resta da definire; l'offerta non contiene più tipi di pacchetto.
5. **Apertura ed estrazione.** Per ciascun pacchetto aperto, il server applica la composizione e le regole casuali approvate per quel tipo e registra il risultato. Il client non sceglie né modifica le carte estratte. La composizione per rarità è richiesta dalla traccia, ma quantità, distribuzione e regole per duplicati sono ancora da definire.
6. **Aggiornamento della collezione.** Per ogni carta estratta, il server inserisce o aggiorna la riga `COLLECTIONS` identificata da utente, carta e variante grafica. Le copie duplicate della stessa variante incrementano il contatore aggregato `owned_replica`; non viene creato un identificativo `copyId` per ciascuna copia. L'aggiornamento di `tradable_replica` deve rispettare la policy di trasferimento e prenotazione F06, ancora da definire per l'apertura.
7. **Risposta e consultazione.** Il client riceve l'esito dell'operazione, il saldo aggiornato e le carte ottenute, oppure recupera l'esito dell'apertura separatamente se questa è differita. La risposta di apertura può elencare le estrazioni; la collezione F04 espone poi i contatori aggregati per carta e variante.

### Coerenza e casi di errore da concordare

- **Unità di successo.** È raccomandato che addebito e registrazione dell'acquisto siano atomici. Se l'apertura avviene nella stessa richiesta, anche estrazione registrata e copie aggiunte devono essere coerenti con l'esito; non deve risultare un acquisto addebitato senza una possibilità definita di recuperare o completare l'apertura.
- **Saldo insufficiente.** La richiesta non deve produrre un addebito né pacchetti o copie. La risposta d'errore e il modo in cui l'interfaccia comunica il saldo residuo sono da definire.
- **Concorrenza.** Acquisti simultanei non devono spendere due volte la stessa disponibilità. Tecnica di serializzazione/controllo, isolamento e comportamento in caso di conflitto sono da definire dopo la scelta dello stack.
- **Retry e doppio invio.** Una richiesta ripetuta non dovrebbe generare addebiti o aperture duplicate; identificatore idempotente, durata e risposta da restituire sono da concordare.
- **Errore durante l'apertura.** Se la generazione o la registrazione del risultato fallisce, l'esito deve restare recuperabile e non creare copie parziali o duplicati. Politica di rollback o ripresa e registrazione della casualità restano da definire.
- **Saldo mostrato.** Il saldo restituito dal server è quello autorevole dopo l'operazione; i valori locali del client sono solo presentazione e devono riallinearsi alla risposta.

### Scelta ancora aperta: apertura immediata o differita

L'offerta contiene pacchetti; non è ancora deciso se l'acquisto li apra subito o li aggiunga prima a una disponibilità dell'utente per un'apertura successiva. In entrambi i casi il server deve impedire di aprire più volte lo stesso pacchetto. Il contratto dovrà quindi specificare stati e operazioni dell'apertura, nonché se l'acquisto restituisce direttamente le carte o solo i pacchetti ottenuti.

### Esempi di dati illustrativi

Gli identificativi e gli importi sono fittizi. Questi esempi aiutano il confronto con il FrontEnd ma non definiscono DTO, endpoint, prezzo, saldo iniziale o composizione approvati.

Saldo esemplificativo dopo l'acquisto:

```json
{
  "userId": 42,
  "balanceCredits": 700
}
```

Risultato esemplificativo nel caso, ancora da decidere, in cui l'apertura avvenga contestualmente all'acquisto. I 300 crediti addebitati e le due carte mostrate non rappresentano regole o valori approvati. Gli ID sono interi illustrativi coerenti con lo schema v1.1; `cardArtId` identifica la variante, non una singola copia posseduta:

```json
{
  "purchaseId": 301,
  "packId": 401,
  "creditsDebited": 300,
  "balanceAfter": 700,
  "cards": [
    {
      "cardId": 101,
      "cardArtId": 201,
      "name": "Creatura dimostrativa",
      "rarity": "COMMON"
    },
    {
      "cardId": 102,
      "cardArtId": 202,
      "name": "Carta dimostrativa B",
      "rarity": "RARE"
    }
  ]
}
```

Se sarà scelta l'apertura differita, il risultato delle carte dovrà essere rappresentato da un esito separato, da definire. Per l'esempio di lettura della collezione aggregata, vedere [F04 — Catalogo e collezione](collezione-carte.md#risposta-collezione).

## Da definire

- Risorse ammesse nei prezzi, importi e limiti di `pack_price`, selettore della risorsa e collegamento offerta/pacchetto; assegnazione iniziale dei crediti e ulteriori fonti di variazione in coordinamento con D03. Non è modellato un registro dei movimenti.
- Aggiornamento delle anteprime persistite quando cambiano dati, disponibilità o varianti del catalogo F04.
- Tipi e disponibilità dei pacchetti, composizione per rarità, carte garantite, gestione dei duplicati, slot/probabilità per rarità, filtri vuoti e applicazione delle quattro probabilità artwork, incluso il trattamento di una variante mancante.
- Corrispondenza delle prenotazioni e modalità di apertura immediata o differita; stati, risultato, recupero dopo errori e gestione dei progressi mercy quando cambia o si riattiva la configurazione.
- Policy iniziale di `mercy_rarity`, ordinamento delle rarità e slot/distribuzione della garanzia.
- Contratto API di acquisto e apertura, deduplicazione, transazioni economiche, concorrenza e recupero dei commit incerti; il CRUD del catalogo è definito nella sezione implementata sopra.

## Collegamenti

- [Interfaccia negozio](../FrontEnd/acquisto-pacchetti.md)
- [Economia](../Digital/economia-crediti-pacchetti.md)
- [Backoffice](gestione-amministrativa.md)
- [Collezione](collezione-carte.md)
- [Indice BackEnd](README.md)
