# Sfide tra utenti

ID: F07 · Perimetro: confermato; specifica in approfondimento.
Implementazione: interfaccia e integrazione React non iniziate; disponibile endpoint PvE dimostrativo BackEnd limitato al SAL. Responsabile: DA DEFINIRE (Web Developer).
Fonte: traccia, risposte e chiarimenti del gruppo; le decisioni note sono riportate sotto.
Fonte dell'approfondimento: regolamento Versione 2.1, proposta per il prototipo e le prove di bilanciamento; integrazione documentale approvata, regole definitive da confermare.
Ultimo aggiornamento significativo: **2026-11-19**, nota di integrazione server-to-server con l'endpoint dimostrativo PvE. Verifiche dell'integrazione React: non eseguite.

## Obiettivo e decisioni note

- Consentire invio e accettazione di una sfida con scelta dei rispettivi mazzi.
- Mostrare il risultato prodotto dalla simulazione server e collegarlo allo storico.

## Implicazioni della proposta di regolamento

- La Versione 2.1 descrive la [preparazione della partita](../Digital/regolamento-base.md#8-preparazione-della-partita) e il [combattimento automatico](../Digital/regolamento-base.md#10-combattimento): la proposta non prevede decisioni manuali durante la partita.
- L'interfaccia dovrà distinguere partita, round e scambi di attacco e rappresentare risultati e pareggi secondo le condizioni proposte di [fine round](../Digital/regolamento-base.md#12-conclusione-del-round) e [fine partita](../Digital/regolamento-base.md#13-conclusione-della-partita), dopo la loro conferma.
- Per F07 completa, il risultato autorevole e gli stati tecnici dipendono dal [contratto canonico BackEnd](../BackEnd/sfide-simulazione.md), ancora da consolidare; il contratto della demo SAL è distinto e descritto sotto. La proposta non conferma animazioni, aggiornamenti in tempo reale o canali di comunicazione.

## Integrazione dimostrativa per il SAL

Il flusso previsto per la simulazione campagna/tutorial è `frontend -> springboot_app/app -> game-engine`: il frontend dovrà rivolgersi al backend applicativo, che a sua volta chiamerà l'API interna `POST /api/battle/pve/{id}` all'indirizzo Compose `http://game-engine:8083/api/battle/pve/{id}`. L'orchestrazione PvE in `springboot_app/app` non è ancora implementata e non è definita né disponibile una rotta pubblica dell'applicazione per questa funzione. Il binding host `127.0.0.1:8083`, modificabile con `ENGINE_PORT`, è riservato al debug e alle prove locali da terminale o `curl`, non all'accesso dal browser. Usare `cardId` allineati ai pool JSON del motore. Payload, risposta ed errori dell'API interna sono descritti nella [sezione tecnica BackEnd dell'endpoint dimostrativo PvE](../BackEnd/sfide-simulazione.md#endpoint-dimostrativo-pve-per-il-sal) e nella [guida al payload](../BackEnd/GUIDA-FRONTEND-SIMULAZIONE-PVE.md); questa nota non duplica il contratto e non definisce l'integrazione F07 completa.

La suite del motore eseguita il **2026-11-19** ha superato 25 test JUnit, 0 fallimenti e 0 errori, inclusi i test aggiornati che verificano la chiamata server-to-server senza header `Origin` e il rifiuto delle richieste preflight senza configurare CORS. L'integrazione React con `springboot_app/app` non è stata implementata né verificata.

## Da definire

- Ricerca degli avversari, schermate, stati delle richieste e presentazione della simulazione.
- Stati di attesa ed errore e recupero del risultato in accordo con il contratto BackEnd; criteri di accessibilità e verifica della rappresentazione dei risultati.

## Collegamenti

- [Servizio sfide](../BackEnd/sfide-simulazione.md)
- [Regolamento Versione 2.1](../Digital/regolamento-base.md)
- [Mazzi](gestione-mazzi.md)
- [Storico](storico-partite.md)
- [Indice FrontEnd](README.md)
