# Sfide tra utenti

ID: F07 · Perimetro: confermato; specifica in approfondimento.
Implementazione: campagna PvE e visualizzatore del risultato React disponibili; sfide PvP e storico persistito non implementati. Responsabile: DA DEFINIRE (Web Developer).
Fonte: traccia, risposte e chiarimenti del gruppo; le decisioni note sono riportate sotto.
Fonte dell'approfondimento: regolamento Versione 2.1, proposta per il prototipo e le prove di bilanciamento; integrazione documentale approvata, regole definitive da confermare.
Ultimo aggiornamento significativo: **2026-11-23**, allineamento richiesto dall'utente al codice corrente. Verifica frontend: 54 test e build superati; browser integrato non verificato in questa revisione.

## Obiettivo e decisioni note

- Consentire invio e accettazione di una sfida con scelta dei rispettivi mazzi.
- Mostrare il risultato prodotto dalla simulazione server e collegarlo allo storico.

## Implicazioni della proposta di regolamento

- La Versione 2.1 descrive la [preparazione della partita](../Digital/regolamento-base.md#8-preparazione-della-partita) e il [combattimento automatico](../Digital/regolamento-base.md#10-combattimento): la proposta non prevede decisioni manuali durante la partita.
- L'interfaccia dovrà distinguere partita, round e scambi di attacco e rappresentare risultati e pareggi secondo le condizioni proposte di [fine round](../Digital/regolamento-base.md#12-conclusione-del-round) e [fine partita](../Digital/regolamento-base.md#13-conclusione-della-partita), dopo la loro conferma.
- Il risultato autorevole e gli stati tecnici dipendono dal [contratto canonico BackEnd di F07](../BackEnd/sfide-simulazione.md). La porzione PvE è documentata nel riferimento API collegato sotto; il contratto completo PvP resta da definire. La proposta non conferma animazioni, aggiornamenti in tempo reale o canali di comunicazione.

## Integrazione PvE implementata

Per endpoint, header, payload, codici di errore e vincoli temporali usare esclusivamente [API delle battaglie per il FrontEnd](../BackEnd/app-battaglie-api.md). `BattleScreen` e `battleService` chiamano l'endpoint applicativo con la sessione F01; questa pagina descrive soltanto il client. Orchestrazione, validazione, pulizia del resoconto, lock e recupero sono definiti in [App battaglie](../BackEnd/app-battaglie.md).

- La schermata carica i mazzi dell'utente e richiede il codice stage, con `stage_01` come esempio; non esiste ancora una scoperta automatica degli stage.
- Durante l'invio mostra l'attesa e impedisce un nuovo avvio dalla stessa schermata.
- Al successo apre il risultato dedicato; non mostra una sezione di suggerimenti tutorial.
- Conflitti, validazione ed errori di servizio vengono mostrati secondo le risposte documentate; le violazioni `422` restano visibili.
- Dopo errori di trasporto informa l'utente senza ripetere automaticamente il POST.

## Risultato della sessione

`BattleResultScreen` usa `outcome` come esito autorevole della partita. `parseBattleReport` interpreta i marcatori riconosciuti del log corrente per mostrare round, creature, equipaggiamenti, statistiche e scambi simultanei; non deduce il vincitore della partita dai nomi. Dati mancanti, ambigui o non riconosciuti restano indisponibili e il log originale è sempre consultabile. Le immagini facoltative F04 non sostituiscono le statistiche del report. Questo adattatore dipende dal formato testuale del motore: non esiste ancora un contratto di eventi strutturati.

La navigazione `#risultato-battaglia?round=1&scambio=2` seleziona un punto del report in memoria. I controlli consentono avanzamento, ritorno, selezione round e replay locale; non effettuano nuove simulazioni. Home e Campagna riaprono l'ultimo risultato, che viene perso al ricaricamento o all'uscita. Senza risultato la pagina invita a tornare alla campagna. Questa vista appartiene a F07 e non implementa lo storico F08.

Verifica del **2026-11-23**: 54 test frontend superati e build riuscita. Coperti parser, log incompleti/ambigui, navigazione, rendering e servizi; test con fixture e HTTP simulato, senza verifica browser o nuova integrazione con Spring. [Evidenze e limiti](../report/2026-11-23-allineamento-documentazione.md).

## Da definire

- Ricerca degli avversari, invio/accettazione e stati delle sfide PvP.
- Recupero persistito del risultato, eventi strutturati, scoperta stage e criteri di accessibilità; verifica browser della rappresentazione e del replay.

## Collegamenti

- [Servizio sfide](../BackEnd/sfide-simulazione.md)
- [Regolamento Versione 2.1](../Digital/regolamento-base.md)
- [Mazzi](gestione-mazzi.md)
- [Storico](storico-partite.md)
- [Indice FrontEnd](README.md)
