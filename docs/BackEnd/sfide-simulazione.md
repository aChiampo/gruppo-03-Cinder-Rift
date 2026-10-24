# Sfide e simulazione

ID: F07 · Perimetro: confermato; specifica sintetica.
Implementazione: non iniziata. Responsabile: DA DEFINIRE (Software Developer).
Fonte: traccia, risposte e chiarimenti del gruppo; le decisioni note sono riportate sotto.

## Obiettivo e decisioni note

- Un utente propone una sfida scegliendo un mazzo; l'avversario accetta scegliendo il proprio.
- Il server mescola i mazzi e simula la modalità base senza decisioni interattive durante la partita.
- Il motore deve essere un componente Java isolato, coperto da test JUnit.
- Risultato e dettaglio dei turni devono essere disponibili ai due giocatori.

## Da definire

- Regolamento numerico, fine partita e gestione dei casi limite.
- Stati della sfida, controllo dei mazzi al momento dell'accettazione e gestione delle richieste ripetute.

## Collegamenti

- [Interfaccia sfide](../FrontEnd/sfide-simulazione.md)
- [Regolamento](../Digital/regolamento-base.md)
- [Mazzi](gestione-mazzi.md)
- [Storico](storico-partite.md)
- [Indice BackEnd](README.md)
