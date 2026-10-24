# Modello di documentazione per funzionalità

Usare `docs/[area]/[nome-funzionalita].md`, adattando i collegamenti relativi.
Le pagine esistenti restano sintetiche. Quando arrivano nuove decisioni,
approfondire la pagina interessata con le sezioni pertinenti del modello completo.
Questo documento definisce il livello futuro atteso, non nuove regole di dominio.

## Compilazione e responsabilità

- Conservare gli ID Fxx/Dxx e distinguere perimetro, maturità della specifica,
  implementazione e verifica. Una specifica concordata non prova che il codice esista.
- Riportare solo decisioni disponibili con la relativa fonte. Usare **DA DEFINIRE**
  per informazioni mancanti e indicare quale scelta blocca lo sviluppo o la verifica.
- Marcare **NON APPLICABILE** con motivazione le sezioni non pertinenti. Nella
  scheda breve possono restare omesse; nell'approfondimento renderne chiaro il motivo.
- Il contratto tecnico condiviso risiede nella pagina BackEnd della funzionalità.
  FrontEnd collega tale riferimento e descrive il client; Digital conserva le
  proprie regole, contenuti, fonti e ipotesi. Evitare copie concorrenti dei contratti.
- Le attività di area hanno branch distinti e si collegano allo stesso ID
  funzionalità e contratto. Gli ID delle attività non rinumerano le funzionalità.
- Prosa e nomi documentali in italiano; codice, commenti e risorse tecniche in inglese.

## Modello breve attuale

Compilare questa scheda per nuovi argomenti ancora privi di dettagli. I metadati
temporali si aggiungono alle pagine esistenti al prossimo aggiornamento significativo,
senza inventare la data originaria o una verifica mai eseguita.

```markdown
# Nome della funzionalità

ID funzionalità: Fxx / Dxx.
Perimetro: confermato / da definire / non confermato.
Specifica: sintetica / in approfondimento / concordata / da aggiornare.
Implementazione: non iniziata / in corso / parziale / integrata.
Responsabile: DA DEFINIRE.
Fonte delle decisioni: riferimento disponibile / DA DEFINIRE.
Ultimo aggiornamento significativo: AAAA-MM-GG.
Ultima verifica: non eseguita / data e riferimento alle evidenze.

## Obiettivo e decisioni note

- Descrivere il risultato e il comportamento confermato.

## Da definire

- Elencare le decisioni mancanti, le dipendenze e l'impatto.

## Collegamenti

- Indice dell'area e pagine pertinenti delle altre aree.
- Contratto canonico, se disponibile; issue, PR e report realmente esistenti.
```

Esempio di perimetro: «F06 comprende scambi privati e pubblici di carte e crediti.
Entrambe le parti devono accettare. Durata e sequenza del consenso sono da definire».

## Modello di approfondimento progressivo

Mantenere i metadati della scheda breve e aggiungere le sezioni sotto quando le
decisioni pertinenti diventano disponibili. Prima di sviluppare un'integrazione,
concordare il contratto necessario tra le aree; eventuali altre lacune restano visibili.
Compilare in modo che un altro contributore possa implementare e verificare la
propria parte senza indovinare nomi dei campi, permessi o comportamento nei casi limite.

