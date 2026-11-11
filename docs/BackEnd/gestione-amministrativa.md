# Gestione amministrativa

ID: F10 · Perimetro: confermato; specifica in approfondimento.
Implementazione: non iniziata. Responsabile: DA DEFINIRE (Software Developer).
Fonte: traccia, risposte e chiarimenti del gruppo; le decisioni note sono riportate sotto.
Fonte dell'approfondimento: regolamento Versione 2.1, proposta per il prototipo e le prove di bilanciamento; integrazione documentale approvata, regole definitive da confermare.
Ultimo aggiornamento significativo: 2026-11-09, allineamento alle risorse in inventario, alla configurazione mercy dei pacchetti e ai riferimenti al regolamento Versione 2.1 su richiesta dell’utente. Verifiche applicative: non eseguite.
Fonte dati: [specifica canonica v1.1](Database/database-schema-v1.1.md#dettaglio-struttura-delle-carte).

## Obiettivo e decisioni note

- Backoffice per il catalogo delle carte e dei pacchetti.
- Prezzo e configurazione del singolo pacchetto sono gestibili dal backoffice; ogni offerta contiene una sola unità, secondo il contratto canonico F03.
- Accesso riservato agli utenti autorizzati; non sono ancora assegnati privilegi ai tre ruoli.

## Cataloghi, vincoli e cancellazione

- Gli ID autonomi di utenti, ruoli, carte, tipi, fazioni, rarità, espansioni, parole chiave, pacchetti e configurazioni sono `int` auto-incrementali. `CARDS_ARTS.id_art` è anch'esso generato e univoco; le FK sono intere senza sequenze autonome. Non si deducono permessi dal valore numerico del ruolo.
- `CARD_TYPES.code` è obbligatorio univoco; i filtri dei pacchetti usano i codici dei cataloghi. Carta e arte sono distinte e vincolate tramite `(id_card, id_art)`. Ogni carta ha al massimo una lista JSONB di parole chiave, con ID interi annidati validati dal backend e range nullable in `KEYWORDS`.
- `PACKS` conserva configurazione e filtri, ma non prezzo o offerta acquistabile: persistenza e risorsa richiesta dal prezzo restano da definire. `INVENTORY` distingue Scaglie (`cobalt_shards`), Nuclei Rift (`rift_cores`) e Rotami (`scraps`), tutti `int` obbligatori con default `0`. Questi saldi non autorizzano conversioni o trasferimenti impliciti, né nuovi permessi di accredito amministrativo. Ogni offerta contiene una sola unità di un pacchetto.
- `PACKS.mercy_threshold` disabilita mercy se `NULL`, altrimenti è positivo. `USER_MERCY` ha PK `(id_user, id_pack)`, FK a utente e pacchetto, rarità minima collegata a `RARITIES.id` e un contatore non negativo inizializzato dal backend dalla soglia. Il progresso avanza sulle aperture del pacchetto corrispondente e si resetta su una pesca qualificante. La configurazione deve consentire almeno una carta/arte valida della rarità richiesta o superiore nei filtri del pacchetto; la policy che assegna inizialmente la rarità e la riconciliazione dei progressi dopo modifiche restano da definire. Le modifiche del catalogo rispettano il guard esclusivo e non attribuiscono nuovi permessi di modifica diretta dei progressi ai ruoli.
- `ROUND_CONFIGURATIONS` definisce destinazioni univoche, parametri positivi e limiti di energia totale e numero di carte. L'immutabilità è una politica applicativa; valori iniziali e altre regole restano aperti. I cataloghi delle missioni hanno `is_deleted` e `progression_required > 0`; premi e comportamento dopo cancellazione logica restano da definire, senza attribuire nuovi permessi amministrativi.
- Cancellare logicamente carte o espansioni richiede il guard esclusivo del catalogo, invalidazione di annunci/trattative e rilascio di tutti gli hold prima della conversione e pulizia di collezione e mazzi. Applicare il [protocollo transazionale comune](Database/database-schema-v1.1.md#protocollo-transazionale-comune); non è un trigger. Tassi, frammenti e recupero per lotti restano da definire.
- `NOTIFICATIONS` usa FK intere per mittente e destinatario, messaggio obbligatorio e stato `new`, `read` o `archived`. Invii di massa e regali richiedono autorizzazione backend e aggiornamenti economici separati dalla notifica.

## Riferimenti alla proposta di regolamento

- Il catalogo delle carte dovrà considerare, dopo la conferma delle regole, i concetti descritti nelle sezioni su [energia e punti potenza](../Digital/regolamento-base.md#3-energia-e-punti-potenza), [caratteristiche delle creature](../Digital/regolamento-base.md#5-caratteristiche-delle-creature), [fazioni](../Digital/regolamento-base.md#6-fazioni) ed [equipaggiamenti](../Digital/regolamento-base.md#7-equipaggiamenti).
- Questi riferimenti sono requisiti di dominio proposti, non uno schema dati o un contratto amministrativo approvato. Statistiche, costi delle carte ed effetti precisi degli equipaggiamenti restano da definire nella documentazione Digital.

## Da definire

- Matrice dei permessi e operazioni consentite su cataloghi già utilizzati.
- Eventuali altre funzioni amministrative.
- Contratto tecnico del catalogo e validazioni; compatibilità delle modifiche alle carte con mazzi e partite esistenti, in coordinamento con F05 e F07.

## Collegamenti

- [Interfaccia backoffice](../FrontEnd/gestione-amministrativa.md)
- [Accesso](accesso-utenti.md)
- [Offerte di pacchetti](acquisto-pacchetti.md)
- [Carte e identità](../Digital/lore-identita-carte.md)
- [Regolamento Versione 2.1](../Digital/regolamento-base.md)
- [Mazzi](gestione-mazzi.md)
- [Sfide](sfide-simulazione.md)
- [Indice BackEnd](README.md)
