# Motore e campagna di gioco

`game_engine` è un'unica applicazione Spring Boot che contiene il motore delle partite e i servizi dimostrativi della campagna. La campagna è organizzata nel package `com.benedetto.game_engine.campaign`; non è più un modulo Maven o un'applicazione avviabile separatamente.

> **Stato:** il codice presente è una prima implementazione dimostrativa. Non costituisce l'implementazione completa delle funzionalità F05, F07 o F08, né conferma le regole proposte come definitive.

Ultimo aggiornamento significativo: **2026-11-09**. Fonte: struttura, sorgenti, configurazione, risorse e test del modulo.

## Motore di gioco

- `MatchEngine` orchestra fino a cinque round e interrompe la partita quando un giocatore raggiunge tre round vinti. Restituisce il resoconto come lista di stringhe.
- `CombatEngine` esegue scambi simultanei fra le creature attive, gestisce la sostituzione delle creature eliminate e il danno in eccesso ai punti vita del giocatore.
- Il bonus d'attacco di fazione è `+2` quando la fazione attaccante è avvantaggiata; al calcolo dell'attacco si somma `Equipment.atkBonus`.
- Ogni round ripristina i punti vita a 30. Il combattimento termina anche al raggiungimento del limite difensivo di 50 scambi.
- `CardLoaderService` deserializza un mazzo JSON da una risorsa del classpath. Il flag `deterministic` disabilita il mescolamento.

Questi comportamenti descrivono il codice attuale, non un contratto di gioco approvato. `Equipment` contiene anche `defBonus`, ma il flusso di combattimento corrente non lo applica alla difesa delle creature. Il loader non esegue la validazione completa dei mazzi prevista dalla specifica F05.

## Campagna

`CampaignService.executeStage` carica uno stage JSON, verifica l'eventuale codice evento richiesto, carica il mazzo nemico e avvia la partita tramite `MatchEngine`. Se l'evento richiesto è assente o inattivo, restituisce un messaggio nel resoconto senza iniziare la partita. I suggerimenti tutorial vengono anteposti al resoconto della partita.

`CampaignStageLoaderService` legge gli stage dal classpath; accetta `classpath:`, un percorso relativo a `campaign/stages/` oppure il nome del file. I dati sono in `src/main/resources/campaign/`:

| Percorso | Contenuto |
| --- | --- |
| `stages/` | Configurazioni di esempio per lo stage 1 e lo stage 3 |
| `decks/` | Mazzi nemici degli stage dimostrativi |

Lo stage 3 richiede l'evento `BLOOD_MOON`. L'applicazione usa H2 in memoria e crea lo schema JPA all'avvio; non è presente un seed per tale evento, quindi lo stage 3 non supera il controllo nella configurazione predefinita.

## Avvio e verifiche

Dalla directory `springboot_app/game_engine/`:

```bash
./mvnw test
./mvnw spring-boot:run
```

All'avvio il `CommandLineRunner` presenta un menu:

1. **Campagna** — avvia il primo stage tutorial (`stage_01_fazioni.json`).
2. **Scontro online** — simula uno scontro usando i due mazzi dimostrativi JSON.

Il menu è un flusso CLI dimostrativo, non un matchmaking online né un'interfaccia web. La scelta del giocatore determina quale flusso invocare; un input terminato prima della scelta produce un errore esplicito. Per disabilitare il prompt in un ambiente senza input interattivo, avviare con `--game-engine.cli.enabled=false`. I test verificano il caricamento del contesto, entrambe le scelte, l'input non valido e il caricamento e l'esecuzione degli stage.

Verifica del **2026-11-09**: `./mvnw test` completato con **19 test, 0 errori e 0 fallimenti**. La verifica usa la configurazione H2; non attesta l'integrazione con PostgreSQL né le funzionalità applicative complete.

Il POM dichiara Java 21 e Spring Boot 4.1.2. La configurazione predefinita usa H2; il driver PostgreSQL è incluso come dipendenza runtime.

## Struttura essenziale

| Percorso | Contenuto |
| --- | --- |
| `src/main/java/.../model/` e `service/` | Modelli e servizi del motore |
| `src/main/java/.../campaign/` | Modello, servizi e accesso agli eventi della campagna |
| `src/main/resources/mocks/` | Mazzi dimostrativi del motore |
| `src/main/resources/campaign/` | Stage e mazzi nemici della campagna |
| `src/test/java/` | Test JUnit del combattimento e della campagna |

Non sono presenti API HTTP per le sfide, persistenza dei risultati di partita o avanzamento della campagna; non vengono implementati sblocco e completamento degli stage. La modalità CLI usa mazzi predefiniti perché il deckbuilder è ancora in sviluppo; non autentica un utente e non salva la progressione.

Nel regolamento proposto il giocatore prende decisioni strategiche prima della partita, componendo e distribuendo il mazzo e scegliendo se inviare o accettare una sfida. Durante il combattimento non sono previste decisioni manuali. La scelta CLI fra campagna e scontro è solo una selezione di modalità; non sostituisce le scelte di mazzo e sfida previste per il prodotto.

## Specifiche collegate

- [F05 — Gestione e validazione dei mazzi](../../docs/BackEnd/gestione-mazzi.md)
- [F07 — Sfide e simulazione](../../docs/BackEnd/sfide-simulazione.md)
- [F08 — Storico delle partite](../../docs/BackEnd/storico-partite.md)
- [Regolamento e svolgimento del gioco](../../docs/Digital/regolamento-base.md)
- [Indice generale e stato delle funzionalità](../../docs/README.md#stato-delle-funzionalità)

Le specifiche collegate distinguono le regole proposte dai contratti tecnici ancora da concordare. La campagna non ha un ID Fxx assegnato nelle specifiche correnti e non è presentata come funzionalità approvata del perimetro.
