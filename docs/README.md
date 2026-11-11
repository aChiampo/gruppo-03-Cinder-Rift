# Indice della documentazione

Le pagine documentano decisioni confermate, proposte e punti aperti, senza fissare dettagli ancora mancanti. Il modello definisce l'approfondimento progressivo da applicare quando le informazioni saranno disponibili; le pagine esistenti non vengono espanse in anticipo. Il regolamento Versione 2.1 è disponibile come proposta per il prototipo e le prove di bilanciamento. Tutte le applicazioni e funzionalità software sono **non implementate**.

Ultimo aggiornamento significativo dell'indice: **2026-11-09**, per il significato di Scaglie, Nuclei Rift e Rotami riportato nel [glossario economico](Digital/economia-crediti-pacchetti.md#risorse-economiche) e per il [sistema mercy sulle aperture](BackEnd/acquisto-pacchetti.md#mercy-sulle-aperture) confermato dall'utente. Il DBML e i diagrammi di inventario distinguono `cobalt_shards`, `rift_cores` e `scraps`, tutti `int` obbligatori con default `0`. `USER_MERCY` usa PK `(id_user, id_pack)`, FK a utenti/pacchetti/rarità e un contatore inizializzato dalla soglia del pacchetto, con garanzia allo zero e reset su ogni pesca qualificante. La risorsa dei campi monetari generici di offerte, prenotazioni e premi resta da definire. Lo stesso aggiornamento, su richiesta dell’utente, rimuove i campi anagrafici dal modello utente, conferma la durata di due giorni degli annunci di scambio pubblici e privati e allinea i riferimenti correnti al regolamento Versione 2.1. La versione corrente della specifica e del DBML è **v1.1**, con file e riferimenti rinominati su richiesta dell’utente del **2026-11-09**. Non sono eseguite migrazioni o verifiche applicative.

Allineamento documentale del **2026-11-05**, per la base di dati **v1.0** e l'allineamento di tutte le funzionalità BackEnd su richiesta dell'utente. La [specifica autonoma v1.1](BackEnd/Database/database-schema-v1.1.md) è la fonte canonica di dati e invarianti; la rappresentazione completa è [db_schema-v1.1.dbml](BackEnd/Database/db_schema-v1.1.dbml). Gli ID generati sono interi auto-incrementali; gli annunci pubblici non prenotano alla pubblicazione e le modifiche delle trattative rilasciano tutti gli hold. Resta la decisione del **2026-11-04** su un solo pacchetto per offerta. L'integrazione documentale della proposta di regolamento resta datata **2026-10-27**. FrontEnd e Digital non sono modificati da questo allineamento. Implementazione non iniziata e verifiche applicative non eseguite: l'aggiornamento non costituisce una prova del software o una validazione del bilanciamento.

## Aree e strumenti comuni