```markdown
## Obiettivo, perimetro e dipendenze

- Risultato atteso, casi inclusi ed esclusi e fonte delle decisioni.
- Dipendenze da altre funzionalità, regole Digital, servizi o configurazioni.
- Responsabile, ID attività, branch e PR per ciascuna area, quando disponibili.
- Stato delle parti implementate e delle integrazioni ancora mancanti.

## Attori, autorizzazioni e precondizioni

- Attori e matrice delle operazioni consentite per ruolo.
- Controlli di autenticazione e proprietà della risorsa; dati pubblici e riservati.
- Stato iniziale, risorse richieste e condizioni che impediscono l'operazione.

## Stati, transizioni e riferimenti temporali

- Stati ammessi; per ogni transizione: origine, comando/evento, attore,
  condizione, destinazione ed effetti osservabili.
- Stati terminali, annullamento, scadenza e recupero dopo interruzioni, se pertinenti.
- Autorità del tempo, fuso, formato delle date, limiti e confronto alle scadenze.

## Flusso principale, alternative ed errori

- Passaggi numerati da richiesta a risultato, distinguendo client e server.
- Flussi alternativi, lista vuota, dati invalidi, permessi mancanti e dipendenze indisponibili.
- Effetti di ogni errore su dati, interfaccia e possibilità di ripetere l'operazione.

## Contratto tecnico condiviso

- Nella pagina BackEnd: metodo, percorso o firma, accesso, input e output
  con nomi, tipi, obbligatorietà, limiti, valori null e regole di validazione.
- Esempi coerenti di richiesta, risposta ed errore; distinguere esempi illustrativi
  da valori approvati. Indicare il formato degli errori e i codici concordati.
- Filtri, paginazione, ordinamento e limiti delle liste, se presenti.
- Eventi e canali, se previsti: destinatari, payload, ordine, consegna e recupero.
- Collegamenti a OpenAPI e strumenti di prova solo quando disponibili.
- Nella pagina FrontEnd: link alla sezione canonica; descrivere qui soltanto
  l'adattamento necessario al client. Indicare le modifiche incompatibili e le PR dipendenti.

## Dati, vincoli e persistenza

- Entità o concetti, identificativi, relazioni, proprietà e invarianti del dominio.
- Campi coinvolti, precisione degli importi, vincoli di unicità e integrità.
- Creazione, modifica, eliminazione e conservazione dei dati secondo le decisioni disponibili.
- Schema e migrazioni, quando lo stack è confermato; link alla fonte eseguibile
  disponibile, evitando copie DDL non mantenute o istruzioni non verificate.

## Transazioni, concorrenza e richieste ripetute

- Confine atomico dell'operazione e dati che devono cambiare insieme.
- Rollback e comportamento in caso di errore parziale.
- Protezione dalle operazioni concorrenti, ordine dei lock o controllo delle versioni.
- Idempotenza: identificazione della richiesta, retry identico e conflitto con dati diversi.
- Pubblicazione degli eventi e notifiche rispetto al commit, retry e recupero
  da errori o interruzioni; distinguere elaborazione e consegna al destinatario.

## Comportamento dell'interfaccia

- Schermate/rotte, azioni disponibili e dipendenze dal contratto condiviso.
- Stati iniziale, caricamento, vuoto, successo, errore e accesso negato.
- Risultati autorevoli del server, aggiornamento o riallineamento dei dati,
  invii ripetuti, attese e riconnessione, se prevista.
- Responsive, tastiera, accessibilità e prestazioni con criteri misurabili concordati.

## Configurazione e operatività

- Prerequisiti, opzioni e variabili con significato, obbligatorietà e default concordati.
- Comandi reali di avvio e verifica dopo la configurazione degli strumenti.
- Osservabilità, limiti noti e recupero dai guasti pertinenti; esempi senza segreti.

## Criteri di accettazione

| ID criterio | Dato / quando | Risultato osservabile | Verifica prevista |
| --- | --- | --- | --- |
| DA DEFINIRE | Condizione iniziale e azione | Risultato misurabile concordato | Metodo o scenario |

- Coprire percorso positivo, validazione, autorizzazione/proprietà e casi limite pertinenti.
- Includere concorrenza, retry, scadenza e rollback quando previsti dal comportamento.

## Verifiche ed evidenze

- Separare controlli previsti da quelli eseguiti; collegare i criteri coperti.
- Per ogni esecuzione: data, commit/PR del codice provato, ambiente,
  comandi o procedura, risultato, controlli saltati e limiti.
- Distinguere prove isolate con server simulato da integrazione reale tra aree.
- Collegare eventuali report datati; qui mantenere l'esito corrente e i limiti.

## Decisioni aperte e collegamenti

- Scelte ancora mancanti e loro impatto; responsabile o riferimento solo se noto.
- Indice dell'area, pagine collegate, contratto canonico, issue/PR e report esistenti.
```

## Adattamento alle aree

| Area | Approfondimento richiesto quando disponibile |
| --- | --- |
| BackEnd | Contratti canonici, permessi, invarianti, persistenza, transazioni, errori e prove server |
| FrontEnd | Flussi e stati UI, integrazione con il contratto canonico, accessibilità e prove client/integrate |
| Digital | Regole e contenuti, fonti e licenze, ipotesi economiche, metriche e criteri verificabili; link alle conseguenze tecniche |

Per Digital distinguere ipotesi, obiettivi, decisioni approvate e risultati misurati.
Non compilare endpoint o transazioni quando non pertinenti. Il regolamento condiviso
deve precisare valori, casi limite e criteri verificabili quando saranno concordati.

## Stato corrente e report storici

L'[indice generale](README.md#stato-delle-funzionalità) riepiloga lo stato corrente;
la pagina di area descrive l'avanzamento della propria porzione. Aggiornare entrambi
quando cambiano specifica, implementazione o verifica.

Un report necessario nasce in `docs/report/AAAA-MM-GG-descrizione.md` solo dopo
un'attività reale e registra ID, data, commit/PR, ambiente, controlli, esito e limiti.
Collegarlo dalla pagina di funzionalità. Un report passato non certifica il codice
successivo; la data di modifica del testo non è una data di verifica. Non sono
richiesti report vuoti, cronologie duplicate o un registro separato delle decisioni.

Esempio di criterio, da approvare quando le regole saranno disponibili:
«Dato uno scambio già completato, quando viene ripetuta la stessa richiesta,
allora non si produce un secondo trasferimento».

[Indice](README.md) · [Linee guida](../Guide%20Lines%20and%20Rules.md)
