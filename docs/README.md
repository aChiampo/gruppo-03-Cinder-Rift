# Indice della documentazione

Aggiornamento F13 del **2026-11-22**: backend amicizie, V10 e cleanup atomico F01 disponibili nella copia di lavoro del branch `feature/f13-friendships-backend`; frontend e F06 esclusi. [Report e verifiche](report/2026-11-22-implementazione-amicizie.md). Le evidenze di importazione sottostanti precedono questa attività.

Importazione parziale del **2026-11-22** da `test-connessione`: API F04/F10, test e mescolamento ordinario del tutorial F07. Migrazioni ferme a V9; V10, modifica al relativo test di migrazione e modifiche frontend escluse. Le verifiche del branch sorgente riportate sotto sono evidenze storiche, non nuove esecuzioni.

Verifiche locali dell'importazione parziale del **2026-11-22**: **220 test applicativi** superati con JDK 21.0.11/PostgreSQL 18.6 isolato e JAR prodotto; **51 test JUnit del motore** superati con H2. [Ambito e limiti](BackEnd/collezione-carte.md#verifica-dellimportazione-parziale-del-2026-11-22).

Le pagine distinguono decisioni confermate, proposte e punti aperti. Il regolamento Versione 2.1 resta una proposta per prototipo e bilanciamento; la specifica e il DBML v1.1 rappresentano il modello completo, implementato solo in parte.

