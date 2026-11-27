# Composizione dei mazzi

ID: F05 · Perimetro: confermato; specifica in approfondimento.
Implementazione frontend: editor React con CRUD F05 e selezione visiva dalla collezione privata F04. Il backend espone le rotte HTTP F05 documentate nel [contratto BackEnd](../BackEnd/gestione-mazzi.md#api-http-implementate). Responsabile: DA DEFINIRE (Web Developer).
Fonte: traccia, risposte e chiarimenti del gruppo; le decisioni note sono riportate sotto.
Fonte dell'approfondimento: regolamento Versione 2.1, proposta per il prototipo e le prove di bilanciamento; integrazione documentale approvata, regole definitive da confermare.
Ultimo aggiornamento significativo: **2026-11-22**, su richiesta dell'utente di mostrare le carte possedute e aggiungerle agli slot tramite clic o trascinamento, ridurre le carte del selettore, unificare la selezione di round ed equipaggiamenti e filtrare le creature per energia. I controlli server di energia e potenza restano quelli del contratto F05.

## Obiettivo e decisioni note

- Consentire composizione e salvataggio di più mazzi.
- Mostrare i vincoli e gli esiti della validazione; la verifica definitiva resta al server.

## Comportamento dell'editor

- Il pannello collezione carica tutte le pagine della [collezione privata F04](../BackEnd/collezione-carte.md), conservando le varianti grafiche distinte. Mostra carte compatte su tre colonne nel pannello laterale, con immagini, nomi, statistiche, copie possedute e copie ancora assegnabili; permette una ricerca per nome. Le dimensioni compatte sono limitate al builder.
- Aggiornamento dell'interfaccia del **2026-11-22**, su richiesta dell'utente: la selezione e la creazione dei mazzi precedono la collezione nel pannello laterale; il titolo «COLLEZIONE CARTE» e il testo introduttivo sono rimossi. Gli slot occupati usano lo stesso componente compatto della collezione, con statistiche visibili e pulsante di rimozione sotto ogni carta, esteso a tutta la larghezza interna dello slot; la carta nello slot è consultabile senza un'azione di selezione aggiuntiva.
- Un unico selettore comprende Round 1–5 ed Equipaggiamenti. Selezionando un round si vedono soltanto creature con energia intera non negativa e non superiore al numero del round; un costo mancante o non valido esclude la carta dal selettore, mentre il costo zero è ammesso. Lo stesso controllo protegge clic e trascinamento. Equipaggiamenti mostra soltanto equipaggiamenti, come gruppo separato dai round numerati. Ogni destinazione offre cinque slot.
- Un clic sulla carta, anche tramite tastiera, riempie il primo slot vuoto della destinazione corrente. Il trascinamento inserisce la carta nello slot vuoto scelto. Slot occupati, gruppi completi, tipi incompatibili e copie esaurite non sono sovrascritti.
- La disponibilità conta le copie della precisa variante già presenti nell'intero mazzo corrente. Rimuovere una copia libera lo slot e ripristina la disponibilità. Il server resta responsabile della validazione completa, compresi energia, potenza, rarità e limiti delle copie per carta.
- Il riepilogo mostra copie ed energia per destinazione nell'ordine ROUND1, ROUND2, ROUND3, ROUND4, ROUND5, EQUIPMENT, indipendentemente dall'ordine delle proprietà restituite dal server (correzione del **2026-11-22**, su richiesta dell'utente).
- Selezionare o creare un mazzo abilita l'inserimento dopo il caricamento. Caricamento, errore con riprova e collezione senza risultati hanno messaggi distinti; durante le scritture l'editor è disabilitato. Le aggiunte sono annunciate con uno stato accessibile e gli errori con un avviso.
- Il salvataggio conserva il payload F05 esistente. Le posizioni degli slot sono stato locale di editing: il contratto persiste copie e destinazioni, senza un indice di posizione. Al caricamento e dopo il salvataggio le copie vengono mostrate in slot consecutivi; i vuoti intermedi non sono persistiti. Le copie prive di metadati F04 restano visibili tramite identificativi e rimovibili.

## Verifiche e limiti

Il **2026-11-22**, sulla copia di lavoro: `npm test` con **44 test superati** e `npm run build` riuscita. Coperti inserimento nel primo slot vuoto e in uno slot indicato, rimozione, possesso per variante fra round, rifiuto di slot occupati e tipi incompatibili, gruppi completi, lettura di più pagine F04, payload F05 e rendering del selettore e degli slot. Verificati anche energia sotto/uguale/sopra la soglia per tutti i cinque round, costi mancanti o invalidi, cambio del filtro al cambio round, selezione di Equipaggiamenti e indicatore energetico compatto. Verifiche statiche e logiche con fixture isolate; trascinamento nativo nel browser e flusso completo contro Spring/PostgreSQL non verificati in questa attività.

Ripetuta la verifica dopo l'aggiornamento dell'interfaccia dello stesso giorno: **44 test superati**, build riuscita e controllo delle differenze senza errori di spaziatura. Il rendering verifica anche l'ordine mazzi/collezione, l'assenza dei testi rimossi e le statistiche negli slot, con il pulsante di rimozione prima della carta. Nessuna nuova verifica nel browser.

Successivo aggiornamento del **2026-11-22**, su richiesta dell'utente: pulsante di rimozione spostato sotto la carta e largo quanto lo spazio interno dello slot. Ripetuti `npm test` (**44 test superati**) e `npm run build` (riuscita); il controllo di rendering verifica ora il pulsante dopo le statistiche della carta. Nessuna nuova verifica nel browser.

## Implicazioni della proposta di regolamento

- La proposta di [struttura dei mazzi](../Digital/regolamento-base.md#2-struttura-del-mazzo) introduce l'assegnazione delle creature ai round e un gruppo separato di equipaggiamenti: l'interfaccia dovrà renderne comprensibile la composizione dopo la conferma delle regole.
- [Energia e punti potenza](../Digital/regolamento-base.md#3-energia-e-punti-potenza) hanno funzioni differenti. La presentazione dei [vincoli di composizione](../Digital/regolamento-base.md#4-budget-e-vincoli-di-composizione) dovrà rispettare questa distinzione senza mantenere una specifica numerica indipendente.
- La presentazione degli errori segue i controlli server di energia per carta e potenza per destinazione approvati il 2026-11-18, con i codici del [contratto F05](../BackEnd/gestione-mazzi.md#implementazione-backend-disponibile).
- Dati scambiati, payload HTTP ed esiti di validazione dipendono dal [contratto canonico BackEnd di F05](../BackEnd/gestione-mazzi.md#api-http-implementate).

## Da definire

- Verifica integrata del flusso di modifica, salvataggio e trascinamento nel browser.
- Conferma dei vincoli proposti e loro presentazione; effetti degli scambi sulle carte usate nei mazzi.
- Verifica completa responsive e accessibilità con tecnologie assistive.

## Collegamenti

- [Validazione server](../BackEnd/gestione-mazzi.md)
- [Regolamento](../Digital/regolamento-base.md)
- [Collezione](collezione-carte.md)
- [Indice FrontEnd](README.md)
