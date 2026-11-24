# Lista di verifica della consegna

Le caselle si completano solo con evidenze pertinenti; il superamento della suite backend non completa la consegna integrata. Collegare le evidenze nelle relative issue o pull request.

Revisione del **2026-11-23**: documentazione confrontata con i sorgenti locali, con correzioni per integrazione React F01/F04/F05/F07, migrazioni V1–V10, backoffice carte e visualizzatore PvE della sessione. Superati 54 test frontend e build. [Report corrente e limiti](report/2026-11-23-allineamento-documentazione.md). Le caselle di consegna restano aperte: questa verifica non certifica l'intera applicazione o i materiali finali.

Aggiornamento documentale F13/F01/F06 del **2026-11-18**, su richiesta dell'utente: aggiunti i requisiti concordati per amicizie e cleanup account. Le caselle sotto restano aperte; la propagazione dei contratti non implementa né verifica la funzionalità.

Revisione storica del **2026-11-13**: **118 test** superati su PostgreSQL **18.6**, migrazioni V1–V4 e JAR verificati. [Report con riferimento e limiti](report/2026-11-13-revisione-stato-sviluppo.md). Anche le note del 2026-11-12 sotto sono storiche.

Revisione documentale F01 del **2026-11-12**, su richiesta dell’utente: backend, test e report locali esistono; stato ed evidenze sono nel [contratto di autenticazione](BackEnd/accesso-utenti.md#verifiche-ed-evidenze). Le caselle di consegna restano distinte dai test già presenti: integrazione React e verifica su PostgreSQL 18 tramite Compose non sono completate da questa revisione.

La base backend del recupero password è stata aggiunta nello stesso giorno; compilazione e 31 test senza database sono superati. I nuovi test PostgreSQL del recupero non sono eseguiti, email e interfaccia sono rinviate. Questo aggiornamento non completa alcuna casella di consegna.

## Identità e perimetro

- [ ] Completare e confermare nome progetto, numero gruppo e account GitHub mancanti; nove membri e Team Leader sono già riportati nel README.
- [ ] Confermare stack frontend e chiarimento PostgreSQL/MySQL rispetto alla traccia; lo stack backend è già configurato nel POM.
- [ ] Completare lore, regolamento e valori numerici.
- [ ] Risolvere F09 e decidere se confermare O01.

## Applicazioni e dati

- [ ] Includere sorgenti frontend e backend con istruzioni effettive di avvio/build/test.
- [ ] Includere export database ripristinabile e istruzioni di importazione.
- [ ] Fornire configurazione di esempio senza credenziali o dati personali reali.
- [ ] Verificare accesso JWT e permessi per `ADMIN` e `USER`.
- [ ] Verificare offerte di pacchetti, apertura, collezione e backoffice.
- [ ] Verificare mazzi e vincoli lato server.
- [ ] Verificare scambi privati/pubblici, carte/crediti, consenso, durata e coerenza dei trasferimenti.
- [ ] Implementare e verificare [F13](BackEnd/amicizie.md): permessi identici di `USER`/`ADMIN` sulle sole proprie relazioni, elenchi completi senza paginazione, validazione `400`/`422` ed errori `409`/`404` per duplicati, ripetizioni e stati incompatibili, con messaggio nel payload.
- [ ] Verificare i dettagli del contratto API F13 definiti il **2026-11-20**: JWT su ogni rotta, nickname/icona/timestamp sempre presenti secondo la nullabilità concordata, ordinamento decrescente per creazione delle richieste o accettazione delle amicizie, parità risolta per ID e formato comune degli errori.
- [ ] Integrare e verificare il [cleanup F01/F06/F13](BackEnd/accesso-utenti.md#integrazione-prevista-con-f13-e-f06) al soft delete: richieste/amicizie e scambi aperti ripuliti, hold rilasciati, scambi completati conservati e annuncio pubblico valido della controparte ripubblicato come da rifiuto; verificare anche TTL e rimozione della sola amicizia.
- [ ] Verificare sfide, storico e dettaglio dei turni.
- [ ] Verificare isolamento del motore Java e test JUnit, compresi i casi limite.
- [ ] Verificare responsive, accessibilità, SEO e prestazioni secondo criteri concordati.

## Documentazione e materiali

- [ ] Aggiornare README, documentazione applicativa e collegamenti tra aree.
- [ ] Approfondire le sezioni pertinenti del modello con le decisioni disponibili: permessi, stati, contratti, dati, errori, transazioni/concorrenza/retry, criteri e verifiche; esplicitare le lacune.
- [ ] Allineare FrontEnd e BackEnd al contratto canonico e collegare le attività sui rispettivi branch separati.
- [ ] Aggiornare lo stato corrente nell'indice e collegare le evidenze con data, commit/PR, ambiente e limiti; mantenere separati i report storici.
- [ ] Completare Swagger/OpenAPI, JSDoc per JS/TS e documentazione Java pertinente.
- [ ] Includere documentazione Digital, materiali del progetto e fonti/licenze degli asset.
- [ ] Includere costi, conversione futura e proiezioni economiche a 12/24/48 mesi.
- [ ] Stimare gli obiettivi di 5.000 utenti a tre mesi e 50.000 al mese 24.
- [ ] Motivare monetizzazione dal mese 13 e pareggio al mese 48, secondo la traccia.
- [ ] Produrre screenshot completi desktop/mobile di tutte le pagine.
- [ ] Includere presentazione finale e documentazione completa del progetto.

## Git e archivio finale

- [ ] Configurare e verificare il vincolo di pull request su main e dev in GitHub.
- [ ] Concordare approvazioni, revisori e strategia di merge.
- [ ] Promuovere la versione approvata da dev a main tramite pull request.
- [ ] Creare `gruppo-##-nome-progetto-scelto.zip` con codice, export database, documentazione e presentazione.
- [ ] Controllare il contenuto dopo l'estrazione ed escludere segreti, cache, log e cartelle Obsidian.

[Indice](README.md) · [README](../README.md)
