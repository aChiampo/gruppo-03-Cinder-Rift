# Gestione e validazione dei mazzi

ID: F05 · Perimetro: confermato; specifica in approfondimento.
Implementazione: non iniziata. Responsabile: DA DEFINIRE (Software Developer).
Fonte: traccia, risposte e chiarimenti del gruppo; le decisioni note sono riportate sotto.
Fonte dell'approfondimento: regolamento Versione 2.0, proposta per il prototipo e le prove di bilanciamento; integrazione documentale approvata, regole definitive da confermare.
Ultimo aggiornamento significativo: 2026-10-27. Verifiche applicative e di bilanciamento: non eseguite.

## Obiettivo e decisioni note

- Ogni utente può comporre e salvare più mazzi.
- Il server verifica proprietà delle carte e vincoli del regolamento.

## Riferimenti alla proposta di regolamento

- La [struttura del mazzo](../Digital/regolamento-base.md#2-struttura-del-mazzo), l'[energia e i punti potenza](../Digital/regolamento-base.md#3-energia-e-punti-potenza) e i [vincoli di composizione](../Digital/regolamento-base.md#4-budget-e-vincoli-di-composizione) sono descritti nella Versione 2.0 e restano proposti fino alla conferma delle regole.
- La [preparazione della partita](../Digital/regolamento-base.md#8-preparazione-della-partita) propone i controlli prima della simulazione e il fissaggio della composizione per la partita.
- I valori e le eccezioni restano nella pagina Digital. Questa pagina ospiterà il contratto tecnico canonico della validazione; il collegamento alla proposta non definisce API, payload o codici di errore.

## Da definire

- Conferma dei vincoli proposti e risoluzione degli [elementi da completare e testare](../Digital/regolamento-base.md#15-elementi-da-completare-e-testare) che incidono sulla validazione.
- Contratto tecnico condiviso: operazioni, input/output, autorizzazioni, rappresentazione delle violazioni e criteri di accettazione.
- Uso delle carte in più mazzi ed effetti degli scambi o delle modifiche al catalogo.

## Collegamenti

- [Interfaccia mazzi](../FrontEnd/gestione-mazzi.md)
- [Regolamento](../Digital/regolamento-base.md)
- [Scambi](scambi-mercato.md)
- [Sfide](sfide-simulazione.md)
- [Indice BackEnd](README.md)
