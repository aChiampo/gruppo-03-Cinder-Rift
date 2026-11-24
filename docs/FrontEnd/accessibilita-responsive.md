# Accessibilità e interfacce responsive

ID: F11 · Perimetro: confermato; specifica sintetica.
Implementazione: parziale; stili responsive e semantica accessibile presenti, conformità e copertura da verificare. Responsabile: DA DEFINIRE (Web Developer).
Fonte: traccia, risposte e chiarimenti del gruppo; le decisioni note sono riportate sotto.
Ultimo aggiornamento significativo: **2026-11-23**, allineamento ai sorgenti richiesto dall'utente.

## Obiettivo e decisioni note

- Applicare il comportamento responsive e l'accessibilità ai flussi del portale.
- Prevedere verifica desktop/mobile e uso delle interfacce con tastiera.

## Base presente e verifiche

La base React include media query in `responsive.css` e negli stili di pagina, etichette per i campi, stati di richiesta, pulsanti semantici e attributi ARIA per filtri, paginazione e replay. I test di rendering statico non attestano navigazione completa da tastiera, contrasto, lettori di schermo o layout su dispositivi reali. Nella [verifica del 2026-11-23](../report/2026-11-23-allineamento-documentazione.md) non è stato eseguito un audit browser.

## Da definire

- Criteri misurabili, dispositivi supportati, modalità di verifica e componenti condivisi.

## Collegamenti

- [SEO e prestazioni](seo-prestazioni.md)
- [Identità visiva](../Digital/lore-identita-carte.md)
- [Lista di consegna](../lista-verifica-consegna.md)
- [Indice FrontEnd](README.md)
