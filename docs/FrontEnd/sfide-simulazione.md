# Sfide tra utenti

ID: F07 · Perimetro: confermato; specifica in approfondimento.
Implementazione: interfaccia e integrazione React non iniziate; contratto applicativo PvE documentato nell'area BackEnd. Responsabile: DA DEFINIRE (Web Developer).
Fonte: traccia, risposte e chiarimenti del gruppo; le decisioni note sono riportate sotto.
Fonte dell'approfondimento: regolamento Versione 2.1, proposta per il prototipo e le prove di bilanciamento; integrazione documentale approvata, regole definitive da confermare.
Ultimo aggiornamento significativo: 2026-11-20, separazione richiesta dall'utente tra comportamento dell'interfaccia e specifica server. Verifiche applicative: non eseguite.

## Obiettivo e decisioni note

- Consentire invio e accettazione di una sfida con scelta dei rispettivi mazzi.
- Mostrare il risultato prodotto dalla simulazione server e collegarlo allo storico.

## Implicazioni della proposta di regolamento

- La Versione 2.1 descrive la [preparazione della partita](../Digital/regolamento-base.md#8-preparazione-della-partita) e il [combattimento automatico](../Digital/regolamento-base.md#10-combattimento): la proposta non prevede decisioni manuali durante la partita.
- L'interfaccia dovrà distinguere partita, round e scambi di attacco e rappresentare risultati e pareggi secondo le condizioni proposte di [fine round](../Digital/regolamento-base.md#12-conclusione-del-round) e [fine partita](../Digital/regolamento-base.md#13-conclusione-della-partita), dopo la loro conferma.
- Il risultato autorevole e gli stati tecnici dipendono dal [contratto canonico BackEnd di F07](../BackEnd/sfide-simulazione.md). La porzione PvE è documentata nel riferimento API collegato sotto; il contratto completo PvP resta da definire. La proposta non conferma animazioni, aggiornamenti in tempo reale o canali di comunicazione.

## Integrazione PvE pianificata

Per endpoint, header, payload, codici di errore e vincoli temporali usare esclusivamente [API delle battaglie per il FrontEnd](../BackEnd/app-battaglie-api.md). L'endpoint applicativo è implementato nella copia di lavoro del branch backend `feature/app_battle_pve_flow`; l'integrazione React resta da realizzare. Questa pagina descrive soltanto il comportamento del client; orchestrazione, validazione, pulizia del resoconto, lock e recupero sono definiti in [App battaglie](../BackEnd/app-battaglie.md).

- Consentire la scelta del mazzo e costruire la chiamata secondo il contratto pubblico, usando il client di autenticazione F01.
- Durante l'invio mostrare lo stato di attesa e disabilitare un nuovo avvio dalla stessa schermata.
- Presentare l'esito e il log testuale ordinato ricevuti, senza ricostruire round o vincitore analizzando le frasi; non mostrare una sezione di suggerimenti tutorial.
- Gestire conflitti, validazione ed errori di servizio secondo le risposte documentate, senza duplicare qui regole o payload server.
- Dopo un errore di trasporto o un esito incerto, informare l'utente senza ripetere automaticamente il POST.

Per le dipendenze non ancora definite consultare la [specifica applicativa BackEnd](../BackEnd/app-battaglie.md#decisioni-aperte-e-stato-delle-verifiche). Layout e messaggi dell'interfaccia restano da definire.

## Da definire

- Ricerca degli avversari, schermate, stati delle richieste e presentazione della simulazione.
- Stati di attesa ed errore e recupero del risultato in accordo con il contratto BackEnd; criteri di accessibilità e verifica della rappresentazione dei risultati.

## Collegamenti

- [Servizio sfide](../BackEnd/sfide-simulazione.md)
- [Regolamento Versione 2.1](../Digital/regolamento-base.md)
- [Mazzi](gestione-mazzi.md)
- [Storico](storico-partite.md)
- [Indice FrontEnd](README.md)
