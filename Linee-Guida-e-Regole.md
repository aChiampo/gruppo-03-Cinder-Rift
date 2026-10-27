# Linee guida e regole

Regole aggiornate secondo le risposte del gruppo. Le scelte mancanti restano **DA DEFINIRE**. Questa guida non configura automaticamente GitHub.

## Lingua e stato delle informazioni

- Documentazione: prosa, titoli e nomi dei file in italiano.
- Codice, commenti nel codice e nomi delle risorse tecniche: inglese.
- Eccezioni: nomi richiesti o standard come `README.md`, `Guide Lines and Rules.md`, `Digital`, `FrontEnd`, `BackEnd`, cartelle applicative e percorsi GitHub.
- Distinguere perimetro confermato, specifica ancora parziale e implementazione effettiva.


## Documentazione per funzionalità

Usare `docs/[area]/[nome-funzionalita].md` con aree `Digital`, `FrontEnd`, `BackEnd`. Per i file usare italiano, minuscolo, kebab-case e nessun accento: `acquisto-crediti.md`, `scambi-mercato.md`.

Gli indici, il [modello](docs/modello-funzionalita.md) e la [lista di consegna](docs/lista-verifica-consegna.md) sono documenti trasversali.

Le pagine attuali restano sintetiche finché mancano le decisioni. Quando diventano disponibili, sviluppare progressivamente le sezioni pertinenti del [modello completo](docs/modello-funzionalita.md#modello-di-approfondimento-progressivo): attori e permessi, precondizioni, stati e transizioni, flussi e alternative, contratti con esempi, dati e vincoli, errori, transazioni, concorrenza, richieste ripetute, criteri di accettazione e verifiche. Descrivere il progetto, senza tutorial generici sulle tecnologie.

Una specifica approfondita deve consentire a un'altra area di integrare il comportamento senza dover indovinare payload, regole o casi limite. Per ogni sezione non ancora risolta scrivere **DA DEFINIRE**, indicando la decisione necessaria; per quelle non pertinenti usare **NON APPLICABILE** con una motivazione. Non trasformare esempi illustrativi in decisioni approvate.

### Contratto condiviso tra aree

Per una funzionalità che coinvolge FrontEnd e BackEnd, la pagina `docs/BackEnd/[nome-funzionalita].md` contiene il riferimento canonico di API, dati scambiati, autorizzazioni, errori e regole server concordate. La pagina FrontEnd collega quel contratto e descrive schermate, stati, accessibilità e comportamento del client, senza copiarne una versione indipendente. Regole di gioco, contenuti e ipotesi economiche restano nelle pagine Digital pertinenti e vengono richiamati per link.

Concordare il contratto tra le aree prima di sviluppare l'integrazione interessata. Una proposta non concordata resta riconoscibile come tale. Documentare le modifiche del contratto nel riferimento canonico, indicare compatibilità e impatto e collegare le PR dipendenti dei branch separati. Un comportamento previsto dal contratto può essere ancora non implementato: dichiararlo esplicitamente.

Documenti centrali separati saranno introdotti solo per architettura o convenzioni effettivamente comuni a più funzionalità, quando saranno disponibili decisioni sufficienti. Gli attuali ID Fxx/Dxx e la struttura per aree restano invariati.

**I documenti devono collegarsi tra loro dove utile**, formando una base di conoscenza navigabile. Ogni pagina deve essere raggiungibile dall'indice dell'area e rimandare alle dipendenze pertinenti.

Aggiornare nella stessa pull request il codice e la documentazione della porzione modificata. Se cambia un contratto condiviso, aggiornare il riferimento canonico e coordinare le PR delle altre aree, indicando dipendenze e stato dell'integrazione; non serve un unico branch per tutte le implementazioni.

### Stato corrente e riferimenti temporali

L'[indice della documentazione](docs/README.md#stato-delle-funzionalità) è il riepilogo dello stato corrente. Distinguere perimetro, maturità della specifica, implementazione e verifica; le pagine di area forniscono i dettagli e gli eventuali avanzamenti diversi. Aggiornare il riepilogo nella stessa PR quando cambia uno di questi stati, senza ripeterne la cronologia in tutti gli indici.

Nei nuovi documenti o negli approfondimenti sostanziali indicare l'ultimo aggiornamento significativo nel formato `AAAA-MM-GG`, la fonte della decisione e, se disponibili, issue/PR o commit pertinenti. La data di aggiornamento del testo è distinta dalla data dell'ultima verifica effettiva. Per la verifica indicare cosa è stato provato, riferimento del codice, ambiente, esito e limiti. Se non eseguita, dichiararlo; non attribuire retroattivamente date alle pagine esistenti e non cambiare la data per sole correzioni tipografiche.

Quando una verifica richiede un report, creare solo allora `docs/report/AAAA-MM-GG-descrizione.md`, con ID interessati, data, commit o PR, ambiente, controlli, risultati e limiti. Collegarlo dalla pagina corrente e precisare il suo carattere storico: non usarlo come backlog o fonte dello stato corrente. Più report nello stesso giorno usano descrizioni distinte. Non creare cartelle o report vuoti in anticipo; non è richiesto un registro separato delle decisioni.

## Formato dei commenti

Scrivere i commenti in inglese, spiegando motivazioni e vincoli non evidenti. Evitare commenti che ripetono il codice.

Usare JSDoc per i contratti JS/TS, Javadoc per i contratti Java e OpenAPI/Swagger per le API dopo la configurazione degli strumenti.

Esempio JavaScript, solo illustrativo:

```javascript
/**
 * Checks whether the available balance covers the requested cost.
 * @param {number} balance - Available credit balance.
 * @param {number} cost - Requested credit cost.
 * @returns {boolean} Whether the balance is sufficient.
 */
function canAfford(balance, cost) {
  return balance >= cost;
}
```

Esempio Java:

```java
/**
 * Validates a deck against the selected rule set.
 *
 * @param deck the deck to validate
 * @return the validation result and any rule violations
 */
ValidationResult validate(Deck deck);
```

Usare il formato `TODO [riferimento]: azione in inglese`. Collegare un'attività GitHub reale quando disponibile; non inventarne il numero.

```java
// TODO [issue-pending]: enforce the approved marketplace expiration policy.
```

## Regole generali di sviluppo


- Verificare autenticazione, permessi e proprietà delle risorse; nessuna gerarchia implicita tra `ADMIN`, `MANAGER`, `USER`.
- Non conservare password in chiaro e non inserire segreti nei sorgenti o nei log.
- Coprire il motore con JUnit; scegliere per gli altri componenti verifiche proporzionate al rischio.
- Aspettare lo stack definitivo prima di introdurre librerie, versioni o strumenti applicativi.

## Nomi e convenzioni

| Elemento | Convenzione | Esempio |
| --- | --- | --- |
| Documento di funzionalità | Italiano, kebab-case | `gestione-mazzi.md` |
| Componente React | Inglese, PascalCase | `DeckEditor` |
| Funzione o variabile | Inglese, camelCase | `acceptTrade`, `availableCredits` |
| Classe Java | Inglese, PascalCase | `TradeService` |
| Package Java | Inglese, minuscolo | `com.example.project.trade`; dominio da definire |
| Costante | Inglese, UPPER_SNAKE_CASE | `MARKETPLACE_TTL` |
| Tabella o colonna | Inglese, snake_case | `trade_offers`, `owner_id` |
| Risorsa REST | Inglese, plurale | `trades`, `credit-bundles` |
| Risorsa statica | Inglese, kebab-case | `card-back.webp` |
| Branch di attività | Inglese, ID attività minuscolo | `feature/fa06-marketplace`, `feature/ba07-trade-logic-implementation` |

Sono convenzioni di naming, non contratti o schemi già implementati. Vedere la [terminologia del progetto](README.md#terminologia-commerciale).

## Modello di documentazione

Copiare il [modello riutilizzabile](docs/modello-funzionalita.md) e compilare solo le informazioni disponibili. Il modello distingue la scheda breve attuale dall'approfondimento tecnico progressivo e specifica il contenuto atteso di ogni sezione. Non espandere automaticamente tutte le pagine esistenti: approfondire quelle interessate quando il gruppo fornisce le decisioni. Applicare i requisiti pertinenti anche alle pagine FrontEnd e Digital, senza imporre dettagli server a contenuti che non ne hanno.

Esempio di criterio, da sviluppare quando saranno definite le regole:

> Dato uno scambio scaduto, quando un utente tenta di accettarlo, allora il server rifiuta l'operazione senza modificare carte o saldi.



## Collaborazione Git

### Branch principali e flusso

- `main`: versione approvata per la produzione.
- `dev`: versione integrata più completa, non ancora approvata come finale.
- Non sono ammessi commit diretti su `main` e `dev`: ogni modifica passa da pull request.
- Creare i branch di funzionalità da `dev` aggiornato e integrarli in `dev` tramite pull request.
- Promuovere la versione approvata da `dev` a `main` tramite pull request.
- Produzione e distribuzione automatica non sono equivalenti: il deploy resta da definire.

### Nomi dei branch

Creare un branch distinto per ogni attività implementativa di area: `feature/<id-attivita>-<descrizione-inglese-in-kebab-case>`. FrontEnd e BackEnd lavorano su branch diversi anche quando contribuiscono alla stessa funzionalità e condividono il medesimo contratto.

Esempio illustrativo per F06 — scambi e marketplace:

| Area | ID attività | Branch | Riferimento comune |
| --- | --- | --- | --- |
| FrontEnd | FA06 | `feature/fa06-marketplace` | F06 e `docs/BackEnd/scambi-mercato.md` |
| BackEnd | BA07 | `feature/ba07-trade-logic-implementation` | F06 e `docs/BackEnd/scambi-mercato.md` |

Gli ID delle attività sono distinti dagli ID di funzionalità Fxx/Dxx: la numerazione può differire tra le aree, ma issue e PR devono dichiarare la relazione con la stessa funzionalità e collegare il contratto canonico. FA06 e BA07 sono esempi di naming, non attività GitHub già create o una rinumerazione di F06. Per le altre aree usare un ID attività concordato; non inventare assegnazioni.

Ogni branch parte da `dev` aggiornato e rientra in `dev` tramite una propria PR. Le PR collegate indicano dipendenze, contratto utilizzato e comportamento già disponibile. Una PR FrontEnd che usa un server simulato deve dichiararlo; la verifica integrata richiede la corrispondente implementazione BackEnd. Integrare in un ordine che mantenga coerente `dev`, rendendo esplicite le parti ancora incomplete.

Per correzioni successive: `fix/<id-attivita>-<descrizione-inglese>`, sempre da e verso `dev` tramite PR, collegando l'ID funzionalità. Per documentazione trasversale o un aggiornamento del solo contratto: `docs/<descrizione-inglese>`, con link alle funzionalità e alle PR coinvolte. La documentazione della singola implementazione accompagna il suo branch di area.


### Commit e revisione

Convenzione dei commit: `tipo(ambito): descrizione breve in inglese`, con tipi `feat`, `fix`, `docs`, `refactor`, `test`, `chore`.

Esempi: `feat(trades): validate participants before settlement`, `docs(f06): describe marketplace scope`.

Usare il [modello di pull request](.github/pull_request_template.md), collegare issue e pagine interessate e riportare i controlli eseguiti. Una specifica sintetica non equivale a un'implementazione completa.

Il vincolo di pull request è confermato. Numero minimo di approvazioni, revisori, strategia di merge e urgenze restano **DA DEFINIRE**. Una revisione da parte di un compagno è raccomandata.

Le protezioni di `main` e `dev` devono essere configurate in GitHub: i documenti non le applicano e non sono stati creati branch in questa fase.

## Modelli GitHub

- [Funzionalità](.github/ISSUE_TEMPLATE/funzionalita.md)
- [Problema](.github/ISSUE_TEMPLATE/problema.md)
- [Documentazione](.github/ISSUE_TEMPLATE/documentazione.md)
- [Pull request](.github/pull_request_template.md)

Esempio di descrizione in una pull request: «Documentati gli scambi pubblici e privati; durata e sequenza del consenso restano da definire».

[README](README.md) · [Indice documentazione](docs/README.md)