- [Digital](Digital/README.md)
- [FrontEnd](FrontEnd/README.md)
- [BackEnd](BackEnd/README.md)
- [Guida per avviare l'applicazione sul proprio computer](../guida-avvio.md)
- [Configurazione locale con Docker Compose nel workspace](../../docker-docs.md)
- [Linee guida e collaborazione Git](../Linee-Guida-e-Regole.md)
- [Istruzioni condivise per gli assistenti](../AGENTS.md) e [importazione Claude](../CLAUDE.md)
- [Modello di funzionalità](modello-funzionalita.md)
- [Lista di verifica della consegna](lista-verifica-consegna.md)

## Stato delle funzionalità

Questo è il riepilogo corrente. I riferimenti principali collegano i dettagli e le altre aree; per una funzionalità trasversale lo stato implementativo complessivo resta parziale finché le porzioni previste non sono integrate. Non dedurre l'implementazione dal solo perimetro confermato.

| ID  | Oggetto                                                                    | Perimetro                                   | Specifica   | Implementazione | Verifica     | Riferimento principale                               |
| --- | -------------------------------------------------------------------------- | ------------------------------------------- | ----------- | --------------- | ------------ | ---------------------------------------------------- |
| F01 | Email/password, JWT, ruoli ADMIN/MANAGER/USER                              | Confermato                                  | In approfondimento   | Non iniziata    | Non eseguita | [Accesso](BackEnd/accesso-utenti.md)                 |
| F03 | Offerte di un solo pacchetto; risorsa del prezzo da definire                      | Confermato                                  | In approfondimento   | Non iniziata    | Non eseguita | [Pacchetti](BackEnd/acquisto-pacchetti.md)           |
| F04 | Collezione                                                                 | Confermato                                  | In approfondimento   | Non iniziata    | Non eseguita | [Collezione](BackEnd/collezione-carte.md)            |
| F05 | Gestione e validazione mazzi                                               | Confermato                                  | In approfondimento | Non iniziata    | Non eseguita | [Mazzi](BackEnd/gestione-mazzi.md)                   |
| F06 | Scambi di carte e token, privati/pubblici, consenso e prenotazioni | Confermato; approvato anche dal committente | In approfondimento | Non iniziata    | Non eseguita | [Scambi](BackEnd/scambi-mercato.md)                  |
| F07 | Sfide e simulazione                                                        | Confermato                                  | In approfondimento | Non iniziata    | Non eseguita | [Sfide](BackEnd/sfide-simulazione.md)                |
| F08 | Storico e dettaglio dei turni                                              | Confermato                                  | In approfondimento | Non iniziata    | Non eseguita | [Storico](BackEnd/storico-partite.md)                |
| F09 | Pagine informative                                                         | DA DEFINIRE; solo segnaposto                | Sintetica   | Non iniziata    | Non eseguita | [Pagine informative](FrontEnd/pagine-informative.md) |
| F10 | Backoffice carte e prodotti                                                | Confermato                                  | In approfondimento | Non iniziata    | Non eseguita | [Backoffice](BackEnd/gestione-amministrativa.md)     |
| F11 | Responsive e accessibilità                                                 | Confermato                                  | Sintetica   | Non iniziata    | Non eseguita | [Interfacce](FrontEnd/accessibilita-responsive.md)   |
| F12 | SEO e prestazioni                                                          | Confermato                                  | Sintetica   | Non iniziata    | Non eseguita | [SEO](FrontEnd/seo-prestazioni.md)                   |
| F13 | Amicizie reciproche e richieste | Modellato nella base di dati; flussi proposti | In approfondimento | Non iniziata | Non eseguita | [Amicizie](BackEnd/amicizie.md) |
| D01 | Lore, identità e carte non interamente generate                            | Confermato                                  | In approfondimento | Non iniziata    | Non eseguita | [Lore e carte](Digital/lore-identita-carte.md)       |
| D02 | Pubblico, buyer persona e competitor                                       | Confermato                                  | Sintetica   | Non iniziata    | Non eseguita | [Analisi](Digital/analisi-pubblico-competitor.md)    |
| D03 | Economia e proiezioni                                                      | Confermato                                  | Sintetica   | Non iniziata    | Non eseguita | [Economia](Digital/economia-crediti-pacchetti.md)    |
| D04 | Piano editoriale, lancio, community, campagne e costi                      | Confermato                                  | Sintetica   | Non iniziata    | Non eseguita | [Comunicazione](Digital/comunicazione-lancio.md)     |
| O01 | Modalità di gioco aggiuntive separate                                      | Non confermato                              | Non redatta | Non iniziata    | Non eseguita | Nessuna pagina dedicata                              |

Per Digital, implementazione indica la realizzazione dei materiali e verifica indica la validazione pertinente, non necessariamente test software. Per tutte le aree la specifica può essere sintetica, in approfondimento, concordata o da aggiornare; l'implementazione non iniziata, in corso, parziale o integrata. L'esito di verifica richiede data, ambito, riferimento ed evidenze, compresi limiti o controlli non eseguiti.

Il [regolamento Versione 2.1](Digital/regolamento-base.md) è la proposta condivisa per F05 e F07, con conseguenze per F08, D01 e F10. L'integrazione del testo è approvata, mentre regole definitive e bilanciamento restano da confermare. Le specifiche interessate sono in approfondimento; API, dati, permessi e altri contratti tecnici mancanti restano DA DEFINIRE nelle pagine BackEnd. I riferimenti FrontEnd descrivono le implicazioni per il client senza introdurre contratti concorrenti. F09 resta un segnaposto e O01 non è confermato.

Aggiornare questa tabella nella PR che cambia lo stato; i dettagli della singola porzione restano nelle pagine di area. I metadati temporali si aggiungono alle pagine esistenti al prossimo aggiornamento significativo, senza attribuire date retroattive. Quando serve una verifica storica dettagliata, collegare un report in `docs/report/AAAA-MM-GG-descrizione.md`, creato solo dopo l'attività reale. Il report registra il passato; questo indice mantiene lo stato corrente.

La versione iniziale della specifica [F13 — Amicizie](BackEnd/amicizie.md) descrive richieste e amicizie reciproche sulla base dello schema v1.0; la relazione `accepted` è una precondizione degli scambi privati F06. Ultimo aggiornamento significativo della voce: **2026-11-06**. Specifica in approfondimento, API proposte da concordare, implementazione non iniziata e verifiche applicative non eseguite.

## Contratti e approfondimento progressivo

Il contratto tecnico canonico di una funzionalità trasversale risiede nella pagina BackEnd pertinente; FrontEnd vi rimanda e documenta flussi e stati del client. Le regole di gioco, i contenuti e le ipotesi economiche restano nelle pagine Digital. Gli ID Fxx/Dxx collegano le aree, anche quando gli ID delle attività e i branch sono diversi.

Le pagine si approfondiscono solo per gli aspetti disponibili, mantenendo riconoscibili le proposte. Il [modello completo](modello-funzionalita.md#modello-di-approfondimento-progressivo) definisce il futuro livello di precisione: permessi, stati, contratti ed esempi, dati, transazioni/concorrenza/retry, errori, criteri di accettazione e verifiche pertinenti. Compilare progressivamente solo le decisioni disponibili e mantenere espliciti i punti DA DEFINIRE.

## Differenze rispetto alla traccia

| Tema | Traccia | Decisione o chiarimento del gruppo |
| --- | --- | --- |
| Database | MySQL | PostgreSQL inizialmente indicato; stack definitivo atteso e differenza da verificare |
| Scambi | Solo carte | Carte e crediti, privati/pubblici; approvazione del committente confermata dal gruppo |
| Offerte del negozio | Acquisto di pacchetti | Ogni offerta contiene un solo pacchetto; prezzo e configurazione del pacchetto gestibili dal backoffice |
| Carte | Creazione con strumenti di intelligenza artificiale | Carte non interamente generate; ripartizione del contributo umano da definire |
| Pagine informative | Richieste dalla traccia | Segnaposto fino a indicazioni successive |

## Decisioni aperte

Da approfondire successivamente nelle pagine interessate:

- Stack definitivo, versioni, strumenti e differenza PostgreSQL/MySQL.
- Permessi dei tre ruoli e dettagli del ciclo di vita JWT.
- Regole di assegnazione delle Scaglie, acquisto dei Nuclei Rift e recupero dei Rotami; risorse ammesse nei prezzi e negli scambi.
- Offerte di un solo pacchetto: prezzi, limiti, contenuto del pacchetto, apertura e disponibilità.
- Scambi: API, integrazione delle prenotazioni con gli altri servizi, giocabilità delle copie prenotate e operatività della pulizia; durata, consenso, modifica e annullamento sono descritti nel [contratto F06](BackEnd/scambi-mercato.md).
- Regolamento: approvazione definitiva della Versione 2.1; soglie di energia, statistiche e punti potenza delle carte, effetti e ordine di risoluzione degli equipaggiamenti, bilanciamento di budget/rarità/copie/bonus, conferma della formula del danno con gli insegnanti e possibilità di cure o prevenzione dei danni. Riferimenti: [formula proposta](Digital/regolamento-base.md#11-danni-ai-giocatori) ed [elementi da completare e testare](Digital/regolamento-base.md#15-elementi-da-completare-e-testare).
- Effetti degli scambi sui mazzi e uso delle carte in più mazzi.
- Numero di approvazioni, revisori, strategia di merge e protezioni remote.
- Nomi del progetto e del gruppo, membri e Team Leader.

L'esempio «1.000 crediti → 1 pacchetto Rare» rispetta la cardinalità confermata di un solo pacchetto per offerta e non stabilisce un prezzo o un tipo definitivo.

## Consegna

La traccia richiede frontend, backend, export database, documentazione tecnica e di progetto, screenshot desktop/mobile e presentazione finale, in un archivio `gruppo-##-nome-progetto-scelto.zip`.

Vedere la [lista di verifica](lista-verifica-consegna.md), le cui caselle restano aperte fino alla verifica effettiva.

[README del progetto](../README.md)
