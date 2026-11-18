# Verifica della configurazione Docker Compose

Data: **2026-11-16**. Riferimento: copia di lavoro con base `6208de5`, durante il completamento Compose richiesto dall’utente. Ambito: infrastruttura comune, prototipi FrontEnd F01/F04/F05 e motore dimostrativo collegato a F07; nessun contratto applicativo modificato.

## Ambiente e isolamento

Windows, Docker Desktop 4.79.0, Docker Engine 29.5.3 con container Linux. Build Java con Temurin 21, build frontend con Node.js 22, runtime Nginx Alpine e PostgreSQL 18 Alpine. Versioni applicative e dipendenze restano quelle dei POM e del lockfile.

La prova usa il progetto separato `cinder-rift-compose-check-20261116`, un volume PostgreSQL dedicato e vuoto e le porte locali `15432`, `18081`, `18082`, `18083`. Un file temporaneo esterno al repository contiene credenziali generate per la prova; nessun segreto è riportato qui. Un override cambia soltanto il nome del volume; le variabili delle porte e della sicurezza vengono fornite tramite `--env-file`. Il database applicativo abituale non è usato.

## Controlli ed esiti

| Controllo | Esito |
| --- | --- |
| `docker compose config --quiet` | Superato |
| `docker compose build react game-engine spring_app` | Tutte e tre le immagini costruite; frontend compilato dai sorgenti con `npm ci`, senza usare il `dist/` versionato |
| Avvio isolato con `up --no-build --wait --wait-timeout 180` | PostgreSQL, backend, React e motore sani |
| Tabelle Flyway nel database vuoto | V1, V2, V3 e V4 presenti con `success=true` |
| Salute backend su `18081` e motore su `18083` | HTTP 200 da `/actuator/health` |
| Nginx su `18082` | HTTP 200 per `/healthz`, `/`, un percorso SPA, un’immagine e il JavaScript generato, con tipi MIME attesi |
| Proxy API | `/api/auth/csrf-tokens` restituisce HTTP 200 e `application/json` tramite Nginx |
| Utente e filesystem dei tre servizi applicativi | React usa `nginx`; i servizi Java usano `10001:10001`; tutti con root filesystem in sola lettura e stato sano |
| JUnit motore | **19 test, 0 fallimenti, 0 errori, 0 saltati** |
| Documentazione e spaziatura | 200 collegamenti relativi e ancore verificati; `git diff --check` superato |

La suite JUnit è eseguita nel medesimo stage Java del Dockerfile, aggiungendo a una copia temporanea lo stage `verification` con `./mvnw -B -ntp test` e la stessa cache Maven. I Dockerfile applicativi saltano l’esecuzione dei test durante il packaging.

## Limiti

- Le prove HTTP non costituiscono una verifica visiva o una prova utente nel browser. Non è stato verificato un login React contro Spring: payload, risposta, profilo, cookie, CSRF e token del prototipo richiedono ancora allineamento a F01.
- La modalità `VITE_USE_MOCK=true` è documentata, ma non è stata oggetto di questa prova, che usa il build predefinito con valore `false`.
- Il motore usa H2 in memoria e non espone API di gioco. Salute e JUnit non attestano integrazione delle sfide, persistenza delle partite, campagna completa o approvazione delle regole.
- La suite completa del backend non è rieseguita; restano separate le [evidenze del 2026-11-13](2026-11-13-revisione-stato-sviluppo.md). Qui sono verificati build, avvio, migrazioni e salute del backend nel nuovo stack.
- `npm ci` segnala due vulnerabilità nelle dipendenze esistenti, una moderata e una alta; non sono state aggiornate dipendenze nell’attività Docker.
- I launcher che aggiornano `dev` non sono eseguiti. Il README conteneva conflitti preesistenti: il testo è stato riconciliato, ma l’indice Git resta non risolto fino alla revisione e allo staging dell’utente.

Il progetto di prova e il relativo volume sono stati rimossi al termine; le immagini locali restano disponibili. Questo report registra le evidenze della copia di lavoro, non una verifica di un commit o deploy successivo.

[Guida di avvio](../../guida-avvio.md) · [Indice corrente](../README.md) · [Frontend](../../cinder-rift/README.md) · [Motore](../../springboot_app/game_engine/README.md)
