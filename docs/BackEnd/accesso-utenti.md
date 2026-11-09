# Accesso utenti

ID: F01 · Perimetro: confermato; specifica in approfondimento.
Implementazione: non iniziata. Responsabile: DA DEFINIRE (Software Developer).
Fonte: traccia, risposte e chiarimenti del gruppo; le decisioni note sono riportate sotto.
Ultimo aggiornamento significativo: 2026-11-05, allineamento alla base di dati v1.0 su richiesta dell'utente. Verifiche applicative: non eseguite.
Fonte dati: [specifica canonica v1.0](Database/database-schema-v1.0.md#sistema-di-autenticazione-e-autorizzazione).

## Obiettivo e decisioni note

- Registrazione e accesso con email e password.
- Un servizio nell'app Spring Boot gestisce l'autenticazione JWT.
- Ruoli confermati: `ADMIN`, `MANAGER`, `USER`.

## Dati, vincoli e registrazione

- `USERS.id` e `ROLES.id` sono `int` auto-incrementali; `USERS.id_role` è una FK intera obbligatoria. L'identificativo dell'utente è lo stesso nei riferimenti pubblici e nelle altre funzionalità. La conoscenza dell'ID non autorizza l'accesso alla risorsa.
- Nickname ed email sono obbligatori e univoci; `hash_password` è obbligatorio. Il backend normalizza l'email prima di scrittura e ricerca. Nome, cognome e icona sono facoltativi. `ranking_points` parte da `1000`, `is_deleted` da `false`.
- La registrazione crea utente e `INVENTORY` nello stesso commit. `INVENTORY.id_user` è obbligatorio e univoco, `token_balance` ha default `0`; la FK da sola non crea l'inventario.
- `PASSWORD_RESET_TOKENS` e `REFRESH_TOKENS` hanno PK intere auto-incrementali, FK intera `id_user`, `token_hash` obbligatorio univoco, scadenza e creazione obbligatorie. `used_at` e `revoked_at` sono nullable nelle rispettive tabelle. Il materiale segreto del token è distinto dall'ID numerico del record.
- Scadenza, uso singolo, revoca e concorrenza richiedono controlli backend. Non sono modellate catene di rotazione, sessioni o una gerarchia di ruoli. La cancellazione logica non attiva le cascate delle FK.

## Da definire

- Permessi di ciascun ruolo, durata/rinnovo/revoca dei token e gestione degli errori.
- API di registrazione e recupero accesso, algoritmi di hash, conservazione dei token e revoca dopo cancellazione logica; nessuno strumento aggiuntivo è già scelto.

## Collegamenti

- [Interfaccia di accesso](../FrontEnd/accesso-utenti.md)
- [Backoffice](gestione-amministrativa.md)
- [Indice BackEnd](README.md)
