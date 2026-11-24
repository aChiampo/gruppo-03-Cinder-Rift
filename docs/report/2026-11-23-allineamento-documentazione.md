# Verifica di allineamento della documentazione

Data: **2026-11-23**. Richiesta: verificare la documentazione rispetto allo stato attuale dell'applicativo.
Riferimento: checkout locale `test-connessione`, commit base `936708e`; copia inizialmente senza modifiche Git. Revisione documentale senza modifiche al codice applicativo.
ID interessati: F01, F04, F05, F07, F08, F10, F11 e F12.

## Metodo e discrepanze corrette

Confrontati i documenti Markdown versionati con schermate, servizi, gestione della sessione e test React, controller e servizi Java, migrazioni Flyway V1–V10, POM, Dockerfile, proxy e Compose. La verifica dello stato riguarda i sorgenti locali; non certifica branch remoti, deploy o l'intero modello dati v1.1.

| Tema | Discrepanza | Allineamento |
| --- | --- | --- |
| F01 | Scheda FrontEnd e indici indicavano integrazione non iniziata o server simulato | Accesso, registrazione, sessione e aggiornamento profilo reali; recupero password UI, email e cancellazione account UI ancora mancanti |
| F04/F05/F10 | Istruzioni e indice backend fermi a F01/F05 e V1–V4/V6; backoffice descritto come assente | Catalogo/collezione F04, editor F05, scritture carte F10 e migrazioni V1–V10 presenti; interfaccia amministrativa e altri flussi ancora assenti |
| F07 | Guide e schede indicavano chiamata React da implementare e risultato solo pianificato | Selezione mazzo/stage, POST applicativo, esito autorevole e visualizzatore round/scambi/replay locale presenti; parser dipendente dal log testuale |
| F08 | Confine fra ultimo risultato e storico non esplicito | Ultimo report in memoria disponibile; nessuna API o persistenza dello storico |
| F11/F12 | Implementazione indicata come non iniziata | Base responsive, semantica, ARIA, titoli e caricamento differito presenti; audit, SEO completo e misure prestazionali mancanti |
| Collegamenti | Due link nel README frontend puntavano a file assenti nel repository | Sostituiti con la scheda F07 corrente e questo report |
| Evidenze | Alcuni controlli del 2026-11-13/18/20 erano ancora chiamati correnti accanto a prove successive | Evidenze storiche conservate con ambito e data; nessun risultato precedente attribuito a questa revisione |

## Verifiche eseguite

Ambiente: Windows/PowerShell, Node.js **24.14.0**, npm **11.9.0**, dipendenze frontend già presenti. Nessuna installazione o aggiornamento di dipendenze.

Dalla cartella `cinder-rift/`:

```text
npm.cmd test
npm.cmd run build -- --outDir node_modules/.cache/documentation-review-dist
```

- **54 test superati**, nessun fallimento o caso saltato: servizi API con HTTP simulato, rendering statico, regole del builder e parser/navigazione del report.
- **Build Vite 5.4.21 riuscita**, 1548 moduli trasformati. Output isolato in `node_modules/.cache/documentation-review-dist`, senza rigenerare la cartella `dist/` versionata.
- Il primo tentativo nel sandbox ha restituito `spawn EPERM` per i processi Node/esbuild; la ripetizione autorizzata fuori dal sandbox ha superato entrambi i comandi. Non si trattava di un fallimento delle asserzioni.
- Controllati **55 documenti Markdown**, con **781 collegamenti relativi**, incluse **273 ancore**: nessuna destinazione o ancora mancante. Rimosse le sezioni di codice prima di estrarre i link, per escludere gli esempi del modello.
- Le **24 rotte dei controller applicativi** corrispondono alle **24 righe implementate** nel riepilogo API: nessuna rotta mancante o aggiunta senza controller. I percorsi pianificati F06/F13 restano distinti.
- `docker compose --file compose.yaml config --quiet` superato; controllati indici, ID condivisi e `git diff --check` senza errori. Nessun container avviato o modificato.

## Limiti e punti ancora aperti

Nessuna nuova esecuzione JUnit, migrazione su PostgreSQL, avvio Compose, prova HTTP fra servizi o verifica browser. Le prove backend/motore citate nelle altre pagine mantengono le date originarie; non certificano una nuova esecuzione sul commit corrente.

Restano aperti recupero password completo, cleanup account F13/F06, acquisto/apertura pacchetti, scambi, amicizie, PvP, persistenza F08, backoffice completo, progressione e scoperta stage. Il modello SQL implementato resta distinto da DBML/specifica v1.1 completa; permane la differenza `ON DELETE CASCADE` già documentata per `decks_cards`.

Nessuna nuova regola di gioco o decisione di prodotto introdotta. Le caselle di consegna restano aperte e richiedono le rispettive evidenze integrate.

[Indice corrente](../README.md) · [FrontEnd](../FrontEnd/README.md) · [BackEnd](../BackEnd/README.md) · [Lista di consegna](../lista-verifica-consegna.md)
