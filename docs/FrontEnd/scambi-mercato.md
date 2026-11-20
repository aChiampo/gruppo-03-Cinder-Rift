# Scambi privati e marketplace

ID: F06 · Perimetro: confermato; specifica sintetica.
Implementazione: non iniziata. Responsabile: DA DEFINIRE (Web Developer).
Fonte: traccia, risposte e chiarimenti del gruppo; le decisioni note sono riportate sotto.
Ultimo aggiornamento significativo: **2026-11-18**, propagazione delle decisioni dell'utente su rimozione amicizia, cancellazione account, storico completato e ripubblicazione dell'annuncio pubblico della controparte. La durata di due giorni è confermata dal 2026-11-09. Verifiche applicative: non eseguite.

## Obiettivo e decisioni note

- Presentare proposte private oppure pubbliche nel marketplace.
- Mostrare carte e crediti offerti/richiesti, consenso delle parti e scadenza autorevole restituita dal server per proposte pubbliche e private, secondo il [contratto canonico F06](../BackEnd/scambi-mercato.md#flussi). La durata confermata è due giorni dalla creazione dell’annuncio; avvio, controproposte e ripubblicazione non rinnovano il termine.
- Lo scambio di carte e crediti è approvato dal committente.
- L'avvio di scambi privati e la prosecuzione di quelli esistenti dopo rimozione dell'amicizia seguono il [contratto F06](../BackEnd/scambi-mercato.md#amicizie-e-cancellazione-degli-account), collegato a [F13](amicizie.md). L'interfaccia deve distinguere l'impossibilità di avviare un nuovo privato dalla possibilità di proseguire una trattativa già esistente, senza rinnovarne la scadenza.
- Cancellazione account, conservazione dello storico completato e ripubblicazione dell'annuncio pubblico originale della controparte seguono la stessa sezione canonica F06. Dopo il riallineamento, la vista riflette stato, offerte e scadenza restituiti dal server; nessuna ripubblicazione visuale rinnova il TTL. I requisiti sono documentati ma non implementati.

## Da definire

- Schermate e presentazione del doppio consenso, degli stati e della scadenza secondo il contratto canonico F06.
- Modifica, annullamento, scadenza e gestione dei conflitti indicati dal server.
- Rappresentazione dello storico con un partecipante eliminato e aggiornamento delle viste dopo cleanup account o rimozione amicizia, senza duplicare il contratto server.

## Collegamenti

- [Servizio scambi](../BackEnd/scambi-mercato.md)
- [Amicizie F13](amicizie.md)
- [Accesso e cancellazione account F01](accesso-utenti.md)
- [Collezione](collezione-carte.md)
- [Indice FrontEnd](README.md)
