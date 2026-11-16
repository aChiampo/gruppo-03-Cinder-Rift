# Interfaccia di backoffice

ID: F10 · Perimetro: confermato; specifica in approfondimento.
Implementazione: non iniziata. Responsabile: DA DEFINIRE (Web Developer).
Fonte: traccia, risposte e chiarimenti del gruppo; le decisioni note sono riportate sotto.
Fonte dell'approfondimento: regolamento Versione 2.1, proposta per il prototipo e le prove di bilanciamento; integrazione documentale approvata, regole definitive da confermare.
Ultimo aggiornamento significativo: 2026-11-09, allineamento dei riferimenti al regolamento Versione 2.1 su richiesta dell’utente. La decisione F03 del 2026-11-04 conferma un solo pacchetto per offerta. Verifiche applicative: non eseguite.

## Obiettivo e decisioni note

- Interfacce di gestione delle carte e dei pacchetti.
- Consentire agli utenti autorizzati di gestire prezzo e configurazione del singolo pacchetto, secondo i contratti canonici BackEnd di F03/F10; ogni offerta contiene un solo pacchetto.
- Il backend riserva il namespace amministrativo ad `ADMIN`, come indicato nel [contratto F10](../BackEnd/gestione-amministrativa.md#obiettivo-e-decisioni-note). Il client deve gestire `401` e `403`; nascondere le funzioni nell’interfaccia non sostituisce il controllo server. Allineamento documentale: **2026-11-12**, su richiesta dell’utente.

## Implicazioni della proposta di regolamento

- Le [caratteristiche delle creature](../Digital/regolamento-base.md#5-caratteristiche-delle-creature) e gli [equipaggiamenti](../Digital/regolamento-base.md#7-equipaggiamenti) sono riferimenti di dominio proposti per la futura gestione delle carte.
- Campi modificabili, validazioni ed esiti dipendono dal [contratto canonico BackEnd di F10](../BackEnd/gestione-amministrativa.md), ancora da definire. La proposta non assegna privilegi ai ruoli e non approva un modulo amministrativo.

## Da definire

- Permessi di dettaglio per le operazioni amministrative, schermate e validazioni; accesso di `USER` al namespace amministrativo escluso.

## Collegamenti

- [Servizio backoffice](../BackEnd/gestione-amministrativa.md)
- [Accesso](accesso-utenti.md)
- [Offerte di pacchetti](acquisto-pacchetti.md)
- [Regolamento Versione 2.1](../Digital/regolamento-base.md)
- [Indice FrontEnd](README.md)
