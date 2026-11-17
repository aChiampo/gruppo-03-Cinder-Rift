# Avviare Cinder Rift sul proprio computer

Guida aggiornata il **2026-11-16**, su richiesta dell’utente di completare Compose. La copia corrente contiene Dockerfile per backend F01/F05, prototipo React in `cinder-rift/` e motore dimostrativo in `springboot_app/game_engine/`. L’avvio dei container non attesta l’integrazione delle funzionalità: il login React richiede ancora l’allineamento a F01 e il motore non espone API di gioco. Lo stato è riferito alla copia di lavoro; i launcher scaricano `dev`, quindi richiedono che le modifiche siano già integrate e che non vi siano conflitti Git locali.

Build, avvio dei quattro servizi, salute, migrazioni e proxy HTTP sono verificati il **2026-11-16** in uno stack isolato; **19 test del motore superati**. [Report con ambiente e limiti](docs/report/2026-11-16-verifica-docker-compose.md). L’integrazione del login e i launcher non sono verificati.


## Configurazione dei servizi Compose

Il primo build richiede Internet per scaricare immagini, pacchetti npm e dipendenze Maven. Non servono Java o Node.js installati sul computer per usare Compose. Prima dell’avvio configura la chiave JWT come descritto nella sezione sul doppio clic e nel [README backend](springboot_app/app/README.md#configurazione-f01).

Le variabili per Compose si impostano nel `.env` della **radice del repository** o nella shell. Il `.env` dentro `cinder-rift/` riguarda invece lo sviluppo Vite locale ed è escluso dal contesto Docker.

| Variabile | Valore predefinito | Effetto |
| --- | --- | --- |
| `APP_SECURITY_JWT_SECRET` | Nessuno utilizzabile | Obbligatoria per avviare il backend; chiave Base64 di almeno 32 byte casuali |
| `POSTGRES_DB`, `POSTGRES_USER`, `POSTGRES_PASSWORD` | `cinder_rift`, `postgres`, `postgres` | Database locale; i valori predefiniti non sono credenziali di produzione |
| `POSTGRES_PORT` | `5432` | Porta locale del database |
| `APPLICATION_PORT` | `8081` | Porta locale del backend; la porta interna resta `8081` |
| `REACT_PORT` | `8082` | Porta locale del frontend; la porta interna resta `8082` |
| `ENGINE_PORT` | `8083` | Porta locale del motore; la porta interna resta `8083` |
| `VITE_API_URL` | `/api` | Argomento di build pubblico; Nginx inoltra `/api/` a `spring_app:8081` preservando il percorso |
| `VITE_USE_MOCK` | `false` | Argomento di build pubblico; `true` abilita il login simulato del prototipo |
| `APP_SECURITY_ALLOWED_ORIGINS` | `http://localhost:8082` | Origini browser autorizzate; aggiornare se cambia origine o porta del client |

I valori `VITE_*` sono incorporati durante il build, come descritto nella [documentazione Vite 5](https://v5.vite.dev/guide/env-and-mode). Dopo averli modificati esegui `docker compose up --build --wait react`; un semplice riavvio non aggiorna il JavaScript. Non inserirvi segreti. Il proxy usa la porta interna del backend, quindi non cambia al variare di `APPLICATION_PORT`.

Il login React usa ancora il contratto del server simulato, diverso da [F01](docs/BackEnd/accesso-utenti.md): percorso del profilo, payload di login/risposta, cookie, CSRF e gestione dei token richiedono allineamento. `VITE_USE_MOCK=true` permette di esplorare la grafica senza verificare il backend; non avvia il server Node `mock-api`. Collezione, mazzi e home usano ancora dati dimostrativi. [Dettagli frontend](cinder-rift/README.md).

Il motore usa H2 in memoria, senza collegarsi al database applicativo; i dati vengono persi al riavvio. Compose disabilita il menu con `GAME_ENGINE_CLI_ENABLED=false`. L’endpoint `/actuator/health` verifica il servizio, ma non rappresenta un’API di simulazione né un collegamento già implementato con `spring_app`. [Dettagli motore](springboot_app/game_engine/README.md#avvio-con-docker-compose).

Le porte sono pubblicate solo su `127.0.0.1`. PostgreSQL conserva i dati nel volume `cinder-rift-postgres-data`; `stop` e `down` lo mantengono. `down --volumes` elimina il database. Backend e motore attendono le dipendenze sane; React attende il backend. I tre servizi applicativi hanno filesystem in sola lettura, `/tmp` scrivibile e controlli di salute.

## Scaricare e installare Docker Desktop

**Windows**

1. Scarica l'installatore adatto al tuo PC. [Download di Docker Desktop per Windows](https://docs.docker.com/desktop/setup/install/windows-install/)
2. Apri il file scaricato e segui le indicazioni. Riavvia il PC se richiesto.
3. Apri **Docker Desktop** dal menu Start e completa la configurazione iniziale.

**macOS**

1. Dal menu Apple  scegli **Informazioni su questo Mac**. Se vedi un chip Apple, scarica la versione **Apple silicon**. Se vedi un processore Intel, scegli **Intel**. [Download di Docker Desktop per Mac](https://docs.docker.com/desktop/setup/install/mac-install/)
2. Apri il file `Docker.dmg` e trascina **Docker** nella cartella **Applicazioni**.
3. Apri **Docker** da **Applicazioni** e completa la configurazione iniziale.

## Avviare il progetto con un doppio clic

Ti servono una connessione a Internet, Git installato e l'accesso al progetto su GitHub. [Download di Git](https://git-scm.com/install/)

Prima di avviare il backend, configura `APP_SECURITY_JWT_SECRET` nel file locale `.env` della cartella del repository o nell’ambiente da cui lanci Compose. Su un database nuovo Flyway applica V1–V4 all’avvio. Se il database contiene già lo schema F01 o tabelle F05, completa la procedura di adozione e riconciliazione prima di avviare il backend. [Migrazioni Flyway](springboot_app/app/README.md#migrazioni-flyway-f01).

La chiave JWT deve essere casuale, codificata in Base64 e rappresentare almeno 32 byte; non esiste una chiave predefinita utilizzabile. Le chiamate di autenticazione usano `/api/auth`; per una porta frontend diversa da `8082`, aggiorna anche `APP_SECURITY_ALLOWED_ORIGINS` all’origine esatta del client. Usa lo stesso nome host nei due URL locali. La [pulizia delle sessioni](springboot_app/app/README.md#pulizia-delle-sessioni-f01) è attiva ogni giorno alle 03:00 UTC mentre il backend è avviato.

La base del [recupero password F01](docs/BackEnd/accesso-utenti.md#recupero-password-base-backend) richiede anche la tabella `PASSWORD_RESET_TOKENS` già prevista dalla v1.1. Il suo job separato elimina token scaduti o usati alle 03:00 UTC; `APP_SECURITY_PASSWORD_RESET_TOKEN_CLEANUP_CRON=-` lo disabilita. L’invio email è rinviato e non richiede configurazione SMTP per questa base; il recupero completo non è ancora disponibile all’utente. [Invio mail futuro](docs/BackEnd/invio-mail.md).

Se ti manca una copia del progetto o hai scaricato un file ZIP, apri il terminale nella cartella in cui vuoi salvarlo ed esegui questo comando.

```text
git clone --branch dev https://github.com/aChiampo/gruppo-03-Cinder-Rift.git
```

Se GitHub ti chiede di accedere, usa il tuo account con accesso al progetto. Puoi seguire i passaggi qui sotto quando il gruppo ha reso disponibili i file di avvio e l'applicazione su `dev`.

1. Apri la cartella principale del progetto, dove trovi i file di avvio.
2. Su **Windows**, fai doppio clic su [start-windows.bat](start-windows.bat).
3. Su **macOS**, la prima volta apri **Terminale**. Scrivi `chmod +x ` con uno spazio alla fine, trascina [start-macos.command](start-macos.command) dal Finder nella finestra del Terminale e premi Invio. Poi fai doppio clic sul file nel Finder. Dalle volte successive basta il doppio clic.
4. Il file scarica gli aggiornamenti del progetto da `dev`. Se Docker Desktop non è installato, scarica l'installer ufficiale e si ferma: installalo e rilancia il file. Se è già installato, lo avvia se necessario. Prima di Compose genera una chiave JWT casuale nel `.env` locale solo se manca e controlla che il file e la variabile siano presenti; non sovrascrive le altre impostazioni già presenti. Dopo che i servizi superano i controlli di salute, apre il frontend nel browser. Il primo avvio può richiedere alcuni minuti. Se Docker Desktop chiede di completare la configurazione, finiscila e poi riprova.

Se compare un errore di aggiornamento, copia il messaggio e mandalo a chi si occupa del progetto.

## Aprire l'applicazione nel browser

Quando leggi **Applicazione pronta**, apri [Cinder Rift su questo computer](http://localhost:8082). Puoi chiudere la finestra del terminale e lasciare aperto Docker Desktop.

L'indirizzo funziona sul computer dove hai avviato l'applicazione. Se cambi `REACT_PORT`, usa la porta scelta. La salute del frontend è disponibile su `/healthz`; quella del motore su `http://localhost:8083/actuator/health`. Un container sano non garantisce l’integrazione del login o delle partite.

## Avviare l'applicazione dal terminale

Apri Docker Desktop e attendi che sia pronto. Poi apri il terminale nella cartella principale del progetto. Su Windows puoi aprire la cartella in Esplora file, scrivere `powershell` nella barra degli indirizzi e premere Invio. Su macOS puoi scrivere `cd ` con uno spazio alla fine, trascinare la cartella del progetto nella finestra del Terminale e premere Invio.

Copia un comando alla volta e premi Invio. Aggiorna da `dev` solo con una copia di lavoro senza modifiche da proteggere o conflitti Git. Se stai verificando modifiche locali, salta i due comandi Git. Se un comando restituisce un errore, fermati e condividi il messaggio con il gruppo.

```text
git switch dev
git pull --ff-only origin dev
docker compose --file compose.yaml config --quiet
docker compose --file compose.yaml up --build --wait
```

Per vedere quali componenti sono in esecuzione, usa:

```text
docker compose --file compose.yaml ps
```

Per diagnosticare un servizio non sano, usa `docker compose logs spring_app game-engine react`. Per verificare il motore separatamente, `docker compose build game-engine` compila senza eseguire i test; esegui la suite JUnit con i comandi del [README motore](springboot_app/game_engine/README.md#avvio-e-verifiche).

Quando hai finito, puoi fermare l'applicazione con:

```text
docker compose --file compose.yaml stop
```
