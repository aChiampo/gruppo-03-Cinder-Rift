# Sfide e simulazione

ID: F07 · Perimetro: confermato; specifica in approfondimento.
Implementazione: endpoint interno del motore, orchestrazione applicativa PvE e client React disponibili; funzionalità complete PvP e persistenza da realizzare. Responsabile: DA DEFINIRE (Software Developer).
Fonte: traccia, risposte e chiarimenti del gruppo; le decisioni note sono riportate sotto.
Fonte dell'approfondimento: regolamento Versione 2.1, proposta per il prototipo e le prove di bilanciamento; integrazione documentale approvata, regole definitive da confermare.
Ultimo aggiornamento significativo il 2026-11-20, implementazione applicativa PvE e completamento del validatore F05 su richiesta dell'utente. [Verifiche applicative e del motore](https://github.com/aChiampo/gruppo-03-Cinder-Rift/blob/73ad701a20b4fbd3e540689e92b5569a4c1e4da0/docs/report/2026-11-20-orchestrazione-pve.md); browser, distribuzione e bilanciamento definitivo restano da verificare.
Fonte dati: [specifica canonica v1.1](Database/database-schema-v1.1.md#storico-battaglie).

## Obiettivo e decisioni note

- Un utente propone una sfida scegliendo un mazzo; l'avversario accetta scegliendo il proprio.
- Il server mescola i mazzi e simula la modalità base senza decisioni interattive durante la partita.
- Il motore deve essere un componente Java isolato, coperto da test JUnit.
- Risultato e dettaglio della simulazione devono essere disponibili ai due giocatori.

## Dati e confini della persistenza v1.1

- Utenti, mazzi, carte, arti e battaglie hanno identificativi `int`; le PK autonome sono auto-incrementali. Le FK copiano gli ID esistenti. `DECKS.is_challengeable`, obbligatorio con default `false`, abilita la selezione nel matchmaking senza attestare la validità del mazzo.
- La porzione PvE rivalida già proprietà, composizione, possesso della precisa variante e configurazioni di `ROUND_CONFIGURATIONS` prima dello snapshot, inclusi numero massimo di carte, energia individuale e potenza aggregata per destinazione. Il [contratto F05](gestione-mazzi.md#implementazione-backend-disponibile) descrive i limiti iniziali, incluso `EQUIPMENT`; l'estensione al flusso PvP resta da realizzare.
- `BATTLE_HISTORY` conserva due partecipanti, vincitore nullable, `deck_player1` e `deck_player2` JSONB obbligatori, data e `ranking_reward` nullable. Vincitore nullo significa pareggio; altrimenti il `CHECK` lo limita a uno dei partecipanti. La diversità dei partecipanti è applicativa.
- `ranking_reward` non nullo è la variazione con segno applicata esclusivamente a `player1_id`, nella transazione del risultato. `player2_id` non riceve variazioni per questa battaglia; nelle amichevoli il valore è `NULL`. Le formule non sono fissate dallo schema.
- Gli snapshot preservano carte e arti usate dopo modifiche dei mazzi o del catalogo. Lo schema non contiene sfide attive, log dei round o un vincolo contro doppi avvii della stessa partita; contratto JSON e idempotenza restano da definire.
- Validazione e snapshot devono condividere un confine coerente con il [protocollo transazionale comune](Database/database-schema-v1.1.md#protocollo-transazionale-comune). Giocabilità delle copie prenotate e lock per tutta la partita restano aperti.

## Riferimenti alla proposta di regolamento

- Le sezioni 6–13 della Versione 2.1 propongono il comportamento del motore: [fazioni](../Digital/regolamento-base.md#6-fazioni), [equipaggiamenti](../Digital/regolamento-base.md#7-equipaggiamenti), [preparazione](../Digital/regolamento-base.md#8-preparazione-della-partita), [inizio del round](../Digital/regolamento-base.md#9-inizio-di-ogni-round), [combattimento](../Digital/regolamento-base.md#10-combattimento) e condizioni di [fine round](../Digital/regolamento-base.md#12-conclusione-del-round) e [fine partita](../Digital/regolamento-base.md#13-conclusione-della-partita).
- La [formula dei danni ai giocatori](../Digital/regolamento-base.md#11-danni-ai-giocatori) richiede ancora conferma con gli insegnanti. Gli altri punti aperti sono raccolti nella [sezione 15](../Digital/regolamento-base.md#15-elementi-da-completare-e-testare).
- Le regole restano nella pagina Digital. Questa pagina è il riferimento generale F07; il contratto pubblico della porzione PvE è delegato al documento API dedicato indicato sotto. Il contratto completo delle sfide PvP resta da concordare.
- Il risultato destinato allo [storico](storico-partite.md) dovrà riferirsi al [resoconto proposto](../Digital/regolamento-base.md#14-resoconto-della-partita), dopo la conferma del relativo contratto.

## Battaglie PvE nel backend applicativo

La specifica del **2026-11-20**, richiesta dall'utente, è raccolta in [App battaglie](app-battaglie.md). Il riferimento canonico di endpoint, header e payload pubblici è [API delle battaglie per il FrontEnd](app-battaglie-api.md). L'integrazione usa il flusso `frontend → applicazione → motore`, identità dal Bearer, ID delle carte del database, log della PR #36 senza messaggi tutorial, rifiuto degli invii sovrapposti e timeout HTTP di 10 secondi.

L'endpoint interno introdotto dalla PR #36, l'orchestrazione applicativa, il validatore F05 condiviso e le deadline cooperative sono presenti nei sorgenti correnti. L'utente ha scelto L2 + T2, transazione breve `REPEATABLE READ`, budget 8/9/10 secondi e rilascio del guard al termine HTTP anche in esito incerto. Contratto interno, comportamento server ed evidenze sono in App battaglie. React chiama il backend e mostra round, scambi e replay del risultato in memoria. Persistenza PvE, ricompense, progressione e scoperta degli stage restano da realizzare o **DA DEFINIRE**; questa porzione non completa F07. Allineamento documentale del **2026-11-23**: [verifica e limiti](../report/2026-11-23-allineamento-documentazione.md).

## Da definire

- Conferma delle regole proposte, della formula del danno e dei casi limite; effetti degli equipaggiamenti e ordine di risoluzione restano da definire.
- Stati e persistenza della sfida, implementazione della rivalidazione prima dello snapshot, deduplicazione degli avvii e formato definitivo dei JSON.
- Contratto tecnico condiviso completo PvP: operazioni, dati, autorizzazioni, errori, persistenza e criteri di accettazione del motore. Per la porzione PvE vedere i punti aperti della specifica applicativa dedicata.

## Collegamenti

- [Interfaccia sfide](../FrontEnd/sfide-simulazione.md)
- [Regolamento](../Digital/regolamento-base.md)
- [Mazzi](gestione-mazzi.md)
- [Storico](storico-partite.md)
- [Indice BackEnd](README.md)
