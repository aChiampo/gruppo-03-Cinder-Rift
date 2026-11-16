# Invio mail

ID funzionalità: F01, dipendenza del recupero password; eventuali altri utilizzatori DA DEFINIRE.
Perimetro: documentazione della funzionalità futura, su richiesta dell’utente.
Specifica: in approfondimento.
Implementazione: non iniziata; soltanto commenti segnaposto nel servizio F01.
Responsabile: DA DEFINIRE.
Fonte delle decisioni: richiesta dell’utente del 2026-11-12 di rinviare l’invio e usare template Thymeleaf.
Ultimo aggiornamento significativo: 2026-11-12.
Ultima verifica: non eseguita; nessun invio o rendering attestato.

## Obiettivo, perimetro e dipendenze

Il servizio futuro renderizzerà template **Thymeleaf** per creare il contenuto delle email e ne gestirà la consegna. Il frontend resta React: Thymeleaf è destinato ai messaggi e non introduce pagine di autenticazione lato server. `spring-boot-starter-mail` e `spring-boot-starter-thymeleaf` sono già dichiarati nel [POM](../../springboot_app/app/pom.xml); la presenza delle dipendenze non equivale a un servizio configurato.

Per F01 sono previsti due punti di integrazione: invio del link di recupero dopo il commit della creazione del token e notifica dopo il commit del cambio password. Il [contratto canonico F01](accesso-utenti.md#recupero-password-base-backend) definisce token, durata, API, transazione e sessioni; questa pagina non ne mantiene una copia concorrente.

## Stato corrente e confine transazionale

`AuthenticationService` contiene soltanto commenti `TODO [issue-pending]` nei due punti di integrazione. Non esistono implementazioni di consegna, template, composizione dell’URL, eventi di invio, coda, outbox o retry. Il token in chiaro viene generato nel servizio e scartato al termine della richiesta; non è restituito dall’API e non è recuperabile dal suo hash. Non è quindi ancora possibile consegnarlo successivamente usando il solo database attuale.

L’implementazione futura dovrà acquisire il materiale necessario durante la richiesta e attivare la consegna **dopo il commit**. Non inviare messaggi all’interno della transazione né dichiarare la consegna sulla sola base del `202`. La gestione dei fallimenti tra commit e invio, la persistenza dell’eventuale messaggio e il recupero dopo arresto richiedono una decisione esplicita; un semplice callback dopo commit non garantisce consegna affidabile.

## Contenuti e configurazione da definire

- Template Thymeleaf per recupero e conferma del cambio password; nome dei file, variabili, lingua, oggetto e versione testuale alternativa DA DEFINIRE. La password non deve comparire nei messaggi.
- Provider SMTP, host, porta, TLS, autenticazione, mittente e gestione delle credenziali tramite configurazione esterna DA DEFINIRE; nessuna credenziale reale nei documenti.
- URL frontend del recupero, composizione del link, dominio e HTTPS DA DEFINIRE nell’attività di invio. Nessuna impostazione relativa ai link è implementata nella base attuale.
- Modalità di consegna, timeout, retry, deduplicazione, gestione dei fallimenti e osservabilità DA DEFINIRE. Non sono state aggiunte tabelle o migrazioni per una coda/outbox.
- Responsabile, ID attività e issue/PR DA DEFINIRE; non assegnati da questo documento.

## Esclusioni e verifiche future

L’utente ha chiesto di non dedicare questa attività all’implementazione email. Le esclusioni dei controlli di recupero richieste nello stesso messaggio sono registrate nel [contratto F01](accesso-utenti.md#controlli-esclusi-su-richiesta-esplicita-dellutente); questa pagina non le considera implementate.

Quando il servizio sarà sviluppato occorrerà verificare rendering e variabili dei template, consegna dopo commit, assenza di invio in caso di rollback e comportamento definito per errori e retry. Nessuna di queste prove è stata eseguita nell’attività corrente. Il flusso completo dipenderà anche dall’integrazione frontend, esclusa dalla base backend.

## Collegamenti

- [F01 — Accesso e recupero password](accesso-utenti.md)
- [Interfaccia F01](../FrontEnd/accesso-utenti.md)
- [Dipendenze e configurazione backend](../../springboot_app/app/README.md)
- [Indice BackEnd](README.md)
- [Indice generale](../README.md)
