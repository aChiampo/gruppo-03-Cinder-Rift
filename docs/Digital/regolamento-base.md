# Regolamento e svolgimento del gioco

Versione 2.0 — Proposta per il prototipo e le prove di bilanciamento

## Scheda documentale

- **ID funzionalità:** F05 / F07.
- **Funzionalità collegate:** F08 — storico delle partite; D01 — lore, identità e carte; F10 — gestione amministrativa.
- **Perimetro:** supporto alle funzionalità confermate di gestione dei mazzi e simulazione delle partite; regolamento finale da confermare.
- **Specifica:** in approfondimento; proposta per il prototipo e le prove di bilanciamento.
- **Implementazione:** non iniziata.
- **Responsabile:** DA DEFINIRE (Digital Strategist).
- **Fonte:** documento «Regolamento e svolgimento del gioco», Versione 2.0, fornito per l'integrazione nella documentazione.
- **Data della bozza di integrazione:** 2026-10-27.
- **Ultimo aggiornamento significativo nel repository:** 2026-10-27.
- **Ultima verifica del regolamento:** non eseguita; approvazione, prove di bilanciamento e verifiche applicative non documentate.


La Versione 2.0 costituisce una proposta: la sua presenza nella documentazione non equivale all'approvazione definitiva delle regole o alla loro implementazione. Le decisioni ancora necessarie sono indicate nel testo, in particolare nelle sezioni 11 e 15.

## 1. Descrizione del gioco

Il gioco è un TCG esclusivamente digitale, con partite automatiche tra due giocatori.

La strategia consiste nella costruzione del mazzo e nella distribuzione delle creature nei cinque round. Durante la partita non vengono prese decisioni manuali: il sistema gestisce mescolamento, ingresso delle creature, equipaggiamenti, combattimenti e risultati.

Ogni partita comprende al massimo cinque round. Vince immediatamente chi conquista tre round. Se nessuno raggiunge tre vittorie entro il quinto round, vince chi ha conquistato più round. In caso di parità, la partita termina in pareggio.

## 2. Struttura del mazzo

Ogni giocatore prepara un mazzo di 30 carte, composto da:

- 25 creature, suddivise prima della partita in cinque mazzetti da cinque carte.
- 5 equipaggiamenti, raccolti in un mazzetto separato.

Ogni mazzetto di creature è assegnato a un round preciso. La sua composizione non può essere modificata durante la partita.

| Round | Creature disponibili | Soglia di energia proposta |
| --- | --- | --- |
| 1 | 5 | 1 |
| 2 | 5 | 2 |
| 3 | 5 | 3 |
| 4 | 5 | 4 |
| 5 | 5 | 5 |

Le creature possono essere utilizzate soltanto nel round al quale sono state assegnate. Le creature non utilizzate e quelle sopravvissute non passano al round successivo.

## 3. Energia e punti potenza

Energia e punti potenza hanno due funzioni differenti.

### Energia

L’energia determina quali creature possono essere inserite nel mazzetto di un determinato round.

Per il prototipo si propone una soglia crescente da 1 a 5: ogni mazzetto può contenere creature con costo di energia uguale o inferiore alla soglia del round.

L’energia non viene consumata quando una creatura entra in campo. È un requisito di accesso al round, non una risorsa da spendere.

### Punti potenza

I punti potenza limitano la forza delle carte inserite nel mazzo. Non vengono utilizzati durante il combattimento.

Ogni carta ha un costo fisso, stabilito dal team entro l’intervallo previsto dalla sua rarità:

| Rarità | Punti potenza per carta |
| --- | --- |
| Comune | 1–2 |
| Rara | 2–3 |
| Epica | 3–4 |
| Leggendaria | 5 |

Rarità ed energia non coincidono: una creatura di rarità elevata può appartenere anche a una fascia di energia bassa.

Il valore in punti potenza delle creature viene valutato rispetto alle altre carte utilizzabili nella stessa fascia di energia. Quello degli equipaggiamenti deve considerarne l’effetto per l’intero round.

## 4. Budget e vincoli di composizione

Il budget massimo del mazzo è 75 punti potenza, diviso in due categorie indipendenti.

| Categoria | Budget massimo |
| --- | --- |
| 25 creature | 65 punti |
| 5 equipaggiamenti | 10 punti |

I punti non utilizzati in una categoria non possono essere trasferiti all’altra. Non è obbligatorio utilizzare tutto il budget.

Ogni mazzetto di cinque creature deve inoltre rispettare un limite di 14 punti potenza, fermo restando il massimo complessivo di 65.

### Vincoli delle creature

| Vincolo | Limite |
| --- | --- |
| Numero complessivo | Esattamente 25 |
| Creature per mazzetto | Esattamente 5 |
| Comuni | Almeno 13 |
| Rare | Massimo 8 |
| Epiche | Massimo 3 |
| Leggendarie | Massimo 1 |
| Copie della stessa creatura | Massimo 2 nell’insieme dei cinque mazzetti |
| Epiche e leggendarie sommate per mazzetto | Massimo 1 |

