# Battaglie PvE nel backend applicativo

ID funzionalità: F07; dipendenze F01, F05 e F08.
Specifica: decisioni applicative consolidate; strategia di lock e recupero **DA DEFINIRE**.
Implementazione applicativa: non iniziata. Responsabile: **DA DEFINIRE**.
Ultimo aggiornamento significativo: **2026-11-20**, su richiesta dell'utente di raccogliere la discussione e le alternative in documenti dedicati.
Fonti: decisioni dell'utente nella conversazione del 2026-11-19/20; [PR #36](https://github.com/aChiampo/gruppo-03-Cinder-Rift/pull/36), revisione `7018b4f2a1f0e1bb33d5d0afc586e4170079451d`.
Verifica applicativa: non eseguita in questa attività documentale.

## Scopo e riferimenti canonici

Descrivere l'orchestrazione di una battaglia PvE nel flusso `frontend → springboot_app/app → game-engine`. Il browser si rivolge esclusivamente all'applicazione; il motore rimane un servizio interno, raggiungibile in Compose su `http://game-engine:8083`. Il binding locale del motore resta disponibile per debug.

Questa pagina approfondisce la porzione applicativa di [F07 — Sfide e simulazione](sfide-simulazione.md). Il documento [API delle battaglie per il FrontEnd](app-battaglie-api.md) è l'unico riferimento canonico di endpoint, header, payload e risposte HTTP di questa porzione. Qui sono definiti flusso server, concorrenza, dipendenze e decisioni aperte, senza duplicare i payload.

La PR #36 è ancora aperta e non integrata in `dev` alla consultazione del **2026-11-20**. Implementa l'API interna dimostrativa del motore, non l'orchestrazione nell'applicazione. Il contratto pubblico descritto nel documento API è pianificato e non è disponibile nel codice di questa base `dev` (`8a77ee5`). L'attività modifica soltanto documentazione, senza anticipare l'integrazione della PR.

## Decisioni consolidate

| Tema | Decisione |
| --- | --- |
| Identità | L'applicazione ricava l'utente dal principal autenticato dopo la validazione del Bearer JWT e della sessione F01. L'identità non si riceve nel body. |
| Mazzo | Il frontend invia il solo ID del mazzo; il backend verifica la proprietà e carica le copie con i dati server. |
| ID delle carte | Si usa direttamente l'ID del database, senza tabella di mapping né identificativi alternativi. La rappresentazione decimale come stringa richiesta dal DTO del motore non cambia l'identità. |
| Pool dimostrativi | Il gruppo aggiornerà gli ID e i riferimenti dei pool, dei mazzi nemici e delle fixture per allinearli al database. Nessuna traduzione verso i vecchi codici mock è introdotta da questo contratto. |
| Validazione | Il gruppo aggiornerà le regole del motore. L'applicazione verifica proprietà e giocabilità secondo F05 prima dell'invio; le differenze correnti sono dipendenze da risolvere, non nuove regole da approvare qui. |
| Resoconto | Si mantiene la struttura della risposta della PR #36 e il suo log testuale ordinato. Non si introduce il modello annidato di round, incontri e scambi precedentemente proposto. |
| Tutorial | I messaggi tutorial sono esclusi dalla risposta pubblica, sia dal campo dedicato sia dal prefisso del log. |
| Concorrenza | Una richiesta di battaglia attiva per utente. Gli invii sovrapposti sono rifiutati, senza accodamento né seconda chiamata al motore. |
| Timeout | Limite complessivo HTTP di **10 secondi**. |
| Persistenza PvE | **DA DEFINIRE**. |
| Ricompense e progressione della campagna | **DA DEFINIRE**. |
| Scoperta degli stage | **DA DEFINIRE**. |

L'autenticazione e la proprietà sono obbligatorie per ogni chiamante. Non si introduce una delega all'uso dei mazzi altrui o una gerarchia implicita tra `ADMIN` e `USER`. Il flag `is_challengeable` riguarda la disponibilità al matchmaking: non certifica da solo la validità e non diventa un prerequisito PvE.

## Flusso applicativo da implementare

