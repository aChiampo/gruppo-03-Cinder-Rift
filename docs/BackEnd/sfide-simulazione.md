# Sfide e simulazione

ID: F07 · Perimetro: confermato; specifica in approfondimento.
Implementazione: non iniziata. Responsabile: DA DEFINIRE (Software Developer).
Fonte: traccia, risposte e chiarimenti del gruppo; le decisioni note sono riportate sotto.
Fonte dell'approfondimento: regolamento Versione 2.0, proposta per il prototipo e le prove di bilanciamento; integrazione documentale approvata, regole definitive da confermare.
Ultimo aggiornamento significativo: 2026-10-27. Verifiche applicative e di bilanciamento: non eseguite.

## Obiettivo e decisioni note

- Un utente propone una sfida scegliendo un mazzo; l'avversario accetta scegliendo il proprio.
- Il server mescola i mazzi e simula la modalità base senza decisioni interattive durante la partita.
- Il motore deve essere un componente Java isolato, coperto da test JUnit.
- Risultato e dettaglio della simulazione devono essere disponibili ai due giocatori.

## Riferimenti alla proposta di regolamento

- Le sezioni 6–13 della Versione 2.0 propongono il comportamento del motore: [fazioni](../Digital/regolamento-base.md#6-fazioni), [equipaggiamenti](../Digital/regolamento-base.md#7-equipaggiamenti), [preparazione](../Digital/regolamento-base.md#8-preparazione-della-partita), [inizio del round](../Digital/regolamento-base.md#9-inizio-di-ogni-round), [combattimento](../Digital/regolamento-base.md#10-combattimento) e condizioni di [fine round](../Digital/regolamento-base.md#12-conclusione-del-round) e [fine partita](../Digital/regolamento-base.md#13-conclusione-della-partita).
- La [formula dei danni ai giocatori](../Digital/regolamento-base.md#11-danni-ai-giocatori) richiede ancora conferma con gli insegnanti. Gli altri punti aperti sono raccolti nella [sezione 15](../Digital/regolamento-base.md#15-elementi-da-completare-e-testare).
- Le regole restano nella pagina Digital. Questa pagina ospiterà il contratto tecnico canonico di sfide e simulazione; la proposta non stabilisce endpoint, payload, stati della sfida o garanzie tecniche non ancora concordate.
- Il risultato destinato allo [storico](storico-partite.md) dovrà riferirsi al [resoconto proposto](../Digital/regolamento-base.md#14-resoconto-della-partita), dopo la conferma del relativo contratto.

## Da definire

- Conferma delle regole proposte, della formula del danno e dei casi limite; effetti degli equipaggiamenti e ordine di risoluzione restano da definire.
- Stati della sfida, controllo dei mazzi al momento dell'accettazione e gestione delle richieste ripetute.
- Contratto tecnico condiviso: operazioni, dati, autorizzazioni, errori, persistenza e criteri di accettazione del motore.

## Collegamenti

- [Interfaccia sfide](../FrontEnd/sfide-simulazione.md)
- [Regolamento](../Digital/regolamento-base.md)
- [Mazzi](gestione-mazzi.md)
- [Storico](storico-partite.md)
- [Indice BackEnd](README.md)
