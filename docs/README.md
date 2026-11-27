# Indice della documentazione

Verifica finale acquisto F03 del **2026-11-27**: **412 test backend superati**, zero fallimenti/errori/casi saltati, e JAR prodotto con JDK 21.0.11 e PostgreSQL 18.6 isolato. Incluse 49 prove di acquisto e 5 del lettore prenotazioni condiviso. Nessuna migrazione o modifica al database applicativo; frontend e flussi F06 esclusi. [Comando, copertura e limiti](../springboot_app/app/README.md#verifica-acquisto-f03-del-2026-11-27).

Aggiornamento F03/F06 del **2026-11-27**, su richiesta dell’utente: acquisto atomico implementato con `DebitService` e `ReservationReader` condivisi; hold monetari F06 in Scaglie. Migrazione F06 esclusa: assenza completa dello storage significa zero hold, storage parziale o incoerente blocca la spesa. Nessuna deduplicazione o cronologia acquisti; frontend e seed commerciale esclusi. [Contratto corrente](BackEnd/acquisto-pacchetti.md#acquisto-pacchetti-contratto-http). Le verifiche dell’apertura sotto precedono questo intervento.

Verifica dell’apertura F03 del **2026-11-27**: **358 test backend superati**, zero fallimenti/errori/casi saltati, e JAR prodotto con JDK 21.0.11 e PostgreSQL 18.6 temporaneo. Incluse 7 prove deterministiche di estrazione e 47 prove API/transazionali di apertura. Database applicativo non modificato; acquisto, seed commerciale e frontend F03 esclusi. [Comandi e copertura](../springboot_app/app/README.md#verifica-dellapertura-f03-del-2026-11-27).

Apertura F03 implementata il **2026-11-27**, su richiesta dell’utente: letture dell’inventario e apertura atomica di 1–10 pacchetti dello stesso tipo, filtri cumulativi, carte equiprobabili, artwork indipendente con fallback Standard e mercy sull’ultimo slot. Il servizio di validazione verifica i flussi amministrativi POST/PUT e ogni apertura. Nessuna nuova migrazione o seed commerciale; acquisto e frontend F03 restano esclusi. [Contratto e limiti](BackEnd/acquisto-pacchetti.md).

Aggiornamento F03 del **2026-11-27**, su decisione dell’utente: V12 crea direttamente `mercy_rarity` in `packs` come FK nullable; `user_mercy` conserva soltanto utente, pacchetto e contatore. CRUD e apertura usano questo campo; la garanzia sostituisce l’ultimo slot con la rarità esatta configurata quando necessaria. Acquisto non implementato. [Contratto corrente](BackEnd/acquisto-pacchetti.md).

Verifica precedente dell’integrazione del **2026-11-27**: **300 test backend**, **51 del motore** e **54 frontend** superati; JAR backend e build React prodotti. [Comandi, copertura e limiti](../springboot_app/app/README.md#verifica-dellintegrazione-f03-del-2026-11-27). Database applicativo non modificato.

La pianificazione F03 del **2026-11-26** è implementata nel backend il **2026-11-27**: [inventario e apertura differita](BackEnd/acquisto-pacchetti.md#Inventario%20e%20apertura%20differita%3A%20contratto%20HTTP), massimo dieci pacchetti, lock non attendente, lotto atomico, estrazione con fallback Standard e risposta diretta dopo commit. Nessuna nuova migrazione o seed commerciale; acquisto e frontend restano esclusi.

Precisazione F03/F04/D03: confermati incremento del solo `ownedReplica`, `tradableReplica` invariato o inizializzato a zero e funzione di apertura singola con mercy passato sequenzialmente nel lotto. Su richiesta esplicita di Chiampo sono esclusi chiavi di richiesta, deduplicazione dei retry, persistenza delle pescate, API di recupero e relative verifiche.

Aggiornamento F03 del **2026-11-26**: catalogo senza paginazione, visibilità per ruolo e riconciliazione amministrativa di possesso e mercy; DELETE rimborsa in `cobalt_shards` al prezzo corrente. [Contratto e limiti](BackEnd/acquisto-pacchetti.md#catalogo-implementato-migrazione-e-api). Le verifiche precedenti sotto sono storiche; l’apertura è disponibile dal 2026-11-27, acquisto escluso.

Integrazione F03 con `dev` del **2026-11-27**, su richiesta dell’utente: conservati i file ripuliti V1–V8 e V11; pacchetti e slot diventano V12/V13. Catena completa: undici script, V1–V8 e V11–V13. [Contratto e compatibilità F03](BackEnd/acquisto-pacchetti.md).

Aggiornamento F04/F05/F07 del **2026-11-27**, su richiesta dell’utente: riscritte le migrazioni esistenti. V3 definisce lo schema completo delle carte/arti; V5 inserisce subito il set CR01 con fazioni, energia e artwork definitivi. Rimossi `cards.code`, `transfer_and_discard` e `duration`; mantenuti gli ID generati da PostgreSQL, usati per le associazioni V8. V9/V10 del vecchio catalogo rimosse; le nuove V12/V13 F03 seguono V11 amicizie. Installazione prevista su database nuovo; nessuna ricreazione del database applicativo eseguita. Le verifiche datate precedenti descrivono la vecchia catena.

Verifica storica del cleanup di `dev` del **2026-11-27**, prima dell’integrazione F03: **263 test backend** e **51 test JUnit del motore** superati, senza fallimenti, errori o casi saltati; JAR backend prodotto. JDK 21.0.11 e PostgreSQL 18.6 in un container temporaneo senza volumi applicativi. Coperti schema completo V3, seed V5, ID generati con righe preesistenti e buchi di sequenza, sette associazioni effetti, arti `STANDARD`, collezione dimostrativa, snapshot PvE e riavvio Flyway senza ulteriori migrazioni. Verificati tutti i 30 file WebP e la conservazione dei contenuti delle carte. I riferimenti ai report assenti dal checkout puntano ora a snapshot Git storici verificati. Frontend, browser e distribuzione Compose non rieseguiti. [Comandi e limiti](../springboot_app/app/README.md#verifica-della-riscrittura-del-2026-11-27).

Revisione dei riferimenti del **2026-11-27**: controllati 51 documenti Markdown attivi e 784 riferimenti locali, senza file mancanti o sezioni irrisolte. V3, DBML e i due diagrammi Mermaid concordano sulle 13 colonne di `cards`. Corrette le descrizioni residue di V9/V10 in frontend, mazzi e battaglie; mantenuta la distinzione tra schema e payload. Venti collegamenti a report rimossi puntano a commit storici verificati; i report esistenti non sono modificati. La pagina F03 distingue il branch `feature/f03-packages` dal checkout corrente e collega sei sorgenti disponibili soltanto in quel branch. Questa revisione riguarda la documentazione: i test applicativi non sono rieseguiti e gli esiti sopra sono quelli della precedente verifica del cleanup.

Verifica del **2026-11-24**: **262 test backend**, **51 test JUnit del motore** e **54 test frontend** superati, senza fallimenti, errori o casi saltati; JAR backend e build Vite prodotti. Backend verificato con JDK 21.0.11 e PostgreSQL 18.6 in un container dedicato. Le prove includono installazione V1–V11, aggiornamenti V9→V10 e V10→V11, conservazione di identità e possesso, API amicizie, concorrenza e cleanup nella transazione F01.

Build delle tre immagini Compose riuscita e quattro servizi sani dopo la ricreazione del database. Attraverso Nginx verificati login/logout, catalogo di 30 carte, 30 varianti possedute `STANDARD`, 30 immagini WebP, elenchi amicizie/richieste e risposte `401`/`422`. Confermate V1–V11 nella cronologia Flyway, tabella `friendships` presente e tabelle pacchetti assenti. Verifica browser non eseguita; frontend F13, scambi F06 e acquisto/apertura pacchetti restano da realizzare.

Aggiornamento F13 del **2026-11-24**: backend richieste e amicizie, migrazione V11 e cleanup amicizie nella transazione F01 disponibili. Frontend F13 e scambi F06 restano da completare. [Verifiche storiche F13](report/2026-11-22-implementazione-amicizie.md).

Evidenza storica dell'importazione parziale del **2026-11-22** in `dev`: **220 test applicativi** e **51 test JUnit del motore** superati; a quella data frontend e V10 immagini erano esclusi. [Ambito e limiti](BackEnd/collezione-carte.md#verifica-dellimportazione-parziale-del-2026-11-22).

Le pagine distinguono decisioni confermate, proposte e punti aperti. Il regolamento Versione 2.1 resta una proposta per prototipo e bilanciamento; la specifica e il DBML v1.1 rappresentano il modello completo, implementato solo in parte.

Verifica di allineamento del **2026-11-23**, checkout locale `test-connessione`, base `936708e`: indici, guide, istruzioni e schede confrontati con i sorgenti. React usa F01/F04/F05/F07 reali e include il visualizzatore PvE in memoria; backend con letture F04, scritture carte F10 e Flyway V1–V10. **54 test frontend e build superati**; browser, backend e database non rieseguiti. [Discrepanze corrette e limiti](report/2026-11-23-allineamento-documentazione.md). Le note datate seguenti conservano le evidenze storiche; la tabella riepiloga l'implementazione attuale.

Aggiornamento F07 del **2026-11-22**, su richiesta dell'utente: rimosso l'ordine fisso degli stage. Il tutorial usa il mescolamento ordinario degli equipaggiamenti a inizio partita e delle creature a inizio di ciascun round, sia nell'API PvE sia nella campagna CLI. [Comportamento del motore](../springboot_app/game_engine/README.md#campagna) · [Specifica interna](BackEnd/app-battaglie.md#api-interna-del-motore-implementata).

Verifica locale della modifica F07 del **2026-11-22**: **51 test JUnit del motore** superati e **10 battaglie tutorial HTTP** sul JAR locale con dieci log distinti, senza incoerenze nell'analisi del combattimento. JDK 21 e H2; applicazione, browser e nuove immagini Compose non verificati. [Ambiente, comandi e limiti](../springboot_app/game_engine/README.md#campagna).

La configurazione Compose introdotta il **2026-11-16** usa React in `cinder-rift/` e il motore dimostrativo in `springboot_app/game_engine/`, entrambi con Dockerfile e controlli di salute. L'orchestrazione [PvE applicativa](BackEnd/app-battaglie.md), guard L2, deadline T2, snapshot coerenti, regole condivise F05 e riparazione del seed V9 sono presenti nei sorgenti correnti. Il motore mantiene H2 per la campagna e non persiste i risultati. React integra F01/F07 e mostra l'ultimo risultato della sessione. [Avvio, variabili e limiti](../guida-avvio.md#configurazione-dei-servizi-compose) · [Verifica storica dell'orchestrazione](https://github.com/aChiampo/gruppo-03-Cinder-Rift/blob/73ad701a20b4fbd3e540689e92b5569a4c1e4da0/docs/report/2026-11-20-orchestrazione-pve.md).

Dal **2026-11-17**, su richiesta dell’utente, la preparazione iniziale usa gli script `set-up` per Windows/macOS e clona sul Desktop; gli script `start-up` aggiornano `dev`, preparano `.env` e la chiave JWT e ricostruiscono Compose. [Guida aggiornata](../guida-avvio.md#preparare-il-computer-e-scaricare-il-progetto). Le verifiche storiche dello stack non attestano i nuovi flussi di installazione su sistemi nativi puliti.

Verifica del **2026-11-16** sulla copia di lavoro con base `6208de5`: build delle tre immagini superata, quattro servizi sani in uno stack Compose isolato, migrazioni V1–V4 applicate, HTTP e proxy API verificati, **19 test JUnit del motore superati**. Integrazione del login e browser non verificati. [Report con ambiente, isolamento e limiti](https://github.com/aChiampo/gruppo-03-Cinder-Rift/blob/400ceeb0a81aca58d7abd6baff355f60baf2c12c/docs/report/2026-11-16-verifica-docker-compose.md).

Verifica storica del **2026-11-13** sulla copia di lavoro con base `97200d9`: backend F01 (autenticazione e profili) e F05 (mazzi) disponibili, migrazioni Flyway V1–V4 e cancellazione account presenti. La revisione ha corretto il requisito CSRF con Bearer, il limite nickname in registrazione e i test non allineati. Invio email ancora da implementare.

La suite completa `mvnw.cmd -o "-Daether.syncContext.named.factory=rwlock-local" verify` ha superato **118 test su PostgreSQL 18.6** e prodotto il JAR. Configurazione Compose, 16 rotte nei riepiloghi API e collegamenti documentali controllati; avvio dell’applicazione tramite Compose e browser non verificati. [Report datato con ambiente, riferimento, risultati e limiti](https://github.com/aChiampo/gruppo-03-Cinder-Rift/blob/6208de547e9c1d6d69a9021e0d0ef1a2add9beba/docs/report/2026-11-13-revisione-stato-sviluppo.md). Le evidenze precedenti nelle schede F01/F05 restano storiche.

Aggiornamento F05 del **2026-11-18**, su approvazione dell’utente, con energia per carta (`max_card_energy`) e potenza per destinazione (`max_round_power`). V4 configura energia da 1 a 5 e potenza 14 per i round creature, energia 6 e potenza 10 per `EQUIPMENT`. Le migrazioni V3/V4 sono aggiornate per la ricreazione del database tramite il reset dei volumi scelto dall’utente; [contratto e procedura di migrazione](BackEnd/gestione-mazzi.md#implementazione-backend-disponibile).

Aggiornamento del **2026-11-21**, su richiesta dell'utente: importate da `feature/api-collezione-catalogo` le API di catalogo e collezione F04 e le scritture amministrative del catalogo F10, con documentazione e test. Verificati 212 test esistenti e 8 test API importati in esecuzioni distinte su JDK 21.0.11/PostgreSQL 18.6 isolato; JAR prodotto. [Evidenze e limiti](BackEnd/collezione-carte.md#verifica-dellintegrazione-del-2026-11-21).

Aggiornamento F04 del **2026-11-22**, su richiesta dell'utente: React collegato a catalogo e collezione, payload canonici conservati e test aggiornati. Verificati 33 test frontend, build e client sorgente contro Spring locale; browser non disponibile. [Comportamento, evidenze e limiti](FrontEnd/collezione-carte.md). Le note precedenti descrivono evidenze storiche; lo stato corrente è nella tabella seguente.

## Aree e strumenti comuni

- [Digital](Digital/README.md)
- [FrontEnd](FrontEnd/README.md)
- [BackEnd](BackEnd/README.md)
- [Riepilogo degli endpoint](BackEnd/GUIDA-API-FRONTEND.md)
- [F07 — Specifica applicativa delle battaglie PvE](BackEnd/app-battaglie.md) e [riferimento endpoint/payload per il FrontEnd](BackEnd/app-battaglie-api.md)
- [Guida per avviare l'applicazione sul proprio computer](../guida-avvio.md)
- [Configurazione locale dei servizi Docker Compose](../compose.yaml)
- [Build e limiti del frontend](../cinder-rift/README.md#avvio-con-docker-compose)
- [Build e limiti del motore dimostrativo](../springboot_app/game_engine/README.md#avvio-con-docker-compose)
- [Linee guida e collaborazione Git](../Linee-Guida-e-Regole.md)
- [Istruzioni condivise per gli assistenti](../AGENTS.md) e [importazione Claude](../CLAUDE.md)
- [Modello di funzionalità](modello-funzionalita.md)
- [Lista di verifica della consegna](lista-verifica-consegna.md)

## Stato delle funzionalità

Aggiornamento dati F01/F05 del **2026-11-16**, su richiesta dell’utente: migrazioni V5/V6 per il set CR01 (30 carte e arti) e l’utente dimostrativo completo di inventario e collezione. [Dettagli e test delle migrazioni](../springboot_app/app/README.md#set-iniziale-e-utente-dimostrativo-f01f05). Verifica storica della copia di lavoro: **241 test backend superati** e JAR prodotto su PostgreSQL 18.6 isolato, inclusi 123 casi delle nuove migrazioni. A quella data integrazione React e API di collezione erano da completare; lo stato attuale è nella tabella.

Aggiornamento F01 del **2026-11-13**, su approvazione dell’utente, con la rinomina degli endpoint e la cancellazione account sotto `/api/auth`. Il [contratto canonico](BackEnd/accesso-utenti.md#cancellazione-account) descrive la cancellazione propria per `USER` e di qualsiasi account per `ADMIN`, inclusi altri amministratori, con revoca delle sessioni e rimozione dei token di recupero.

Questo è il riepilogo corrente. I riferimenti principali collegano i dettagli e le altre aree; per una funzionalità trasversale lo stato implementativo complessivo resta parziale finché le porzioni previste non sono integrate. Non dedurre l'implementazione dal solo perimetro confermato.

| ID  | Oggetto                                                                    | Perimetro                                   | Specifica   | Implementazione | Verifica     | Riferimento principale                               |
| --- | -------------------------------------------------------------------------- | ------------------------------------------- | ----------- | --------------- | ------------ | ---------------------------------------------------- |
| F01 | Registrazione, accesso, profilo, rinnovo, uscita, cancellazione account, recupero password e ruoli `ADMIN` e `USER` | Confermato | Contratto backend aggiornato il 2026-11-18 con requisiti di cleanup F13/F06 | Backend, pulizie token, base recupero, migrazioni Flyway e cancellazione account implementati; cleanup amicizie implementato; cleanup scambi aperti F06 da completare. React usa login, registrazione, rinnovo, uscita e aggiornamento profilo reali; cancellazione account, recupero password UI ed email da completare | 118 test superati il 2026-11-13 nella suite completa su PostgreSQL 18.6; JAR prodotto. [Evidenze e limiti](https://github.com/aChiampo/gruppo-03-Cinder-Rift/blob/6208de547e9c1d6d69a9021e0d0ef1a2add9beba/docs/report/2026-11-13-revisione-stato-sviluppo.md). F13 verificato nella suite corrente; F06 da implementare | [Accesso](BackEnd/accesso-utenti.md) |
| F03 | Catalogo pacchetti, acquisto, inventario e apertura differita; prezzi del seed da definire; pagamento in Scaglie e regole di estrazione confermati | Confermato | CRUD con rarità mercy nullable nel pacchetto; estrazione e ultimo slot confermati, contratto di acquisto, inventario e apertura implementati | Migrazioni V12/V13 e CRUD disponibili con rarità mercy nullable nel pacchetto e slot relazionali, rarità configurabili, probabilità decimal(7,4), is_acquirable e FK artwork; catalogo per ruolo senza paginazione, PUT conserva possesso e cancella mercy, DELETE rimborsa in Scaglie al prezzo corrente; nessun seed dei pacchetti; tre rotte inventario/apertura disponibili; acquisto atomico con servizi condivisi e hold Scaglie; nessuna migrazione F06 o cronologia acquisti | 412 test backend e JAR superati il 2026-11-27 su PostgreSQL 18.6 isolato; incluse 49 prove acquisto, 5 lettore prenotazioni e 47 API/transazionali di apertura. Frontend e flussi del mercato F06 esclusi | [Pacchetti](BackEnd/acquisto-pacchetti.md) |
| F04 | Catalogo e collezione | Confermato | Contratto HTTP implementato; payload backend invariati | API backend e schermate React collegate; filtri, ordinamento, paginazione, varianti e 30 illustrazioni WebP CR01 associate direttamente da V5 | Il 2026-11-27: 263 test backend, seed V5 definitivo, ID generati e arti verificati su PostgreSQL 18.6 isolato; JAR prodotto. Il 2026-11-22: 33 test frontend, build e 30 URL immagini verificati. Frontend, browser e distribuzione Compose non rieseguiti il 2026-11-27 | [Catalogo e collezione](BackEnd/collezione-carte.md) · [Interfaccia e verifiche](FrontEnd/collezione-carte.md) |
| F05 | Gestione e validazione mazzi | Confermato | Limiti energia/potenza e regole V2.1 del prototipo confermati per il validatore condiviso il 2026-11-20; bilanciamento definitivo aperto | Persistenza, API REST, composizione, possesso, energia, potenza, rarità e limiti copie implementati; editor React con CRUD reale e collezione F04 compatta, filtro energia per round, selettore con Equipaggiamenti, clic e trascinamento negli slot | Il 2026-11-27: 263 test backend e 51 del motore superati; verificati schema V3, seed V5, ID generati, validazione e snapshot PvE. [Evidenze storiche F05/F07](https://github.com/aChiampo/gruppo-03-Cinder-Rift/blob/73ad701a20b4fbd3e540689e92b5569a4c1e4da0/docs/report/2026-11-20-orchestrazione-pve.md). Frontend: 44 test e build superati il 2026-11-22; browser integrato non verificato. [Limiti F05](FrontEnd/gestione-mazzi.md#verifiche-e-limiti) | [Mazzi](BackEnd/gestione-mazzi.md) |
| F06 | Scambi di carte e token, privati/pubblici, consenso e prenotazioni | Confermato; approvato anche dal committente | In approfondimento; hold monetari in Scaglie confermati il 2026-11-27 | Lettore prenotazioni e addebito condivisi disponibili; migrazione e scritture del mercato escluse | Verifica dei servizi condivisi descritta in F03; flussi F06 non eseguiti | [Scambi](BackEnd/scambi-mercato.md) |
| F07 | Sfide e simulazione | Confermato | PvE concordato il 2026-11-20: L2 + T2, snapshot REPEATABLE READ, budget 8/9/10 secondi, rilascio del guard al termine HTTP e accesso interno tramite Docker | Endpoint, orchestrazione e client React presenti; risultato con round/scambi/replay in memoria e parser del log corrente. Tutorial mescolato normalmente. Persistenza PvE e funzionalità complete PvP da realizzare | 54 test frontend e build superati il 2026-11-23, senza browser. [Verifica corrente](report/2026-11-23-allineamento-documentazione.md). Evidenze precedenti: 51 test motore e 10 battaglie HTTP il 2026-11-22; 212 test applicativi, 49 motore e flusso fra JAR il 2026-11-20. [Report backend](https://github.com/aChiampo/gruppo-03-Cinder-Rift/blob/73ad701a20b4fbd3e540689e92b5569a4c1e4da0/docs/report/2026-11-20-orchestrazione-pve.md) | [Interfaccia](FrontEnd/sfide-simulazione.md) · [Sfide](BackEnd/sfide-simulazione.md) · [App battaglie](BackEnd/app-battaglie.md) · [API PvE](BackEnd/app-battaglie-api.md) |
| F08 | Storico e dettaglio dei turni                                              | Confermato                                  | In approfondimento | Non iniziata    | Non eseguita | [Storico](BackEnd/storico-partite.md)                |
| F09 | Pagine informative                                                         | DA DEFINIRE; solo segnaposto                | Sintetica   | Non iniziata    | Non eseguita | [Pagine informative](FrontEnd/pagine-informative.md) |
| F10 | Backoffice carte e prodotti | Confermato | CRUD catalogo documentato; altre funzioni in approfondimento | API amministrative carte e CRUD pacchetti disponibili; altre funzioni non implementate | 251 test backend e JAR superati il 2026-11-25, inclusi slot e disponibilità F03; integrazione React non verificata | [Backoffice](BackEnd/gestione-amministrativa.md) |
| F11 | Responsive e accessibilità | Confermato | Sintetica; criteri da concordare | Parziale: media query, campi etichettati, stati e attributi ARIA | Ispezione e rendering statico il 2026-11-23; audit browser, tastiera e dispositivi non eseguiti | [Interfacce](FrontEnd/accessibilita-responsive.md) |
| F12 | SEO e prestazioni | Confermato | Sintetica; obiettivi da concordare | Base tecnica parziale: lingua, viewport, titoli e immagini differite; SEO completo da realizzare | Build verificata il 2026-11-23; Lighthouse/PageSpeed e prestazioni browser non misurati | [SEO](FrontEnd/seo-prestazioni.md) |
| F13 | Amicizie reciproche e richieste | Relazioni proprie USER/ADMIN, Bearer JWT, elenchi completi e ordinati; modello v1.1 | Contratto API definito; UI e F06 esclusi | Backend F13, V11 e cleanup amicizie F01 implementati il 2026-11-22 nella copia di lavoro | Suite completa: 260 test backend superati, inclusi 40 F13, e JAR prodotto il 2026-11-22 su PostgreSQL 18.6 isolato. [Report](report/2026-11-22-implementazione-amicizie.md) | [Contratto amicizie](BackEnd/amicizie.md) |
| D01 | Lore, identità e carte non interamente generate | Confermato | In approfondimento | 30 illustrazioni dimostrative CR01 disponibili; lore e contenuti definitivi in approfondimento | Ritagli e conversione lossless verificati il 2026-11-22; contenuti definitivi e bilanciamento non validati | [Lore e carte](Digital/lore-identita-carte.md) |
| D02 | Pubblico, buyer persona e competitor                                       | Confermato                                  | Sintetica   | Non iniziata    | Non eseguita | [Analisi](Digital/analisi-pubblico-competitor.md)    |
| D03 | Economia e proiezioni                                                      | Confermato                                  | Sintetica   | Non iniziata    | Non eseguita | [Economia](Digital/economia-crediti-pacchetti.md)    |
| D04 | Piano editoriale, lancio, community, campagne e costi                      | Confermato                                  | Sintetica   | Non iniziata    | Non eseguita | [Comunicazione](Digital/comunicazione-lancio.md)     |
| O01 | Modalità di gioco aggiuntive separate                                      | Non confermato                              | Non redatta | Non iniziata    | Non eseguita | Nessuna pagina dedicata                              |

Per Digital, implementazione indica la realizzazione dei materiali e verifica indica la validazione pertinente, non necessariamente test software. Per tutte le aree la specifica può essere sintetica, in approfondimento, concordata o da aggiornare; l'implementazione non iniziata, in corso, parziale o integrata. L'esito di verifica richiede data, ambito, riferimento ed evidenze, compresi limiti o controlli non eseguiti.

Il [regolamento Versione 2.1](Digital/regolamento-base.md) è la proposta condivisa per F05 e F07, con conseguenze per F08, D01 e F10. L'integrazione del testo è approvata, mentre regole definitive e bilanciamento restano da confermare. Le specifiche interessate sono in approfondimento; API, dati, permessi e altri contratti tecnici mancanti restano DA DEFINIRE nelle pagine BackEnd. I riferimenti FrontEnd descrivono le implicazioni per il client senza introdurre contratti concorrenti. F09 resta un segnaposto e O01 non è confermato.

Aggiornare questa tabella nella PR che cambia lo stato; i dettagli della singola porzione restano nelle pagine di area. I metadati temporali si aggiungono alle pagine esistenti al prossimo aggiornamento significativo, senza attribuire date retroattive. Quando serve una verifica storica dettagliata, collegare un report in `docs/report/AAAA-MM-GG-descrizione.md`, creato solo dopo l'attività reale. Il report registra il passato; questo indice mantiene lo stato corrente.

Aggiornamento **2026-11-22** di [F13](BackEnd/amicizie.md): implementati API backend e cleanup amicizie nella transazione F01, con migrazione amicizie oggi numerata V11, secondo le scelte dell’utente. Frontend e F06 esclusi. [Report e verifiche](report/2026-11-22-implementazione-amicizie.md). Le regole confermate F06 restano: amicizia accettata per nuovi privati, permanenza degli scambi alla rimozione amicizia e cleanup dei soli scambi aperti al soft delete, con storico conservato e ripubblicazione degli annunci validi della controparte.

## Contratti e approfondimento progressivo

Il contratto tecnico canonico di una funzionalità trasversale risiede nella pagina BackEnd pertinente; FrontEnd vi rimanda e documenta flussi e stati del client. Le regole di gioco, i contenuti e le ipotesi economiche restano nelle pagine Digital. Gli ID Fxx/Dxx collegano le aree, anche quando gli ID delle attività e i branch sono diversi.

Le pagine si approfondiscono solo per gli aspetti disponibili, mantenendo riconoscibili le proposte. Il [modello completo](modello-funzionalita.md#modello-di-approfondimento-progressivo) definisce il futuro livello di precisione: permessi, stati, contratti ed esempi, dati, transazioni/concorrenza/retry, errori, criteri di accettazione e verifiche pertinenti. Compilare progressivamente solo le decisioni disponibili e mantenere espliciti i punti DA DEFINIRE.

## Differenze rispetto alla traccia

| Tema | Traccia | Decisione o chiarimento del gruppo |
| --- | --- | --- |
| Database | MySQL | PostgreSQL 18 configurato in Compose; F01 usa lo schema v1.1 via JDBC. Build, avvio, migrazioni e salute verificati nello stack isolato il 2026-11-16; integrazione funzionale completa da verificare |
| Scambi | Solo carte | Carte e crediti, privati/pubblici; approvazione del committente confermata dal gruppo |
| Offerte del negozio | Acquisto di pacchetti | Ogni offerta contiene un solo pacchetto; prezzo e configurazione del pacchetto gestibili dal backoffice |
| Carte | Creazione con strumenti di intelligenza artificiale | Carte non interamente generate; ripartizione del contributo umano da definire |
| Pagine informative | Richieste dalla traccia | Segnaposto fino a indicazioni successive |

## Decisioni aperte

Da approfondire successivamente nelle pagine interessate:

- Allineamento del prototipo React 18/Vite 5 ai contratti canonici e chiarimento della differenza PostgreSQL/MySQL rispetto alla traccia, con backend già configurato nel POM.
- Permessi di `ADMIN` e `USER` nelle altre funzionalità; invio email e frontend del recupero password F01, con base backend già implementata.
- Regole di assegnazione delle Scaglie, acquisto dei Nuclei Rift e recupero dei Rotami. Acquisti e importi monetari F06 usano Scaglie; eventuali estensioni ad altre risorse restano da definire.
- Offerte di un solo pacchetto: prezzi, composizioni e disponibilità iniziali, seed commerciale e frontend; contratto di acquisto backend implementato.
- Scambi: API, integrazione delle prenotazioni con gli altri servizi, giocabilità delle copie prenotate e operatività della pulizia; durata, consenso, modifica e annullamento sono descritti nel [contratto F06](BackEnd/scambi-mercato.md).
- Regolamento: approvazione definitiva della Versione 2.1; soglie di energia, statistiche e punti potenza delle carte, effetti futuri (attivazione all’ingresso e ordine equipaggiamenti prima delle creature confermati il 2026-11-20), bilanciamento di budget/rarità/copie/bonus, conferma della formula del danno con gli insegnanti e possibilità di cure o prevenzione dei danni. Riferimenti: [formula proposta](Digital/regolamento-base.md#11-danni-ai-giocatori) ed [elementi da completare e testare](Digital/regolamento-base.md#15-elementi-da-completare-e-testare).
- Effetti degli scambi sui mazzi e uso delle carte in più mazzi.
- Numero di approvazioni, revisori, strategia di merge e protezioni remote.
- Conferma del nome del progetto e del numero di gruppo e completamento dell’account GitHub mancante; nove membri e Team Leader sono già riportati nel README.

L'esempio «1.000 crediti → 1 pacchetto Rare» rispetta la cardinalità confermata di un solo pacchetto per offerta e non stabilisce un prezzo o un tipo definitivo.

## Consegna

La traccia richiede frontend, backend, export database, documentazione tecnica e di progetto, screenshot desktop/mobile e presentazione finale, in un archivio `gruppo-##-nome-progetto-scelto.zip`.

Vedere la [lista di verifica](lista-verifica-consegna.md), le cui caselle restano aperte fino alla verifica effettiva.

[README del progetto](../README.md)
