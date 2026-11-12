# Avviare Cinder Rift sul proprio computer

Guida aggiornata il **2026-11-12** per allineare i prerequisiti di autenticazione allo stack corrente, su richiesta dell’utente. L’avvio locale descritto usa la versione di sviluppo (`dev`), quando i relativi componenti sono disponibili su quel branch.

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

Prima di avviare il backend, configura `APP_SECURITY_JWT_SECRET` nel file locale `.env` della cartella del repository o nell’ambiente da cui lanci Compose. Su un database nuovo Flyway crea le tabelle F01 e inserisce i ruoli all’avvio. Se il database contiene già lo schema F01, completa la procedura di adozione prima di avviare il backend. [Migrazioni Flyway F01](springboot_app/app/README.md#migrazioni-flyway-f01).

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
4. Il file scarica gli aggiornamenti del progetto da `dev`, avvia Docker Desktop se serve e prepara l'applicazione. Il primo avvio può richiedere alcuni minuti. Se Docker Desktop chiede di completare la configurazione, finiscila e poi riprova.

Se compare un errore di aggiornamento, copia il messaggio e mandalo a chi si occupa del progetto.

## Aprire l'applicazione nel browser

Quando leggi **Applicazione pronta**, apri [Cinder Rift su questo computer](http://localhost:8082). Puoi chiudere la finestra del terminale e lasciare aperto Docker Desktop.

L'indirizzo funziona sul computer dove hai avviato l'applicazione. Se compare un errore perché manca `compose.yaml` o un file chiamato `Dockerfile`, chiedi al gruppo se la configurazione di avvio e i componenti dell'applicazione sono già disponibili su `dev`.

## Avviare l'applicazione dal terminale

Apri Docker Desktop e attendi che sia pronto. Poi apri il terminale nella cartella principale del progetto. Su Windows puoi aprire la cartella in Esplora file, scrivere `powershell` nella barra degli indirizzi e premere Invio. Su macOS puoi scrivere `cd ` con uno spazio alla fine, trascinare la cartella del progetto nella finestra del Terminale e premere Invio.

Copia un comando alla volta e premi Invio. Se un comando restituisce un errore, fermati e condividi il messaggio con il gruppo.

```text
git switch dev
git pull --ff-only origin dev
docker compose --file compose.yaml up --build --wait
```

Per vedere quali componenti sono in esecuzione, usa:

```text
docker compose --file compose.yaml ps
```

Quando hai finito, puoi fermare l'applicazione con:

```text
docker compose --file compose.yaml stop
```
