# Area FrontEnd

Documentazione dei Web Developer: interfacce e flussi del portale. Il frontend React 18/Vite 5 è in `cinder-rift/` e dispone di [build Docker e istruzioni](../../cinder-rift/README.md#avvio-con-docker-compose). Allineamento ai sorgenti: **2026-11-27**. Accesso, registrazione, sessione e profilo usano [F01 reale](accesso-utenti.md); catalogo/collezione, editor mazzi e campagna PvE usano rispettivamente F04, F05 e F07. Il visualizzatore PvE resta in memoria. Il backend dispone ora di API PvP e storico, ma client e schermate React non sono integrati; negozio e F06 restano indisponibili. Non esiste una modalità mock.

Quando saranno disponibili le decisioni, approfondire flussi e stati UI, errori,
accessibilità e verifiche secondo il [modello](../modello-funzionalita.md).
Per API e dati collegare il contratto canonico nella pagina BackEnd pertinente.
Le attività FrontEnd hanno branch distinti da quelle BackEnd, collegati allo stesso
ID funzionalità e contratto. Stato corrente nell'[indice generale](../README.md#stato-delle-funzionalità).

## Funzionalità

- [F01 — Registrazione, accesso e recupero password](accesso-utenti.md) — accesso, registrazione, sessione e profilo integrati; cancellazione account e recupero password UI da realizzare, invio email rinviato.
- [F03 — Negozio delle offerte di pacchetti](acquisto-pacchetti.md)
- [F04 — Consultazione della collezione](collezione-carte.md)
- [F05 — Composizione dei mazzi](gestione-mazzi.md)
- [F06 — Scambi privati e marketplace](scambi-mercato.md)
- [F07 — Sfide tra utenti](sfide-simulazione.md) — PvE integrato; API Ranked e Amichevoli backend senza client React.
- [F07 — Endpoint e payload PvE](../BackEnd/app-battaglie-api.md) — endpoint applicativo e chiamata React implementati; risultato con round, scambi e replay locale. Comportamento server descritto esclusivamente nell'area BackEnd.
- [F08 — Consultazione dello storico](storico-partite.md) — endpoint backend per partite PvP; chiamata e interfaccia React da realizzare.
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
