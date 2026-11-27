# Offerte e acquisto di pacchetti

Verifica finale acquisto F03 del **2026-11-27**: **412 test backend superati**, zero fallimenti/errori/casi saltati, e JAR prodotto con JDK 21.0.11 e PostgreSQL 18.6 isolato. Incluse 49 prove di acquisto e 5 del lettore prenotazioni condiviso. Nessuna migrazione o modifica al database applicativo; frontend e flussi F06 esclusi. [Comando, copertura e limiti](../../springboot_app/app/README.md#verifica-acquisto-f03-del-2026-11-27).

Aggiornamento acquisto del **2026-11-27**, su richiesta dell’utente: implementato `POST /api/users/me/pack-purchases`, con addebito tramite `DebitService` condiviso e lettura delle prenotazioni tramite `ReservationReader` condiviso. Confermata la corrispondenza degli hold monetari F06 con le Scaglie. Nessuna nuova migrazione, deduplicazione o cronologia acquisti; seed commerciale e frontend esclusi. [Contratto di acquisto](#acquisto-pacchetti-contratto-http). Le verifiche dell’apertura sotto sono precedenti a questo intervento.

Verifica dell’apertura F03 del **2026-11-27**: **358 test backend superati**, zero fallimenti/errori/casi saltati, e JAR prodotto con JDK 21.0.11 e PostgreSQL 18.6 temporaneo. Incluse 7 prove deterministiche di estrazione e 47 prove API/transazionali di apertura. Database applicativo non modificato; acquisto, seed commerciale e frontend F03 esclusi. [Comandi e copertura](../../springboot_app/app/README.md#verifica-dellapertura-f03-del-2026-11-27).

Verifica precedente dell’integrazione del **2026-11-27**: **300 test backend**, **51 del motore** e **54 frontend** superati; JAR backend e build React prodotti. [Comandi, copertura e limiti](../../springboot_app/app/README.md#verifica-dellintegrazione-f03-del-2026-11-27). Database applicativo non modificato.

**Integrazione con dev — 2026-11-27.** Su richiesta dell’utente, `feature/f03-packages` conserva il cleanup di `dev`: V1–V8 e V11 restano inalterate, V3 definisce lo schema completo e V5 inserisce direttamente carte e arti `STANDARD` con ID generati. Le migrazioni F03 diventano V12/V13 per seguire V11 amicizie senza collisioni. La catena completa contiene undici script: V1–V8 e V11–V13. La cronologia precedente al cleanup, incluse le vecchie V10/V11 F03, richiede una conversione separata: non usare `repair` per mascherare checksum o versioni incompatibili. Il database applicativo non viene modificato da questa integrazione.

ID: F03 · Perimetro: confermato; specifica in approfondimento.
Implementazione: migrazioni, CRUD, validazione delle pescate, inventario, apertura e acquisto disponibili. Responsabile: Chiampo (Software Developer).
Fonte: traccia, risposte e chiarimenti del gruppo; le decisioni note sono riportate sotto.
Ultimo aggiornamento significativo: **2026-11-27**, acquisto implementato su richiesta dell’utente con lettore prenotazioni e servizio di addebito condivisi, senza migrazione F06, deduplicazione o cronologia acquisti. Catalogo, apertura, mercy e rimborsi amministrativi restano disponibili; seed commerciale escluso. Le verifiche precedenti sono evidenze storiche.

Pianificazione inventario e apertura del **2026-11-26**, aggiornata su richiesta esplicita di Chiampo: il [contratto HTTP](#Inventario%20e%20apertura%20differita%3A%20contratto%20HTTP) comprende due letture del proprio inventario e l’apertura di **1–10 pacchetti**, con lock non attendente, rollback dell’intero lotto e risposta diretta dopo commit. Nessuna persistenza delle pescate, chiave di richiesta o rotta di recupero; gli ulteriori problemi di retry sono esclusi dal perimetro richiesto. Nota storica della pianificazione: l’implementazione è stata avviata e completata nel backend il 2026-11-27; nessuna nuova migrazione richiesta.

Decisioni confermate di Chiampo del **2026-11-26**: le pescate incrementano **solo `ownedReplica`**, lasciando `tradableReplica` invariato o inizializzandolo a zero; mercy avanza con una funzione per singolo pacchetto applicata sequenzialmente nello stesso lotto. Per la concorrenza usare soltanto il lock semplice descritto sotto. Le precedenti proposte di deduplicazione persistente, registro delle pescate, recupero e conservazione sono rimosse su richiesta dell’utente.

Revisione documentale del **2026-11-26**: aggiunti gli schemi di richiesta e risposta alla tabella degli endpoint, confrontati con i record Java; collegamenti alle sezioni adattati al formato dei titoli di Obsidian. Nessuna modifica al contratto o nuova esecuzione dei test backend.

Aggiornamento implementativo del **2026-11-27**, su istruzione esplicita dell’utente: V12 definisce direttamente la rarità mercy nullable nel pacchetto. CRUD allineato e garanzia sull’ultimo slot ora implementata; la verifica di questa revisione dello schema è storica rispetto all’apertura. Verifica della precedente revisione dello schema: **38 test mirati superati**, zero fallimenti/errori/casi saltati, con JDK 21.0.11 e PostgreSQL 18.7 isolato; comando e copertura nel README backend. Nessuna modifica al database applicativo.

## Decisioni confermate del 2026-11-27

Regole implementate nel backend per configurazione, apertura e acquisto. Seed commerciale ancora da definire.

- **Filtri e carte:** i filtri sono cumulativi. Un array vuoto non ammette carte; per nessuna restrizione si elencano tutti i codici ammessi. Dopo la rarità dello slot, le carte eleggibili sono equiprobabili. Duplicati e arti diverse della stessa carta sono consentiti nello stesso pacchetto.
- **Validazione delle pescate:** `PackConfigurationValidationService`, richiamato dai flussi POST/PUT amministrativi e dalle aperture, rifiuta ogni configurazione in cui una rarità a probabilità positiva non ha carte eleggibili. I campi `granted_number`, `granted_rarity` e `best_pull_rarity` sono soltanto metadati; le regole dipendono dai tipi di slot e da mercy. Le anteprime restano amministrative, senza aggiornamento automatico.
- **Artwork:** estrazione indipendente per slot. Se la variante estratta manca, usare `STANDARD` della stessa carta; la variante Standard deve esistere.
- **Mercy:** ordine `COMMON < RARE < EPIC < LEGENDARY`, configurazione condivisa nel pacchetto e sostituzione dell’ultimo slot secondo le regole sotto.
- **Acquisto:** ogni pacchetto rappresenta la propria offerta, con prezzo unitario `pack_price` in Scaglie (`cobalt_shards`). Sono acquistabili più unità dello stesso tipo, sempre chiuse e accreditate nell’inventario. Nessuna apertura immediata né limite commerciale di possesso; quantità limitata dal saldo disponibile, entro i limiti tecnici degli interi. Scaglie prenotate dal mercato non spendibili, con messaggio backend che spiega il rifiuto. Gli hold monetari F06 riservano Scaglie, su conferma dell’utente del 2026-11-27.
- **Seed:** tipi, slot, prezzi, soglie e rarità iniziali sono in definizione. Acquisizione delle risorse: **DA DEFINIRE**. Attività solo backend; nessuna implementazione frontend.

### Definizione della rarità mercy in V12

[V12](../../springboot_app/app/src/main/resources/db/migration/V12__create_f03_packs_schema.sql) definisce direttamente `packs.mercy_rarity integer NULL`, senza default, con FK `packs_mercy_rarity_fk` a `rarities.id`. `user_mercy` nasce con i soli campi `id_user`, `id_pack` e `mercy_counter`. Su istruzione esplicita dell’utente si modifica lo script iniziale: non viene aggiunta una migrazione successiva o una conversione di dati legacy. La catena resta V1–V8 e V11–V13. Nessuna modifica al database applicativo.

## Catalogo implementato: migrazione e API

La migrazione [V12__create_f03_packs_schema.sql](../../springboot_app/app/src/main/resources/db/migration/V12__create_f03_packs_schema.sql) aggiunge `artwork_pull_rates`, `packs`, `inventory_packs` e `user_mercy` alla base V1–V8 e V11 di `dev`; riusa `inventory` e i cataloghi esistenti. Inizializza solo la configurazione artwork approvata e vincola i quattro codici delle arti già inizializzate da V5, senza inserire pacchetti, possesso o progressi.

Tutte le rotte richiedono Bearer JWT F01. `USER` consulta soltanto pacchetti con `is_acquirable = true`, anche sul dettaglio; `ADMIN` vede tutti i pacchetti e può scriverli. Le scritture configurano prodotti senza caricare immagini o assegnare pacchetti/carte; PUT elimina i progressi mercy relativi e DELETE rimborsa il possesso come descritto sotto.

| Metodo e percorso | Comportamento | Payload richiesta | Risposta e schema del payload |
| --- | --- | --- | --- |
| `GET /api/packs` | Elenco completo dei pacchetti visibili, per `id` crescente; senza paginazione | Nessun corpo; nessun parametro di query previsto | `200`, array di [PackView](#Schema%20della%20risposta%20PackView); catalogo vuoto restituisce `[]` |
| `GET /api/packs/{packId}` | Dettaglio di un pacchetto | Nessun corpo; `packId` intero positivo a 32 bit nel percorso | `200`, [PackView](#Schema%20della%20risposta%20PackView) |
| `POST /api/admin/packs` | Crea una definizione | JSON [PackWriteRequest](#Schema%20della%20richiesta%20PackWriteRequest) | `201`, [PackView](#Schema%20della%20risposta%20PackView) e `Location: /api/packs/{id}` |
| `PUT /api/admin/packs/{packId}` | Sostituisce la definizione, conserva il possesso ed elimina i progressi mercy relativi | JSON [PackWriteRequest](#Schema%20della%20richiesta%20PackWriteRequest); `packId` intero positivo a 32 bit nel percorso | `200`, [PackView](#Schema%20della%20risposta%20PackView) |
| `DELETE /api/admin/packs/{packId}` | Rimborsa il possesso, elimina i riferimenti e la definizione | Nessun corpo; `packId` intero positivo a 32 bit nel percorso | `204`, senza corpo |

POST e PUT ricevono gli stessi campi: `pack_name`, `pack_description`, `pack_price`, `is_acquirable`, `pack_image_url`, `allowed_factions`, `allowed_types`, `allowed_expansions`, `allowed_rarities`, `mercy_threshold`, `mercy_rarity`, `pack_slots`, `id_artwork_pull_rates`. Il dettaglio aggiunge `id`, `artwork_pull_rates` e `slot_pull_types` di sola lettura al medesimo oggetto, senza involucro `definition`. `pack_image_url`, `mercy_threshold` e `mercy_rarity` sono facoltativi e nullable; gli altri campi sono obbligatori. Gli ID devono essere interi positivi a 32 bit. Nome e testo descrittivo non possono essere vuoti. `pack_price` è un intero a 32 bit non negativo: la risorsa approvata è `cobalt_shards`; gli importi commerciali restano in definizione. `is_acquirable` è un booleano obbligatorio: abilita nuove acquisizioni e determina la visibilità nel catalogo USER. Disabilitarlo non revoca pacchetti già posseduti e non rimborsa crediti; il possesso resta apribile. Acquisto disponibile secondo il contratto HTTP dedicato.

`pack_description` segue la struttura confermata sotto. `card_number > 0`, `0 <= granted_number <= card_number`; i due codici di rarità devono essere presenti nel catalogo non cancellato. `example_pull` contiene esattamente `card1`…`cardN`, con `N = card_number`. Ogni valore deve coincidere con il dettaglio completo corrente F04 della carta, inclusi campi nullable e tutte le arti: dati incompleti, inventati o proprietà aggiuntive sono rifiutati. Il client amministrativo può ottenere queste rappresentazioni da F04 e inserirle nella mappa. La creazione non accredita le carte di anteprima.

I quattro filtri sono array non vuoti di codici distinti ed esatti, esistenti nei cataloghi non cancellati. Un array vuoto non ammette alcuna carta ed è rifiutato dal servizio di validazione delle pescate. `pack_slots` è un array ordinato di ID di `slot_pull_type`, lungo esattamente `card_number`; lo stesso ID può comparire più volte. Il CRUD controlla esistenza, totale 100 e rarità non cancellate dei tipi, bloccandoli durante la scrittura. Filtri, duplicati, artwork e garanzie seguono le decisioni implementate sopra. Una rarità con probabilità positiva deve avere almeno una carta eleggibile; mercy attivo richiede una carta della rarità esatta configurata, anche se assente dalle probabilità ordinarie dell’ultimo slot. I campi descrittivi `granted_*` e `best_pull_rarity` restano metadati; le garanzie operative derivano dai tipi di slot e da mercy.

PUT e DELETE acquisiscono `SELECT ... FOR UPDATE` sulla definizione e sono consentiti anche quando esistono righe in `inventory_packs` o `user_mercy`. PUT conserva tutte le quantità possedute e i saldi; dopo una scrittura valida elimina le righe mercy del pacchetto, anche per il solo cambio del flag. La successiva apertura le inizializza dalla configurazione corrente: impostare il contatore a zero attiverebbe invece subito la garanzia e non rappresenta un reset alla soglia.

DELETE accredita a ogni inventario associato `item_quantity × pack_price` in `cobalt_shards`, usando il prezzo corrente della definizione bloccata, come confermato dall’utente il 2026-11-26. Il database non registra il prezzo originariamente speso: questo rimborso non ricostruisce lo storico e si applica a ogni quantità posseduta. `rift_cores`, `scraps`, altri pacchetti e relativi progressi restano invariati. Gli inventari coinvolti vengono bloccati per ID crescente e aggiornati con un incremento SQL; quindi vengono eliminate le righe di possesso e mercy del pacchetto, infine la definizione e i suoi slot. I tipi di slot condivisi restano disponibili.

Definizione, saldi e pulizia condividono la stessa transazione: una validazione o scrittura fallita annulla tutti gli effetti. I calcoli del rimborso usano intermedi a 64 bit; se il saldo finale non è rappresentabile nell’attuale colonna `integer`, l’operazione fallisce senza overflow silenzioso o perdita di risorse. L’apertura protegge la definizione con `FOR SHARE NOWAIT` dopo il lock del possesso. Se PUT/DELETE la stanno modificando, restituisce `503 TEMPORARILY_UNAVAILABLE` senza attesa sul lock inverso. L’acquisto usa la transazione dedicata descritta sotto.

Errori: `400` per input, codici o anteprime non validi; `401` per sessione assente o non valida; `403` per scritture senza `ADMIN`; `404` per ID assente o pacchetto non acquisibile richiesto da USER. Il precedente `409` per possesso o mercy è rimosso. Gli errori applicativi usano le risposte Problem Details configurate in Spring. `page` e `size` non fanno più parte del contratto e, se presenti nella query, vengono ignorati. La risposta lista passa dall’involucro paginato all’array: il frontend deve adeguare la lettura. POST ripetuti creano definizioni distinte: nessuna chiave idempotente o unicità del nome è introdotta. Dopo una risposta incerta non ripetere automaticamente POST. PUT ripetuto converge allo stesso contenuto ed elimina eventuali progressi mercy presenti al momento della scrittura; DELETE ripetuto restituisce `404` senza un secondo rimborso.

L’anteprima è una fotografia validata alla scrittura, non una copia aggiornata automaticamente dopo modifiche al catalogo F04. GET conserva la fotografia; un nuovo PUT deve usare i dati correnti. Il versionamento del catalogo resta aperto; le anteprime non sono aggiornate automaticamente. Le nuove rotte di inventario e apertura sono descritte sotto. L’acquisto accredita pacchetti chiusi tramite il contratto dedicato sotto; seed commerciale escluso.

### Schema della richiesta PackWriteRequest

POST e PUT inviano `Content-Type: application/json` e lo stesso oggetto completo. La tabella descrive i campi JSON del record [PackWriteRequest](../../springboot_app/app/src/main/java/webapp/pack/PackWriteRequest.java); PUT non è un aggiornamento parziale. `integer` indica un intero a 32 bit, con massimo `2147483647`.

| Campo | Tipo JSON | Obbligatorio | Vincoli |
| --- | --- | --- | --- |
| `id_artwork_pull_rates` | integer | Sì | Positivo; ID di una configurazione artwork esistente. |
| `pack_name` | string | Sì | Non vuoto e non composto soltanto da spazi. |
| `pack_description` | object | Sì | Tutti i campi della [descrizione strutturata](#Descrizione%20strutturata%20e%20prezzo%20del%20pacchetto); `example_pull` usa lo [schema completo della carta](#Schema%20JSON%20completo%20di%20una%20carta). Valgono i controlli su quantità, codici e anteprime descritti sopra. |
| `pack_price` | integer | Sì | Da `0` a `2147483647`. |
| `is_acquirable` | boolean | Sì | `true` oppure `false`, mai `null`. |
| `pack_image_url` | string oppure null | No | Se omesso vale `null`; nessun caricamento di immagini tramite questa API. |
| `allowed_factions` | array di stringhe | Sì | Codici esatti, distinti, non vuoti e non composti soltanto da spazi, esistenti nel catalogo fazioni attivo; `[]` non ammette carte ed è rifiutato. |
| `allowed_types` | array di stringhe | Sì | Stessi vincoli per il catalogo tipi; `[]` non ammette carte ed è rifiutato. |
| `allowed_expansions` | array di stringhe | Sì | Stessi vincoli per il catalogo espansioni; `[]` non ammette carte ed è rifiutato. |
| `allowed_rarities` | array di stringhe | Sì | Stessi vincoli per il catalogo rarità; `[]` non ammette carte ed è rifiutato. |
| `mercy_threshold` | integer oppure null | No | Positivo quando presente; mercy richiede anche rarità non nulla. |
| `mercy_rarity` | integer oppure null | No | ID positivo di una rarità esistente e non cancellata; omesso vale `null`. Mercy richiede soglia e rarità non nulle. |
| `pack_slots` | array di integer | Sì | ID positivi di tipi validi; lunghezza uguale a `pack_description.card_number`; ordine significativo e ripetizioni ammesse. |

### Schema della risposta PackView

GET dettaglio, POST e PUT restituiscono lo stesso oggetto JSON [PackView](../../springboot_app/app/src/main/java/webapp/pack/PackView.java). GET lista restituisce un array di questi oggetti, senza involucro di paginazione. Ogni oggetto espone tutti i campi di `PackWriteRequest` al primo livello, inclusi i due campi nullable, più i seguenti campi di sola lettura; non contiene una proprietà `definition`.

| Campo aggiuntivo | Tipo JSON | Contenuto |
| --- | --- | --- |
| `id` | integer | Identificativo del pacchetto. |
| `artwork_pull_rates` | object | Configurazione collegata da `id_artwork_pull_rates`; schema sotto. |
| `slot_pull_types` | array di object | Una voce per tipo usato in `pack_slots`, ordinata per `id`; schema sotto. |

| Campo di `artwork_pull_rates` | Tipo JSON | Contenuto |
| --- | --- | --- |
| `id` | integer | ID della configurazione. |
| `standard`, `extended_art`, `full_art`, `alternate_art` | number | Quattro proprietà obbligatorie: percentuali da 0 a 100, precisione SQL `decimal(7,4)` e somma 100. |

| Campo di ogni elemento di `slot_pull_types` | Tipo JSON | Contenuto |
| --- | --- | --- |
| `id` | integer | ID del tipo di slot. |
| `description` | string | Descrizione del tipo. |
| `pull_rates` | array di object | Distribuzione per rarità, ordinata per `id_rarity`; ogni elemento contiene i tre campi sotto. |
| `pull_rates[].id_rarity` | integer | ID della rarità. |
| `pull_rates[].rarity_code` | string | Codice della rarità. |
| `pull_rates[].probability` | number | Percentuale da 0 a 100, precisione SQL `decimal(7,4)`; somma 100 per tipo. Le rarità omesse hanno probabilità zero. |

Gli oggetti annidati seguono i record [ArtworkPullRates](../../springboot_app/app/src/main/java/webapp/pack/ArtworkPullRates.java) e [SlotPullType](../../springboot_app/app/src/main/java/webapp/pack/SlotPullType.java); gli esempi sono nelle sezioni [artwork](#Tipi%20e%20probabilit%C3%A0%20degli%20artwork) e [slot](#Slot%20e%20probabilit%C3%A0%20per%20rarit%C3%A0). Questi campi di sola lettura non fanno parte del payload POST/PUT. DELETE riuscita non restituisce JSON; gli errori mantengono il formato Problem Details indicato sopra.


### Slot e probabilità per rarità

La migrazione [V13](../../springboot_app/app/src/main/resources/db/migration/V13__normalize_f03_slot_pull_rates.sql) sostituisce `packs.slot_pulls` con tre tabelle:

| Tabella | Colonne e vincoli |
| --- | --- |
| `slot_pull_type` | `id` generato, `description` obbligatoria; una distribuzione riutilizzabile. |
| `type_pull_rate` | PK `(id_slot_pull_type, id_rarity)`; FK al tipo e a `rarities.id`; `probability decimal(7,4)` obbligatoria fra 0 e 100. Le rarità omesse hanno probabilità zero. |
| `pack_slots` | PK `(id_pack, slot_position)`; posizione positiva e FK al tipo. La posizione parte da 1 nelle scritture API. Cancellare un pacchetto elimina i suoi slot, conservando i tipi. |

La somma delle probabilità di ciascun tipo deve essere `100.0000`. Trigger differibili controllano il totale al commit, anche per tipi vuoti, rimozioni e spostamenti di righe. Creare il tipo e tutte le sue probabilità nella stessa transazione; non sono introdotti endpoint HTTP di gestione dei tipi. Le scritture sulle probabilità acquisiscono un lock sul tipo; un tipo referenziato da `pack_slots` non può cambiare distribuzione. Per una revisione si crea un nuovo tipo e si cambia il riferimento di un pacchetto modificabile. Le FK impediscono riferimenti inesistenti; la cancellazione di un tipo non referenziato elimina le sue probabilità. Aggiungere una rarità non richiede nuove colonne.

Esempio di frammento API con ID e valori illustrativi:

```json
{
  "is_acquirable": true,
  "pack_slots": [4, 4, 9],
  "slot_pull_types": [
    {"id": 4, "description": "Slot base", "pull_rates": [
      {"id_rarity": 1, "rarity_code": "COMMON", "probability": 98.9999},
      {"id_rarity": 2, "rarity_code": "RARE", "probability": 1.0001}
    ]},
    {"id": 9, "description": "Slot raro", "pull_rates": [
      {"id_rarity": 2, "rarity_code": "RARE", "probability": 100.0000}
    ]}
  ]
}
```

`slot_pull_types` contiene una sola rappresentazione per tipo, ordinata per ID; le righe delle probabilità sono ordinate per ID di rarità, senza attribuire agli ID un rango. POST/PUT inviano `pack_slots` e `is_acquirable`; `slot_pull_types` è solo in risposta. Il vecchio formato `slot_pulls` non è più il contratto di scrittura. L’amministrazione ricava gli ID dalle tabelle, come per gli artwork.

La precisione SQL è `decimal(7,4)`: tre cifre intere più quattro decimali, equivalente a `numeric(7,4)`. `decimal(3,4)` non descrive il dominio richiesto. Sono percentuali, non frazioni; artwork e rarità usano lo stesso tipo e totali 100. I valori con più di quattro decimali sono arrotondati da PostgreSQL alla scala della colonna.

V13 conserva pacchetti e percentuali artwork esistenti, inizializza `is_acquirable = false` e non inventa tipi o probabilità. Configurazioni legacy `{}`/`[]` diventano assenza di slot; prima di un nuovo PUT occorre fornire gli ID coerenti con `card_number`. Un JSON legacy non vuoto blocca e annulla la migrazione con errore esplicito: salvare il contenuto e predisporre una mappatura verificata prima di riprovare. V1–V8 e V11 di `dev` restano inalterate; V12/V13 sostituiscono le precedenti versioni F03 nella nuova catena.


## Tipi e probabilità degli artwork

Decisione confermata dall’utente il **2026-11-24**: quattro tipi di artwork, senza Showcase e senza progettare o introdurre un livello di effetti speciali. I codici sono esatti e distinguono maiuscole e minuscole:

| Tipo | `CARDS_ARTS.art_code` | Colonna in `ARTWORK_PULL_RATES` | Percentuale iniziale |
| --- | --- | --- | --- |
| Standard Art | `STANDARD` | `standard` | 81,00% |
| Extended Art | `EXTENDED_ART` | `extended_art` | 10,00% |
| Full Art | `FULL_ART` | `full_art` | 7,50% |
| Alternate Art | `ALTERNATE_ART` | `alternate_art` | 1,50% |

`artwork_pull_rates` contiene ID intero generato e quattro percentuali esatte `decimal(7,4)` dopo V13, obbligatorie, ciascuna fra 0 e 100 e con somma 100. V12 inizializza la configurazione approvata; V13 estende la precisione senza cambiarla. Ogni pacchetto richiede `id_artwork_pull_rates`, FK diretta obbligatoria senza default. Più pacchetti possono riutilizzare una configurazione; eliminarne uno non elimina la configurazione. Una configurazione referenziata non può essere eliminata.

POST e PUT dei pacchetti richiedono `id_artwork_pull_rates` positivo ed esistente; riferimenti mancanti o sconosciuti restituiscono `400`. Non vengono create o modificate probabilità implicitamente. Per scegliere una configurazione, l’amministrazione legge gli ID da `SELECT id, standard, extended_art, full_art, alternate_art FROM artwork_pull_rates ORDER BY id`; non si assume che una sequenza sia consecutiva. La gestione delle righe avviene a livello database; non è introdotto un CRUD HTTP separato. Cambiare configurazione di un pacchetto significa sostituirne la FK, senza aggiornare una riga condivisa; possesso e mercy seguono la riconciliazione amministrativa descritta sopra.

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

Le percentuali descrivono una singola estrazione del tipo di artwork, distinta dalla rarità di gioco; non stabiliscono da sole una probabilità complessiva per pacchetto. Ogni slot estrae l’artwork indipendentemente; variante assente risolta con `STANDARD` della stessa carta. I filtri sono cumulativi. Estrazione e apertura seguono il contratto implementato sotto.

V5 crea direttamente le arti `STANDARD` con immagini e ID generati. V12 aggiunge il `CHECK` sui quattro codici canonici senza rinominare arti o modificare immagini, collezione e riferimenti dei mazzi. Eventuali dati esterni con alias legacy richiedono una mappatura esplicita prima della migrazione; non si perdono varianti automaticamente. Le migrazioni ripulite V1–V8 e V11 di `dev` restano inalterate.

## Obiettivo e decisioni note

- Ogni offerta acquistabile contiene esattamente una unità di un pacchetto, senza insiemi di pacchetti differenti.
- Scaglie, Nuclei Rift e Rotami sono risorse distinte secondo la [terminologia economica confermata](../Digital/economia-crediti-pacchetti.md#Risorse%20economiche), rappresentate rispettivamente da `INVENTORY.cobalt_shards`, `rift_cores` e `scraps`. Gli acquisti usano soltanto Scaglie (`cobalt_shards`).
- Prezzo e configurazione sono gestibili dal backoffice; lo schema v1.1 include `PACKS.pack_price int` per il prezzo di un pacchetto. Importi in definizione; risorsa `cobalt_shards`. Il pacchetto rappresenta la propria offerta, senza tabella separata.
- Distinguere offerta, singolo pacchetto e carte risultanti dall'apertura.
- La traccia richiede carte casuali con quantità fissate per livello di rarità; valori e distribuzione non sono ancora definiti.
- Per i pacchetti con mercy abilitato, il conto alla rovescia avanza all'apertura del pacchetto corrispondente. Ogni pesca della rarità richiesta o superiore, naturale o garantita, lo riporta alla soglia del pacchetto.

## Dati, transazioni e disponibilità

- `PACKS.id` e `INVENTORY.id` sono `int` auto-incrementali. I riferimenti a utente e pacchetto sono interi assegnati dai record esistenti, senza auto-incremento autonomo.
- `PACKS` conserva nome obbligatorio, `pack_description jsonb` obbligatorio secondo il contratto sotto, `pack_price int` obbligatorio senza default, `is_acquirable boolean NOT NULL DEFAULT false`, immagine facoltativa e JSONB obbligatori per fazioni, tipi, espansioni e rarità. I filtri usano i codici dei cataloghi. Gli slot e le probabilità per rarità sono relazionali secondo la sezione sopra; non è presente una colonna `slot_pulls`. La validazione dei JSON descrittivi e dei filtri resta applicativa, senza FK annidate.
- `INVENTORY_PACKS` usa PK `(id_inventory, id_pack)`. `item_quantity` è obbligatoria, senza default e con `CHECK > 0`; consumare l'ultimo pacchetto elimina la riga. Una coppia assente vale zero dopo una lettura riuscita.
- I tre saldi di `INVENTORY` sono `int`, obbligatori, con default `0` e senza nuovi `CHECK` di non negatività: il backend verifica quantità, disponibilità, saldo finale non negativo e risultati nel dominio `int`.
- L'acquisto deve leggere il prezzo autorevole da `PACKS.pack_price` e conoscere la risorsa richiesta dall'offerta, addebitare il relativo saldo disponibile e incrementare i pacchetti nello stesso commit. Il pagamento usa soltanto `cobalt_shards`, senza selettore multirisorsa. L'apertura decrementa i pacchetti e incrementa `COLLECTIONS` per l'esatta coppia carta/arte estratta; errori annullano tutta l'operazione.
- Le scritture rispettano il [protocollo transazionale comune](Database/database-schema-v1.1.md#Protocollo%20transazionale%20comune), con guard utente e letture aggiornate dopo i lock. `TRADE_TOKEN_RESERVATIONS.token_amount` non distingue una delle tre risorse: il suo abbinamento a un saldo, oppure un'estensione del contratto di prenotazione, deve essere definito prima di proteggere addebiti monetari condivisi. L'apertura non addebita nuovamente il prezzo.

## Descrizione strutturata e prezzo del pacchetto

Contratto dati confermato su richiesta dell’utente il **2026-11-23**, usato dal CRUD F03/F10 implementato sopra. Questa sezione definisce i dati; percorsi e autorizzazioni HTTP sono mantenuti nella sezione del catalogo. `pack_price` è una colonna distinta dalla descrizione. `pack_description` passa da testo a oggetto JSON, non a una stringa contenente JSON: è una modifica incompatibile con un client che legga direttamente una stringa. Il testo va letto da `pack_description.pack_description`.

| Campo | Tipo | Contenuto |
| --- | --- | --- |
| `pack_price` | integer | Prezzo del singolo pacchetto, obbligatorio e senza default; il CRUD richiede un valore non negativo a 32 bit. Risorsa di pagamento: Scaglie; importi commerciali da definire. |
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

`example_pull` è un’anteprima dimostrativa, non il risultato di un’apertura né un accredito alla collezione. I campi descrittivi di garanzia e miglior pesca sono metadati frontend; non determinano le probabilità e non sostituiscono i tipi di slot. Restano i controlli strutturali su quantità, forma, tipi e riferimenti delle anteprime. I campi `granted_*` descrivono la garanzia del pacchetto e non sostituiscono soglia o progresso mercy. JSONB valida la sintassi JSON ma non questi controlli; non sono introdotti nuovi `CHECK` SQL. Anteprime aggiornate solo dagli amministratori. Le regole di estrazione sopra sono implementate dal servizio di apertura.

Il modello aggiornato è nella [specifica database](Database/database-schema-v1.1.md#Inventario%20e%20pacchetti) e nel relativo DBML. La revisione documentale del 2026-11-23 non applicava SQL; la V12 di questo branch crea `PACKS`, come descritto sopra. Eventuali dati preesistenti in altre installazioni richiederanno una conversione esplicita della descrizione e la valorizzazione del prezzo, senza inventare default.

## Mercy sulle aperture

Le regole seguenti sono implementate dal servizio di apertura. PUT/DELETE amministrativi continuano a eliminare i progressi del pacchetto.

Regole confermate dall'utente il **2026-11-09**, rappresentate anche nella [specifica autonoma del database](Database/database-schema-v1.1.md#Inventario%20e%20pacchetti).

- `PACKS.mercy_threshold` è nullable e positivo quando presente. `PACKS.mercy_rarity` è una FK nullable a `RARITIES.id`, condivisa da tutti gli utenti del pacchetto. Mercy è attivo solo con entrambi i campi non nulli; altrimenti non si inizializzano o avanzano progressi. Nessun default commerciale.
- `USER_MERCY` contiene soltanto `id_user`, `id_pack` e `mercy_counter`; PK composta e FK obbligatorie a utente e pacchetto, senza auto-incremento. La rarità non è più nel progresso utente.
- Il progresso persiste dopo il consumo dell’ultimo pacchetto. Alla prima apertura attiva il backend inizializza il contatore dalla soglia corrente e applica anche l’apertura in corso.
- Ordine `COMMON < RARE < EPIC < LEGENDARY`, senza confrontare gli ID. Una carta di rarità uguale o superiore a `PACKS.mercy_rarity` qualifica per il reset.
- Calcolare `remaining = max(mercy_counter - 1, 0)` e generare normalmente tutti gli slot. Se una carta qualifica naturalmente, conservare le pescate e ripristinare la soglia, anche prima della scadenza.
- Se nessuna carta qualifica e `remaining == 0`, sostituire soltanto la carta dell’ultimo slot con una carta equiprobabile di rarità esattamente `PACKS.mercy_rarity`, rispettando tutti i filtri. La sostituzione supera la distribuzione ordinaria dell’ultimo `slot_pull_type`, senza aggiungere carte. Artwork indipendente e fallback `STANDARD` se la variante manca. Poi ripristinare la soglia.
- Senza esito qualificante e senza scadenza, salvare `remaining`. Un contatore già zero segue la stessa garanzia. Con soglia dieci, nove aperture non qualificanti lasciano `9, 8, …, 1`; la decima garantisce e salva `10`.
- Verificare contatore non negativo e non superiore alla soglia, oltre alla presenza di una carta valida della rarità esatta configurata per la sostituzione. Configurazione incompatibile: errore e rollback dell’intero lotto. I valori del seed restano in definizione; l’origine della rarità è risolta dal campo del pacchetto.

### Apertura atomica

Il servizio apre da uno a dieci pacchetti nella stessa transazione. La funzione `openOnePack(configuration, mercyState)` usa la sorgente casuale iniettata e restituisce carte e prossimo stato mercy senza aprire o committare transazioni autonome; il lotto usa lo stato restituito per l’iterazione successiva. Consumo dei pacchetti, accredito della collezione e stato mercy condividono il commit; un errore annulla tutto il lotto.

Il PUT amministrativo elimina i progressi esistenti: la successiva apertura li inizializza dalla soglia corrente, usando la rarità del pacchetto. La garanzia è risolta nella composizione del singolo pacchetto, senza ricompense differite.

## Inventario e apertura differita: contratto HTTP

ID: F03, dipendenze F01 per identità/ruoli e F04 per carte/varianti. Stato: **Implementato** il 2026-11-27. Il percorso conserva il lock semplice e la risposta diretta richiesti dall’utente.

### Decisioni confermate e perimetro

- Il chiamante legge il proprio inventario e i pacchetti posseduti; apre un solo tipo di pacchetto per richiesta.
- `quantity` è un intero da **1 a 10**, estremi inclusi, validato dal backend prima degli effetti di dominio.
- Un lock di riga non attendente su `inventory_packs` protegge la coppia inventario/pacchetto durante l’intero lotto. Se la riga è già bloccata, la richiesta viene rifiutata con `409 OPENING_IN_PROGRESS`, senza accodarla.
- Dopo il lock il backend verifica quantità effettiva e configurazione corrente. La lettura dell’inventario del frontend non riserva copie.
- Il lotto è atomico: un errore annulla consumo, accrediti e modifiche mercy di tutte le aperture.
- Le copie estratte incrementano solo `ownedReplica`; `tradableReplica` resta invariato su righe esistenti ed è `0` su nuove righe, anche per duplicati.
- Il server committa prima di restituire le carte nella risposta HTTP. Il risultato della pesca resta in memoria per questa risposta.
- Su richiesta esplicita di Chiampo, chiavi di richiesta, deduplicazione dopo il completamento, persistenza dei risultati, recupero delle pescate e relative verifiche sono esclusi. Non vengono introdotti registro aperture, durata di conservazione o cleanup dei risultati.
- L’algoritmo di estrazione e l’acquisto sono implementati; seed commerciale escluso.

### Autorizzazioni e percorsi

Tutte le rotte richiedono Bearer JWT F01 e ruolo `USER`; non esiste una gerarchia implicita con `ADMIN`. Il proprietario deriva esclusivamente dal principal autenticato: nessun parametro permette di selezionare un altro utente o inventario.

| Metodo e percorso | Richiesta | Risposta |
| --- | --- | --- |
| `GET /api/users/me/inventory` | Nessun corpo o parametro di query previsto | `200`, `InventoryView` |
| `GET /api/users/me/inventory/packs` | Nessun corpo o parametro di query previsto | `200`, array completo di `OwnedPackView`, per `packId` crescente |
| `POST /api/users/me/pack-openings` | JSON `OpenPacksRequest` | `200`, `OpenPacksResponse`, senza header `Location` |

La collezione resta disponibile tramite `GET /api/users/me/collections`, secondo [F04](collezione-carte.md#Contratto%20HTTP%20condiviso). L’inventario include anche le definizioni possedute con `is_acquirable = false`; tale flag limita nuove acquisizioni e non impedisce l’apertura del possesso esistente. Il dettaglio è incluso nella lettura dell’inventario perché il catalogo USER nasconde queste definizioni.

### Servizi e transazioni implementati

- [PackOpeningController](../../springboot_app/app/src/main/java/webapp/pack/PackOpeningController.java) espone le tre rotte solo a `USER`; proprietario dal principal e JSON validato senza coercizioni o proprietà aggiuntive.
- [PackOpeningService](../../springboot_app/app/src/main/java/webapp/pack/PackOpeningService.java) acquisisce prima il lock di possesso `NOWAIT`, poi la definizione `FOR SHARE NOWAIT`. Successivamente protegge l’utente attivo con lock condiviso e carica la configurazione corrente. Nessuna attesa precede il tentativo sul possesso. Il lock condiviso utente coordina la cancellazione F01 senza serializzare tra loro tutte le aperture dello stesso utente.
- [PackConfigurationValidationService](../../springboot_app/app/src/main/java/webapp/pack/PackConfigurationValidationService.java) applica gli stessi filtri a POST, PUT e apertura. Cataloghi cancellati esclusi; ogni rarità positiva e la rarità esatta mercy attiva richiedono carte eleggibili. Per quelle carte deve esistere `STANDARD`. Probabilità zero non richiedono carte; non si rinormalizzano probabilità per aggirare una configurazione invalida. Con mercy attivo sono ammesse soltanto le quattro rarità ordinate confermate.
- Il caricamento protegge con lock condivisi configurazione artwork, tipi/probabilità, carte, riferimenti attivi e arti. Le modifiche amministrative alle carte attendono il rilascio dei relativi lock. Non è introdotto un guard catalogo globale o il futuro mercato F06.
- [PackDrawingService](../../springboot_app/app/src/main/java/webapp/pack/PackDrawingService.java) estrae su un milione di unità intere, equivalenti a percentuali con quattro decimali; una sorgente `SecureRandom` decide rarità, carta equiprobabile e artwork indipendente. La funzione per singolo pacchetto non ha commit o scritture.
- Gli accrediti sono aggregati per carta/arte e applicati in ordine di ID con upsert SQL additivo. Le righe nuove iniziano con scambiabilità zero; sulle righe esistenti cambia solo `owned_replica`. Overflow o qualsiasi errore annullano anche le scritture precedenti del lotto. Consumo e stato mercy condividono lo stesso commit; il risultato diventa visibile al controller solo dopo il commit Spring.

### Schemi delle letture e della richiesta

I nuovi DTO utente usano camelCase; il [PackView esistente](#Schema%20della%20risposta%20PackView) conserva i propri campi snake_case. Gli identificativi sono interi positivi a 32 bit, massimo `2147483647`. Tutti i campi elencati sono obbligatori, salvo nullabilità esplicita.

`InventoryView`:

```json
{
  "cobaltShards": 1200,
  "riftCores": 50,
  "scraps": 80
}
```

Sono tre saldi interi distinti letti da `inventory`; i valori sono illustrativi. L’inventario mancante per un account valido è un errore di coerenza (`500`), non una lettura riuscita con zeri inventati; F01 crea già la riga alla registrazione.

| Campo di `OwnedPackView` | Tipo | Regola |
| --- | --- | --- |
| `packId` | integer | Positivo; uguale a `pack.id`. |
| `quantity` | integer | Positivo; valore di `inventory_packs.item_quantity`. |
| `pack` | object | PackView completo anche per definizioni non acquisibili. |

Nessuna paginazione. Un inventario senza pacchetti restituisce `200` con `[]`; un errore di lettura non equivale a un elenco vuoto.

`OpenPacksRequest`, con `Content-Type: application/json`:

```json
{
  "packId": 401,
  "quantity": 2
}
```

| Campo | Validazione |
| --- | --- |
| `packId` | Obbligatorio, intero positivo a 32 bit. |
| `quantity` | Obbligatorio, intero da `1` a **`10`**. |

Rifiutare null, stringhe o frazioni al posto degli interi, overflow e proprietà aggiuntive. Quantità `0`, negative o superiori a `10` producono `400 INVALID_REQUEST`, senza estrazioni o scritture di dominio. Non richiedere chiavi di richiesta. Il client non invia carte scelte, probabilità, stato mercy o saldi autorevoli.

### Schema della risposta di apertura

`OpenPacksResponse` è il risultato immediato della richiesta, senza identificativo storico o risorsa di recupero:

| Campo | Tipo | Regola |
| --- | --- | --- |
| `packId` | integer | Tipo di pacchetto aperto. |
| `quantityOpened` | integer | Quantità interamente aperta, da `1` a `10`. |
| `remainingPackQuantity` | integer | Quantità non negativa dopo il lotto. |
| `packs` | array di `OpenedPackView` | Esattamente `quantityOpened` elementi nell’ordine di apertura. |
| `mercy` | object oppure null | Stato finale del lotto; `null` se disabilitato. |

`OpenedPackView` contiene `sequence` intero da `1` a `quantityOpened` e `cards`, array ordinato di `PulledCardView`. Ogni estrazione è una voce distinta, compresi i duplicati.

| Campo di `PulledCardView` | Tipo e regola |
| --- | --- |
| `slot` | Intero positivo, posizione nel singolo pacchetto. |
| `cardId`, `cardArtId` | Interi positivi dell’esatta coppia carta/variante. |
| `artCode` | `STANDARD`, `EXTENDED_ART`, `FULL_ART` oppure `ALTERNATE_ART`. |
| `name` | Stringa, nome carta. |
| `rarity` | Riferimento F04 con `id`, `code` e `name`, mantenendo la nullabilità F04 dei dati descrittivi. |
| `imageUrl` | Stringa oppure `null`. |

Se attivo, `mercy` contiene `threshold` intero positivo, `counter` intero fra zero e soglia e `rarity` nel formato di riferimento F04. La rarità proviene da `packs.mercy_rarity`; la garanzia sostituisce l’ultimo slot secondo la regola approvata.

### Lock semplice sulle aperture concorrenti

Nella transazione, la riga da proteggere è individuata tramite principal e pacchetto, non tramite un inventario scelto dal client:

```sql
SELECT ip.item_quantity
FROM inventory_packs ip
JOIN inventory i ON i.id = ip.id_inventory
WHERE i.id_user = :authenticatedUserId
  AND ip.id_pack = :packId
FOR UPDATE OF ip NOWAIT;
```

Il lock riguarda la coppia inventario/pacchetto: richieste su altre righe non sono rifiutate da questo solo lock. Acquisirlo senza prima accodare la richiesta su un altro lock del flusso di apertura. Una riga già occupata produce rollback e `409 OPENING_IN_PROGRESS`; non usare un’attesa o un retry automatico lato server per quella richiesta.

Il lock dura fino a commit o rollback e protegge controllo e consumo delle quantità. Se la riga è assente, verificare l’esistenza della definizione: `404 PACK_NOT_FOUND` se inesistente, altrimenti `409 INSUFFICIENT_PACK_QUANTITY` con disponibilità zero. Non serve bloccare una riga assente per rifiutare un’apertura senza possesso.

Questa protezione rifiuta le sovrapposizioni che incontrano la riga occupata. Le richieste ricevute dopo il rilascio del lock vengono valutate come nuove aperture; ulteriori problemi di retry e recupero sono esclusi per decisione esplicita di Chiampo.

Il lock del possesso precede `FOR SHARE NOWAIT` sulla definizione. Se un PUT/DELETE ha già acquisito il lock esclusivo della definizione, l’apertura risponde `503 TEMPORARILY_UNAVAILABLE` senza pescare o consumare. Se l’apertura ha già protetto la definizione, PUT/DELETE attende il suo commit: PUT conserva il possesso residuo e cancella mercy; DELETE rimborsa soltanto le copie ancora chiuse. Non esiste inversione con attesa da parte dell’apertura sulla definizione amministrativa.

Il rifiuto immediato riguarda il possesso e la definizione. Gli upsert della collezione possono attendere scritture sulla stessa variante; sommano al valore committato dal concorrente e conservano `tradable_replica`. L’ordine stabile carta/arte coordina aperture di tipi diversi che condividono varianti. Non sono introdotti nuovi lock globali o scrittori F06.

### Approccio confermato per mercy nei lotti

Usare una sola funzione per il singolo pacchetto, senza commit interni, passando il nuovo stato all’iterazione successiva:

```text
begin transaction
quantityOwned = lockOwnedPackNowait(authenticatedUser, packId)
lockPackConfigurationShareNowait(packId)
lockActiveUserShared(authenticatedUser)
pack = readCurrentPackConfiguration(packId)
requireEnoughCopies(quantityOwned, quantity)
state = loadOrInitializeActiveMercy(user, pack)
results = []
for sequence in 1..quantity:
    result = openOnePack(pack, state, drawSource)
    results.append(result.cards)
    state = result.nextMercy
grantOwnedCardsOnly(results)
consumeOwnedPacks(quantity)
saveActiveMercy(state)
commit
return buildOpenPacksResponse(results, state)
```

Pseudocodice del comportamento implementato da `PackOpeningService` e `PackDrawingService`. Con mercy disabilitato non inizializzare, avanzare o cancellare progressi inattivi. Il risultato delle carte resta in memoria; sono persistiti soltanto possesso, consumo e progresso attivo.

Con soglia `10`, contatore iniziale `3` e nessuna pesca qualificante anticipata, quattro aperture producono gli stati finali `2, 1, 10, 9`: la terza attiva la garanzia e resetta la soglia. Una pesca naturale qualificante resetta subito il contatore nell’iterazione corrente.

### Sequenza transazionale implementata

1. Autenticare, autorizzare e validare identificativi e quantità `1–10`.
2. Iniziare la transazione e tentare il lock della riga di possesso con `NOWAIT`; rifiutare il conflitto senza accodare la richiesta.
3. Verificare esistenza e configurazione corrente del pacchetto e disponibilità effettiva sotto lock.
4. Aprire sequenzialmente i pacchetti, mantenendo in memoria carte e stato mercy intermedio.
5. Incrementare solo `ownedReplica` per carta/arte; conservare `tradableReplica` esistente o inizializzarlo a zero. Proteggere incrementi concorrenti e overflow.
6. Consumare la quantità richiesta; eliminare la riga `inventory_packs` se il residuo è zero, conservando il progresso mercy.
7. Salvare il progresso mercy attivo e committare tutto il lotto. Nessun commit intermedio o registro delle pescate.
8. Restituire `200` con le carte e le quantità risultanti, dopo il commit.

Qualsiasi errore prima del commit annulla l’intero lotto. Il servizio di estrazione non scrive sul database; restituisce carte e stato al servizio transazionale del lotto. L’integrazione con i lock già usati da PUT/DELETE amministrativi deve mantenere operazioni coerenti sul possesso; non introdurre attese preliminari che trasformino il rifiuto concorrente in un’apertura accodata.

### Codici HTTP ed errori

Le risposte applicative di errore usano `application/problem+json` con campo stabile `code`; preservare la semantica di autenticazione F01 e verificare il formato prodotto dalla catena di sicurezza.

| HTTP | `code` | Condizione |
| --- | --- | --- |
| `400` | `INVALID_REQUEST` | JSON/campi non validi, quantità fuori `1–10`, ID non positivo, overflow o proprietà aggiuntive. |
| `401` | `UNAUTHENTICATED` | Bearer assente, scaduto, revocato o non valido. |
| `403` | `FORBIDDEN` | Ruolo non autorizzato. |
| `404` | `PACK_NOT_FOUND` | Definizione inesistente. |
| `409` | `INSUFFICIENT_PACK_QUANTITY` | Quantità posseduta insufficiente, anche zero. |
| `409` | `OPENING_IN_PROGRESS` | Riga di possesso già bloccata; richiesta rifiutata senza attesa. |
| `409` | `PACK_CONFIGURATION_INVALID` | Configurazione incapace di produrre un’apertura valida. |
| `415` | `UNSUPPORTED_MEDIA_TYPE` | POST con tipo di contenuto non JSON. |
| `500` | `INTERNAL_ERROR` | Errore inatteso o violazione degli invarianti. |
| `503` | `TEMPORARILY_UNAVAILABLE` | Indisponibilità tecnica temporanea o definizione bloccata da PUT/DELETE amministrativo. |

Letture e apertura riuscite restituiscono `200`. Gli errori contengono `type`, `title`, `status`, `detail`, `instance` e `code`; `instance` identifica il percorso richiesto. `401` include `WWW-Authenticate: Bearer`. In caso di insufficienza includere anche `packId`, `requestedQuantity` e `availableQuantity` del chiamante. OpenAPI pubblica gli schemi delle risposte e i codici applicativi nelle descrizioni degli errori. Non esporre SQL, stack trace o dati altrui.

### Criteri di accettazione e dipendenze aperte

- Letture isolate per principal, ruoli corretti, inventario vuoto distinto da errore e possesso non acquisibile incluso.
- Quantità `1` e `10` accettate con precondizioni valide; input fuori limite, null, stringhe, frazioni e overflow rifiutati senza effetti. OpenAPI espone `minimum: 1` e `maximum: 10`.
- Con una transazione che mantiene il lock di possesso, un secondo invio sulla stessa riga riceve `409 OPENING_IN_PROGRESS` senza estrazioni o effetti di dominio; dopo il rilascio una nuova richiesta può procedere se ha copie sufficienti.
- Errore intermedio: rollback di tutte le carte, del consumo e del progresso mercy del lotto.
- Variante nuova: `tradableReplica = 0`; due copie aggiunte a una variante con contatori `(4, 1)` producono `(6, 1)`.
- Con estrazioni controllate, il lotto e applicazioni singole della transizione mercy producono gli stessi stati intermedi e carte, includendo reset naturali e garanzia.
- Nessun header di deduplicazione, identificativo storico, risultato delle pescate persistito o endpoint di recupero.
- **DA DEFINIRE:** seed commerciali di pacchetti, slot, prezzi, soglie e rarità. Queste dipendenze non cambiano il limite, l’atomicità o il lock confermati.

Riferimenti tecnici: [lock di riga PostgreSQL](https://www.postgresql.org/docs/18/sql-select.html#SQL-FOR-UPDATE-SHARE), [durata dei lock](https://www.postgresql.org/docs/18/explicit-locking.html), [Problem Details](https://www.rfc-editor.org/rfc/rfc9457.html).


## Acquisto pacchetti: contratto HTTP

Implementato il **2026-11-27** su richiesta dell’utente. Il pagamento accredita soltanto pacchetti chiusi; nessuna scrittura di collezione, artwork, mercy, Nuclei Rift o Rotami. Non si applica il limite di dieci previsto per l’apertura.

### Richiesta e risposta di acquisto

`POST /api/users/me/pack-purchases`: Bearer JWT, ruolo esplicito `USER`, proprietario dal principal; `ADMIN` non eredita `USER`. `Content-Type: application/json`. Nessun parametro per selezionare un altro proprietario.

```json
{"packId": 401, "quantity": 2}
```

Entrambi i campi sono obbligatori e interi positivi a 32 bit, massimo `2147483647`. Rifiutati null, stringhe, frazioni, overflow e proprietà aggiuntive. Quantità e possesso finale sono limitati dal dominio degli interi e dal saldo disponibile. Prezzo zero ammesso come nel CRUD; nessun limite commerciale aggiuntivo.

Risposta `200 OK`, senza `Location`, solo dopo il commit:

```json
{
  "packId": 401,
  "quantity": 2,
  "unitPrice": 300,
  "totalCost": 600,
  "ownedQuantity": 5,
  "cobaltShards": 400,
  "reservedCobaltShards": 150,
  "availableCobaltShards": 250
}
```

Valori illustrativi, non prezzi o seed approvati. `packId`, `quantity`, `unitPrice`, `ownedQuantity` e `cobaltShards` sono interi a 32 bit; costo e prenotazioni usano `long`. Tutti i campi sono obbligatori e non nullable. `ownedQuantity` è il possesso totale successivo all’acquisto. I saldi rappresentano lo stato al cutoff della transazione e possono cambiare dopo il commit. Il server applica il `pack_price` corrente letto sotto lock, anche se diverso dal prezzo visto prima dal client. Prezzo pagato restituito soltanto nella risposta, senza persistenza; DELETE amministrativo continua a rimborsare al prezzo corrente.

### Servizi condivisi, prenotazioni e transazione di acquisto

- [PackPurchaseService](../../springboot_app/app/src/main/java/webapp/pack/PackPurchaseService.java) delimita una sola transazione `READ_COMMITTED`: definizione `FOR SHARE NOWAIT`, utente attivo `USER` `FOR UPDATE NOWAIT`, inventario `FOR UPDATE NOWAIT`, possesso esistente `FOR UPDATE NOWAIT`. Il prezzo e `is_acquirable` sono riletti sotto lock; una definizione assente restituisce `404`, una non acquisibile `409`.
- [DebitService](../../springboot_app/app/src/main/java/webapp/economy/DebitService.java) è indipendente da F03 e richiede una transazione del chiamante (`MANDATORY`). Protegge utente attivo e inventario, calcola disponibilità e addebita solo Scaglie non prenotate, anche per costo zero. Non effettua commit autonomi e non autorizza una specifica funzionalità: il chiamante deve farlo. Per operazioni con più utenti, acquisire prima tutti i lock utente in ordine crescente.
- [ReservationReader](../../springboot_app/app/src/main/java/webapp/economy/ReservationReader.java) richiede transazione e lock esclusivo dell’utente già acquisito. Usa un solo `clock_timestamp()` del database dopo i lock e somma gli hold con `expires_at > cutoff`; all’uguaglianza sono scaduti. Tutti gli hold monetari riservano `cobalt_shards`. Nessuna prenotazione viene consumata dall’acquisto.
- Il lettore verifica versione, partecipante, consenso, importo esatto dell’offerta, stato `PENDING`, pubblicatore, flag condivisi e scadenza uguale all’annuncio. Un’offerta monetaria accettata e ancora valida deve avere il proprio hold; la sua assenza blocca la spesa. Hold vivi incoerenti, scadenze anticipate rispetto a un annuncio ancora vivo, somma oltre `long` o prenotazioni superiori al saldo impediscono l’addebito. Gli hold realmente scaduti non richiedono una pulizia preventiva per liberare disponibilità.
- Su istruzione dell’utente **non si aggiunge la migrazione F06**. Quando tutte e tre le tabelle `original_announces`, `current_negotiations`, `trade_token_reservations` sono assenti, nessun hold F06 è rappresentabile e la riserva vale zero. Se solo alcune tabelle esistono, il servizio restituisce `503`; errori SQL o tabelle malformate non diventano disponibilità libera. La presenza è verificata per chiamata nel search path corrente, senza cache.
- Ogni futuro scrittore F06 deve acquisire il medesimo lock esclusivo utente prima di creare, modificare, rilasciare o consumare hold. Non basta bloccare le righe di prenotazione già esistenti. Il regolamento di uno scambio deve consumare i propri hold nella stessa transazione prima di usare l’addebito ordinario, preservando quelli altrui.
- `available = cobalt_shards - reserved`; `totalCost = (long) pack_price * quantity`. L’addebito precede l’upsert additivo del possesso; il credito verifica il limite `2147483647`. Lock, overflow o qualsiasi errore precedente al commit annullano sia addebito sia credito, anche se già eseguiti.
- Il tentativo `NOWAIT` sul possesso evita l’attesa nell’ordine inverso rispetto all’apertura, che blocca prima il possesso e poi definizione/utente. PUT/DELETE amministrativi attendono il lock della definizione; un acquisto che incontra una modifica già in corso viene rifiutato. Nessun retry automatico lato servizio.

### Errori e richieste ripetute di acquisto

Errori `application/problem+json` con `code`, secondo la convenzione delle aperture:

| HTTP | Codice |
| --- | --- |
| `400` | `INVALID_REQUEST` |
| `401` | `UNAUTHENTICATED` |
| `403` | `FORBIDDEN` |
| `404` | `PACK_NOT_FOUND` |
| `409` | `PACK_NOT_ACQUIRABLE`, `INSUFFICIENT_SCAGLIE`, `PACK_QUANTITY_LIMIT_EXCEEDED` |
| `415` | `UNSUPPORTED_MEDIA_TYPE` |
| `500` | `INTERNAL_ERROR`, `RESERVATION_STATE_INVALID` |
| `503` | `TEMPORARILY_UNAVAILABLE`: lock occupato, guasto transitorio o storage F06 parziale |

`INSUFFICIENT_SCAGLIE` aggiunge `requiredScaglie`, `cobaltShards`, `reservedCobaltShards`, `availableCobaltShards`; se ci sono hold, il messaggio spiega che le Scaglie prenotate non sono spendibili. Inventario mancante per un utente valido è `500`, senza creazione implicita.

Su decisione esplicita dell’utente: nessuna chiave di idempotenza, deduplicazione, cronologia acquisti o rotta di recupero. Ogni richiesta riuscita ripetuta acquista di nuovo. Dopo perdita della risposta il client non deve reinviare automaticamente: può rileggere inventario e saldo, ma non attribuire uno stato a una specifica richiesta precedente. Un errore successivo al commit o una perdita di connessione non provano il rollback.

## Percorso acquisto, addebito, estrazione e collezione

1. Il client consulta il catalogo e conferma quantità e tipo; il prezzo applicato proviene dalla definizione corrente sotto lock.
2. Il [contratto di acquisto](#acquisto-pacchetti-contratto-http) addebita Scaglie disponibili e accredita pacchetti chiusi nello stesso commit.
3. L’utente sceglie successivamente quando aprire il possesso, secondo il contratto di apertura già implementato. Nessuna apertura immediata nell’acquisto.
4. L’apertura aggiorna possesso, collezione e mercy atomicamente e restituisce direttamente le carte. L’acquisto non modifica la collezione.
5. Dopo successo il client usa i saldi restituiti e può riallineare le letture dell’inventario. Dopo un esito incerto non ripete automaticamente l’acquisto: non esiste un identificativo storico con cui deduplicarlo o recuperarlo.

## Da definire

- Seed dei pacchetti, tipi di slot, prezzi, composizioni, soglie e rarità. Acquisizione delle risorse: **DA DEFINIRE**.
- Migrazione e scritture del mercato F06, escluse in questa attività. Prezzo pagato restituito nella risposta senza cronologia; hold monetari associati alle Scaglie. Ogni futuro scrittore deve rispettare il guard utente condiviso.
- Seed commerciali e integrazione frontend; inventario, validazione, estrazione, fallback Standard e sostituzione dell’ultimo slot sono già implementati nel backend.
- Ulteriori operazioni sulla scambiabilità e integrazione F06; l’apertura incrementa solo il possesso. PUT elimina i progressi relativi.
- Versionamento del catalogo. Nessun aggiornamento automatico delle anteprime. Persistenza delle pescate, deduplicazione e recupero restano esclusi.

## Collegamenti

- [Interfaccia negozio](../FrontEnd/acquisto-pacchetti.md)
- [Economia](../Digital/economia-crediti-pacchetti.md)
- [Backoffice](gestione-amministrativa.md)
- [Collezione](collezione-carte.md)
- [Indice BackEnd](README.md)
