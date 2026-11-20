# Verifica dell'orchestrazione applicativa PvE

Data: **2026-11-20**. ID: **F07**, dipendenze **F01/F05**; F08 esclusa.
Riferimento: copia di lavoro non committata del branch `feature/app_battle_pve_flow`, base `83262dd`.
Fonte delle scelte: risposte dell'utente nella conversazione del 2026-11-20.

## Ambiente e comandi

- Windows, JDK **21.0.11**, Maven Wrapper dei due moduli, dipendenze locali in modalità offline.
- PostgreSQL **18.6**, immagine `postgres:18-alpine`, container dedicato `cinder-rift-f07-test`, porta locale `55439`, database di prova isolato. Il database applicativo dell'utente non è stato modificato.
- Motore con H2 in memoria per la campagna; per la prova fra servizi, JAR del motore su `18083` e JAR applicativo su `18081`.
- Variabili di prova: `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD`, `APP_SECURITY_JWT_SECRET`, `SPRING_DOCKER_COMPOSE_ENABLED=false`; nessuna credenziale reale.

Dalla cartella di ciascun modulo, con `JAVA_HOME` impostato al JDK 21:

```text
# Applicazione: suite completa e packaging
mvnw.cmd -o "-Daether.syncContext.named.factory=rwlock-local" clean verify -q

# Motore: suite completa, seguita da packaging senza rieseguire i test
mvnw.cmd -o "-Daether.syncContext.named.factory=rwlock-local" test -q
mvnw.cmd -o "-Daether.syncContext.named.factory=rwlock-local" -DskipTests package -q

# Applicazione: ripetizione mirata dopo la revisione della chiusura dei guard
mvnw.cmd -o "-Daether.syncContext.named.factory=rwlock-local" "-Dtest=BattleFlowDatabaseTests" verify -q
# Dopo aver arrestato i JAR temporanei della prova HTTP, packaging finale
mvnw.cmd -o "-Daether.syncContext.named.factory=rwlock-local" -DskipTests package -q
```

## Risultati

Suite completa applicativa: **212 test**, zero fallimenti, errori o casi saltati; JAR prodotto.
Suite completa del motore: **49 test**, zero fallimenti, errori o casi saltati; JAR prodotto.

La ripetizione mirata ha superato **22 test F07** dopo la revisione della chiusura dei guard. Il relativo obiettivo `verify` ha poi incontrato un lock Windows sul JAR ancora usato dal processo della prova HTTP; arrestati i due processi temporanei, il packaging separato è riuscito. Questo errore di packaging non era un fallimento dei test.

| Ambito | Evidenza |
| --- | --- |
| Identità e contratto pubblico | JWT reali F01; `USER` e `ADMIN`; token assenti, non validi e revocati rifiutati; body mancanti/null, campi extra, stringhe, decimali, booleani e overflow rifiutati. |
| Proprietà e giocabilità | Mazzi mancanti e altrui indistinguibili; composizione completa richiesta indipendentemente da `is_challengeable`; errori di giocabilità `422` senza chiamata remota. |
| Regole condivise F05 | Quote di rarità, fasce inclusive di potenza, budget indipendenti 65/10 senza overflow, conteggio copie per carta attraverso arti e destinazioni, minimi come warning nelle bozze e errori nella validazione completa. |
| Snapshot | Dati e identità esclusivamente server; ID numerici, ordine effetti SQL e una voce per copia; test di commit concorrente del catalogo fra validazione e lettura statistiche, con vista `REPEATABLE READ` coerente. |
| Guard L2 | Acquisizione PostgreSQL attraverso due istanze del componente; stesso utente rifiutato, altri indipendenti; lock utente e mazzo disponibili durante il lavoro remoto; doppia chiusura di un vecchio handle non rilascia il nuovo proprietario. |
| Timeout e cleanup | Motore HTTP di prova bloccato oltre il budget; risposta `504` entro 10 secondi e nuovo guard acquisibile mentre l'esito remoto resta incerto. Nessun secondo avvio automatico. |
| Adattamento | Rimozione del solo prefisso tutorial verificato; campo omesso, titolo e altre righe/spazi/ritorni a capo conservati, incluse righe `[EFFECT]` e ripetizioni successive del testo dei suggerimenti. Esito strutturato anche con nomi uguali. |
| Errori interni | `404`/`409` con sole proprietà approvate; `400`/`422` e risposte interne malformate tradotte in `502`; indisponibilità `503`, timeout `504`; nessun Bearer inoltrato. |
| Deadline T2 | Header scaduto `504` e malformato `400`; interruzione osservata; checkpoint nel ciclo di combattimento interrompono senza restituire un risultato parziale. |
| Migrazione V9 | Installazione vuota V1–V9, aggiornamento da V8, seconda esecuzione senza migrazioni; ID, possesso, effetti, valori non nulli e carte estranee preservati. Mazzo delle 30 carte CR01 migrato valido per F07. |

Prova HTTP fra i due JAR effettivi, con login F01 e mazzo CR01 del database di prova: `POST /api/battle/pve/stage_01` ha restituito **200**, esito `DRAW`, **216 righe** di combattimento, nessun campo tutorial, identità coerente con il token e durata osservata **1124 ms**. Gli invii allo stage con evento inattivo e a uno stage sconosciuto hanno restituito rispettivamente **409** e **404**. Il documento OpenAPI espone la rotta pubblica.

Controlli finali: `docker compose --file compose.yaml config --quiet` e `git diff --check` superati; verificati **14 documenti modificati**, **384 link relativi/ancore** e **13 esempi JSON**, senza errori. I due processi Java temporanei sono stati arrestati e il container PostgreSQL di prova con i suoi dati è stato rimosso al termine delle verifiche.

## Limiti

- La prova usa processi Java locali e PostgreSQL Docker isolato; non è una distribuzione delle nuove immagini sullo stack applicativo dell'utente.
- Browser e integrazione React non verificati; nessuna persistenza PvE, ricompensa o progressione introdotta.
- La politica approvata rilascia il guard al termine HTTP anche in esito incerto. Non si dichiara esclusione forte del calcolo dopo crash o partizioni, né sono stati iniettati crash del runtime o perdite della sessione DB.
- Il budget inizia prima dell'autenticazione, ma il timer dell'orchestratore parte dopo autenticazione e decodifica, sottraendo il tempo trascorso. Blocchi in tali fasi, pause del runtime e operazioni non interrompibili non sono un limite temporale rigido garantito.
- La prova fra due componenti guard usa connessioni PostgreSQL distinte nello stesso processo; non è una prova di failover fra due JVM applicative.

[Specifica applicativa e stato corrente](../BackEnd/app-battaglie.md) · [Contratto pubblico](../BackEnd/app-battaglie-api.md) · [F05](../BackEnd/gestione-mazzi.md).
