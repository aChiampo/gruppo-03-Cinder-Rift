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
2. Acquisizione di crediti tramite offerte con acquisto simulato ed email di conferma; nessun addebito reale nella fase 1.
3. Acquisto con crediti di offerte contenenti più pacchetti, anche di tipi diversi; prezzi e composizione sono gestibili dal backoffice.
4. Apertura dei pacchetti e consultazione della collezione; composizione e validazione dei mazzi.
5. Scambi privati o pubblici nel marketplace, con carte e crediti, consenso di entrambe le parti e durata standard da definire.
6. Sfide con scelta dei mazzi, simulazione sul server e storico con risultati turno per turno.

### Terminologia commerciale

| Termine | Significato | Esempio illustrativo, non configurazione approvata |
| --- | --- | --- |
| Offerta di crediti | Importo monetario simulato in cambio di crediti | 2 $ → 1.000 crediti |
| Offerta di pacchetti | Crediti in cambio di un insieme di pacchetti | 1.000 crediti → 2 pacchetti «Rare» + 1 «Ultra» |
| Pacchetto | Contenitore di carte con composizione e apertura da definire | Un pacchetto del tipo «Rare» |
| Carta | Elemento della collezione utilizzabile nei mazzi | Creatura con attacco, difesa e fazione |

Le due offerte corrispondono ai due usi di «bundle» nelle risposte. Gli esempi non stabiliscono valuta, prezzi, nomi commerciali, rarità o quantità definitive.

## Tecnologie

Stack inizialmente indicato: React, Java/Spring Boot e PostgreSQL. Il gruppo comunicherà lo **stack definitivo** in seguito: versioni, librerie, linguaggio frontend, build e distribuzione restano da definire.

La traccia prevede GitHub, GitHub Projects, OpenAPI/Swagger, JSDoc per JS/TS e motore Java isolato con test JUnit. Gli strumenti non sono ancora configurati.

## Componenti del gruppo

| Nome e cognome | GitHub | Ruolo |
| --- | --- | --- |
| DA DEFINIRE — persona 1 | @DA_DEFINIRE_1 | Digital Strategist |
| DA DEFINIRE — persona 2 | @DA_DEFINIRE_2 | Digital Strategist |
| DA DEFINIRE — persona 3 | @DA_DEFINIRE_3 | Digital Strategist |
| DA DEFINIRE — persona 4 | @DA_DEFINIRE_4 | Web Developer |
| DA DEFINIRE — persona 5 | @DA_DEFINIRE_5 | Web Developer |
| DA DEFINIRE — persona 6 | @DA_DEFINIRE_6 | Web Developer |
| DA DEFINIRE — persona 7 | @DA_DEFINIRE_7 | Software Developer |
| DA DEFINIRE — persona 8 | @DA_DEFINIRE_8 | Software Developer |
| DA DEFINIRE — persona 9 | @DA_DEFINIRE_9 | Software Developer |

**Team Leader, repository remoto e GitHub Projects:** DA DEFINIRE.

## Lore

**DA DEFINIRE:** nome, ambientazione, personaggi e fazioni. Il lavoro è confermato, ma i contenuti non sono ancora scelti. Le carte non saranno interamente generate con intelligenza artificiale.

Riferimento: [Lore, identità e carte](docs/Digital/lore-identita-carte.md).

## Regole del gioco

**DA DEFINIRE:** regolamento completo, valori numerici, bilanciamento, composizione dei mazzi e condizioni di conclusione. La strategia della modalità base si concentra sulla composizione dei mazzi; il sistema simula le partite.

Riferimento: [Regolamento base](docs/Digital/regolamento-base.md). Le modalità aggiuntive non sono confermate.

## Struttura del repository

```text
/
├── README.md
├── Guide Lines and Rules.md
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
    └── .gitkeep
```

I file `.gitkeep` mantengono versionabili le cartelle applicative vuote. I documenti di funzionalità usano nomi italiani e si collegano tra aree; codice, commenti nel codice e risorse tecniche usano l'inglese.

Le istruzioni condivise per gli assistenti sono in [AGENTS.md](AGENTS.md);
[CLAUDE.md](CLAUDE.md) importa lo stesso file. Le regole complete restano nelle
[linee guida](Guide%20Lines%20and%20Rules.md). Le specifiche attuali rimangono
sintetiche: il [modello](docs/modello-funzionalita.md) definisce l'approfondimento
tecnico progressivo quando saranno disponibili le decisioni. L'[indice documentale](docs/README.md#stato-delle-funzionalità)
riepiloga lo stato corrente, distinguendolo dalle evidenze storiche di verifica.

## Collaborazione Git

- `main`: versione approvata per la produzione.
- `dev`: versione integrata più completa, ancora da approvare come finale.
- Branch distinti per attività di area: `feature/<id-attivita>-<descrizione-inglese>`.
- FrontEnd e BackEnd della stessa funzionalità condividono il contratto nella pagina BackEnd, mantenendo branch separati e collegati all'ID Fxx/Dxx.
- Funzionalità verso `dev` tramite pull request; promozione da `dev` a `main` tramite pull request.
- Nessun commit diretto su `main` o `dev`.

Le [regole Git](Guide%20Lines%20and%20Rules.md#collaborazione-git) includono esempi. Le protezioni remote sono ancora da configurare; numero di approvazioni e strategia di merge restano da decidere. `main` non implica un deploy automatico.

Esempio illustrativo: `feature/fa06-marketplace` e
`feature/ba07-trade-logic-implementation` sono attività distinte collegate a F06
e al medesimo contratto. Gli ID attività non sostituiscono l'ID della funzionalità.

## Avvio e verifica

Le applicazioni non sono inizializzate: non sono disponibili comandi applicativi o test eseguibili. Prerequisiti, configurazione, migrazioni, build e test saranno documentati dopo lo stack definitivo.

Non inserire credenziali reali. Gli esempi di ambiente, i lockfile e i wrapper di build restano versionabili secondo la `.gitignore`.
