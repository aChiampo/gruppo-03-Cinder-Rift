# Istruzioni per lavorare sul progetto

## Contesto e fonti

- I backend F01 (autenticazione e profili), F04 (catalogo e collezione),
  F05 (mazzi), F07 (orchestrazione PvE), F13 (amicizie) e le scritture catalogo F10 sono implementati
  in `springboot_app/app/` con Java 21,
  Spring Boot 4.1.2, Spring Security e JWT locali; Compose prevede PostgreSQL 18.
  Il frontend React in `cinder-rift/` usa F01, F04, F05 e F07 reali;
  il risultato PvE è consultabile in memoria, senza storico persistito.
  I contratti correnti sono in `docs/BackEnd/accesso-utenti.md`,
  `docs/BackEnd/gestione-mazzi.md`, `docs/BackEnd/collezione-carte.md`
  e `docs/BackEnd/app-battaglie-api.md`; Flyway applica undici script: V1–V8 e V11–V13.
  V3 contiene lo schema completo carte/arti e V5 il seed definitivo CR01 con artwork
  `STANDARD` e URL delle illustrazioni; gli ID carta sono generati dal database.
  V11 crea le amicizie; il cleanup F13 è nella transazione di cancellazione account.
  V12 crea le strutture F03, inizializza `artwork_pull_rates` (81/10/7,5/1,5)
  con FK diretta da `packs` e vincola i quattro tipi di artwork.
  V13 normalizza gli slot in `pack_slots`, `slot_pull_type` e `type_pull_rate`,
  usa percentuali `decimal(7,4)` e aggiunge `packs.is_acquirable`.
  Catalogo e CRUD pacchetti sono implementati, senza seed dei pacchetti o dei tipi;
  distribuzioni referenziate immutabili e totali per rarità verificati al commit.
  Showcase ed effetti speciali sono esclusi. Acquisto, apertura e avanzamento mercy
  restano rinviati; contratto in `docs/BackEnd/acquisto-pacchetti.md`.
  V1–V8 e V11 di `dev` restano inalterate; la cronologia precedente al cleanup
  richiede una conversione separata. Frontend F13 e F06 restano da completare.
  Allineamento ai sorgenti del 2026-11-27; stato e limiti in `docs/README.md`.
- Leggi [README](README.md), [linee guida](Linee-Guida-e-Regole.md) e
  [indice della documentazione](docs/README.md) prima di intervenire.
- Per ogni attività consulta le pagine pertinenti di `docs/Digital/`,
  `docs/FrontEnd/` e `docs/BackEnd/`, seguendo i collegamenti tra aree.
- Conserva gli ID condivisi Fxx/Dxx, il perimetro confermato e le decisioni
  aperte. Non inventare responsabili, issue, contratti o risultati di verifica.

## Convenzioni e confini

- Prosa, titoli e nomi dei documenti sono in italiano; codice, commenti e
  risorse tecniche sono in inglese. Rispetta le eccezioni nelle linee guida.
- Rispettare stack e configurazioni esistenti; non introdurre nuove applicazioni,
  versioni, dipendenze o strumenti senza un incarico pertinente.
- Per autenticazione, permessi e proprietà delle risorse consulta le regole
  concordate. Non assumere gerarchie tra ADMIN e USER.
- Non inserire segreti, credenziali reali o dati personali nei sorgenti,
  nella documentazione, nei log o negli esempi.

## Documentazione e contratti

- Usa il [modello di funzionalità](docs/modello-funzionalita.md). Mantieni le
  pagine sintetiche finché mancano le decisioni; approfondisci le sezioni
  pertinenti quando diventano disponibili e marca gli altri punti DA DEFINIRE.
- Il contratto tecnico condiviso di una funzionalità è descritto nella pagina
  BackEnd pertinente. FrontEnd vi rimanda e descrive il proprio comportamento.
  Le regole editoriali, economiche e di gioco restano nelle pagine Digital.
- Coordina gli aggiornamenti del contratto tra le aree; non duplicare payload,
  permessi o regole server in specifiche concorrenti.
- Lo stato corrente è riepilogato in `docs/README.md`. Distingui sempre
  maturità della specifica, implementazione e verifica. Riporta date reali di
  aggiornamento e riferimenti verificabili, senza presentare report storici
  come nuove verifiche. Crea report datati separati solo quando servono evidenze.

## Collaborazione Git

- Segui il flusso e il naming delle linee guida: attività FrontEnd e BackEnd
  hanno branch distinti anche quando appartengono alla stessa funzionalità.
- Collega ciascun branch/PR all'ID condiviso, al contratto canonico e alle
  attività dipendenti. Gli ID di attività non sostituiscono gli ID Fxx/Dxx.
- Funzionalità e correzioni partono da `dev` e rientrano in `dev` tramite PR;
  la promozione da `dev` a `main` passa da PR. Non fare commit diretti su questi
  branch. I documenti non configurano le protezioni remote.
- Aggiorna nella PR il codice e la documentazione della porzione interessata;
  coordina le PR collegate quando cambia il contratto condiviso.

## Verifiche

- Il backend dispone di Maven Wrapper e test di autenticazione, autorizzazione,
  cancellazione account, profili, mazzi e pulizie dei token. Distinguere i report già presenti dalle verifiche eseguite
  nell'attività corrente; riportare esiti, prerequisiti e limiti reali.
- Per modifiche documentali controlla link relativi, indici, ID, coerenza tra
  aree e assenza di decisioni inventate; riporta controlli effettivi e limiti.
- Usa i comandi documentati per i componenti disponibili e verifiche
  proporzionate al rischio. Il motore Java richiede test JUnit.
- Consulta la [lista di consegna](docs/lista-verifica-consegna.md); segna un
  elemento come completato solo con evidenze pertinenti.
