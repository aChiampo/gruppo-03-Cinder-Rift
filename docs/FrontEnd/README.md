# Area FrontEnd

Documentazione dei Web Developer: interfacce e flussi del portale. Il frontend React è previsto in `react_app/`; implementazione e integrazione F01 sono ancora da avviare. Il backend di autenticazione è già disponibile: il client segue il [contratto F01](../BackEnd/accesso-utenti.md) per JWT in memoria, cookie, CSRF e percorsi `/api/auth`. Allineamento documentale: **2026-11-12**, su richiesta dell’utente; versioni e strumenti frontend restano da completare.

Quando saranno disponibili le decisioni, approfondire flussi e stati UI, errori,
accessibilità e verifiche secondo il [modello](../modello-funzionalita.md).
Per API e dati collegare il contratto canonico nella pagina BackEnd pertinente.
Le attività FrontEnd hanno branch distinti da quelle BackEnd, collegati allo stesso
ID funzionalità e contratto. Stato corrente nell'[indice generale](../README.md#stato-delle-funzionalità).

## Funzionalità

- [F01 — Registrazione, accesso e recupero password](accesso-utenti.md) — interfaccia da avviare; invio email rinviato.
- [F03 — Negozio delle offerte di pacchetti](acquisto-pacchetti.md)
- [F04 — Consultazione della collezione](collezione-carte.md)
- [F05 — Composizione dei mazzi](gestione-mazzi.md)
- [F06 — Scambi privati e marketplace](scambi-mercato.md)
- [F07 — Sfide tra utenti](sfide-simulazione.md)
- [F08 — Consultazione dello storico](storico-partite.md)
- [F09 — Pagine informative](pagine-informative.md) — DA DEFINIRE
- [F10 — Interfaccia di backoffice](gestione-amministrativa.md)
- [F11 — Accessibilità e interfacce responsive](accessibilita-responsive.md)
- [F12 — SEO e prestazioni](seo-prestazioni.md)

## Collegamenti

- [Indice generale e stato delle funzionalità](../README.md)
- [Area Digital](../Digital/README.md)
- [Area BackEnd](../BackEnd/README.md)
- [Modello di documentazione](../modello-funzionalita.md)
- [Linee guida](../../Linee-Guida-e-Regole.md)
- [README del progetto](../../README.md)
