# Accesso utenti

ID: F01 · Perimetro: confermato; specifica sintetica.
Implementazione: non iniziata. Responsabile: DA DEFINIRE (Software Developer).
Fonte: traccia, risposte e chiarimenti del gruppo; le decisioni note sono riportate sotto.

## Obiettivo e decisioni note

- Registrazione e accesso con email e password.
- Un servizio nell'app Spring Boot gestisce l'autenticazione JWT.
- Ruoli confermati: `ADMIN`, `MANAGER`, `USER`.

## Da definire

- Permessi di ciascun ruolo, durata/rinnovo/revoca dei token e gestione degli errori.
- Dettagli di registrazione e recupero accesso; nessuno strumento aggiuntivo è già scelto.

## Collegamenti

- [Interfaccia di accesso](../FrontEnd/accesso-utenti.md)
- [Backoffice](gestione-amministrativa.md)
- [Indice BackEnd](README.md)
