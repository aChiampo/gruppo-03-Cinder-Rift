# Gioco di carte collezionabili digitale

> Nome progetto e numero gruppo: **DA DEFINIRE**. Stato: documentazione iniziale; applicazioni non inizializzate.

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

1. Registrazione e accesso con email e password; un servizio nell'app Spring Boot gestisce JWT e ruoli `ADMIN`, `MANAGER`, `USER`.
2. Acquisto con crediti di offerte contenenti più pacchetti, anche di tipi diversi; prezzi e composizione sono gestibili dal backoffice.
3. Apertura dei pacchetti e consultazione della collezione; composizione e validazione dei mazzi.
4. Scambi privati o pubblici nel marketplace, con carte e crediti, consenso di entrambe le parti e durata standard da definire.
5. Sfide con scelta dei mazzi, simulazione sul server e storico con risultati turno per turno.

Le modalità di assegnazione dei crediti in fase 1 restano **DA DEFINIRE**; i pagamenti reali sono esclusi dallo sviluppo previsto dalla traccia.

### Terminologia commerciale

| Termine | Significato | Esempio illustrativo, non configurazione approvata |
| --- | --- | --- |
| Offerta di pacchetti | Crediti in cambio di un insieme di pacchetti | 1.000 crediti → 2 pacchetti «Rare» + 1 «Ultra» |
| Pacchetto | Contenitore di carte con composizione e apertura da definire | Un pacchetto del tipo «Rare» |
| Carta | Elemento della collezione utilizzabile nei mazzi | Creatura con attacco, difesa e fazione |

Il termine «bundle» indica un'offerta di pacchetti. Gli esempi non stabiliscono prezzi, nomi commerciali, rarità o quantità definitive.

## Tecnologie

Stack inizialmente indicato: React, Java/Spring Boot e PostgreSQL. Il gruppo comunicherà lo **stack definitivo** in seguito: versioni, librerie, linguaggio frontend, build e distribuzione restano da definire.

La traccia prevede GitHub, GitHub Projects, Swagger, JSDoc per JS/TS e motore Java isolato con test JUnit. Gli strumenti non sono ancora configurati.

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

**DA DEFINIRE:** nome, ambientazione, personaggi e asset definitivi. Le fazioni e le caratteristiche delle carte sono descritte nella proposta di regolamento Versione 2.0, ancora da confermare. Le carte non saranno interamente generate con intelligenza artificiale.

Riferimento: [Lore, identità e carte](docs/Digital/lore-identita-carte.md).

## Regole del gioco

È disponibile la **Versione 2.0 — Proposta per il prototipo e le prove di bilanciamento**, integrata nella documentazione il **2026-10-27**. Descrive composizione dei mazzi, energia e punti potenza, fazioni, equipaggiamenti, combattimento automatico, round e conclusione della partita. Il testo del regolamento è riportato senza modifiche; metadati e collegamenti sono separati dalle regole.

L'integrazione documentale è approvata; l'approvazione definitiva delle regole, il bilanciamento e la conferma della formula del danno restano aperti. Le applicazioni non sono inizializzate e non sono state eseguite verifiche applicative.

Riferimento: [Regolamento e svolgimento del gioco — Versione 2.0](docs/Digital/regolamento-base.md), con [elementi da completare e testare](docs/Digital/regolamento-base.md#15-elementi-da-completare-e-testare). Le modalità aggiuntive non sono confermate.

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
	├── applicazione/
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

Le applicazioni non sono inizializzate: non sono disponibili comandi applicativi o test eseguibili. Prerequisiti, configurazione, migrazioni, build e test saranno documentati dopo lo stack definitivo.

Non inserire credenziali reali. Gli esempi di ambiente, i lockfile e i wrapper di build restano versionabili secondo la `.gitignore`.
