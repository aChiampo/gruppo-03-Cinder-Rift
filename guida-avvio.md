# Avviare Cinder Rift sul proprio computer

Guida aggiornata il **2026-11-17**, su richiesta dell’utente: preparazione iniziale (`set-up`) separata dall’avvio quotidiano (`start-up`), con clone sul Desktop dell’utente. La copia corrente contiene Dockerfile per backend F01/F05, prototipo React in `cinder-rift/` e motore dimostrativo in `springboot_app/game_engine/`. L’avvio dei container non attesta l’integrazione delle funzionalità: il login React richiede ancora l’allineamento a F01 e il motore non espone API di gioco. Gli script usano `dev`: prima di distribuirli, le modifiche devono essere integrate su quel branch.



## Configurazione dei servizi Compose

Il primo build richiede Internet per scaricare immagini, pacchetti npm e dipendenze Maven. Non servono Java o Node.js installati sul computer per usare Compose. Gli script `start-up` preparano automaticamente la chiave JWT locale; per l’avvio manuale configurala come descritto nel [README backend](springboot_app/app/README.md#configurazione-f01).



## Preparare il computer e scaricare il progetto

Ti servono Internet, un sistema compatibile con Docker Desktop e un account GitHub autorizzato se il repository richiede l’accesso. Non serve avere già Git o una copia del progetto. Scarica **il file originale** di set-up per il tuo sistema usando il pulsante di download del file su GitHub; non salvare la pagina HTML. I due file sono autonomi e possono essere eseguiti dalla cartella Downloads.

| Sistema | Preparazione iniziale | Avvii successivi, dalla cartella clonata |
| --- | --- | --- |
| Windows | [set-up-windows.bat](set-up-windows.bat) | [start-up-windows.bat](start-up-windows.bat) |
| macOS | [set-up-macos.command](set-up-macos.command) | [start-up-macos.command](start-up-macos.command) |

### Windows

1. Fai doppio clic su `set-up-windows.bat`.
2. Se Git o Docker Desktop mancano, il set-up scarica gli installer ufficiali adatti al processore nella cartella `Downloads` del tuo account. Se mancano entrambi, tenta entrambi i download prima di fermarsi.
3. Apri gli installer scaricati e segui le indicazioni. Per Git, abilita l’utilizzo dal prompt dei comandi; non serve installare altri strumenti di sviluppo. Per Docker, segui i [prerequisiti Windows, inclusi WSL e virtualizzazione](https://docs.docker.com/desktop/setup/install/windows-install/). Riavvia il computer se richiesto, poi apri Docker Desktop e completa la configurazione iniziale.
4. Rilancia `set-up-windows.bat`: quando le dipendenze sono disponibili, clona `dev` in **Desktop → gruppo-03-Cinder-Rift**. Il percorso del Desktop è quello effettivo dell’account, anche se reindirizzato a OneDrive. Se GitHub richiede l’accesso, usa il tuo account autorizzato.

### macOS

1. La prima volta apri Terminale, scrivi `chmod +x ` con uno spazio alla fine, trascina `set-up-macos.command` dal Finder nel Terminale e premi Invio. Poi fai doppio clic sul file nel Finder. Se macOS blocca un file scaricato, controlla l’origine e segui le indicazioni di Privacy e sicurezza senza disabilitare le protezioni di sistema.
2. Se Git manca, il set-up avvia la richiesta di installazione degli **Apple Command Line Tools**: confermala e attendi il completamento. Non installa Homebrew. [Metodi di installazione Git per macOS](https://git-scm.com/install/mac).
3. Se Docker Desktop manca, scarica `~/Downloads/Docker.dmg` per Apple silicon o Intel. Apri il DMG, trascina Docker in Applicazioni e completa il primo avvio. [Prerequisiti e installazione Docker per macOS](https://docs.docker.com/desktop/setup/install/mac-install/).
4. Completa tutte le installazioni, poi rilancia il set-up: clona `dev` in **`~/Desktop/gruppo-03-Cinder-Rift`** e rende eseguibile lo script di start-up. Consenti l’accesso al Desktop se macOS lo richiede.

### Destinazione e ripetizione del set-up

Il set-up non avvia l'applicazione. Se la cartella esiste già, verifica che sia un checkout di questo repository e lo riutilizza, senza aggiornare i sorgenti. Se contiene file estranei, un altro repository o una copia ZIP priva di `.git`, si ferma: sposta o rinomina tu la cartella prima di riprovare. Nessun contenuto viene eliminato automaticamente. Un clone fallito può lasciare una cartella incompleta: controllala prima di spostarla e rilanciare il set-up.

Se esegui il set-up dalla radice di un checkout già esistente di Cinder Rift, riutilizza quella posizione anche se non è sul Desktop. Negli altri casi usa il Desktop. Il percorso scelto viene mostrato al termine. Se lo start-up manca nel clone, verifica che i nuovi script siano stati integrati su `dev`.

## Avviare il progetto con un doppio clic

1. Apri **Desktop → gruppo-03-Cinder-Rift**, oppure la posizione esistente indicata dal set-up.
2. Su Windows fai doppio clic su `start-up-windows.bat`; su macOS su `start-up-macos.command`. Se hai ottenuto lo script Mac senza eseguire il set-up, rendilo prima eseguibile con `chmod +x` come indicato sopra.
   Lo start-up :
   1. verifica Git, Docker e Compose; non scarica installer. Richiede una copia di lavoro senza modifiche Git locali, passa a `dev` e applica `git pull --ff-only origin dev`. Su macOS ignora le sole differenze del bit eseguibile durante questi comandi e ripristina il permesso di esecuzione dello start-up dopo l’aggiornamento, senza cambiare la configurazione Git. In caso di errori si ferma senza scartare file o risolvere conflitti automaticamente. Le impostazioni `.env`, ignorate da Git, restano locali.
   2. Se necessario apre Docker Desktop e attende il motore per un massimo di 120 secondi. Se non è pronto, completa la configurazione di Docker e riprova.
   3. Crea `.env` nella radice se manca e genera `APP_SECURITY_JWT_SECRET` quando assente o vuota.
   4. Verifica la configurazione con `docker compose --file compose.yaml config --quiet`, poi avvia `docker compose --file compose.yaml up --build --wait`. Dopo ogni pull riuscito, lo start-up richiede sempre il build delle immagini applicative: Docker riutilizza i passaggi invariati dalla cache e riesegue quelli interessati dalle modifiche e i passaggi successivi che ne dipendono. Per esempio, una modifica al codice React fa rieseguire la compilazione del frontend, mentre le dipendenze npm possono essere riutilizzate se i relativi file non cambiano. `--wait` attende che i servizi siano in esecuzione e superino gli eventuali controlli di salute; poi lo start-up apre automaticamente **http://localhost:8082** nel browser. Il primo build può richiedere diversi minuti.



### Se qualcosa non funziona

- **Dipendenze mancanti:** esegui il set-up, installa i componenti indicati e rilancialo. I download `.partial` indicano un trasferimento incompleto.
- **Accesso GitHub o rete:** controlla la connessione e l’account autorizzato. Non inserire token o password negli script.
- **Modifiche locali o aggiornamento Git fallito:** salva il lavoro con Git o chiedi assistenza al gruppo; lo start-up non esegue reset, stash o merge automatici.
- **Docker non pronto:** apri Docker Desktop, completa la configurazione e controlla i prerequisiti del sistema; poi riprova.
- **Chiave JWT non valida o duplicata:** correggi la singola assegnazione in `.env`. Le altre impostazioni e la chiave esistente non vengono sostituite automaticamente in questo caso.
- **Compose non sano o porta occupata:** controlla i messaggi nella finestra e usa i comandi diagnostici sotto. Per condividere un errore, copia il messaggio senza includere `.env` o segreti.

## Aprire l'applicazione nel browser

Quando leggi **Applicazione pronta**, apri [Cinder Rift su questo computer](http://localhost:8082). Puoi chiudere la finestra del terminale e lasciare aperto Docker Desktop.

L'indirizzo funziona sul computer dove hai avviato l'applicazione. Gli script aprono sempre `http://localhost:8082`: per usare questo indirizzo lascia `REACT_PORT` al valore predefinito oppure impostala a `8082`. Se scegli un'altra porta, apri manualmente l'indirizzo corrispondente. La salute del frontend è disponibile su `/healthz`; quella del motore su `http://localhost:8083/actuator/health`. Un container sano non garantisce l’integrazione del login o delle partite.

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