Le due copie della stessa creatura possono essere collocate nello stesso mazzetto oppure in mazzetti diversi, purché siano rispettati tutti gli altri vincoli.

### Vincoli degli equipaggiamenti

| Vincolo | Limite |
| --- | --- |
| Numero complessivo | Esattamente 5 |
| Comuni | Almeno 3 |
| Rare | Massimo 2 |
| Epiche | Massimo 1 |
| Leggendarie | Non previste nel primo set |
| Copie dello stesso equipaggiamento | Massimo 1 |

Tutti i conteggi si riferiscono alle copie effettivamente presenti. Il giocatore deve possedere tutte le carte inserite.

La composizione delle fazioni è libera.

## 5. Caratteristiche delle creature

Ogni creatura possiede:

- Nome e identificativo.
- Fazione.
- Rarità.
- Costo di energia.
- Costo in punti potenza.
- Attacco base.
- Difesa iniziale.

L’attacco determina i danni inflitti a ogni scambio. La difesa diminuisce con i danni subiti e non si rigenera automaticamente.

Una creatura muore quando la sua difesa raggiunge zero o un valore inferiore. L’attacco base rimane invariato; eventuali modifiche dipendono dalla fazione e dall’equipaggiamento.

Le statistiche delle cinque fasce di energia devono essere definite e bilanciate: i precedenti intervalli di attacco e difesa non vengono estesi automaticamente alla nuova struttura.

## 6. Fazioni

Sono previste cinque fazioni, ciascuna forte contro due e debole contro altre due.

| Fazione | Forte contro | Debole contro |
| --- | --- | --- |
| Cyborg | Mostri Radioattivi, Sapiens | Alieni, Cultisti |
| Mostri Radioattivi | Sapiens, Cultisti | Cyborg, Alieni |
| Sapiens | Cultisti, Alieni | Cyborg, Mostri Radioattivi |
| Cultisti | Cyborg, Alieni | Sapiens, Mostri Radioattivi |
| Alieni | Cyborg, Mostri Radioattivi | Sapiens, Cultisti |

Una creatura ottiene +2 attacco contro una fazione su cui ha vantaggio.

Contro la stessa fazione non si applicano bonus. La creatura in svantaggio non riceve ulteriori penalità.

Il bonus viene ricalcolato ogni volta che cambia la creatura avversaria e si somma agli eventuali modificatori dell’equipaggiamento.

## 7. Equipaggiamenti

Ogni giocatore dispone di cinque equipaggiamenti differenti.

All’inizio della partita il server mescola il relativo mazzetto. All’inizio di ogni round viene estratto un equipaggiamento, senza reinserirlo tra quelli disponibili.

L’equipaggiamento:

- Viene assegnato alla prima creatura schierata.
- Rimane attivo per tutto il round.
- Passa automaticamente alla creatura successiva quando quella attiva muore.
- Viene scartato al termine del round.

Il trasferimento non cura le creature e non trasferisce alla successiva i danni subiti dalla precedente. Ogni nuova creatura entra con la propria difesa iniziale, modificata dall’equipaggiamento.

Il testo della carta deve specificare chiaramente effetto, condizioni di attivazione ed eventuali limiti. Il passaggio a una nuova creatura non ripete effetti immediati, salvo indicazione esplicita.

Se la partita termina anticipatamente, gli equipaggiamenti non ancora estratti rimangono inutilizzati.

## 8. Preparazione della partita

- Il giocatore A invia una sfida scegliendo un mazzo.
- Il giocatore B accetta scegliendo il proprio.
- Il sistema verifica quantità, possesso delle carte, budget, rarità, copie e assegnazione ai round.
- La composizione viene fissata per tutta la partita.
- Il server mescola separatamente ciascun mazzetto di creature e quello degli equipaggiamenti.
- Il punteggio dei round parte da 0–0.

Tutte le estrazioni sono gestite dal server e non possono essere influenzate dai giocatori.

## 9. Inizio di ogni round

All’inizio del round:

- Ogni giocatore riceve 30 punti vita, indipendentemente dal risultato precedente.
- Vengono attivati i due mazzetti di creature assegnati a quel round.
- Ogni giocatore scopre la prima creatura.
- Viene estratto e applicato un equipaggiamento per giocatore.
- Inizia il combattimento automatico.

Ogni giocatore ha una sola creatura attiva alla volta. Non esiste una mano da gestire.

## 10. Combattimento

Uno scambio di attacco consiste in un attacco simultaneo delle due creature attive.

Il sistema calcola gli attacchi effettivi, includendo bonus di fazione ed equipaggiamenti, e applica contemporaneamente i danni alle difese.

Una creatura infligge il proprio attacco anche se muore nello stesso scambio.

### Entrambe sopravvivono

Rimangono in campo con la difesa ridotta e combattono nuovamente.

### Muore una sola creatura

