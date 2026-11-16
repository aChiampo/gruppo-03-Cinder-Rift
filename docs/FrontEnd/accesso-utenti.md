# Registrazione e accesso

ID: F01 · Perimetro: confermato; specifica sintetica.
Implementazione: non iniziata. Responsabile: DA DEFINIRE (Web Developer).
Fonte: traccia, risposte e chiarimenti del gruppo; le decisioni note sono riportate sotto.
Ultimo aggiornamento significativo: **2026-11-12**, allineamento al contratto della base backend di recupero password su richiesta dell’utente; interfaccia, integrazione e verifiche browser non eseguite.

## Obiettivo e decisioni note

- Interfacce di registrazione e accesso con email e password.
- Integrazione con il servizio JWT nell'app Spring Boot e con i ruoli `ADMIN` e `USER`.
- L’interfaccia dovrà integrare il flusso CSRF, il rinnovo e l’uscita secondo il [contratto F01](../BackEnd/accesso-utenti.md#contratto-tecnico-condiviso) e il [comportamento del client](../BackEnd/accesso-utenti.md#comportamento-del-client). Dal 2026-11-12, su richiesta dell’utente, le chiamate F01 usano il prefisso `/api/auth` del contratto; i precedenti percorsi non hanno alias di compatibilità.
- Il client conserva il JWT di accesso in memoria e include i cookie nelle richieste di autenticazione; il segreto di rinnovo e il cookie CSRF sono HttpOnly. Il token CSRF leggibile si ottiene dal bootstrap previsto nel contratto. Dopo un rinnovo riuscito sostituisce il JWT in memoria; serializza rinnovo e uscita e scarta il JWT all’uscita o dopo un rinnovo rifiutato con `401`.
- Usare l’origine frontend configurata nel backend, lo stesso nome host in sviluppo e HTTPS nello stesso sito in distribuzione, secondo i requisiti CORS e cookie del contratto. Non è previsto un accesso tramite provider OAuth2 esterno.
- Il backend dispone della [base del recupero password](../BackEnd/accesso-utenti.md#recupero-password-base-backend). L’interfaccia di recupero è esclusa dall’attività backend corrente; l’[invio email con Thymeleaf](../BackEnd/invio-mail.md) è rinviato, quindi il flusso utente completo non è disponibile. I client futuri dovranno seguire il contratto canonico anche per CSRF e fine della sessione, senza dedurre la consegna email dal solo esito della richiesta.

## Da definire

- Schermate, stati, messaggi di errore e comportamento per ruolo.
- Implementazione e verifica della gestione della sessione nell’app React.
- Schermate e integrazione del recupero password in un’attività frontend separata.

## Collegamenti

- [Servizio di accesso](../BackEnd/accesso-utenti.md)
- [Accessibilità](accessibilita-responsive.md)
- [Indice FrontEnd](README.md)