1. Validare autenticazione F01 e forma della richiesta pubblica. Ricavare ID e nickname dal principal; non accettare identità o statistiche inviate dal browser.
2. Tentare l'acquisizione atomica del guard di battaglia associato all'ID utente. Se è già occupato, terminare secondo il contratto API senza attendere la battaglia precedente.
3. Caricare il mazzo con una ricerca vincolata al proprietario autenticato. Un mazzo assente e uno appartenente ad altri rimangono indistinguibili al chiamante.
4. Rivalidare le copie e preparare dati coerenti per il motore, seguendo [F05](gestione-mazzi.md) e il [protocollo transazionale del database](Database/database-schema-v1.1.md#protocollo-transazionale-comune). Un precedente controllo del frontend non sostituisce questa operazione. I soli ID non garantiscono statistiche immutabili se i cataloghi vengono modificati tra controllo ed esecuzione: coordinamento del catalogo tra i servizi **DA DEFINIRE**.
5. Costruire i metadati del partecipante usando il profilo server; espandere le copie del mazzo nel formato interno della PR. La corrispondenza tra ID database e pool aggiornati è diretta.
6. Chiamare l'API interna con un budget temporale compatibile con i 10 secondi complessivi. Non inoltrare il Bearer dell'utente come meccanismo di autenticazione del motore: l'eventuale protezione server-to-server resta **DA DEFINIRE**; l'API dimostrativa non autentica i metadati del partecipante.
7. Validare la risposta, eliminare i contenuti tutorial e costruire la risposta pubblica. Gli errori interni vanno tradotti nel contratto API; un difetto di allineamento dei pool non deve essere presentato come errore imputabile al mazzo dell'utente.
8. Terminare la richiesta e rilasciare il guard di cui si è proprietari, secondo la politica di timeout/recupero che verrà selezionata. Il rilascio dopo errori di validazione, quando il motore non è stato avviato, non richiede riconciliazione.

La scelta di lock non autorizza a mantenere automaticamente aperta una transazione sui dati utente per l'intera chiamata remota. Validazione, preparazione dei dati e guard di esecuzione sono responsabilità distinte.

## Adattamento del resoconto della PR

Nella revisione consultata, `PveBattleService` costruisce il log completo anteponendo i suggerimenti tutorial al log del motore e restituisce anche i suggerimenti separatamente. Eliminare soltanto il campo dedicato lascerebbe quindi gli stessi messaggi nel resoconto pubblico.

L'adattatore applicativo deve verificare il blocco iniziale corrispondente ai suggerimenti ricevuti, rimuoverlo e omettere il campo tutorial. Il resto del log conserva ordine, stringhe, spazi e ritorni a capo. Non filtrare per parola chiave e non eliminare globalmente messaggi uguali: l'operazione riguarda il prefisso definito dal contratto interno. Un'incoerenza del formato interno richiede gestione come errore di contratto, senza inoltrare suggerimenti per errore. Il titolo dello stage è metadato e rimane invariato, anche se contiene la parola «Tutorial».

L'esito strutturato già presente nella PR è autorevole rispetto al testo del log. Non si deduce il vincitore confrontando nomi o analizzando frasi. Non vengono introdotti ID di battaglia, eventi strutturati, premi o stato della campagna nella risposta.

## Concorrenza, durata del lock e richieste ripetute

Il guard è riferito all'utente, non alla sessione, al mazzo o allo stage: due schede o due sessioni dello stesso utente concorrono per lo stesso accesso. Gli altri utenti restano indipendenti. Dopo il termine e il rilascio del guard, un nuovo invio avvia una nuova simulazione; non si definisce una deduplicazione permanente delle richieste concluse.

L'acquisizione deve essere atomica. Un controllo «libero» seguito da un'assegnazione separata non protegge dalla concorrenza. Solo il proprietario corrente può rilasciare o rinnovare il guard. La disabilitazione del pulsante nel frontend non sostituisce questa protezione server.

Il termine della richiesta è osservato dal backend: non è possibile attendere una prova che il browser abbia ricevuto la risposta. La sola disconnessione del browser non dimostra che il lavoro del motore sia terminato.

Occorre distinguere due garanzie:

- **Una richiesta HTTP attiva per utente:** il guard può essere rilasciato alla conclusione HTTP, incluso il timeout. Una precedente simulazione potrebbe però continuare nel motore.
- **Una simulazione attiva per utente anche dopo un guasto:** il guard resta finché completamento o arresto sono accertati. Può sopravvivere alla risposta di timeout; durante il recupero gli invii restano rifiutati.

L'utente ha confermato il rifiuto delle richieste sovrapposte e il timeout HTTP. La scelta della garanzia dopo timeout/crash e della relativa strategia resta **DA DEFINIRE**: nessuna delle alternative seguenti è selezionata da questo documento.

## Opzioni per lock, istanze multiple e recupero

| Opzione | Meccanismo | Coordinamento tra istanze | Recupero e limiti |
| --- | --- | --- | --- |
| **L1 — Guard in memoria** | Struttura atomica per ID utente nel processo Java. | No; ammissibile solo con una singola istanza dichiarata. | Al crash il guard locale scompare, ma il motore remoto può continuare. Non soddisfa da solo il coordinamento tra più backend. |
| **L2 — Advisory lock PostgreSQL** | Acquisizione non bloccante su chiave di dominio battaglie e ID utente. | Sì, usando lo stesso PostgreSQL. | Nessuna nuova tabella; il lock di sessione termina al rilascio esplicito o alla chiusura della sessione DB. Occorre riservare una connessione per richiesta attiva. La perdita della sessione non arresta il motore. |
| **L3 — Registro operativo PostgreSQL** | Claim atomico per utente con identificativo richiesta, proprietario, stato e scadenza operativa. | Sì. | Stato ispezionabile da altre istanze; recupero dei record abbandonati e riconciliazione col motore. Richiede migrazione e logica operativa, senza implicare che lo storico F08 sia definito. |
| **L4 — Lease Redis** | Chiave per utente con token proprietario, durata e rinnovo. | Sì. | Nuovo servizio e dipendenza da concordare. La scadenza permette recupero, ma non prova la cessazione del vecchio lavoro; richiede regole di proprietà, rinnovo e guasto. |

### L2: gestione della connessione

Usare una funzione di tentativo, ad esempio `pg_try_advisory_lock`, per non accodare la battaglia. Un lock di sessione si acquisisce e si rilascia sulla stessa connessione fisica, mantenuta riservata fino alla pulizia. Restituirla al pool non chiude la sessione: il lock potrebbe restare attivo o essere riacquisito dalla stessa sessione. Il rollback della transazione non rilascia un lock di sessione.

Al crash il rilascio avviene quando PostgreSQL rileva la fine della sessione; non è necessariamente immediato. I lock di transazione, invece, richiederebbero di mantenere aperta una transazione per tutta la loro durata: non sono la variante implicitamente scelta da L2.

### L3: registro operativo e proprietà

Campi indicativi, non uno schema approvato: `user_id`, `request_id`, `owner_id`, `status`, `lease_until`, `updated_at`. Claim, rinnovo e pulizia sono operazioni atomiche con transazioni brevi. La pulizia deve verificare il proprietario e l'ID della richiesta, impedendo a una vecchia esecuzione di eliminare il guard di quella nuova.

La scadenza indica che il proprietario potrebbe essere perso, non che il motore sia fermo. Con la garanzia forte, una lease scaduta entra nel recupero prima di autorizzare una nuova simulazione. Un token che impedisce a una vecchia richiesta di scrivere risultati non impedisce, da solo, la sovrapposizione fisica di due calcoli.

### L4: dipendenza aggiuntiva

Redis non è richiesto dall'architettura corrente e non viene aggiunto da questa attività. L'opzione richiede rilascio condizionato al token proprietario e una politica di scadenza/rinnovo coerente con il lavoro del motore. Un TTL fisso di 10 secondi, da solo, non equivale a cancellazione né a esclusione sicura dopo un guasto.

## Opzioni dopo timeout HTTP

| Opzione | Comportamento | Quando si libera l'utente | Lavoro richiesto al motore |
| --- | --- | --- | --- |
| **T1 — Interrompere l'attesa** | Il backend termina la richiesta e scarta l'eventuale risultato tardivo. | Alla conclusione HTTP. | Minimo; il calcolo può continuare e sovrapporsi al successivo. |
| **T2 — Deadline cooperativa** | Il backend comunica un termine di esecuzione; il motore lo verifica prima dell'avvio e tra round/scambi. | Dopo l'arresto accertato; gli esiti incerti richiedono una politica di recupero. | Propagazione della deadline, controlli nel ciclo e uscita cooperativa. Non risolve da sola la perdita del backend. |
| **T3 — Tracciare, cancellare e riconciliare** | Operazione identificata; dopo timeout si richiede cancellazione o si consulta lo stato finché diventa certo. | Dopo completamento, fallimento o cancellazione effettivamente accertati; può essere oltre i 10 secondi. | Identificativi operativi, controllo degli avvii, stato durevole, consultazione/cancellazione e recupero. |

Il timeout HTTP resta **10 secondi** in tutte le opzioni. Il termine del motore e quello del backend devono lasciare un margine per produrre la risposta prima dell'abort frontend; i margini esatti sono **DA DEFINIRE**. Il wrapper React corrente interrompe l'attesa dopo 10.000 ms: ciò non dimostra l'arresto della simulazione.

### T1: garanzia limitata alla richiesta

La risposta tardiva viene ignorata e non si ritenta automaticamente la simulazione. Questa opzione permette un nuovo invio mentre il lavoro precedente può essere ancora attivo. È selezionabile soltanto accettando tale limite; non soddisfa la garanzia di un solo calcolo attivo per utente.

### T2: cancellazione cooperativa

Il motore controlla il termine anche prima di iniziare un lavoro arrivato in ritardo. Il solo annullamento della chiamata HTTP non esegue questi controlli. `Future.cancel(true)` tenta un'interruzione, che il codice deve gestire; `CompletableFuture.cancel(true)` non interrompe il calcolo sottostante. La cancellazione di un future non è una prova sufficiente dell'uscita del worker.

La cooperazione è adatta al ciclo di combattimento corrente, ma non garantisce un arresto istantaneo di thread sospesi o operazioni non interrompibili. Dopo un crash o una partizione resta da decidere come accertare che la vecchia esecuzione non continui.

### T3: ciclo operativo e riconciliazione

Stati indicativi da concordare: `RUNNING`, `COMPLETED`, `FAILED`, `CANCEL_REQUESTED`, `CANCELLED`. Una richiesta di cancellazione non equivale a una cancellazione completata.

Il backend assegna un ID interno; il motore controlla atomicamente gli avvii per richiesta e per utente. Un processo di recupero consulta o cancella la stessa operazione, senza rilanciarla alla cieca. Lo stato deve sopravvivere ai riavvii: H2 in memoria e un registro locale del solo processo non bastano. Un risultato «non trovato» non dimostra da solo che nulla stia eseguendo, soprattutto con richieste ritardate o istanze differenti.

I futuri endpoint interni di stato/cancellazione, il protocollo contro gli avvii tardivi, la durata di conservazione operativa e l'accesso del motore allo stato durevole sono **DA DEFINIRE**. Non si introduce ora alcuna rotta di polling o cancellazione per il frontend. Se il motore è irraggiungibile, la garanzia forte può mantenere l'utente bloccato fino al recupero.

## Combinazioni da scegliere

| Combinazione | Uso previsto | Limite o costo |
| --- | --- | --- |
| **L2 + T1** | Minimo intervento con coordinamento tra backend. | Il lavoro remoto può sopravvivere al timeout o al crash. |
| **L2 + T2** | Soluzione intermedia per il prototipo con PostgreSQL esistente. | Connessione riservata e incertezze residue dopo crash o partizioni; non è una garanzia forte di recupero. |
| **L3 + T3**, con controlli di deadline | Recupero esplicito e controllo delle esecuzioni durante esiti incerti. | Registro operativo, protocollo del motore e possibile blocco oltre la risposta HTTP. |
| **L4 + T3** | Coordinamento Redis se si sceglie tale infrastruttura. | Nuovo servizio, semantica delle lease e registro delle operazioni. |

L1 resta un'alternativa limitata a una sola istanza. L2 + T2 è stata suggerita nella discussione come compromesso per il prototipo; L3 + T3 come soluzione per la garanzia forte. Sono valutazioni tecniche, **non decisioni dell'utente**. Non implementare una combinazione finché non viene selezionata.

## Persistenza, ricompense e campagna

[F08](storico-partite.md) e la [specifica del database](Database/database-schema-v1.1.md#storico-battaglie) documentano già `BATTLE_HISTORY`: due partecipanti con FK obbligatorie a utenti, vincitore nullable, fotografie dei mazzi, data e variazione di classifica. Le migrazioni applicative V1–V6 non creano lo storico.

Non è corretto dichiarare assente ogni documentazione della tabella, ma quella esistente non definisce il bot PvE né la conservazione del log testuale della PR. Rappresentazione dell'avversario, record PvE, formato del log salvato, conservazione e migrazione restano **DA DEFINIRE**. Non inventare un utente bot o una nuova tabella di storico in questa fase.

Ricompense e progressione della campagna restano **DA DEFINIRE**, senza applicazione implicita delle regole di ranking PvP. Anche la scoperta degli stage rimane **DA DEFINIRE**. Un eventuale registro operativo L3/T3 non decide automaticamente questi aspetti di dominio.

## Criteri di accettazione da verificare

| ID | Scenario | Risultato atteso |
| --- | --- | --- |
| F07-APP-01 | Richiesta valida con proprio mazzo e sessione F01 attiva. | Una chiamata al motore, risposta conforme al documento API. |
| F07-APP-02 | Token assente, scaduto o sessione revocata. | Nessuna chiamata al motore e rifiuto di autenticazione. |
| F07-APP-03 | ID mazzo assente dal database oppure appartenente ad altri. | Stessa risposta pubblica, nessuna esposizione della proprietà altrui. |
| F07-APP-04 | Invii concorrenti dello stesso utente da sessioni, schede, mazzi o stage differenti. | Uno solo acquisisce il guard; gli altri sono rifiutati senza accodamento. Per L2/L3/L4 provare due istanze. |
| F07-APP-05 | Errore di validazione dopo acquisizione del guard e prima dell'avvio remoto. | Nessuna simulazione; pulizia del solo guard posseduto. |
| F07-APP-06 | Risposta con suggerimenti separati e anteposti al log. | Nessun suggerimento nel payload pubblico; resto del log invariato. |
| F07-APP-07 | Pool aggiornati e mazzo database. | Stessi ID, con sola conversione numerica/stringa dove richiesta; nessun mapping ai codici mock precedenti. |
| F07-APP-08 | Deadline HTTP raggiunta o browser disconnesso. | Risposta/errore di trasporto coerenti con il contratto; comportamento del guard conforme all'opzione selezionata. |
| F07-APP-09 | Crash, perdita della sessione DB, lease scaduta o risposta del motore persa. | Garanzie e recupero verificati secondo L/T selezionati; nessuna pulizia da parte di un vecchio proprietario. |
| F07-APP-10 | Risposta inattesa del motore o cataloghi non allineati. | Errore di integrazione, senza attribuire il difetto al chiamante. |

Gli scenari di crash e cancellazione diventano verificabili in modo definitivo dopo la scelta della strategia. Non costituiscono test eseguiti.

## Decisioni aperte e stato delle verifiche

- **DA DEFINIRE:** combinazione L/T, garanzia dopo timeout e momento di rilascio del guard in esito incerto.
- **DA DEFINIRE:** dettagli di recupero, coordinamento tra eventuali istanze del motore, margini temporali e cancellazione.
- **DA DEFINIRE:** consistenza dei dati di catalogo durante la preparazione/esecuzione e protezione delle chiamate interne.
- **DA DEFINIRE:** persistenza PvE, ricompense, progressione della campagna e scoperta degli stage.
- Dipendenze note: aggiornamenti dei pool e della validazione del motore da parte del gruppo; orchestrazione applicativa e adattatore frontend da implementare.

La revisione della PR dichiara 25 test JUnit superati il 2026-11-19. È un'evidenza riportata dall'autore, non una nuova esecuzione di questa attività né una verifica dell'integrazione applicativa.

Controlli documentali eseguiti il **2026-11-20** sui due nuovi file e sui sei riferimenti aggiornati: 6 esempi JSON validi, 204 link relativi con destinazioni/ancore esistenti, blocchi di codice bilanciati e invarianti dei payload verificate; `git diff --check` senza errori. Verificata anche l'invarianza dei 16 file già modificati o non tracciati nel checkout originario, tramite confronto SHA-256. Nessuna build o prova runtime è stata eseguita; questi controlli non attestano il funzionamento del flusso.

## Fonti tecniche e collegamenti

- [PR #36 — Endpoint PvE del motore](https://github.com/aChiampo/gruppo-03-Cinder-Rift/pull/36) e [servizio nella revisione consultata](https://github.com/aChiampo/gruppo-03-Cinder-Rift/blob/7018b4f2a1f0e1bb33d5d0afc586e4170079451d/springboot_app/game_engine/src/main/java/com/benedetto/game_engine/battle/service/PveBattleService.java).
- [PostgreSQL 18 — Advisory lock](https://www.postgresql.org/docs/18/explicit-locking.html#ADVISORY-LOCKS) e [funzioni di tentativo e rilascio](https://www.postgresql.org/docs/18/functions-admin.html#FUNCTIONS-ADVISORY-LOCKS).
- [Redis — Lock distribuiti](https://redis.io/docs/latest/develop/clients/patterns/distributed-locks/).
- [Java 21 — Future](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/concurrent/Future.html#cancel(boolean)) e [CompletableFuture](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/concurrent/CompletableFuture.html#cancel(boolean)).
- [API pubblica per il FrontEnd](app-battaglie-api.md) · [F07](sfide-simulazione.md) · [F01](accesso-utenti.md) · [F05](gestione-mazzi.md) · [F08](storico-partite.md).
- [Interfaccia F07](../FrontEnd/sfide-simulazione.md) · [Regolamento](../Digital/regolamento-base.md) · [Indice BackEnd](README.md) · [Stato delle funzionalità](../README.md#stato-delle-funzionalità).
