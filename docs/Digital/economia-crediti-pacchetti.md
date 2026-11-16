# Economia delle risorse e dei pacchetti

ID: D03 · Perimetro: confermato; specifica sintetica.
Implementazione: non iniziata. Responsabile: DA DEFINIRE (Digital Strategist).
Fonte: traccia, risposte e chiarimenti del gruppo; le decisioni note sono riportate sotto.
Ultimo aggiornamento significativo: 2026-11-09, per la terminologia delle risorse e per la garanzia mercy sulle aperture confermata dall'utente. Verifiche applicative: non eseguite.

## Obiettivo e decisioni note

- Distinguere offerte composte da pacchetti e contenuto dei singoli pacchetti.
- La conversione monetaria futura va pianificata ai fini delle proiezioni economiche richieste dalla traccia.
- Definire prezzi, rarità, composizioni e proiezioni a 12/24/48 mesi.
- Gli esempi commerciali nel README non sono valori approvati.

## Risorse economiche

Terminologia confermata dall'utente il **2026-11-09**:

| Risorsa | Significato confermato |
| --- | --- |
| Scaglie | Risorsa guadagnata nel mondo di gioco |
| Nuclei Rift | Riserva premium acquistabile |
| Rotami | Residui recuperabili dai doppioni |

Le tre risorse sono concetti distinti: non vanno considerate sinonimi di un unico saldo. La definizione non stabilisce importi, tassi di conversione, risorse ammesse nei prezzi o trasferibilità fra utenti. Il recupero dai doppioni non definisce automaticamente il trattamento delle carte cancellate dal catalogo.

La [specifica dell'inventario](../BackEnd/Database/database-schema-v1.1.md#inventario-e-pacchetti) associa Scaglie a `cobalt_shards`, Nuclei Rift a `rift_cores` e Rotami a `scraps`: tre saldi interi obbligatori, inizializzati a zero. Canali di acquisto della riserva premium, regole di assegnazione delle Scaglie e condizioni/quantità di recupero dei Rotami restano **DA DEFINIRE**. Prezzi, importi generici degli scambi e quantità generiche dei premi richiedono ancora una corrispondenza esplicita con le risorse.

## Garanzia mercy all'apertura

Il sistema mercy segue le aperture riuscite del pacchetto corrispondente. Per ogni utente e pacchetto abilitato il contatore parte dalla soglia configurata e scende verso zero; l'apertura che raggiunge zero garantisce una carta della rarità minima richiesta o superiore. Ogni pesca qualificante, anche naturale prima dello zero, ripristina la soglia. I progressi di pacchetti diversi restano separati.

Con soglia dieci e rarità minima epica, la decima apertura senza una precedente epica o superiore è garantita; una pesca qualificante precedente fa ripartire il conto da dieci. È un esempio di configurazione, non una soglia o un ID di rarità fissati per tutti i pacchetti. Una soglia nulla disabilita il sistema. Il [contratto tecnico F03](../BackEnd/acquisto-pacchetti.md#mercy-sulle-aperture) descrive progressi e apertura atomica.

## Da definire

- Modalità di assegnazione delle Scaglie, acquisto dei Nuclei Rift e recupero dei Rotami dai doppioni.
- Risorse utilizzabili per ciascun acquisto, conversioni e trasferibilità; nessuna equivalenza fra le tre risorse è implicita.
- Offerte di pacchetti, prezzi, limiti, quantità per rarità e ipotesi economiche.
- Costi e sostenibilità coerenti con gli obiettivi della traccia.
- Soglia e rarità minima per ciascun pacchetto, distribuzione dello slot garantito e trattamento dei progressi dopo modifiche della configurazione.

## Collegamenti

- [Terminologia commerciale](../../README.md#terminologia-commerciale)
- [Offerte pacchetti](../BackEnd/acquisto-pacchetti.md)
- [Piano di lancio](comunicazione-lancio.md)
- [Indice Digital](README.md)
