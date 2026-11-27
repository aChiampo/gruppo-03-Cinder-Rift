# Area BackEnd

Documentazione dei Software Developer: servizi e regole server. Il backend in `springboot_app/app/` usa Java 21, Spring Boot 4.1.2, Spring Security e JWT locali; Compose prevede PostgreSQL 18. Sono implementati [autenticazione e profili F01](accesso-utenti.md), [catalogo e collezione F04](collezione-carte.md), [mazzi e validazione F05](gestione-mazzi.md), orchestrazione PvE F07 e [scritture amministrative carte F10](gestione-amministrativa.md). Flyway applica nove script, V1–V8 e V11; il modello v1.1 completo resta parzialmente implementato. Allineamento ai sorgenti: **2026-11-27**. Le altre funzionalità mantengono lo stato indicato nelle rispettive schede.

Le pagine BackEnd ospitano i contratti tecnici canonici delle funzionalità
trasversali, concordati con le altre aree. Approfondire progressivamente permessi,
stati, payload, dati, errori, transazioni, concorrenza e verifiche secondo il
[modello](../modello-funzionalita.md). Le attività BackEnd hanno branch distinti
da quelle FrontEnd, collegati allo stesso ID funzionalità e contratto.
Stato corrente nell'[indice generale](../README.md#stato-delle-funzionalità).

Dal **2026-11-16** il [motore dimostrativo](../../springboot_app/game_engine/README.md#avvio-con-docker-compose) dispone di build Docker e salute Actuator, con H2 in memoria e menu CLI disabilitato nel container. L'[API pubblica PvE](app-battaglie-api.md) e l'orchestrazione sono presenti nei sorgenti correnti, con L2 + T2, snapshot coerenti e validatore F05 condiviso. React avvia la simulazione e mostra il risultato della sessione; integrazione browser non verificata in questa revisione. Comportamento server ed evidenze sono in [App battaglie](app-battaglie.md). [Avvio comune](../../guida-avvio.md) · [Verifica corrente](../report/2026-11-23-allineamento-documentazione.md) · [Verifica storica dello stack isolato](https://github.com/aChiampo/gruppo-03-Cinder-Rift/blob/400ceeb0a81aca58d7abd6baff355f60baf2c12c/docs/report/2026-11-16-verifica-docker-compose.md).

## Base di dati v1.1

Ultimo aggiornamento significativo: **2026-11-23**, su richiesta dell’utente: `PACKS.pack_description` diventa JSONB strutturato con anteprime compatibili con il catalogo F04 e si aggiunge `pack_price int`, secondo il [contratto F03](acquisto-pacchetti.md#descrizione-strutturata-e-prezzo-del-pacchetto). Pacchetti e prezzo sono definiti nel modello, senza migrazioni o API implementate. La [specifica autonoma della base di dati v1.1](Database/database-schema-v1.1.md) è la fonte canonica di tabelle, tipi, vincoli e invarianti; [db_schema-v1.1.dbml](Database/db_schema-v1.1.dbml) ne è la rappresentazione completa in DBML. `INVENTORY` distingue Scaglie (`cobalt_shards`), Nuclei Rift (`rift_cores`) e Rotami (`scraps`), tutti interi obbligatori con default `0`. `USER_MERCY` registra il conto alla rovescia per utente e pacchetto con PK composta e FK a utenti, pacchetti e rarità: avanza sulle aperture corrispondenti e si resetta a ogni pesca qualificante, come descritto in [F03](acquisto-pacchetti.md#mercy-sulle-aperture). La risorsa dei campi generici di scambi, prenotazioni e ricompense resta da definire.

Gli ID generati sono `int` auto-incrementali; FK e chiavi associative ereditano gli interi. Carte e arti sono distinte anche nei mazzi, le prenotazioni appartengono alle trattative e ai consensi correnti, le missioni usano flag di riscossione booleani. Le regole/API ancora proposte restano da concordare. Nessuna migrazione, implementazione o verifica applicativa è attestata da questo allineamento.

Aggiornamento del **2026-11-27**: V3 contiene lo schema completo delle carte e V5 inserisce direttamente fazioni, energia e arti definitive. V9/V10 sono rimosse. V7/V8 gestiscono gli effetti condivisi tra creature ed equipaggiamenti; gli ID sono generati dal database e V8 associa gli effetti tramite `cards.id`. [Schema e vincoli](Database/database-schema-v1.1.md#perimetro-sql-implementato) · [Effetti nel motore](app-battaglie.md#attivazione-e-responsabilità-degli-effetti) · [Verifica della riscrittura](../../springboot_app/app/README.md#verifica-della-riscrittura-del-2026-11-27).

## Funzionalità

- [Riepilogo degli endpoint](GUIDA-API-FRONTEND.md) — elenco di autenticazione, profili, mazzi, PvE, rotte proposte per scambi e contratto definito per amicizie, con percorso e metodo HTTP, funzione, stato (`Planned`, `BE Implemented`, `FE Implemented`), autenticazione, codici HTTP, ruoli e riferimenti canonici.
- [F01 — Accesso utenti](accesso-utenti.md)
- [F01 — Invio mail con template Thymeleaf](invio-mail.md) — dipendenza documentata; implementazione rinviata.
- [F03 — Offerte e acquisto di pacchetti](acquisto-pacchetti.md)
- [F04 — Catalogo e collezione delle carte](collezione-carte.md) — letture separate nei package `card` e `collection`.
- [F05 — Gestione e validazione dei mazzi](gestione-mazzi.md)
- [F06 — Scambi e marketplace](scambi-mercato.md)
- [F07 — Sfide e simulazione](sfide-simulazione.md)
- [F07 — Battaglie PvE nel backend applicativo](app-battaglie.md) — orchestrazione implementata con L2 + T2, budget 8/9/10 secondi e rilascio del guard al termine HTTP.
- [F07 — Endpoint e payload PvE per il FrontEnd](app-battaglie-api.md) — riferimento canonico del contratto pubblico implementato e usato da React.
- [F08 — Storico delle partite](storico-partite.md)
- [F10 — Gestione amministrativa](gestione-amministrativa.md)
- [F13 — Amicizie; dipendenza di F06](amicizie.md) — backend, migrazione V11 e cleanup amicizie F01 implementati il **2026-11-22** nella copia di lavoro. Frontend e F06 esclusi; [report e verifiche](../report/2026-11-22-implementazione-amicizie.md).

Aggiornamento F13/F01 del **2026-11-22**: il cleanup delle amicizie usa la stessa transazione della [cancellazione account](accesso-utenti.md#integrazione-prevista-con-f13-e-f06). Il [cleanup scambi F06](scambi-mercato.md#amicizie-e-cancellazione-degli-account) resta da implementare.

## Collegamenti

- [Applicazione backend e dipendenze](../../springboot_app/app/README.md)
- [Indice generale e stato delle funzionalità](../README.md)
- [Area Digital](../Digital/README.md)
- [Area FrontEnd](../FrontEnd/README.md)
- [Modello di documentazione](../modello-funzionalita.md)
- [Linee guida](../../Linee-Guida-e-Regole.md)
- [README del progetto](../../README.md)
