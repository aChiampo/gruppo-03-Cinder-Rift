# Registrazione e accesso

ID: F01 · Perimetro: confermato; specifica in approfondimento.
Implementazione: accesso, registrazione, rinnovo, uscita e aggiornamento profilo integrati con Spring; cancellazione account e recupero password UI da realizzare. Responsabile: DA DEFINIRE (Web Developer).
Fonte: traccia, risposte e chiarimenti del gruppo; le decisioni note sono riportate sotto.
Ultimo aggiornamento significativo: **2026-11-23**, verifica richiesta dall'utente e allineamento ai sorgenti React. I requisiti F13/F06 alla cancellazione account risalgono al 2026-11-18; cleanup e relative verifiche restano da eseguire.

## Obiettivo e decisioni note

- Interfacce di registrazione e accesso con email e password.
- Integrazione con il servizio JWT nell'app Spring Boot e con i ruoli `ADMIN` e `USER`.
- Integrare la cancellazione account secondo il [contratto F01](../BackEnd/accesso-utenti.md#cancellazione-account), con i permessi di `USER` e `ADMIN` definiti dal server. Al completamento della cancellazione del proprio account, scartare il JWT e tornare alla schermata di accesso. Interfaccia di conferma e messaggi sono da definire.
- La futura pulizia di richieste, amicizie e scambi aperti, con conservazione degli scambi completati, segue l'[integrazione prevista F01/F13/F06](../BackEnd/accesso-utenti.md#integrazione-prevista-con-f13-e-f06). Non è ancora compresa nel backend F01 corrente: il client non deve presentare la risposta attuale come prova di questa integrazione. Testi di conferma e rappresentazione dello storico con account eliminato restano da concordare con F06.
- L’interfaccia integra CSRF, rinnovo e uscita secondo il [contratto F01](../BackEnd/accesso-utenti.md#contratto-tecnico-condiviso) e il [comportamento del client](../BackEnd/accesso-utenti.md#comportamento-del-client). Le chiamate usano i percorsi correnti `/api/auth`; il profilo è aggiornato tramite F01.
- Il client conserva il JWT di accesso in memoria e include i cookie nelle richieste di autenticazione; il segreto di rinnovo e il cookie CSRF sono HttpOnly. Il token CSRF leggibile si ottiene dal bootstrap previsto nel contratto. Dopo un rinnovo riuscito sostituisce il JWT in memoria; serializza rinnovo e uscita e scarta il JWT all’uscita o dopo un rinnovo rifiutato con `401`.
- Usare l’origine frontend configurata nel backend, lo stesso nome host in sviluppo e HTTPS nello stesso sito in distribuzione, secondo i requisiti CORS e cookie del contratto. Non è previsto un accesso tramite provider OAuth2 esterno.
- Il backend dispone della [base del recupero password](../BackEnd/accesso-utenti.md#recupero-password-base-backend). L’interfaccia di recupero è esclusa dall’attività backend corrente; l’[invio email con Thymeleaf](../BackEnd/invio-mail.md) è rinviato, quindi il flusso utente completo non è disponibile. I client futuri dovranno seguire il contratto canonico anche per CSRF e fine della sessione, senza dedurre la consegna email dal solo esito della richiesta.

## Comportamento implementato e verifiche

`LoginScreen` presenta accesso e registrazione con conferma password; `ProfileScreen` mostra i dati dell'utente e salva nickname, email e icona, con stati di attesa, errore e conferma. La gestione della sessione in `AuthContext` ripristina tramite cookie dopo il ricaricamento e rinnova prima della scadenza; un Bearer rifiutato viene ritentato una volta dopo il rinnovo. Un errore di trasporto sulle scritture non provoca retry automatico. Il fallimento dell'uscita server viene segnalato dopo la chiusura locale della sessione.

Verifica del **2026-11-23**: 54 test frontend e build superati, inclusi servizi F01 e rendering statico. Gestione interattiva della sessione in browser e integrazione runtime Spring/PostgreSQL non verificate in questa revisione. [Ambiente e limiti](../report/2026-11-23-allineamento-documentazione.md).

## Da definire

- Interfaccia di cancellazione account e comportamento amministrativo oltre il profilo personale.
- Verifica browser della gestione della sessione, di CSRF e dei cookie.
- Schermate e integrazione del recupero password in un’attività frontend separata.

## Collegamenti

- [Servizio di accesso](../BackEnd/accesso-utenti.md)
- [Amicizie F13](amicizie.md)
- [Scambi F06](scambi-mercato.md)
- [Accessibilità](accessibilita-responsive.md)
- [Indice FrontEnd](README.md)
