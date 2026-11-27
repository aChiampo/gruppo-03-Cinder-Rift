# Consultazione dello storico

ID: F08 · Perimetro: confermato; endpoint backend PvP implementato, interfaccia frontend non integrata.
Implementazione: il backend persiste e restituisce lo storico Ranked/Friendly; non esiste ancora la chiamata React o la schermata di consultazione. Responsabile: DA DEFINIRE (Web Developer).
Ultimo aggiornamento significativo: **2026-11-27**, allineamento al contratto F08 disponibile.

## Comportamento frontend da realizzare

La UI dovrà chiamare `GET /api/battles/history` con il Bearer JWT della sessione F01. Il backend restituisce l'array delle partite PvP dell'utente; il parametro facoltativo `mode` filtra per modalità. L'API non offre paginazione. Campi, ordinamento, autorizzazioni e semantica dei dati sono definiti nel [contratto canonico BackEnd F08](../BackEnd/storico-partite.md), senza duplicarli qui.

La schermata dovrà prevedere stati di caricamento, lista vuota ed errore, e presentare i dati autorevoli del report. La specifica non sceglie layout, accessibilità visiva o controlli di filtro, che restano da definire nell'implementazione UI.

Lo storico copre le partite Ranked e Friendly PvP, non le simulazioni PvE: il report PvE resta disponibile solo in memoria nella sessione F07. Il risultato PvP restituisce `rankingDelta = 0`; non mostrare Elo, classifica o ricompense.

## Collegamenti

- [Servizio storico F08](../BackEnd/storico-partite.md)
- [Sfide F07](sfide-simulazione.md)
- [Indice FrontEnd](README.md)
