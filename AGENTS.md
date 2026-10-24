# Istruzioni per lavorare sul progetto

## Contesto e fonti

- Il progetto è nella fase di documentazione iniziale. `react_app/` e
  `springboot_app/` sono segnaposto; lo stack definitivo è ancora da confermare.
- Leggi [README](README.md), [linee guida](Guide%20Lines%20and%20Rules.md) e
  [indice della documentazione](docs/README.md) prima di intervenire.
- Per ogni attività consulta le pagine pertinenti di `docs/Digital/`,
  `docs/FrontEnd/` e `docs/BackEnd/`, seguendo i collegamenti tra aree.
- Conserva gli ID condivisi Fxx/Dxx, il perimetro confermato e le decisioni
  aperte. Non inventare responsabili, issue, contratti o risultati di verifica.

## Convenzioni e confini

- Prosa, titoli e nomi dei documenti sono in italiano; codice, commenti e
  risorse tecniche sono in inglese. Rispetta le eccezioni nelle linee guida.
- Non inizializzare applicazioni o introdurre versioni, dipendenze, strumenti
  e comandi applicativi prima della conferma dello stack e del relativo incarico.
- Per autenticazione, permessi e proprietà delle risorse consulta le regole
  concordate. Non assumere gerarchie tra ADMIN, MANAGER e USER.
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

- Oggi non sono disponibili build o test applicativi: non dichiararli eseguiti.
- Per modifiche documentali controlla link relativi, indici, ID, coerenza tra
  aree e assenza di decisioni inventate; riporta controlli effettivi e limiti.
- Quando le applicazioni saranno inizializzate, usa i comandi documentati e
  verifiche proporzionate al rischio. Il motore Java richiede test JUnit.
- Consulta la [lista di consegna](docs/lista-verifica-consegna.md); segna un
  elemento come completato solo con evidenze pertinenti.
