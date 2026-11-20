# Registrazione e accesso

ID: F01 · Perimetro: confermato; specifica sintetica.
Implementazione: non iniziata. Responsabile: DA DEFINIRE (Web Developer).
Fonte: traccia, risposte e chiarimenti del gruppo; le decisioni note sono riportate sotto.
Ultimo aggiornamento significativo il **2026-11-18**, su richiesta dell'utente, con la propagazione dei requisiti F13/F06 alla cancellazione account. L'allineamento alla rinomina degli endpoint F01 risale al 2026-11-13. L'integrazione del cleanup F13/F06 e le relative verifiche browser sono da eseguire.

## Obiettivo e decisioni note

- Interfacce di registrazione e accesso con email e password.
- Integrazione con il servizio JWT nell'app Spring Boot e con i ruoli `ADMIN` e `USER`.
- Integrare la cancellazione account secondo il [contratto F01](../BackEnd/accesso-utenti.md#cancellazione-account), con i permessi di `USER` e `ADMIN` definiti dal server. Al completamento della cancellazione del proprio account, scartare il JWT e tornare alla schermata di accesso. Interfaccia di conferma e messaggi sono da definire.
- La futura pulizia di richieste, amicizie e scambi aperti, con conservazione degli scambi completati, segue l'[integrazione prevista F01/F13/F06](../BackEnd/accesso-utenti.md#integrazione-prevista-con-f13-e-f06). Non è ancora compresa nel backend F01 corrente: il client non deve presentare la risposta attuale come prova di questa integrazione. Testi di conferma e rappresentazione dello storico con account eliminato restano da concordare con F06.
- L’interfaccia dovrà integrare il flusso CSRF, il rinnovo e l’uscita secondo il [contratto F01](../BackEnd/accesso-utenti.md#contratto-tecnico-condiviso) e il [comportamento del client](../BackEnd/accesso-utenti.md#comportamento-del-client). Le chiamate F01 usano il prefisso `/api/auth` introdotto il 2026-11-12; registrazione, accesso, profilo e uscita devono usare i percorsi rinominati nel contratto il 2026-11-13.
- Il client conserva il JWT di accesso in memoria e include i cookie nelle richieste di autenticazione; il segreto di rinnovo e il cookie CSRF sono HttpOnly. Il token CSRF leggibile si ottiene dal bootstrap previsto nel contratto. Dopo un rinnovo riuscito sostituisce il JWT in memoria; serializza rinnovo e uscita e scarta il JWT all’uscita o dopo un rinnovo rifiutato con `401`.
- Usare l’origine frontend configurata nel backend, lo stesso nome host in sviluppo e HTTPS nello stesso sito in distribuzione, secondo i requisiti CORS e cookie del contratto. Non è previsto un accesso tramite provider OAuth2 esterno.
- Il backend dispone della [base del recupero password](../BackEnd/accesso-utenti.md#recupero-password-base-backend). L’interfaccia di recupero è esclusa dall’attività backend corrente; l’[invio email con Thymeleaf](../BackEnd/invio-mail.md) è rinviato, quindi il flusso utente completo non è disponibile. I client futuri dovranno seguire il contratto canonico anche per CSRF e fine della sessione, senza dedurre la consegna email dal solo esito della richiesta.

## Da definire

- Schermate, stati, messaggi di errore e comportamento per ruolo.
- Implementazione e verifica della gestione della sessione nell’app React.
- Schermate e integrazione del recupero password in un’attività frontend separata.

## Collegamenti

- [Servizio di accesso](../BackEnd/accesso-utenti.md)
- [Amicizie F13](amicizie.md)
- [Scambi F06](scambi-mercato.md)
- [Accessibilità](accessibilita-responsive.md)
- [Indice FrontEnd](README.md)
