# Area BackEnd

Documentazione dei Software Developer: servizi e regole server. Il backend F01 è disponibile in `springboot_app/app/` con Java 21, Spring Boot 4.1.2, Spring Security e JWT locali; Compose prevede PostgreSQL 18. Il [contratto F01](accesso-utenti.md) definisce autenticazione, ruoli, cookie, CSRF e pulizia delle sessioni. Sono implementati anche profili F01 e gestione iniziale dei mazzi F05, con migrazioni V1–V4; il [contratto F05](gestione-mazzi.md) distingue regole attive e proposte V2.1. Le altre funzionalità mantengono lo stato indicato nelle rispettive schede. Allineamento documentale allo stack corrente: **2026-11-13**, nella revisione richiesta dall’utente.

Le pagine BackEnd ospitano i contratti tecnici canonici delle funzionalità
trasversali, concordati con le altre aree. Approfondire progressivamente permessi,
stati, payload, dati, errori, transazioni, concorrenza e verifiche secondo il
[modello](../modello-funzionalita.md). Le attività BackEnd hanno branch distinti
da quelle FrontEnd, collegati allo stesso ID funzionalità e contratto.
Stato corrente nell'[indice generale](../README.md#stato-delle-funzionalità).

Dal **2026-11-16** il [motore dimostrativo](../../springboot_app/game_engine/README.md#avvio-con-docker-compose) dispone di build Docker e salute Actuator, con H2 in memoria e menu CLI disabilitato nel container. Allineamento del **2026-11-20**: la PR #36 è integrata e l'API interna PvE è implementata; comportamento server e limiti sono in [App battaglie](app-battaglie.md). La funzionalità applicativa e l'[API pubblica](app-battaglie-api.md) sono documentate, ma orchestrazione e integrazione React non sono implementate. [Avvio comune](../../guida-avvio.md) · [Verifica storica dello stack isolato](../report/2026-11-16-verifica-docker-compose.md).

## Base di dati v1.1

Ultimo aggiornamento significativo: **2026-11-09**, per l'inventario aggiornato e per il sistema mercy confermato dall'utente. La [specifica autonoma della base di dati v1.1](Database/database-schema-v1.1.md) è la fonte canonica di tabelle, tipi, vincoli e invarianti; [db_schema-v1.1.dbml](Database/db_schema-v1.1.dbml) ne è la rappresentazione completa in DBML. `INVENTORY` distingue Scaglie (`cobalt_shards`), Nuclei Rift (`rift_cores`) e Rotami (`scraps`), tutti interi obbligatori con default `0`. `USER_MERCY` registra il conto alla rovescia per utente e pacchetto con PK composta e FK a utenti, pacchetti e rarità: avanza sulle aperture corrispondenti e si resetta a ogni pesca qualificante, come descritto in [F03](acquisto-pacchetti.md#mercy-sulle-aperture). La risorsa dei campi generici di scambi, prenotazioni e ricompense resta da definire.

Gli ID generati sono `int` auto-incrementali; FK e chiavi associative ereditano gli interi. Carte e arti sono distinte anche nei mazzi, le prenotazioni appartengono alle trattative e ai consensi correnti, le missioni usano flag di riscossione booleani. Le regole/API ancora proposte restano da concordare. Nessuna migrazione, implementazione o verifica applicativa è attestata da questo allineamento.

Aggiornamento implementato del **2026-11-20**: le migrazioni V7/V8 aggiungono gli effetti condivisi tra creature ed equipaggiamenti e i relativi seed CR01. Il modello SQL, i diagrammi e il DBML sono allineati. Verificati 139 test applicativi su PostgreSQL 18.6 isolato e 45 test del motore; l'orchestrazione applicativa delle battaglie resta non implementata. [Schema e vincoli](Database/database-schema-v1.1.md#perimetro-sql-implementato) · [Effetti nel motore](app-battaglie.md#attivazione-e-responsabilità-degli-effetti).

## Funzionalità

- [Riepilogo degli endpoint](GUIDA-API-FRONTEND.md) — elenco con percorso e metodo HTTP, funzione e stato (`Planned`, `BE Implemented`, `FE Implemented`), con riferimenti ai contratti canonici.
- [F01 — Accesso utenti](accesso-utenti.md)
- [F01 — Invio mail con template Thymeleaf](invio-mail.md) — dipendenza documentata; implementazione rinviata.
- [F03 — Offerte e acquisto di pacchetti](acquisto-pacchetti.md)
- [F04 — Collezione delle carte](collezione-carte.md)
- [F05 — Gestione e validazione dei mazzi](gestione-mazzi.md)
- [F06 — Scambi e marketplace](scambi-mercato.md)
- [F07 — Sfide e simulazione](sfide-simulazione.md)
- [F07 — Battaglie PvE nel backend applicativo](app-battaglie.md) — decisioni del 2026-11-20 e opzioni di lock/recupero ancora da scegliere; implementazione applicativa non iniziata.
- [F07 — Endpoint e payload PvE per il FrontEnd](app-battaglie-api.md) — riferimento canonico del contratto pubblico documentato; implementazione applicativa non iniziata.
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
