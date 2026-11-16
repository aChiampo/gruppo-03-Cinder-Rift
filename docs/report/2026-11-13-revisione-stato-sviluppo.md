# Revisione dello stato di sviluppo — 2026-11-13

## Ambito e riferimento

Revisione richiesta dall’utente per errori e allineamento della documentazione allo sviluppo corrente. ID interessati: F01 e F05; stato generale delle altre funzionalità verificato rispetto ai componenti presenti.

Riferimento: copia di lavoro di `feature/f01-accesso-utenti`, base `97200d9e827649418267ef9e81502a7def6bd7bb`, comprendente modifiche non committate già presenti e correzioni di questa revisione. Il riferimento locale `origin/dev` coincideva con tale base; `dev` locale era due commit indietro. Non sono stati eseguiti fetch, cambi di branch o commit. Il report attesta questa copia, senza garantire lo stato remoto successivo.

## Errori corretti

| Problema osservato | Correzione e riscontro |
| --- | --- |
| Le richieste Bearer potevano saltare il CSRF richiesto per la cancellazione account: il test con JWT reale riceveva `204` senza CSRF | Applicata al `CsrfFilter` la politica esplicita dopo l’inizializzazione dei configurer, evitando l’esenzione Bearer predefinita del resource server. Il test PostgreSQL usa bootstrap, cookie e header reali e verifica rifiuti e successo; sei casi aggiuntivi coprono i POST F01 con header Bearer |
| Registrazione senza limite del nickname, a differenza del contratto e del PATCH profilo | `RegistrationRequest` normalizza il nickname prima della validazione e applica il massimo di 50 caratteri; test per limite, superamento, valori nulli/vuoti e rifiuto HTTP prima del servizio |
| Test profilo rimasto sul percorso precedente | Aggiornato a `GET /api/auth/user/me` |
| Test cancellazione lasciavano connessioni del pool sullo schema temporaneo poi eliminato, causando quattro errori SQL nei test mazzi | Ripristinato lo schema originale prima di restituire ogni connessione al pool; l’intera suite ora passa nello stesso processo |
| Contratto F01 con quattro percorsi obsoleti e senza cancellazione account; cinque link puntavano alla sezione mancante | Allineate tabelle, esempi, header, permessi, transazione e cookie al codice; aggiunta la sezione canonica collegata dalle altre aree |
| Guide ferme a F01/V2 o con frontend descritto come già realizzato | Documentati F01/F05, V1–V4, seed effettivi e comando Compose limitato al backend disponibile; aggiornati indici, istruzioni e stato delle verifiche |

[Sicurezza](../../springboot_app/app/src/main/java/webapp/auth/SecurityConfiguration.java) · [Controller F01](../../springboot_app/app/src/main/java/webapp/auth/AuthController.java) · [Test cancellazione con database](../../springboot_app/app/src/test/java/webapp/auth/AccountDeletionDatabaseTests.java) · [Test catena di sicurezza](../../springboot_app/app/src/test/java/webapp/auth/SecurityConfigurationTests.java) · [Test registrazione](../../springboot_app/app/src/test/java/webapp/auth/RegistrationRequestTests.java).

## Ambiente e comandi

- Windows, JDK 21.0.11 selezionato tramite `JAVA_HOME`; Maven Wrapper configurato per Maven 3.10.0; Spring Boot 4.1.2.
- PostgreSQL **18.6**, versione restituita da `SHOW server_version` nell’immagine locale `postgres:18-alpine`; non 18.7 come nelle evidenze precedenti.
- Container di prova dedicato con nome casuale, porta casuale su loopback e database vuoto `cinder_rift_audit`. Nessun volume o database applicativo usato. Container fermato e rimosso al termine.
- Variabili `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD` e `APP_SECURITY_JWT_SECRET` impostate nel solo processo di prova con valori temporanei. `SPRING_DOCKER_COMPOSE_ENABLED=false`; entrambi i cron di pulizia disabilitati durante le prove applicative.

Dalla cartella `springboot_app/app`:

```powershell
.\mvnw.cmd -o '-Daether.syncContext.named.factory=rwlock-local' verify
```

La modalità offline usa le dipendenze già presenti nella cache Maven locale. Da un ambiente nuovo occorre prima risolverle. Dalla radice è stato eseguito anche `docker compose config --quiet`.

## Risultati

Verifica completata il **2026-11-13 alle 17:51 Europe/Rome**:

```text
Tests run: 118, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

La suite comprende 15 classi: sicurezza, autenticazione, profili, cancellazione account, recupero password, pulizie dei token, validatore, servizio e controller mazzi. L’avvio dei contesti applicativi ha applicato con successo le quattro migrazioni Flyway su database vuoto. I test con database verificano anche rollback e concorrenza; quelli HTTP usano MockMvc, inclusi JWT reali e il flusso CSRF della cancellazione. Il packaging ha prodotto `springboot_app/app/target/app-0.0.1-SNAPSHOT.jar`.

La verifica iniziale senza database aveva superato 67 test prima delle nuove regressioni. Il risultato completo di 118 test sopra è quello corrente; i precedenti 31, 44, 52 e 70 test riportati nelle schede restano evidenze storiche dei rispettivi ambiti.

Controlli documentali: corrispondenza delle 16 rotte di `AuthController`, `UserController` e `DeckController` con i riepiloghi API; **460 collegamenti locali e ancore Markdown**, senza riferimenti mancanti; nessuna tabella Markdown con numero incoerente di colonne; `git diff --check` superato. La configurazione Compose è valida. I report Surefire e il log locale `springboot_app/app/target/audit-verify.log` sono artefatti non versionati, sostituibili da esecuzioni successive; questo report conserva il riepilogo verificato.

## Limiti e punti aperti

- Non eseguiti build dell’immagine backend, avvio dell’applicazione tramite Compose, prove HTTP sul JAR, test browser o verifiche dei job a orario reale. Il container di questa revisione ospita soltanto il database dei test.
- React e motore di gioco restano segnaposto; email e template Thymeleaf non sono implementati. I launcher completi richiedono componenti ancora assenti. F03, F04 come servizio/API, F06, F07, F08, F10 e F13 non diventano implementati per la sola presenza di tabelle o dipendenze.
- V1–V4 realizzano una porzione del modello v1.1. V3 usa `ON DELETE CASCADE` per le righe dei mazzi, mentre il DBML non la dichiara: [differenza esplicita](../BackEnd/Database/database-schema-v1.1.md#perimetro-sql-implementato), da riconciliare senza dedurre un’approvazione di nuove regole.
- Restano aperti il guard condiviso del catalogo, le regole avanzate V2.1 e la compatibilità tra energia aggregata e parametri iniziali dei round, già descritti nel [contratto F05](../BackEnd/gestione-mazzi.md#implementazione-backend-disponibile). Il superamento dei test non valida il bilanciamento.
- L’adozione di database già popolati e la procedura di baseline non sono state rieseguite in questa revisione. La guida ora distingue anche la presenza di tabelle F05 preesistenti.
- Identità e stack frontend richiedono coerenza documentale finale: il README mantiene nome/numero da confermare e un account GitHub mancante, pur riportando nove membri e Team Leader. Nessuna casella di consegna complessiva è completata da questa revisione.

[Stato corrente delle funzionalità](../README.md#stato-delle-funzionalità) · [Contratto F01](../BackEnd/accesso-utenti.md) · [Contratto F05](../BackEnd/gestione-mazzi.md) · [Guida di avvio](../../guida-avvio.md).
