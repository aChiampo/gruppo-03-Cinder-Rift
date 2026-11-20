# Area BackEnd

Documentazione dei Software Developer: servizi e regole server. Il backend F01 è disponibile in `springboot_app/app/` con Java 21, Spring Boot 4.1.2, Spring Security e JWT locali; Compose prevede PostgreSQL 18. Il [contratto F01](accesso-utenti.md) definisce autenticazione, ruoli, cookie, CSRF e pulizia delle sessioni. Sono implementati anche profili F01 e gestione iniziale dei mazzi F05, con migrazioni V1–V4; il [contratto F05](gestione-mazzi.md) distingue regole attive e proposte V2.1. Le altre funzionalità mantengono lo stato indicato nelle rispettive schede. Allineamento documentale allo stack corrente: **2026-11-13**, nella revisione richiesta dall’utente.

Le pagine BackEnd ospitano i contratti tecnici canonici delle funzionalità
trasversali, concordati con le altre aree. Approfondire progressivamente permessi,
stati, payload, dati, errori, transazioni, concorrenza e verifiche secondo il
[modello](../modello-funzionalita.md). Le attività BackEnd hanno branch distinti
da quelle FrontEnd, collegati allo stesso ID funzionalità e contratto.
Stato corrente nell'[indice generale](../README.md#stato-delle-funzionalità).

Dal **2026-11-16** il [motore dimostrativo](../../springboot_app/game_engine/README.md#avvio-con-docker-compose) dispone di build Docker e salute Actuator, con H2 in memoria e menu CLI disabilitato nel container. Aggiornamento del **2026-11-20**: l'[API pubblica PvE](app-battaglie-api.md) e l'orchestrazione sono implementate nella copia di lavoro di `feature/app_battle_pve_flow`, con L2 + T2, snapshot coerenti e validatore F05 completato; l'integrazione React resta da realizzare. Comportamento server ed evidenze sono in [App battaglie](app-battaglie.md). [Avvio comune](../../guida-avvio.md) · [Verifica storica dello stack isolato](../report/2026-11-16-verifica-docker-compose.md).

## Base di dati v1.1

Ultimo aggiornamento significativo: **2026-11-09**, per l'inventario aggiornato e per il sistema mercy confermato dall'utente. La [specifica autonoma della base di dati v1.1](Database/database-schema-v1.1.md) è la fonte canonica di tabelle, tipi, vincoli e invarianti; [db_schema-v1.1.dbml](Database/db_schema-v1.1.dbml) ne è la rappresentazione completa in DBML. `INVENTORY` distingue Scaglie (`cobalt_shards`), Nuclei Rift (`rift_cores`) e Rotami (`scraps`), tutti interi obbligatori con default `0`. `USER_MERCY` registra il conto alla rovescia per utente e pacchetto con PK composta e FK a utenti, pacchetti e rarità: avanza sulle aperture corrispondenti e si resetta a ogni pesca qualificante, come descritto in [F03](acquisto-pacchetti.md#mercy-sulle-aperture). La risorsa dei campi generici di scambi, prenotazioni e ricompense resta da definire.

Gli ID generati sono `int` auto-incrementali; FK e chiavi associative ereditano gli interi. Carte e arti sono distinte anche nei mazzi, le prenotazioni appartengono alle trattative e ai consensi correnti, le missioni usano flag di riscossione booleani. Le regole/API ancora proposte restano da concordare. Nessuna migrazione, implementazione o verifica applicativa è attestata da questo allineamento.

Aggiornamento implementato del **2026-11-20**: V7/V8 aggiungono gli effetti condivisi tra creature ed equipaggiamenti; V9 ripara soltanto fazioni mancanti delle creature CR01 note ed energia nulla dei cinque equipaggiamenti iniziali. Il modello SQL, i diagrammi e il DBML degli effetti restano invariati da V9. [Schema e vincoli](Database/database-schema-v1.1.md#perimetro-sql-implementato) · [Effetti nel motore](app-battaglie.md#attivazione-e-responsabilità-degli-effetti) · [Verifiche correnti](app-battaglie.md#decisioni-aperte-e-stato-delle-verifiche).

## Funzionalità

- [Riepilogo degli endpoint](GUIDA-API-FRONTEND.md) — elenco di autenticazione, profili, mazzi, PvE, rotte proposte per scambi e contratto definito per amicizie, con percorso e metodo HTTP, funzione, stato (`Planned`, `BE Implemented`, `FE Implemented`), autenticazione, codici HTTP, ruoli e riferimenti canonici.
- [F01 — Accesso utenti](accesso-utenti.md)
- [F01 — Invio mail con template Thymeleaf](invio-mail.md) — dipendenza documentata; implementazione rinviata.
- [F03 — Offerte e acquisto di pacchetti](acquisto-pacchetti.md)
- [F04 — Collezione delle carte](collezione-carte.md)
- [F05 — Gestione e validazione dei mazzi](gestione-mazzi.md)
- [F06 — Scambi e marketplace](scambi-mercato.md)
- [F07 — Sfide e simulazione](sfide-simulazione.md)
- [F07 — Battaglie PvE nel backend applicativo](app-battaglie.md) — orchestrazione implementata con L2 + T2, budget 8/9/10 secondi e rilascio del guard al termine HTTP.
- [F07 — Endpoint e payload PvE per il FrontEnd](app-battaglie-api.md) — riferimento canonico del contratto pubblico implementato; integrazione React da realizzare.
- [F08 — Storico delle partite](storico-partite.md)
- [F10 — Gestione amministrativa](gestione-amministrativa.md)
- [F13 — Amicizie; dipendenza di F06](amicizie.md) — contratto API definito il **2026-11-20** su decisioni e delega dell'utente: Bearer JWT, sole relazioni proprie per `USER`/`ADMIN`, elenchi completi dal più recente con nickname, icona e timestamp. Implementazione non iniziata; UI e coordinamento F01/F06 da completare.

Allineamento documentale F13/F01/F06 del **2026-11-18**: la [cancellazione account](accesso-utenti.md#integrazione-prevista-con-f13-e-f06) richiede una futura pulizia delle amicizie e degli scambi aperti, con storico completato conservato. La [rimozione dell'amicizia e il cleanup degli scambi](scambi-mercato.md#amicizie-e-cancellazione-degli-account) hanno effetti distinti sul TTL e sulla ripubblicazione. La propagazione dei requisiti non modifica il codice F01 corrente né implementa F13/F06.

## Collegamenti

- [Applicazione backend e dipendenze](../../springboot_app/app/README.md)
- [Indice generale e stato delle funzionalità](../README.md)
- [Area Digital](../Digital/README.md)
- [Area FrontEnd](../FrontEnd/README.md)
- [Modello di documentazione](../modello-funzionalita.md)
- [Linee guida](../../Linee-Guida-e-Regole.md)
- [README del progetto](../../README.md)