- La creatura morta viene scartata.
- Il suo proprietario subisce l’eventuale danno in eccesso.
- Se il round continua, entra la creatura successiva del suo mazzetto e riceve l’equipaggiamento.
- La superstite mantiene la difesa residua.
- Vengono ricalcolati i bonus di fazione.

### Entrambe muoiono

- Entrambe vengono scartate.
- Nessun giocatore subisce danni da combattimento.
- Se il round continua, entrambi schierano la creatura successiva e le assegnano il proprio equipaggiamento.

Non si possono pescare creature dai mazzetti destinati ad altri round.

## 11. Danni ai giocatori

Quando una sola creatura muore, il suo proprietario subisce il danno in eccesso:

Danno = massimo tra 0 e attacco effettivo della superstite − difesa della creatura sconfitta prima dello scambio letale.

Esempio: una creatura con attacco effettivo 6 sconfigge una creatura con 2 difesa residua. Se sopravvive allo scambio, infligge 4 danni al giocatore avversario.

Se entrambe le creature muoiono, nessun danno in eccesso viene applicato.

La formula rimane da confermare con gli insegnanti rispetto all’interpretazione del brief.

## 12. Conclusione del round

Dopo ogni scambio vengono controllate le seguenti condizioni, nell’ordine indicato.

### Punti vita esauriti

Chi raggiunge zero punti vita o un valore inferiore perde il round.

Se futuri effetti degli equipaggiamenti permettono a entrambi di raggiungere zero nello stesso momento, il round termina in pareggio.

### Creature esaurite

Chi non ha più creature vive né creature da schierare perde il round, se l’avversario ne ha ancora almeno una viva o disponibile.

Avere esaurito le carte coperte non provoca la sconfitta finché l’ultima creatura attiva sopravvive.

### Eliminazione simultanea delle ultime creature

Se entrambi esauriscono tutte le creature nello stesso scambio:

- Vince chi conserva più punti vita.
- Se i punti vita sono uguali, il round termina in pareggio.

Il vincitore conquista una vittoria nel conteggio dei round. Un round pareggiato non assegna vittorie.

Al termine, creature ed equipaggiamenti del round vengono rimossi. Nessun danno o effetto passa al round successivo.

## 13. Conclusione della partita

La partita termina immediatamente quando un giocatore conquista tre round.

Se dopo cinque round nessuno ha raggiunto tre vittorie:

- Vince chi ha conquistato più round.
- A parità di round vinti, la partita termina in pareggio.

Esempi:

| Risultato | Esito |
| --- | --- |
| 3–0 | Vittoria al termine del terzo round |
| 3–1 | Vittoria al termine del quarto round |
| 3–2 | Vittoria al termine del quinto round |
| 2–1 e due round pareggiati | Vince chi ha conquistato due round |
| 2–2 e un round pareggiato | Partita in pareggio |

I punti vita servono a risolvere il singolo round: non vengono sommati tra round e non costituiscono un ulteriore spareggio della partita.

## 14. Resoconto della partita

Il sistema conserva e rende consultabili:

- Risultato complessivo e punteggio dei round.
- Vincitore o pareggio di ciascun round.
- Equipaggiamenti estratti.
- Ordine di ingresso delle creature.
- Attacchi, bonus, danni e difesa residua.
- Variazioni dei punti vita.
- Numero degli scambi e motivo della conclusione.

## 15. Elementi da completare e testare

Prima dell’implementazione definitiva, il team dovrà:

- Confermare le soglie di energia proposte e l’accesso delle creature di costo inferiore ai round successivi.
- Definire statistiche e punti potenza delle carte.
- Scrivere gli effetti precisi degli equipaggiamenti e il loro ordine di risoluzione.
- Verificare budget, rarità, copie e bonus di fazione.
- Confermare la formula dei danni in eccesso.
- Definire se gli equipaggiamenti possono introdurre cure o impedire i danni.

## Collegamenti

### Contratti tecnici e dipendenze BackEnd

- [F05 — Gestione e validazione dei mazzi](../BackEnd/gestione-mazzi.md)
- [F07 — Sfide e simulazione](../BackEnd/sfide-simulazione.md)
- [F08 — Storico delle partite](../BackEnd/storico-partite.md)
- [F10 — Gestione amministrativa](../BackEnd/gestione-amministrativa.md)

### Documentazione FrontEnd

- [F05 — Composizione dei mazzi](../FrontEnd/gestione-mazzi.md)
- [F07 — Sfide tra utenti](../FrontEnd/sfide-simulazione.md)
- [F08 — Consultazione dello storico](../FrontEnd/storico-partite.md)

### Contenuti Digital e riferimenti comuni

- [D01 — Lore, identità e carte](lore-identita-carte.md)
- [Indice Digital](README.md)
- [Indice generale e stato delle funzionalità](../README.md)
- [Modello di documentazione per funzionalità](../modello-funzionalita.md)
- [Linee guida e regole](../../Linee-Guida-e-Regole.md)
- [Lista di verifica della consegna](../lista-verifica-consegna.md)
- [README del progetto](../../README.md)
