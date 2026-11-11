# Avviare Cinder Rift sul proprio computer

Guida aggiornata il **2026-11-11** per l'avvio locale dalla versione di sviluppo (`dev`).

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
