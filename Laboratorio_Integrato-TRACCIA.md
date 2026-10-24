Laboratorio Integrato – Digital Strategist, Web e Software Developer. 
### Il progetto
Titolo progetto: gioco di carte collezionabili digitale (nome da definire nel laboratorio) Committente: investitore privato 
Referente aziendale: da definire 
Dominio web: da definire nel laboratorio 
### Macro descrizione 
Un investitore vuole entrare nel mercato dei giochi di carte collezionabili con un prodotto esclusivamente digitale, senza alcuna versione cartacea. Le carte si acquistano, si collezionano, si scambiano e si giocano solo sul portale ufficiale del gioco. 
Ogni gruppo dovrà inventare il gioco da zero: ambientazione, carte, identità visiva, piano di comunicazione e portale. 
### Obiettivi del progetto
- Ideazione del tema e dell'ambientazione del gioco (ad esempio fantascientifica, fantasy, storica). 
- Creazione del set di carte con strumenti di intelligenza artificiale. 
- Definizione di nome, logo e visual identity del gioco. 
- Strategia di marketing e comunicazione e strategia di lancio per costruire una community di giocatori 
- Realizzazione del portale per acquisto pacchetti, gestione mazzi, scambi e sfide tra utenti. 
### Funzionalità richieste del portale
Il portale gestisce l'intero ciclo di vita delle carte: dall'acquisto alla sfida. 
- **Registrazione e accesso** degli utenti. 
- **Crediti virtuali**: ogni utente possiede un saldo di crediti. L'acquisto di crediti con moneta reale va pianificato per una futura fase 2, ma attualmente NON deve essere sviluppato; le modalità con cui l'utente ottiene crediti in fase 1 sono a scelta del gruppo. In fase 1 va comunque pianificata e quantificata la conversione crediti/moneta reale affinché lato marketing si possa calcolare la redditività della piattaforma a 12, 24 e 48 mesi. 
- **Acquisto pacchetti**: l'utente spende crediti per acquistare pacchetti con contenuto casuale, che vanno ad arricchire la sua collezione. Le carte dovranno seguire un criterio di rarità (TIER) che andrà definito dagli studenti. I pacchetti, pur fornendo contenuto casuale, dovranno contenere un numero specifico di carte per ogni TIER, anche questo da definire. Infine, alla luce di questi elementi, si dovrà delineare un costo in crediti coerente per il singolo pacchetto. 
- **Collezione**: consultazione delle carte possedute. 
- **Mazzi**: ogni utente può comporre e salvare più mazzi. Il portale verifica che ogni mazzo rispetti i vincoli del regolamento. 
- **Scambi**: gli utenti possono proporsi scambi liberi di sole carte, anche in quantità diverse (ad esempio 3 carte contro 1, 1 contro 5). Lo scambio avviene solo se il destinatario accetta. - Sfide: l'utente A sfida l'utente B scegliendo uno dei propri mazzi. Quando B accetta, scegliendo a sua volta un mazzo, la partita viene simulata dal sistema e il risultato (turno per turno) è disponibile per entrambi. 
- **Storico** delle sfide giocate e dei relativi risultati. 
- **Pagine statiche**: Chi siamo, Contatti, FAQ, Privacy Policy, Glossario e altre pagine statiche ritenute necessarie dal team 
- **Backoffice** per la gestione del catalogo carte e dei pacchetti. 
- Responsive design e ottimizzazione SEO. 
### Regolamento base 
Il regolamento base è obbligatorio e comune a tutti i gruppi. Definisce cosa deve accadere, non i valori numerici: quantità, soglie e bilanciamento sono scelte del gruppo, da documentare e motivare. 
#### Le carte 
- Tutte le carte sono **creature**, con un valore di **attacco** e un valore di **difesa**. 
- Ogni creatura appartiene a una **fazione**. Le fazioni hanno forze e debolezze reciproche: una fazione ottiene un vantaggio quando affronta la fazione contro cui è "forte". Esempio: fuoco ottiene +1 in attacco contro erba, acqua ottiene +1 contro fuoco, erba ottiene +1 contro acqua. Numero di fazioni, relazioni e tipo di vantaggio (attacco, difesa, entrambe, altro) sono a scelta del gruppo. 
#### Il mazzo 
Ogni mazzo deve rispettare dei vincoli, che il portale deve verificare. Il gruppo stabilisce i valori, ma devono essere presenti almeno: 
- un numero di carte stabilito per ogni mazzo; 
- un numero massimo di copie della stessa carta; 
- un vincolo di bilanciamento sulla forza complessiva del mazzo, che impedisca di comporlo solo con le carte più forti. Il vincolo di bilanciamento potrà essere impostato anche in base ai TIER di carte (ad esempio: un mazzo potrebbe avere un massimo di 8 carte “rare”, e solo due “mitiche”). 
#### Svolgimento della partita 
1. Ogni giocatore ha dei **punti vita**. 
2. All'inizio della partita il mazzo di ogni giocatore viene mischiato casualmente. 
3. A ogni turno ciascun giocatore pesca la prima carta del proprio mazzo e la mette in gioco. 
4. Le due creature si scontrano contemporaneamente: l'attacco di ciascuna colpisce la difesa dell'altra, tenendo conto degli eventuali vantaggi di fazione. 
5. I danni subiti riducono la difesa della creatura e non vengono rigenerati. L'attacco resta invariato. Una creatura muore quando la sua difesa arriva a zero. 
6. Se muore una sola creatura, la vincitrice infligge al giocatore avversario un danno pari alla differenza tra il proprio attacco e la difesa residua della creatura sconfitta. 
7. Se le creature si uccidono a vicenda, i giocatori non subiscono danni. 
8. Se nessuna delle due creature muore, lo scontro prosegue con un nuovo round tra le stesse creature. 
9. Quando una creatura muore, il suo proprietario pesca la carta successiva. 
#### Fine della partita 
- Perde il giocatore i cui punti vita arrivano a zero. 
- La partita deve prevedere una conclusione anche quando i mazzi si esauriscono o lo scontro si prolunga oltre un certo numero di turni, con relativo criterio di vittoria o pareggio. 
#### Nota di game design 
Con questo regolamento il giocatore non prende decisioni durante la partita: la strategia sta interamente nella **composizione del mazzo**. Per questo la partita può essere simulata dal sistema senza interazione, subito dopo l'accettazione della sfida. Il mescolamento deve avvenire lato server, in modo che nessun giocatore possa influenzarlo. 
#### Modalità di gioco aggiuntive (facoltative) 
I gruppi possono proporre e implementare regole proprie, valutando pro e contro dell'implementazione dal punto di vista sia esperienziale che tecnico. Le varianti vanno realizzate come **modalità di gioco separate e attivabili**, senza modificare il comportamento della modalità base, che deve restare sempre giocabile e verificabile. 
Spunti possibili, in ordine di complessità crescente: 
- abilità speciali delle creature (ad esempio un effetto quando entrano in gioco o quando muoiono); 
- carte non creatura, come incantesimi o potenziamenti; 
Ogni variante va documentata nel regolamento del gioco rivolto ai giocatori. 
### Requisiti tecnici
- Backend: Java + Spring Boot 
- Frontend: HTML/CSS/SCSS/JS/TS oppure ReactJS 
- Database: MySQL - Versionamento: GitHub (il nome del progetto DEVE seguire la forma “gruppo-##-nome-progetto-scelto”) 
- Gestione progetto: GitHub Projects
- Documentazione: README.md, Swagger, JSDoc (obbligatorio se si usa JS or TS) 
	- il README.md deve essere completo e dettagliato. Possono essere più readme, e.s. generale, componente front-end, componente back-end
	- il README deve contenere il nome di ogni studente associato al nome dell'account GitHub nel formato @username. 
- Il motore di gioco deve essere un componente Java isolato dal resto dell'applicazione e coperto da test automatici (JUnit). 
### Attività richieste 

**Attività comuni a tutto il gruppo** 
- Elezione del Team Leader 
- Scelta del tema e dell'ambientazione del gioco
- Scelta di nome e logo 
- Definizione dei valori del regolamento: punti vita, vincoli del mazzo, fazioni e relativi vantaggi 
- Bilanciamento delle statistiche delle carte 
- Test del gioco e del portale 
- Documentazione e presentazione finale 
**Digital Strategist** 
- Analisi buyer persona e competitor 
- Sviluppo della lore e dell'ambientazione 
- Creazione del set di carte con l'intelligenza artificiale: illustrazioni, nomi, testi descrittivi e grafica della carta 
- Proposta di logo e visual identity 
- Definizione TOV, linee editoriali, PED 
- Modello economico dei crediti: come si ottengono, prezzo e contenuto dei pacchetti, rarità delle carte 
- Regolamento del gioco rivolto ai giocatori e testi del portale, con ottimizzazione SEO 
- Strategia di lancio e costruzione della community 
- Campagna paid (Meta, Google Ads, TikTok o altri canali ritenuti adatti al target) 
**Web Developer** 
- Conversione mockup in codice 
- UX/UI e accessibilità 
- Interfacce di negozio, collezione, composizione mazzi, scambi e sfide 
- Validazione dati lato client 
- Integrazione API 
- Ottimizzazione SEO tecnica e on-page 
	- Applicare le migliori pratiche SEO, includendo on-page SEO per ottimizzare contenuti, meta tag, immagini, titoli e altri elementi, e technical SEO per migliorare struttura, velocità, indicizzazione e file come robots.txt e sitemap.xml. Collaborare con gli studenti del corso Digital Strategist per attività di off-page SEO. 
- Performance di caricamento: Lighthouse e pagespeed 
- Screenshot completi di tutte le pagine dell'applicazione (sia in versione desktop che mobile)
**Software Developer** 
- Configurazione database 
- Sviluppo backend e API REST 
- Gestione utenti e saldo crediti
- Apertura dei pacchetti secondo le probabilità definite 
- Verifica dei vincoli dei mazzi lato server 
- Gestione degli scambi, garantendo che nessuna carta venga persa o duplicata 
- Motore di gioco secondo il regolamento base, incluse le fazioni 
- Gestione delle sfide e dello storico delle partite 
#### Budget indicativo 
Progettare una campagna che preveda l'acquisizione di 5.000 utenti nei primi 3 mesi, e che al mese 24 porti gli utenti registrati sul portale a 50.000 (obiettivo da stimare con proiezioni dedicate). 
Quantificare, inoltre, i costi di sviluppo del portale (architettura, design, SEO, motore di gioco). 
Tutti i costi devono essere coperti dal modello di monetizzazione che verrà implementato nel portale, che non è da sviluppare in fase 1, ma sarà implementato in fase 2, indicativamente al mese 13 dal lancio. La richiesta, infatti, è quella di arrivare a break-even (pareggio di bilancio) al mese 48 dal lancio, e quindi impostare un modello di monetizzazione che riesca, tra il mese 13 ed il mese 48, a coprire tutti i costi di marketing e sviluppo sostenuti all’avvio del progetto. Dal mese 49, al netto dei costi fissi di mantenimento, il progetto generare utili. 
#### Media e strumenti coinvolti 
- Social: Instagram, TikTok, YouTube (possibile estensione a Twitch, Discord, Reddit) 
- Strumenti: Meta Ads, Google Ads, TikTok Ads, strumenti di intelligenza artificiale per la generazione di immagini e testi 
#### Deliverable
Il file di consegna deve essere un unico archivio ZIP nominato gruppo-##-nome-progetto-scelto.zip e deve contenere: 
1. Codice frontend 
2. Codice backend 
3. Export del database 
4. Documentazione tecnica (README.md, Swagger, JSDoc) 
5. Documentazione completa del progetto (PDF, file testuali, immagini, ecc.) 
6. Presentazione finale 
#### Note finali 
- Il committente è aperto a proposte innovative, purché sostenibili e coerenti con gli obiettivi. 
- Quantità, valori e bilanciamento non indicati nel regolamento sono lasciati volutamente al gruppo: ogni scelta va motivata nella documentazione. 
- L'acquisto di crediti con moneta reale è escluso dal perimetro di sviluppo. 
- È richiesta una documentazione completa del progetto e una presentazione finale. 
- Ogni gruppo dovrà eleggere un Team Leader e proporre un nome e un logo per il gioco.