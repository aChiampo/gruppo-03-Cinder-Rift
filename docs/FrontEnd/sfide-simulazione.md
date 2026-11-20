# Sfide tra utenti

ID: F07 · Perimetro: confermato; specifica in approfondimento.
Implementazione: non iniziata. Responsabile: DA DEFINIRE (Web Developer).
Fonte: traccia, risposte e chiarimenti del gruppo; le decisioni note sono riportate sotto.
Fonte dell'approfondimento: regolamento Versione 2.1, proposta per il prototipo e le prove di bilanciamento; integrazione documentale approvata, regole definitive da confermare.
Ultimo aggiornamento significativo: 2026-11-20, collegamento del contratto applicativo PvE su richiesta dell'utente. Verifiche applicative: non eseguite.

## Obiettivo e decisioni note

- Consentire invio e accettazione di una sfida con scelta dei rispettivi mazzi.
- Mostrare il risultato prodotto dalla simulazione server e collegarlo allo storico.

## Implicazioni della proposta di regolamento

- La Versione 2.1 descrive la [preparazione della partita](../Digital/regolamento-base.md#8-preparazione-della-partita) e il [combattimento automatico](../Digital/regolamento-base.md#10-combattimento): la proposta non prevede decisioni manuali durante la partita.
- L'interfaccia dovrà distinguere partita, round e scambi di attacco e rappresentare risultati e pareggi secondo le condizioni proposte di [fine round](../Digital/regolamento-base.md#12-conclusione-del-round) e [fine partita](../Digital/regolamento-base.md#13-conclusione-della-partita), dopo la loro conferma.
- Il risultato autorevole e gli stati tecnici dipendono dal [contratto canonico BackEnd di F07](../BackEnd/sfide-simulazione.md), ancora da definire. La proposta non conferma animazioni, aggiornamenti in tempo reale o canali di comunicazione.

## Integrazione PvE pianificata

Per endpoint, header e payload usare esclusivamente [API delle battaglie per il FrontEnd](../BackEnd/app-battaglie-api.md); flusso server e decisioni aperte sono in [App battaglie](../BackEnd/app-battaglie.md). La rotta applicativa è pianificata e non ancora disponibile: il browser non chiama direttamente il motore della PR #36.

L'interfaccia invia il solo ID del mazzo con il Bearer F01, usa l'esito restituito e presenta il log testuale ordinato senza dedurre round o vincitore dalle frasi. La specifica prevede che il backend escluda i messaggi tutorial prima della risposta pubblica. Disabilitare l'invio durante l'attesa non sostituisce il guard server; gestire il conflitto per richiesta già attiva e non ritentare automaticamente dopo un esito incerto. Il timeout HTTP è 10 secondi, mentre l'eventuale blocco durante recupero resta legato all'opzione server da scegliere.

Persistenza PvE, ricompense, progressione della campagna e scoperta degli stage restano **DA DEFINIRE**. Nessuna nuova API di polling o cancellazione pubblica è concordata.

## Da definire

- Ricerca degli avversari, schermate, stati delle richieste e presentazione della simulazione.
- Stati di attesa ed errore e recupero del risultato in accordo con il contratto BackEnd; criteri di accessibilità e verifica della rappresentazione dei risultati.

## Collegamenti

- [Servizio sfide](../BackEnd/sfide-simulazione.md)
- [Regolamento Versione 2.1](../Digital/regolamento-base.md)
- [Mazzi](gestione-mazzi.md)
- [Storico](storico-partite.md)
- [Indice FrontEnd](README.md)
