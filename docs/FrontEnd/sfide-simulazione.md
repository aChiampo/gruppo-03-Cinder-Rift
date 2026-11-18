# Sfide tra utenti

ID: F07 · Perimetro: confermato; specifica in approfondimento.
Implementazione: interfaccia e integrazione React non iniziate; disponibile endpoint PvE dimostrativo BackEnd limitato al SAL. Responsabile: DA DEFINIRE (Web Developer).
Fonte: traccia, risposte e chiarimenti del gruppo; le decisioni note sono riportate sotto.
Fonte dell'approfondimento: regolamento Versione 2.1, proposta per il prototipo e le prove di bilanciamento; integrazione documentale approvata, regole definitive da confermare.
Ultimo aggiornamento significativo: **2026-11-18**, nota di integrazione con l'endpoint dimostrativo PvE. Verifiche dell'integrazione React: non eseguite.

## Obiettivo e decisioni note

- Consentire invio e accettazione di una sfida con scelta dei rispettivi mazzi.
- Mostrare il risultato prodotto dalla simulazione server e collegarlo allo storico.

## Implicazioni della proposta di regolamento

- La Versione 2.1 descrive la [preparazione della partita](../Digital/regolamento-base.md#8-preparazione-della-partita) e il [combattimento automatico](../Digital/regolamento-base.md#10-combattimento): la proposta non prevede decisioni manuali durante la partita.
- L'interfaccia dovrà distinguere partita, round e scambi di attacco e rappresentare risultati e pareggi secondo le condizioni proposte di [fine round](../Digital/regolamento-base.md#12-conclusione-del-round) e [fine partita](../Digital/regolamento-base.md#13-conclusione-della-partita), dopo la loro conferma.
- Per F07 completa, il risultato autorevole e gli stati tecnici dipendono dal [contratto canonico BackEnd](../BackEnd/sfide-simulazione.md), ancora da consolidare; il contratto della demo SAL è distinto e descritto sotto. La proposta non conferma animazioni, aggiornamenti in tempo reale o canali di comunicazione.

## Integrazione dimostrativa per il SAL

Per la simulazione campagna/tutorial del SAL, i componenti React possono invocare `POST http://localhost:8083/api/battle/pve/1`. Usare `cardId` allineati ai pool JSON del motore. Endpoint, struttura dei dati, risposta ed errori sono descritti nella [sezione tecnica BackEnd dell'endpoint dimostrativo PvE](../BackEnd/sfide-simulazione.md#endpoint-dimostrativo-pve-per-il-sal); questa nota non duplica il payload e non definisce l'integrazione F07 completa.

## Da definire

- Ricerca degli avversari, schermate, stati delle richieste e presentazione della simulazione.
- Stati di attesa ed errore e recupero del risultato in accordo con il contratto BackEnd; criteri di accessibilità e verifica della rappresentazione dei risultati.

## Collegamenti

- [Servizio sfide](../BackEnd/sfide-simulazione.md)
- [Regolamento Versione 2.1](../Digital/regolamento-base.md)
- [Mazzi](gestione-mazzi.md)
- [Storico](storico-partite.md)
- [Indice FrontEnd](README.md)
