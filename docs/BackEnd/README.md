# Area BackEnd

Documentazione sintetica dei Software Developer: servizi e regole server. Il codice sarà in `springboot_app/`; lo stack definitivo è ancora atteso.

Le pagine BackEnd ospitano i contratti tecnici canonici delle funzionalità
trasversali, concordati con le altre aree. Approfondire progressivamente permessi,
stati, payload, dati, errori, transazioni, concorrenza e verifiche secondo il
[modello](../modello-funzionalita.md). Le attività BackEnd hanno branch distinti
da quelle FrontEnd, collegati allo stesso ID funzionalità e contratto.
Stato corrente nell'[indice generale](../README.md#stato-delle-funzionalità).

## Base di dati v1.1

Ultimo aggiornamento significativo: **2026-11-09**, per l'inventario aggiornato e per il sistema mercy confermato dall'utente. La [specifica autonoma della base di dati v1.1](Database/database-schema-v1.1.md) è la fonte canonica di tabelle, tipi, vincoli e invarianti; [db_schema-v1.1.dbml](Database/db_schema-v1.1.dbml) ne è la rappresentazione completa in DBML. `INVENTORY` distingue Scaglie (`cobalt_shards`), Nuclei Rift (`rift_cores`) e Rotami (`scraps`), tutti interi obbligatori con default `0`. `USER_MERCY` registra il conto alla rovescia per utente e pacchetto con PK composta e FK a utenti, pacchetti e rarità: avanza sulle aperture corrispondenti e si resetta a ogni pesca qualificante, come descritto in [F03](acquisto-pacchetti.md#mercy-sulle-aperture). La risorsa dei campi generici di scambi, prenotazioni e ricompense resta da definire.

Gli ID generati sono `int` auto-incrementali; FK e chiavi associative ereditano gli interi. Carte e arti sono distinte anche nei mazzi, le prenotazioni appartengono alle trattative e ai consensi correnti, le missioni usano flag di riscossione booleani. Le regole/API ancora proposte restano da concordare. Nessuna migrazione, implementazione o verifica applicativa è attestata da questo allineamento.

## Funzionalità

- [F01 — Accesso utenti](accesso-utenti.md)
- [F03 — Offerte e acquisto di pacchetti](acquisto-pacchetti.md)
- [F04 — Collezione delle carte](collezione-carte.md)
- [F05 — Gestione e validazione dei mazzi](gestione-mazzi.md)
- [F06 — Scambi e marketplace](scambi-mercato.md)
- [F07 — Sfide e simulazione](sfide-simulazione.md)
- [F08 — Storico delle partite](storico-partite.md)
- [F10 — Gestione amministrativa](gestione-amministrativa.md)
- [F13 — Amicizie; dipendenza di F06](amicizie.md)

## Collegamenti

- [Applicazione backend e dipendenze](../../springboot_app/app/README.md)
- [Indice generale e stato delle funzionalità](../README.md)
- [Area Digital](../Digital/README.md)
- [Area FrontEnd](../FrontEnd/README.md)
- [Modello di documentazione](../modello-funzionalita.md)
- [Linee guida](../../Linee-Guida-e-Regole.md)
- [README del progetto](../../README.md)
