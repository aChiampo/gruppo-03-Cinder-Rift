# Indice della documentazione

Le pagine documentano decisioni confermate, proposte e punti aperti, senza fissare dettagli ancora mancanti. Il modello definisce l'approfondimento progressivo da applicare quando le informazioni saranno disponibili; le pagine esistenti non vengono espanse in anticipo. Il regolamento Versione 2.0 è disponibile come proposta per il prototipo e le prove di bilanciamento. Tutte le applicazioni e funzionalità software sono **non implementate**.

Ultimo aggiornamento significativo dell'indice: **2026-10-27**. La data riguarda l'integrazione documentale della proposta di regolamento, i relativi riferimenti tra aree e l'allineamento del perimetro alla richiesta dell'utente del 2026-10-27. Le verifiche applicative non sono eseguite: le applicazioni non sono ancora inizializzate. Non costituisce una prova del software o una validazione del bilanciamento.

## Aree e strumenti comuni

- [Digital](Digital/README.md)
- [FrontEnd](FrontEnd/README.md)
- [BackEnd](BackEnd/README.md)
- [Linee guida e collaborazione Git](../Linee-Guida-e-Regole.md)
- [Istruzioni condivise per gli assistenti](../AGENTS.md) e [importazione Claude](../CLAUDE.md)
- [Modello di funzionalità](modello-funzionalita.md)
- [Lista di verifica della consegna](lista-verifica-consegna.md)

## Stato delle funzionalità

Questo è il riepilogo corrente. I riferimenti principali collegano i dettagli e le altre aree; per una funzionalità trasversale lo stato implementativo complessivo resta parziale finché le porzioni previste non sono integrate. Non dedurre l'implementazione dal solo perimetro confermato.

| ID  | Oggetto                                                                    | Perimetro                                   | Specifica   | Implementazione | Verifica     | Riferimento principale                               |
| --- | -------------------------------------------------------------------------- | ------------------------------------------- | ----------- | --------------- | ------------ | ---------------------------------------------------- |
| F01 | Email/password, JWT, ruoli ADMIN/MANAGER/USER                              | Confermato                                  | Sintetica   | Non iniziata    | Non eseguita | [Accesso](BackEnd/accesso-utenti.md)                 |
| F03 | Offerte di più pacchetti acquistabili con crediti                          | Confermato                                  | Sintetica   | Non iniziata    | Non eseguita | [Pacchetti](BackEnd/acquisto-pacchetti.md)           |
| F04 | Collezione                                                                 | Confermato                                  | Sintetica   | Non iniziata    | Non eseguita | [Collezione](BackEnd/collezione-carte.md)            |
| F05 | Gestione e validazione mazzi                                               | Confermato                                  | In approfondimento | Non iniziata    | Non eseguita | [Mazzi](BackEnd/gestione-mazzi.md)                   |
| F06 | Scambi di carte e crediti, privati/pubblici, consenso di entrambe le parti | Confermato; approvato anche dal committente | Sintetica   | Non iniziata    | Non eseguita | [Scambi](BackEnd/scambi-mercato.md)                  |
| F07 | Sfide e simulazione                                                        | Confermato                                  | In approfondimento | Non iniziata    | Non eseguita | [Sfide](BackEnd/sfide-simulazione.md)                |
| F08 | Storico e dettaglio dei turni                                              | Confermato                                  | In approfondimento | Non iniziata    | Non eseguita | [Storico](BackEnd/storico-partite.md)                |
| F09 | Pagine informative                                                         | DA DEFINIRE; solo segnaposto                | Sintetica   | Non iniziata    | Non eseguita | [Pagine informative](FrontEnd/pagine-informative.md) |
| F10 | Backoffice carte e prodotti                                                | Confermato                                  | In approfondimento | Non iniziata    | Non eseguita | [Backoffice](BackEnd/gestione-amministrativa.md)     |
| F11 | Responsive e accessibilità                                                 | Confermato                                  | Sintetica   | Non iniziata    | Non eseguita | [Interfacce](FrontEnd/accessibilita-responsive.md)   |
| F12 | SEO e prestazioni                                                          | Confermato                                  | Sintetica   | Non iniziata    | Non eseguita | [SEO](FrontEnd/seo-prestazioni.md)                   |
| D01 | Lore, identità e carte non interamente generate                            | Confermato                                  | In approfondimento | Non iniziata    | Non eseguita | [Lore e carte](Digital/lore-identita-carte.md)       |
| D02 | Pubblico, buyer persona e competitor                                       | Confermato                                  | Sintetica   | Non iniziata    | Non eseguita | [Analisi](Digital/analisi-pubblico-competitor.md)    |
| D03 | Economia e proiezioni                                                      | Confermato                                  | Sintetica   | Non iniziata    | Non eseguita | [Economia](Digital/economia-crediti-pacchetti.md)    |
| D04 | Piano editoriale, lancio, community, campagne e costi                      | Confermato                                  | Sintetica   | Non iniziata    | Non eseguita | [Comunicazione](Digital/comunicazione-lancio.md)     |
| O01 | Modalità di gioco aggiuntive separate                                      | Non confermato                              | Non redatta | Non iniziata    | Non eseguita | Nessuna pagina dedicata                              |

