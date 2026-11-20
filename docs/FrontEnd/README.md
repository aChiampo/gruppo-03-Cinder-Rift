# Area FrontEnd

Documentazione dei Web Developer: interfacce e flussi del portale. Il prototipo React 18/Vite 5 è in `cinder-rift/` e dispone di [build Docker e istruzioni](../../cinder-rift/README.md#avvio-con-docker-compose). Allineamento documentale: **2026-11-16**, completamento Compose richiesto dall’utente. Il login usa ancora il contratto del server simulato; l’integrazione con Spring richiede l’adeguamento al [contratto F01](../BackEnd/accesso-utenti.md) per payload, JWT in memoria, cookie, CSRF e percorsi `/api/auth`. Collezione, home e deck builder usano dati dimostrativi; container sani non attestano funzionalità integrate.

Quando saranno disponibili le decisioni, approfondire flussi e stati UI, errori,
accessibilità e verifiche secondo il [modello](../modello-funzionalita.md).
Per API e dati collegare il contratto canonico nella pagina BackEnd pertinente.
Le attività FrontEnd hanno branch distinti da quelle BackEnd, collegati allo stesso
ID funzionalità e contratto. Stato corrente nell'[indice generale](../README.md#stato-delle-funzionalità).

## Funzionalità

- [F01 — Registrazione, accesso e recupero password](accesso-utenti.md) — login prototipale disponibile; integrazione Spring e altre schermate F01 da completare, invio email rinviato.
- [F03 — Negozio delle offerte di pacchetti](acquisto-pacchetti.md)
- [F04 — Consultazione della collezione](collezione-carte.md)
- [F05 — Composizione dei mazzi](gestione-mazzi.md)
- [F06 — Scambi privati e marketplace](scambi-mercato.md)
- [F07 — Sfide tra utenti](sfide-simulazione.md)
- [F07 — Endpoint e payload PvE](../BackEnd/app-battaglie-api.md) — contratto pubblico documentato per l'implementazione FrontEnd; endpoint applicativo non ancora implementato. Comportamento server descritto esclusivamente nell'area BackEnd.
- [F08 — Consultazione dello storico](storico-partite.md)
- [F09 — Pagine informative](pagine-informative.md) — DA DEFINIRE
- [F10 — Interfaccia di backoffice](gestione-amministrativa.md)
- [F11 — Accessibilità e interfacce responsive](accessibilita-responsive.md)
- [F12 — SEO e prestazioni](seo-prestazioni.md)
- [F13 — Richieste e amicizie reciproche](amicizie.md) — contratto API definito il **2026-11-20**: azioni personali per entrambi i ruoli, Bearer JWT ed elenchi completi dal più recente con nickname, icona e timestamp. UI in approfondimento; implementazione e verifiche non iniziate.

## Collegamenti

- [Indice generale e stato delle funzionalità](../README.md)
- [Area Digital](../Digital/README.md)
- [Area BackEnd](../BackEnd/README.md)
- [Modello di documentazione](../modello-funzionalita.md)
- [Linee guida](../../Linee-Guida-e-Regole.md)
- [README del progetto](../../README.md)