La configurazione Compose introdotta il **2026-11-16** usa il prototipo React in `cinder-rift/` e il motore dimostrativo in `springboot_app/game_engine/`, entrambi con Dockerfile e controlli di salute. Aggiornamento del **2026-11-20**: il branch `feature/app_battle_pve_flow` implementa l'orchestrazione [PvE applicativa](BackEnd/app-battaglie.md), guard L2, deadline T2, snapshot coerenti, regole condivise F05 e riparazione del seed V9. Il motore mantiene H2 per la campagna e non persiste i risultati. L'integrazione React F01/F07 resta da completare. [Avvio, variabili e limiti](../guida-avvio.md#configurazione-dei-servizi-compose) · [Verifica corrente](report/2026-11-20-orchestrazione-pve.md). Le evidenze storiche seguenti restano distinte.

Dal **2026-11-17**, su richiesta dell’utente, la preparazione iniziale usa gli script `set-up` per Windows/macOS e clona sul Desktop; gli script `start-up` aggiornano `dev`, preparano `.env` e la chiave JWT e ricostruiscono Compose. [Guida aggiornata](../guida-avvio.md#preparare-il-computer-e-scaricare-il-progetto). Le verifiche storiche dello stack non attestano i nuovi flussi di installazione su sistemi nativi puliti.

Verifica del **2026-11-16** sulla copia di lavoro con base `6208de5`: build delle tre immagini superata, quattro servizi sani in uno stack Compose isolato, migrazioni V1–V4 applicate, HTTP e proxy API verificati, **19 test JUnit del motore superati**. Integrazione del login e browser non verificati. [Report con ambiente, isolamento e limiti](report/2026-11-16-verifica-docker-compose.md).

Verifica storica del **2026-11-13** sulla copia di lavoro con base `97200d9`: backend F01 (autenticazione e profili) e F05 (mazzi) disponibili, migrazioni Flyway V1–V4 e cancellazione account presenti. La revisione ha corretto il requisito CSRF con Bearer, il limite nickname in registrazione e i test non allineati. Invio email ancora da implementare.

La suite completa `mvnw.cmd -o "-Daether.syncContext.named.factory=rwlock-local" verify` ha superato **118 test su PostgreSQL 18.6** e prodotto il JAR. Configurazione Compose, 16 rotte nei riepiloghi API e collegamenti documentali controllati; avvio dell’applicazione tramite Compose e browser non verificati. [Report datato con ambiente, riferimento, risultati e limiti](report/2026-11-13-revisione-stato-sviluppo.md). Le evidenze precedenti nelle schede F01/F05 restano storiche.

Aggiornamento F05 del **2026-11-18**, su approvazione dell’utente, con energia per carta (`max_card_energy`) e potenza per destinazione (`max_round_power`). V4 configura energia da 1 a 5 e potenza 14 per i round creature, energia 6 e potenza 10 per `EQUIPMENT`. Le migrazioni V3/V4 sono aggiornate per la ricreazione del database tramite il reset dei volumi scelto dall’utente; [contratto e procedura di migrazione](BackEnd/gestione-mazzi.md#implementazione-backend-disponibile).

Aggiornamento del **2026-11-21**, su richiesta dell'utente: importate da `feature/api-collezione-catalogo` le API di catalogo e collezione F04 e le scritture amministrative del catalogo F10, con documentazione e test. Verificati 212 test esistenti e 8 test API importati in esecuzioni distinte su JDK 21.0.11/PostgreSQL 18.6 isolato; JAR prodotto. [Evidenze e limiti](BackEnd/collezione-carte.md#verifica-dellintegrazione-del-2026-11-21).

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

Aggiornamento dati F01/F05 del **2026-11-16**, su richiesta dell’utente: migrazioni V5/V6 per il set CR01 (30 carte e arti) e l’utente dimostrativo completo di inventario e collezione. [Dettagli e test delle migrazioni](../springboot_app/app/README.md#set-iniziale-e-utente-dimostrativo-f01f05). Verifica della copia di lavoro: **241 test backend superati** e JAR prodotto su PostgreSQL 18.6 isolato, inclusi 123 casi delle nuove migrazioni. L’integrazione React e le API di collezione restano da completare.

Aggiornamento F01 del **2026-11-13**, su approvazione dell’utente, con la rinomina degli endpoint e la cancellazione account sotto `/api/auth`. Il [contratto canonico](BackEnd/accesso-utenti.md#cancellazione-account) descrive la cancellazione propria per `USER` e di qualsiasi account per `ADMIN`, inclusi altri amministratori, con revoca delle sessioni e rimozione dei token di recupero.

Questo è il riepilogo corrente. I riferimenti principali collegano i dettagli e le altre aree; per una funzionalità trasversale lo stato implementativo complessivo resta parziale finché le porzioni previste non sono integrate. Non dedurre l'implementazione dal solo perimetro confermato.

| ID  | Oggetto                                                                    | Perimetro                                   | Specifica   | Implementazione | Verifica     | Riferimento principale                               |
| --- | -------------------------------------------------------------------------- | ------------------------------------------- | ----------- | --------------- | ------------ | ---------------------------------------------------- |
| F01 | Autenticazione, profili, cancellazione account e recupero password | Confermato | Contratto backend e integrazione F13 aggiornati il 2026-11-22 | Backend e cleanup amicizie implementati; frontend, email e cleanup F06 da completare | [Verifiche F01/F13](report/2026-11-22-implementazione-amicizie.md); evidenze precedenti nel contratto | [Accesso](BackEnd/accesso-utenti.md) |
| F03 | Offerte di un solo pacchetto; risorsa del prezzo da definire                      | Confermato                                  | In approfondimento   | Non iniziata    | Non eseguita | [Pacchetti](BackEnd/acquisto-pacchetti.md)           |
| F04 | Catalogo e collezione | Confermato | Contratto API importato il 2026-11-21 | API backend aggiunte; integrazione React da realizzare | 8 test API superati il 2026-11-21 su PostgreSQL 18.6 isolato; integrazione React non verificata | [Catalogo e collezione](BackEnd/collezione-carte.md) |
| F05 | Gestione e validazione mazzi | Confermato | Limiti energia/potenza e regole V2.1 del prototipo confermati per il validatore condiviso il 2026-11-20; bilanciamento definitivo aperto | Persistenza, API REST, composizione, possesso, energia, potenza, rarità e limiti copie implementati; deck builder React dimostrativo non integrato | Suite completa di 212 test applicativi superata il 2026-11-20 su PostgreSQL 18.6 isolato; JAR e V9 verificati. [Report F05/F07](report/2026-11-20-orchestrazione-pve.md) | [Mazzi](BackEnd/gestione-mazzi.md) |
| F06 | Scambi di carte e token, privati/pubblici, consenso e prenotazioni | Confermato; approvato anche dal committente | In approfondimento; effetti F13 e soft delete aggiornati il 2026-11-18 | Non iniziata | Non eseguita | [Scambi](BackEnd/scambi-mercato.md) |
| F07 | Sfide e simulazione | Confermato | PvE concordato il 2026-11-20: L2 + T2, snapshot REPEATABLE READ, budget 8/9/10 secondi, rilascio del guard al termine HTTP e accesso interno tramite Docker | Endpoint e orchestrazione applicativi implementati nella copia di lavoro di `feature/app_battle_pve_flow`; validatore F05 completato e V9 disponibile. React, persistenza PvE e funzionalità complete PvP da realizzare | 212 test applicativi e 49 del motore superati il 2026-11-20 su JDK 21/PostgreSQL 18.6 isolato/H2; flusso reale fra JAR e OpenAPI verificati. Browser e nuove immagini Compose non verificati. [Report](report/2026-11-20-orchestrazione-pve.md) | [Sfide](BackEnd/sfide-simulazione.md) · [App battaglie](BackEnd/app-battaglie.md) · [API PvE](BackEnd/app-battaglie-api.md) |
| F08 | Storico e dettaglio dei turni                                              | Confermato                                  | In approfondimento | Non iniziata    | Non eseguita | [Storico](BackEnd/storico-partite.md)                |
| F09 | Pagine informative                                                         | DA DEFINIRE; solo segnaposto                | Sintetica   | Non iniziata    | Non eseguita | [Pagine informative](FrontEnd/pagine-informative.md) |
| F10 | Backoffice carte e prodotti | Confermato | CRUD catalogo documentato; altre funzioni in approfondimento | API amministrative carte aggiunte; altre funzioni non implementate | 8 test API superati il 2026-11-21 su PostgreSQL 18.6 isolato; integrazione React non verificata | [Backoffice](BackEnd/gestione-amministrativa.md) |
| F11 | Responsive e accessibilità                                                 | Confermato                                  | Sintetica   | Non iniziata    | Non eseguita | [Interfacce](FrontEnd/accessibilita-responsive.md)   |
| F12 | SEO e prestazioni                                                          | Confermato                                  | Sintetica   | Non iniziata    | Non eseguita | [SEO](FrontEnd/seo-prestazioni.md)                   |
| F13 | Amicizie reciproche e richieste | Relazioni proprie USER/ADMIN, Bearer JWT, elenchi completi e ordinati; modello v1.1 | Contratto API definito; UI e F06 esclusi | Backend F13, V10 e cleanup amicizie F01 implementati il 2026-11-22 nella copia di lavoro | Suite completa: 260 test backend superati, inclusi 40 F13, e JAR prodotto il 2026-11-22 su PostgreSQL 18.6 isolato. [Report](report/2026-11-22-implementazione-amicizie.md) | [Contratto amicizie](BackEnd/amicizie.md) |
| D01 | Lore, identità e carte non interamente generate                            | Confermato                                  | In approfondimento | Non iniziata    | Non eseguita | [Lore e carte](Digital/lore-identita-carte.md)       |
| D02 | Pubblico, buyer persona e competitor                                       | Confermato                                  | Sintetica   | Non iniziata    | Non eseguita | [Analisi](Digital/analisi-pubblico-competitor.md)    |
| D03 | Economia e proiezioni                                                      | Confermato                                  | Sintetica   | Non iniziata    | Non eseguita | [Economia](Digital/economia-crediti-pacchetti.md)    |
| D04 | Piano editoriale, lancio, community, campagne e costi                      | Confermato                                  | Sintetica   | Non iniziata    | Non eseguita | [Comunicazione](Digital/comunicazione-lancio.md)     |
| O01 | Modalità di gioco aggiuntive separate                                      | Non confermato                              | Non redatta | Non iniziata    | Non eseguita | Nessuna pagina dedicata                              |

Per Digital, implementazione indica la realizzazione dei materiali e verifica indica la validazione pertinente, non necessariamente test software. Per tutte le aree la specifica può essere sintetica, in approfondimento, concordata o da aggiornare; l'implementazione non iniziata, in corso, parziale o integrata. L'esito di verifica richiede data, ambito, riferimento ed evidenze, compresi limiti o controlli non eseguiti.

Il [regolamento Versione 2.1](Digital/regolamento-base.md) è la proposta condivisa per F05 e F07, con conseguenze per F08, D01 e F10. L'integrazione del testo è approvata, mentre regole definitive e bilanciamento restano da confermare. Le specifiche interessate sono in approfondimento; API, dati, permessi e altri contratti tecnici mancanti restano DA DEFINIRE nelle pagine BackEnd. I riferimenti FrontEnd descrivono le implicazioni per il client senza introdurre contratti concorrenti. F09 resta un segnaposto e O01 non è confermato.

Aggiornare questa tabella nella PR che cambia lo stato; i dettagli della singola porzione restano nelle pagine di area. I metadati temporali si aggiungono alle pagine esistenti al prossimo aggiornamento significativo, senza attribuire date retroattive. Quando serve una verifica storica dettagliata, collegare un report in `docs/report/AAAA-MM-GG-descrizione.md`, creato solo dopo l'attività reale. Il report registra il passato; questo indice mantiene lo stato corrente.

Aggiornamento **2026-11-22** di [F13](BackEnd/amicizie.md): implementati API backend, V10 e cleanup amicizie nella transazione F01, secondo le scelte dell’utente. Frontend e F06 esclusi. [Report e verifiche](report/2026-11-22-implementazione-amicizie.md). Le regole confermate F06 restano: amicizia accettata per nuovi privati, permanenza degli scambi alla rimozione amicizia e cleanup dei soli scambi aperti al soft delete, con storico conservato e ripubblicazione degli annunci validi della controparte.

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
- Regole di assegnazione delle Scaglie, acquisto dei Nuclei Rift e recupero dei Rotami; risorse ammesse nei prezzi e negli scambi.
- Offerte di un solo pacchetto: prezzi, limiti, contenuto del pacchetto, apertura e disponibilità.
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
