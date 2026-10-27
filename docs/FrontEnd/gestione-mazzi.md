# Composizione dei mazzi

ID: F05 · Perimetro: confermato; specifica in approfondimento.
Implementazione: non iniziata. Responsabile: DA DEFINIRE (Web Developer).
Fonte: traccia, risposte e chiarimenti del gruppo; le decisioni note sono riportate sotto.
Fonte dell'approfondimento: regolamento Versione 2.0, proposta per il prototipo e le prove di bilanciamento; integrazione documentale approvata, regole definitive da confermare.
Ultimo aggiornamento significativo: 2026-10-27. Verifiche applicative: non eseguite.

## Obiettivo e decisioni note

- Consentire composizione e salvataggio di più mazzi.
- Mostrare i vincoli e gli esiti della validazione; la verifica definitiva resta al server.

## Implicazioni della proposta di regolamento

- La proposta di [struttura dei mazzi](../Digital/regolamento-base.md#2-struttura-del-mazzo) introduce l'assegnazione delle creature ai round e un gruppo separato di equipaggiamenti: l'interfaccia dovrà renderne comprensibile la composizione dopo la conferma delle regole.
- [Energia e punti potenza](../Digital/regolamento-base.md#3-energia-e-punti-potenza) hanno funzioni differenti. La presentazione dei [vincoli di composizione](../Digital/regolamento-base.md#4-budget-e-vincoli-di-composizione) dovrà rispettare questa distinzione senza mantenere una specifica numerica indipendente.
- Dati scambiati ed esiti di validazione dipendono dal [contratto canonico BackEnd di F05](../BackEnd/gestione-mazzi.md), ancora da definire. I riferimenti alla proposta non stabiliscono layout, payload o codici di errore.

## Da definire

- Layout, operazioni disponibili e presentazione delle violazioni.
- Conferma dei vincoli proposti e loro presentazione; effetti degli scambi sulle carte usate nei mazzi.
- Stati di caricamento, errore e salvataggio, navigazione da tastiera e comunicazione accessibile delle violazioni, in accordo con il contratto BackEnd.

## Collegamenti

- [Validazione server](../BackEnd/gestione-mazzi.md)
- [Regolamento](../Digital/regolamento-base.md)
- [Collezione](collezione-carte.md)
- [Indice FrontEnd](README.md)
