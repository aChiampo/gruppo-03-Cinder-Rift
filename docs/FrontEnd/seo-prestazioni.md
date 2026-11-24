# SEO e prestazioni

ID: F12 · Perimetro: confermato; specifica sintetica.
Implementazione: base tecnica parziale; attività SEO e misurazione delle prestazioni da completare. Responsabile: DA DEFINIRE (Web Developer).
Fonte: traccia, risposte e chiarimenti del gruppo; le decisioni note sono riportate sotto.
Ultimo aggiornamento significativo: **2026-11-23**, allineamento ai sorgenti richiesto dall'utente.

## Obiettivo e decisioni note

- Collaborazione con Digital per contenuti e SEO.
- La traccia richiede SEO on-page e tecnica, ottimizzazione del caricamento e verifiche Lighthouse/PageSpeed.
- Pianificare metadati, immagini, indicizzazione, robots.txt e sitemap.xml in base alle pagine effettive.

## Base presente e verifiche

`index.html` dichiara lingua italiana, viewport, favicon e titolo; `App.jsx` aggiorna il titolo per la pagina hash corrente. Le immagini delle carte usano caricamento differito e il build Vite produce gli asset di distribuzione. Non sono presenti `robots.txt`, `sitemap.xml` o metadati SEO completi; mancano misure Lighthouse/PageSpeed e criteri approvati. La [build verificata il 2026-11-23](../report/2026-11-23-allineamento-documentazione.md) non misura le prestazioni nel browser.

## Da definire

- Pagine pubbliche indicizzabili, obiettivi misurabili, strumenti e risultati da produrre.

## Collegamenti

- [Comunicazione](../Digital/comunicazione-lancio.md)
- [Pagine informative](pagine-informative.md)
- [Responsive](accessibilita-responsive.md)
- [Indice FrontEnd](README.md)
