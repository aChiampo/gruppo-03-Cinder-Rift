# Gioco di carte collezionabili digitale

Verifica finale acquisto F03 del **2026-11-27**: **412 test backend superati**, zero fallimenti/errori/casi saltati, e JAR prodotto con JDK 21.0.11 e PostgreSQL 18.6 isolato. Incluse 49 prove di acquisto e 5 del lettore prenotazioni condiviso. Nessuna migrazione o modifica al database applicativo; frontend e flussi F06 esclusi. [Comando, copertura e limiti](springboot_app/app/README.md#verifica-acquisto-f03-del-2026-11-27).

Acquisto F03 implementato il **2026-11-27**, su richiesta dell’utente: `POST /api/users/me/pack-purchases`, addebito Scaglie e credito di pacchetti chiusi atomici. `DebitService` e `ReservationReader` sono condivisi fra funzionalità; gli hold monetari F06 riservano Scaglie. Nessuna nuova migrazione, deduplicazione o cronologia acquisti; frontend e seed commerciale esclusi. [Contratto corrente](docs/BackEnd/acquisto-pacchetti.md#acquisto-pacchetti-contratto-http). Le verifiche dell’apertura sotto precedono questo intervento.

Verifica dell’apertura F03 del **2026-11-27**: **358 test backend superati**, zero fallimenti/errori/casi saltati, e JAR prodotto con JDK 21.0.11 e PostgreSQL 18.6 temporaneo. Incluse 7 prove deterministiche di estrazione e 47 prove API/transazionali di apertura. Database applicativo non modificato; acquisto, seed commerciale e frontend F03 esclusi. [Comandi e copertura](springboot_app/app/README.md#verifica-dellapertura-f03-del-2026-11-27).

Apertura F03 implementata il **2026-11-27**, su richiesta dell’utente: letture dell’inventario e apertura atomica di 1–10 pacchetti dello stesso tipo, filtri cumulativi, carte equiprobabili, artwork indipendente con fallback Standard e mercy sull’ultimo slot. Il servizio di validazione verifica i flussi amministrativi POST/PUT e ogni apertura. Nessuna nuova migrazione o seed commerciale; acquisto e frontend F03 restano esclusi. [Contratto e limiti](docs/BackEnd/acquisto-pacchetti.md).

Aggiornamento F03 del **2026-11-27**, su decisione dell’utente: V12 crea direttamente `mercy_rarity` in `packs` come FK nullable; `user_mercy` conserva soltanto utente, pacchetto e contatore. CRUD e apertura usano questo campo; la garanzia sostituisce l’ultimo slot con la rarità esatta configurata quando necessaria. Acquisto non implementato. [Contratto corrente](docs/BackEnd/acquisto-pacchetti.md).

Verifica precedente dell’integrazione del **2026-11-27**: **300 test backend**, **51 del motore** e **54 frontend** superati; JAR backend e build React prodotti. [Comandi, copertura e limiti](springboot_app/app/README.md#verifica-dellintegrazione-f03-del-2026-11-27). Database applicativo non modificato.

Integrazione F03 con `dev` del **2026-11-27**, su richiesta dell’utente: preservati schema V3 e seed V5 ripuliti, artwork `STANDARD`, ID generati e amicizie V11. Pacchetti e slot sono ora V12/V13; catena completa di undici script, V1–V8 e V11–V13. Catalogo, CRUD, inventario e apertura disponibili; acquisto e seed commerciale rinviati. [Contratto e compatibilità](docs/BackEnd/acquisto-pacchetti.md).

Verifica storica del cleanup di `dev` del **2026-11-27**, prima dell’integrazione F03: **263 test backend** e **51 del motore** superati; JAR backend prodotto e catena V1–V8 e V11 verificata su PostgreSQL 18.6 isolato. [Comandi, copertura e limiti](springboot_app/app/README.md#verifica-della-riscrittura-del-2026-11-27). Frontend, browser e distribuzione Compose non rieseguiti.

Verifica storica del **2026-11-24**: **262 test backend**, **51 del motore** e **54 frontend** superati; build e stack Compose verificati con artwork `STANDARD` e amicizie V11. [Ambiente, controlli HTTP e limiti](docs/README.md).

Aggiornamento F13 del **2026-11-24**: backend richieste e amicizie, migrazione V11 e cleanup amicizie nella transazione F01 disponibili. Frontend F13 e scambi F06 restano da completare. [Verifiche storiche F13](docs/report/2026-11-22-implementazione-amicizie.md).

Evidenza storica dell'importazione parziale del **2026-11-22** in `dev`: **220 test applicativi** e **51 test JUnit del motore** superati; a quella data frontend e V10 immagini erano esclusi. [Ambito e limiti](docs/BackEnd/collezione-carte.md#verifica-dellimportazione-parziale-del-2026-11-22).

> Nome progetto e numero gruppo: **DA DEFINIRE**. Configurazione Docker aggiornata il **2026-11-16**, su richiesta dell’utente: backend F01/F05, prototipo React e motore dimostrativo dispongono di build Compose. Nota storica: a quella data invio email e integrazione React erano da completare; vedere l'aggiornamento corrente sotto.

Aggiornamento del **2026-11-21**: importate le API F04 di [catalogo e collezione](docs/BackEnd/collezione-carte.md) e le scritture amministrative F10 da `feature/api-collezione-catalogo`. Verificati 212 test esistenti e 8 test API importati in esecuzioni distinte; JAR prodotto su PostgreSQL 18.6 isolato.

Aggiornamento del **2026-11-22**, su richiesta dell'utente: catalogo e collezione React collegati alle API F04 esistenti, con filtri, ordinamenti, paginazione e varianti; 33 test frontend superati, build riuscita e client sorgente verificato contro Spring locale. [Comportamento e limiti della verifica F04](docs/FrontEnd/collezione-carte.md). Le note precedenti descrivono lo stato alle rispettive date; il frontend corrente usa autenticazione F01 reale, senza modalità mock.

Verifica documentale del **2026-11-23** sulla copia locale `test-connessione`, base `936708e`: allineati indici, guide e schede al codice corrente. React integra F01/F04/F05/F07 e include il risultato PvE con round, scambi e replay locale; il report resta in memoria, senza storico F08. **54 test frontend e build superati**; backend, database e browser non rieseguiti. [Discrepanze corrette, controlli e limiti](docs/report/2026-11-23-allineamento-documentazione.md).

## Indice generale

- [Gioco di carte collezionabili digitale](#gioco-di-carte-collezionabili-digitale)
  - [Indice generale](#indice-generale)
  - [Funzionamento previsto](#funzionamento-previsto)
  - [Terminologia commerciale](#terminologia-commerciale)
  - [Tecnologie](#tecnologie)
  - [Componenti del gruppo](#componenti-del-gruppo)
  - [Lore](#lore)
  - [Regole del gioco](#regole-del-gioco)
  - [Struttura del repository](#struttura-del-repository)
  - [Collaborazione Git](#collaborazione-git)
  - [Avvio e verifica](#avvio-e-verifica)



## Funzionamento previsto

1. Registrazione e accesso con email e password. Il backend Spring Boot implementa JWT locali, rinnovo tramite cookie, ruoli `ADMIN` e `USER` e cancellazione logica degli account con revoca dei token. Gli endpoint usano `/api/auth`. È disponibile anche la base del recupero password, con consegna email e interfaccia rinviate. [Contratto F01](docs/BackEnd/accesso-utenti.md) · [Invio mail futuro con Thymeleaf](docs/BackEnd/invio-mail.md).
2. Acquisto con crediti di offerte contenenti ciascuna esattamente un pacchetto; prezzo e configurazione del singolo pacchetto sono gestibili dal backoffice.
3. Apertura dei pacchetti e consultazione della collezione; composizione e validazione dei mazzi.
4. Scambi privati o pubblici nel marketplace, con carte e crediti, consenso di entrambe le parti e durata di due giorni dalla creazione dell’annuncio, secondo il [contratto F06](docs/BackEnd/scambi-mercato.md).
5. Sfide con scelta dei mazzi, simulazione sul server e storico con risultati turno per turno.
6. Richieste e amicizie reciproche F13: `USER` e `ADMIN` gestiscono soltanto le proprie relazioni tramite Bearer JWT; elenchi completi senza paginazione, con nickname, icona e timestamp, dal più recente. Il [contratto API F13](docs/BackEnd/amicizie.md) è definito il **2026-11-20**. Backend, V11 e cleanup amicizie F01 implementati il **2026-11-22** nella copia di lavoro; frontend e F06 esclusi. [Report e verifiche](docs/report/2026-11-22-implementazione-amicizie.md).

Aggiornamento F13/F01 del **2026-11-22**: il soft delete elimina richieste e amicizie nella stessa transazione. Il [cleanup F06](docs/BackEnd/accesso-utenti.md#integrazione-prevista-con-f13-e-f06) resta da implementare. Rimuovere una sola amicizia non introduce operazioni sugli scambi.

Le modalità di assegnazione dei crediti in fase 1 restano **DA DEFINIRE**; i pagamenti reali sono esclusi dallo sviluppo previsto dalla traccia.

### Terminologia commerciale

| Termine | Significato | Esempio illustrativo, non configurazione approvata |
| --- | --- | --- |
| Offerta di un pacchetto | Crediti in cambio di una sola unità di un pacchetto | 1.000 crediti → 1 pacchetto «Rare» |
| Pacchetto | Contenitore di carte con slot configurabili e apertura backend implementata; composizioni commerciali da definire | Un pacchetto del tipo «Rare» |
| Carta | Elemento della collezione utilizzabile nei mazzi | Creatura con attacco, difesa e fazione |

Ogni offerta contiene esattamente un pacchetto; gli insiemi di più pacchetti non rientrano nel perimetro. Gli esempi non stabiliscono prezzi, nomi commerciali, rarità o quantità definitive. Catalogo, CRUD, inventario, acquisto e apertura F03 disponibili senza seed commerciale. [Contratto canonico](docs/BackEnd/acquisto-pacchetti.md).

## Tecnologie

Il backend usa Java 21, Spring Boot 4.1.2, Spring MVC, Spring Security e PostgreSQL tramite JDBC; sono implementati autenticazione e profili F01, CRUD e validazione dei mazzi F05 e orchestrazione applicativa PvE F07. Le undici migrazioni V1–V8 e V11–V13 creano e inizializzano questa base e gli effetti configurabili delle carte, includendo il set CR01 di 30 carte e un utente dimostrativo con collezione; V3 definisce lo schema completo delle carte e V5 inserisce direttamente fazioni corrette, energia e arti `STANDARD` collegate alle illustrazioni WebP. Gli ID delle carte sono generati dal database e usati dalle associazioni degli effetti; `cards.code`, `transfer_and_discard` e `duration` sono rimossi. V11 crea le amicizie; V12/V13 aggiungono pacchetti, possesso, mercy e slot relazionali. Riscrittura del 2026-11-27: V9/V10 rimosse, installazione su database nuovo. [Asset e distribuzione F04](docs/BackEnd/collezione-carte.md#immagini-del-set-cr01). Non realizzano tutto il modello v1.1. [Seed e credenziali dimostrative](springboot_app/app/README.md#set-iniziale-e-utente-dimostrativo-f01f05). Il POM e Compose definiscono rispettivamente dipendenze e PostgreSQL 18. L’autenticazione è gestita nell’applicazione: password BCrypt, access token JWT HS256 e token di rinnovo opachi persistiti come hash. Il prototipo frontend usa React 18 e Vite 5 in `cinder-rift/`; Docker compila con Node.js 22 e serve i file con Nginx. Le schermate di accesso, registrazione, rinnovo, uscita e aggiornamento profilo usano il backend F01 reale. [Stack e configurazione backend](springboot_app/app/README.md).

La traccia prevede GitHub, GitHub Projects, Swagger, JSDoc per JS/TS e motore Java isolato con test JUnit.

## Componenti del gruppo

| Nome e cognome           | GitHub                | Ruolo              |
| ------------------------ | --------------------- | ------------------ |
| Gennaro Raffaele         | @Raffo-DSG1           | Digital Strategist |
| Belvedere Paolo          | @DA_DEFINIRE_2        | Digital Strategist |
| Cortese Giovanni         | @giovannicortese-dot  | Digital Strategist |
| Brancato Benedetto       | @benedettobrancato    | Software Developer |
| Chiampo Andrea           | @aChiampo             | Software Developer |
| Serrain Gabriele         | @bibriserra           | Software Developer |
| Panigari Federico        | @Federico-Panigari    | Web Developer      |
| Nigra-Gattinotta Daniele | @danielenigra-creator | Web Developer      |
| Borra Mirko              | @mirkoborra-pixel     | Web Developer      |

**Team Leader:** @Raffo-DSG1
## Lore

**DA DEFINIRE:** nome, ambientazione, personaggi e asset definitivi. Le fazioni e le caratteristiche delle carte sono descritte nella proposta di regolamento Versione 2.1, ancora da confermare. Le carte non saranno interamente generate con intelligenza artificiale.

Riferimento: [Lore, identità e carte](docs/Digital/lore-identita-carte.md).

## Regole del gioco

È disponibile la **Versione 2.1 — Proposta per il prototipo e le prove di bilanciamento**. L’integrazione documentale iniziale risale al **2026-10-27**; i riferimenti correnti sono allineati alla Versione 2.1 su richiesta dell’utente del **2026-11-09**. Il regolamento descrive composizione dei mazzi, energia e punti potenza, fazioni, equipaggiamenti, combattimento automatico, round e conclusione della partita.

L'integrazione documentale è approvata; l'approvazione definitiva delle regole, il bilanciamento e la conferma della formula del danno restano aperti. Il prototipo descritto nel [README del motore e della campagna](springboot_app/game_engine/README.md) non equivale all'implementazione completa delle funzionalità; consultare quel documento per i comandi e le verifiche del modulo. Le verifiche del backend F01 non verificano il regolamento o il motore di gioco.

Riferimento: [Regolamento e svolgimento del gioco — Versione 2.1](docs/Digital/regolamento-base.md), con [elementi da completare e testare](docs/Digital/regolamento-base.md#15-elementi-da-completare-e-testare). Le modalità aggiuntive non sono confermate.

## Struttura del repository

```text
/
├── README.md
├── Linee-Guida-e-Regole.md
├── AGENTS.md
├── CLAUDE.md
├── .gitignore
├── .github/
│   ├── ISSUE_TEMPLATE/
│   │   ├── funzionalita.md
│   │   ├── problema.md
│   │   └── documentazione.md
│   └── pull_request_template.md
├── docs/
│   ├── README.md
│   ├── modello-funzionalita.md
│   ├── lista-verifica-consegna.md
│   ├── Digital/
│   ├── FrontEnd/
│   └── BackEnd/
├── compose.yaml
├── cinder-rift/
│   ├── Dockerfile
│   ├── nginx.conf
│   └── src/
└── springboot_app/
    ├── app/
    └── game_engine/
```

`react_app/` resta un segnaposto e non è usata da Compose. I documenti di funzionalità usano nomi italiani e si collegano tra aree; codice, commenti nel codice e risorse tecniche usano l'inglese.

Il modulo `springboot_app/game_engine/` contiene il motore e il package dimostrativo della campagna nella stessa applicazione Spring Boot; non esiste più un modulo `game_campaign` separato. Per dettagli e comandi consultare il [README del motore e della campagna](springboot_app/game_engine/README.md).

Le istruzioni condivise per gli assistenti sono in [AGENTS.md](AGENTS.md);
[CLAUDE.md](CLAUDE.md) importa lo stesso file. Le regole complete restano nelle
[linee guida](Linee-Guida-e-Regole.md). Le specifiche si approfondiscono
progressivamente: il [modello](docs/modello-funzionalita.md) distingue le proposte
dalle decisioni confermate e definisce il dettaglio tecnico atteso. L'[indice documentale](docs/README.md#stato-delle-funzionalità)
riepiloga lo stato corrente, distinguendolo dalle evidenze storiche di verifica.

## Collaborazione Git

- `main`: versione approvata per la produzione.
- `dev`: versione integrata più completa, ancora da approvare come finale.
- Branch distinti per attività di area: `feature/<id-attivita>-<descrizione-inglese>`.
- FrontEnd e BackEnd della stessa funzionalità condividono il contratto nella pagina BackEnd, mantenendo branch separati e collegati all'ID Fxx/Dxx.
- Funzionalità verso `dev` tramite pull request; promozione da `dev` a `main` tramite pull request.
- Nessun commit diretto su `main` o `dev`.

Le [regole Git](Linee-Guida-e-Regole.md#collaborazione-git) includono esempi. Le protezioni remote sono ancora da configurare; numero di approvazioni e strategia di merge restano da decidere. `main` non implica un deploy automatico.

Esempio illustrativo: `feature/fa06-marketplace` e
`feature/ba07-trade-logic-implementation` sono attività distinte collegate a F06
e al medesimo contratto. Gli ID attività non sostituiscono l'ID della funzionalità.

## Avvio e verifica

Per il primo utilizzo esegui [set-up-windows.bat](set-up-windows.bat) o [set-up-macos.command](set-up-macos.command): preparano Git e Docker Desktop e clonano `dev` sul Desktop. Dopo aver completato le eventuali installazioni e rilanciato il set-up, usa [start-up-windows.bat](start-up-windows.bat) o [start-up-macos.command](start-up-macos.command) dalla cartella clonata: aggiornano il progetto, preparano `.env` e la chiave JWT e ricostruiscono i container. [Procedura completa e destinazioni](guida-avvio.md#preparare-il-computer-e-scaricare-il-progetto). Modifica documentale del **2026-11-17**, su richiesta dell’utente di separare preparazione e avvio.

Per l’avvio manuale, configura la chiave JWT e segui la procedura Flyway per un database nuovo o esistente, poi esegui dalla radice:

```text
docker compose --file compose.yaml up --build --wait
```

Il frontend è disponibile su `http://localhost:8082`; `/api/` viene inoltrato al backend. Login e registrazione usano F01 reale; catalogo e collezione usano F04 con Bearer, filtri e paginazione server. Non è disponibile una modalità mock. Mazzi F05 e simulazione PvE F07 dispongono di chiamate React; il builder mostra la collezione privata con inserimento tramite clic o trascinamento negli slot. [Comportamento e limiti F05](docs/FrontEnd/gestione-mazzi.md). Il motore usa H2 per la campagna e non salva i risultati. [Verifiche F04 e limiti](docs/FrontEnd/collezione-carte.md#verifiche-e-limiti) · [Evidenze backend F05/F07 del 2026-11-20](https://github.com/aChiampo/gruppo-03-Cinder-Rift/blob/73ad701a20b4fbd3e540689e92b5569a4c1e4da0/docs/report/2026-11-20-orchestrazione-pve.md) · [Guida di avvio locale](guida-avvio.md).

Build e comandi specifici: [backend](springboot_app/app/README.md#build-dellimmagine-docker), [frontend](cinder-rift/README.md#avvio-con-docker-compose), [motore](springboot_app/game_engine/README.md#avvio-con-docker-compose). [Migrazioni Flyway F01](springboot_app/app/README.md#migrazioni-flyway-f01).

Con il backend avviato sulla porta locale predefinita `8081`, puoi consultare e provare le API in [Swagger UI](http://localhost:8081/swagger-ui/index.html). Se hai impostato `APPLICATION_PORT` in Compose, usa quella porta. [Configurazione del backend](springboot_app/app/README.md#configurazione-f01).

Non inserire credenziali reali. Gli esempi di ambiente, i lockfile e i wrapper di build restano versionabili secondo la `.gitignore`.
