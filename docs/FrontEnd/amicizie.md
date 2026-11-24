# Amicizie

ID funzionalità: **F13**; dipendenze: **F01** e **F06**.
Perimetro: richieste e amicizie reciproche; **contratto API definito**, specifica dell'interfaccia in approfondimento.
Implementazione: **non iniziata**. Responsabile: **DA DEFINIRE (Web Developer)**.
Fonte: decisioni dell'utente del **2026-11-18** e **2026-11-20**, con delega delle restanti scelte API semplici.
Ultimo aggiornamento significativo: **2026-11-24**. Verifiche applicative frontend: **non eseguite**.

## Obiettivo e decisioni note

- Consentire la consultazione delle proprie amicizie e delle richieste ricevute o inviate, secondo il [contratto canonico F13](../BackEnd/amicizie.md). API, payload, permessi, errori e regole server sono implementati nel backend; l'integrazione frontend resta da realizzare.
- L'interfaccia è personale per entrambi i ruoli; `ADMIN` usa le stesse azioni di `USER` e non dispone di una funzione per eliminare relazioni altrui. Disponibilità di accettazione, rifiuto, annullamento e rimozione deriva dai [vincoli di partecipazione](../BackEnd/amicizie.md#attori-autorizzazioni-e-precondizioni), verificati dal server.
- Gli elenchi F13 sono completi e senza paginazione. L'interfaccia non richiede controlli per caricare pagine successive; il filtro delle richieste ricevute/inviate segue il contratto. Il client mantiene l'ordine server: richieste dalla creazione più recente e amici dall'accettazione più recente. Ogni voce comprende nickname, icona e timestamp; un'icona assente è rappresentata da `null` secondo il contratto, con presentazione grafica da definire.
- Tutte le chiamate F13 usano il JWT Bearer F01. Il client mostra i messaggi di errore e segue le regole di autenticazione e autorizzazione del contratto, senza affidarsi alla sola visibilità dei comandi nell'interfaccia.
- Per errori di validazione, invii duplicati e azioni ripetute, il client segue gli esiti del [contratto F13](../BackEnd/amicizie.md#contratto-tecnico-condiviso), mostra il messaggio applicativo disponibile e riallinea la vista con lo stato autorevole. Il ruolo amministrativo non modifica questi esiti.
- Gli effetti di rimozione amicizia e cancellazione account sugli scambi sono nel [contratto F06](../BackEnd/scambi-mercato.md#amicizie-e-cancellazione-degli-account). La presentazione del client deve riflettere la scadenza autorevole senza suggerire il rinnovo del TTL o l'eliminazione dello storico completato.

## Da definire

- Schermate, accesso al profilo dell'altro utente, ricerca o selezione del destinatario e integrazione con F01.
- Presentazione degli stati iniziale, vuoto, caricamento, successo, errore e accesso negato; aggiornamenti concorrenti e riconciliazione dopo errori.
- Layout delle voci, presentazione dei timestamp, conferme delle azioni, accessibilità e comportamento responsive. La prima versione non prevede notifiche dedicate.
- Implementazione delle chiamate e verifiche browser/integrate sul contratto API definito; nessuna chiamata React è introdotta da questa pagina.

## Collegamenti

- [Contratto amicizie F13](../BackEnd/amicizie.md)
- [Accesso e cancellazione account F01](accesso-utenti.md)
- [Interfaccia scambi F06](scambi-mercato.md)
- [Accessibilità](accessibilita-responsive.md)
- [Indice FrontEnd](README.md)
- [Indice e stato delle funzionalità](../README.md)