Per Digital, implementazione indica la realizzazione dei materiali e verifica indica la validazione pertinente, non necessariamente test software. Per tutte le aree la specifica può essere sintetica, in approfondimento, concordata o da aggiornare; l'implementazione non iniziata, in corso, parziale o integrata. L'esito di verifica richiede data, ambito, riferimento ed evidenze, compresi limiti o controlli non eseguiti.

Il [regolamento Versione 2.0](Digital/regolamento-base.md) è la proposta condivisa per F05 e F07, con conseguenze per F08, D01 e F10. L'integrazione del testo è approvata, mentre regole definitive e bilanciamento restano da confermare. Le specifiche interessate sono in approfondimento; API, dati, permessi e altri contratti tecnici mancanti restano DA DEFINIRE nelle pagine BackEnd. I riferimenti FrontEnd descrivono le implicazioni per il client senza introdurre contratti concorrenti. F09 resta un segnaposto e O01 non è confermato.

Aggiornare questa tabella nella PR che cambia lo stato; i dettagli della singola porzione restano nelle pagine di area. I metadati temporali si aggiungono alle pagine esistenti al prossimo aggiornamento significativo, senza attribuire date retroattive. Quando serve una verifica storica dettagliata, collegare un report in `docs/report/AAAA-MM-GG-descrizione.md`, creato solo dopo l'attività reale. Il report registra il passato; questo indice mantiene lo stato corrente.

## Contratti e approfondimento progressivo

Il contratto tecnico canonico di una funzionalità trasversale risiede nella pagina BackEnd pertinente; FrontEnd vi rimanda e documenta flussi e stati del client. Le regole di gioco, i contenuti e le ipotesi economiche restano nelle pagine Digital. Gli ID Fxx/Dxx collegano le aree, anche quando gli ID delle attività e i branch sono diversi.

Le pagine si approfondiscono solo per gli aspetti disponibili, mantenendo riconoscibili le proposte. Il [modello completo](modello-funzionalita.md#modello-di-approfondimento-progressivo) definisce il futuro livello di precisione: permessi, stati, contratti ed esempi, dati, transazioni/concorrenza/retry, errori, criteri di accettazione e verifiche pertinenti. Compilare progressivamente solo le decisioni disponibili e mantenere espliciti i punti DA DEFINIRE.

## Differenze rispetto alla traccia

| Tema | Traccia | Decisione o chiarimento del gruppo |
| --- | --- | --- |
| Database | MySQL | PostgreSQL inizialmente indicato; stack definitivo atteso e differenza da verificare |
| Scambi | Solo carte | Carte e crediti, privati/pubblici; approvazione del committente confermata dal gruppo |
| Offerte del negozio | Acquisto di pacchetti | Offerte composte da più pacchetti; struttura e prezzi gestibili dal backoffice |
| Carte | Creazione con strumenti di intelligenza artificiale | Carte non interamente generate; ripartizione del contributo umano da definire |
| Pagine informative | Richieste dalla traccia | Segnaposto fino a indicazioni successive |

## Decisioni aperte

Da approfondire successivamente nelle pagine interessate:

- Stack definitivo, versioni, strumenti e differenza PostgreSQL/MySQL.
- Permessi dei tre ruoli e dettagli del ciclo di vita JWT.
- Modalità di assegnazione dei crediti in fase 1.
- Offerte di pacchetti: quantità, prezzi, limiti, apertura e disponibilità.
- Scambi: durata standard del marketplace, sequenza del consenso, prenotazione, modifica e annullamento.
- Regolamento: approvazione definitiva della Versione 2.0; soglie di energia, statistiche e punti potenza delle carte, effetti e ordine di risoluzione degli equipaggiamenti, bilanciamento di budget/rarità/copie/bonus, conferma della formula del danno con gli insegnanti e possibilità di cure o prevenzione dei danni. Riferimenti: [formula proposta](Digital/regolamento-base.md#11-danni-ai-giocatori) ed [elementi da completare e testare](Digital/regolamento-base.md#15-elementi-da-completare-e-testare).
- Effetti degli scambi sui mazzi e uso delle carte in più mazzi.
- Numero di approvazioni, revisori, strategia di merge e protezioni remote.
- Nomi del progetto e del gruppo, membri e Team Leader.

L'esempio «1.000 crediti → 2 pacchetti Rare + 1 Ultra» chiarisce la composizione di un'offerta e non stabilisce una configurazione definitiva.

## Consegna

La traccia richiede frontend, backend, export database, documentazione tecnica e di progetto, screenshot desktop/mobile e presentazione finale, in un archivio `gruppo-##-nome-progetto-scelto.zip`.

Vedere la [lista di verifica](lista-verifica-consegna.md), le cui caselle restano aperte fino alla verifica effettiva.

[README del progetto](../README.md)
