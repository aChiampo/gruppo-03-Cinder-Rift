# Gioco di carte collezionabili digitale

> Nome progetto e numero gruppo: **DA DEFINIRE**. Stato: backend F01 e base del recupero password implementati; invio email, frontend e integrazione di autenticazione da avviare. Aggiornamento F01: **2026-11-12**, su richiesta dell’utente.

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

1. Registrazione e accesso con email e password. Il backend Spring Boot implementa JWT locali, rinnovo tramite cookie e ruoli `ADMIN` e `USER`; gli endpoint usano `/api/auth`. È disponibile anche la base del recupero password; la consegna email e l’interfaccia sono rinviate, quindi il flusso di recupero completo non è disponibile. [Contratto F01](docs/BackEnd/accesso-utenti.md) · [Invio mail futuro con Thymeleaf](docs/BackEnd/invio-mail.md).
2. Acquisto con crediti di offerte contenenti ciascuna esattamente un pacchetto; prezzo e configurazione del singolo pacchetto sono gestibili dal backoffice.
3. Apertura dei pacchetti e consultazione della collezione; composizione e validazione dei mazzi.
4. Scambi privati o pubblici nel marketplace, con carte e crediti, consenso di entrambe le parti e durata di due giorni dalla creazione dell’annuncio, secondo il [contratto F06](docs/BackEnd/scambi-mercato.md).
5. Sfide con scelta dei mazzi, simulazione sul server e storico con risultati turno per turno.

Le modalità di assegnazione dei crediti in fase 1 restano **DA DEFINIRE**; i pagamenti reali sono esclusi dallo sviluppo previsto dalla traccia.

### Terminologia commerciale

| Termine | Significato | Esempio illustrativo, non configurazione approvata |
| --- | --- | --- |
| Offerta di un pacchetto | Crediti in cambio di una sola unità di un pacchetto | 1.000 crediti → 1 pacchetto «Rare» |
| Pacchetto | Contenitore di carte con composizione e apertura da definire | Un pacchetto del tipo «Rare» |
| Carta | Elemento della collezione utilizzabile nei mazzi | Creatura con attacco, difesa e fazione |

Ogni offerta contiene esattamente un pacchetto; gli insiemi di più pacchetti non rientrano nel perimetro. Gli esempi non stabiliscono prezzi, nomi commerciali, rarità o quantità di carte definitive. Ultimo aggiornamento significativo di F03: **2026-11-04**, su istruzione dell'utente; riferimento canonico: [offerte e acquisto di pacchetti](docs/BackEnd/acquisto-pacchetti.md). Implementazione non iniziata; verifiche applicative non eseguite.

## Tecnologie

Il backend F01 usa Java 21, Spring Boot 4.1.2, Spring MVC, Spring Security e PostgreSQL tramite JDBC; il POM e Compose definiscono rispettivamente dipendenze e PostgreSQL 18. L’autenticazione è gestita nell’applicazione: password BCrypt, access token JWT HS256 e token di rinnovo opachi persistiti come hash. Il frontend previsto è React; versione, strumenti e integrazione client restano da completare. [Stack e configurazione backend](springboot_app/app/README.md).

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

L'integrazione documentale è approvata; l'approvazione definitiva delle regole, il bilanciamento e la conferma della formula del danno restano aperti. Le verifiche del backend F01 non verificano il regolamento o il motore di gioco.

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
├── react_app/
│   └── .gitkeep
└── springboot_app/
    ├── app/
    └── game-engine/
```

I file `.gitkeep` mantengono versionabili le cartelle applicative vuote. I documenti di funzionalità usano nomi italiani e si collegano tra aree; codice, commenti nel codice e risorse tecniche usano l'inglese.

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

La guida del [backend](springboot_app/app/README.md#build-dellimmagine-docker) descrive la build dell'immagine `spring_app`. La [guida di avvio locale](guida-avvio.md) e [Compose](compose.yaml) descrivono avvio e configurazione dei servizi. Prima dell’avvio configurare la chiave JWT e seguire la procedura Flyway per un database nuovo o esistente. [Migrazioni Flyway F01](springboot_app/app/README.md#migrazioni-flyway-f01).

Con il backend avviato sulla porta locale predefinita `8081`, puoi consultare e provare le API in [Swagger UI](http://localhost:8081/swagger-ui/index.html). Se hai impostato `APPLICATION_PORT` in Compose, usa quella porta. [Configurazione del backend](springboot_app/app/README.md#configurazione-f01).

Non inserire credenziali reali. Gli esempi di ambiente, i lockfile e i wrapper di build restano versionabili secondo la `.gitignore`.
