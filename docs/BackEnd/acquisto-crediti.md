# Acquisto simulato di crediti

ID: F02 · Perimetro: confermato; specifica sintetica.
Implementazione: non iniziata. Responsabile: DA DEFINIRE (Software Developer).
Fonte: traccia, risposte e chiarimenti del gruppo; le decisioni note sono riportate sotto.

## Obiettivo e decisioni note

- L'utente sceglie un'offerta di crediti associata a un importo monetario simulato.
- Il sistema simula l'acquisto, invia un'email di conferma e consente l'uso dei crediti; nessun addebito reale nella fase 1.
- La progettazione dei dati deve predisporre gli acquisti futuri e distinguere le operazioni simulate.

## Da definire

- Offerte, prezzi, valuta, limiti di acquisto e dettagli dei dati persistenti.
- Rapporto tra accredito e conferma email, errori e ritentativi senza duplicare crediti.

## Collegamenti

- [Interfaccia crediti](../FrontEnd/acquisto-crediti.md)
- [Economia](../Digital/economia-crediti-pacchetti.md)
- [Offerte di pacchetti](acquisto-pacchetti.md)
- [Indice BackEnd](README.md)
