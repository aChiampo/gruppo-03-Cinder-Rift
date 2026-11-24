# Catalogo e collezione delle carte

ID: F04 · Perimetro: confermato; contratto HTTP implementato.
Implementazione: catalogo e collezione React collegati alle API backend esistenti.
Ultimo aggiornamento significativo: **2026-11-22**, su richiesta dell'utente di usare le API backend, pubblicare le 30 illustrazioni CR01 in WebP e allineare documentazione e verifiche. Fonte canonica: [contratto BackEnd F04](../BackEnd/collezione-carte.md).

## Schermate e richieste

- Catalogo e collezione usano componenti condivisi; il parametro `catalog` sceglie la rotta di lettura documentata. Le richieste passano dal client F01 con Bearer, rinnovo della sessione e cancellazione delle richieste superate.
- Il catalogo mostra una riga per carta; il dettaglio permette di scegliere fra tutte le varianti grafiche restituite. La collezione mostra una riga per carta e variante, identificata dalla coppia degli ID, con copie possedute e scambiabili registrate.
- I dettagli sono già presenti negli elenchi: le due rotte di dettaglio backend non sono chiamate da queste schermate.
- Il client conserva i payload canonici, inclusi riferimenti, valori nulli e contatori. Non inventa durata, effetti, lore, saldi o disponibilità netta agli scambi.
- Immagini assenti o non caricabili mostrano un segnaposto; statistiche nulle mostrano un trattino. Riferimenti sconosciuti usano nome o codice ricevuto; riferimenti assenti mostrano «Non disponibile».

## Filtri, ordinamento e paginazione

- Ricerca per nome, tipo, fazione, rarità, codice espansione e intervalli inclusivi di energia, attacco, difesa e punti potenza.
- Ogni filtro categoriale seleziona un solo codice, come previsto dal contratto. I selettori di fazione e rarità offrono i codici del set corrente; non esiste una rotta di metadati e le opzioni non sono dedotte da una sola pagina di risultati.
- I limiti numerici sono facoltativi e indipendenti: un campo vuoto non invia il limite, mentre zero resta un valore esplicito. Non vengono applicati intervalli impliciti che escludano carte con statistiche nulle.
- Tutti gli ordinamenti canonici sono disponibili, con direzione crescente o decrescente; default nome crescente.
- Il client richiede 15 elementi per pagina. L'interfaccia usa pagine a base uno e traduce soltanto la query in base zero; totale e numero di pagine provengono dal server.
- Cambiare un filtro o l'ordinamento riporta alla prima pagina; «Azzera filtri» conserva l'ordinamento. La ricerca ha debounce di 200 ms e le richieste superate sono annullate. Una pagina divenuta vuota con totale positivo riporta alla prima pagina.

## Stati e accessibilità

- Caricamento con scheletri e stato accessibile; errori con messaggio del server e pulsante «Riprova», mantenendo i filtri.
- Il catalogo vuoto, la collezione vuota e una ricerca senza risultati hanno messaggi distinti. Un errore non diventa un elenco vuoto riuscito.
- I parametri non validi restano soggetti alla validazione backend; il client mostra anche gli errori degli intervalli invertiti.
- Un `401` segue il rinnovo F01 e, se rifiutato, il ritorno all'accesso. I payload riusciti ma malformati producono un errore `502` locale.
- Filtri numerici, selettori, ricerca e paginazione hanno etichette accessibili. Su mobile il pannello dei filtri è richiudibile.

## Verifiche e limiti

Verifica del **2026-11-22**, copia di lavoro di `test-connessione`: **33 test frontend superati** con `npm test`, inclusi servizi e rendering statico; build Vite riuscita. Coperti endpoint distinti, Bearer e rinnovo, query, paginazione, varianti, conteggi, nullabilità, risposte malformate, cancellazione, stati vuoti ed errori.

Il client sorgente è stato provato contro Spring locale sulla porta 8081: accesso con l'account dimostrativo documentato, catalogo e collezione con 30 elementi ciascuno, filtro combinato, seconda pagina e risposte `400`/`401` attese; sessione di prova chiusa. Dopo la ricostruzione del solo frontend con Compose, ripetute le letture attraverso Nginx sulla porta 8082: verificati anche i cinque equipaggiamenti, la conservazione dei payload, il bundle aggiornato e lo stato di salute. Nessuna modifica al catalogo o alle copie. La suite backend non è stata ripetuta perché il contratto e il codice Java non sono cambiati.

La verifica browser non è disponibile in questa sessione perché l'host di accesso al browser non è raggiungibile. I test statici non attestano interazioni, responsive o caricamento effettivo delle immagini nel browser.

## Immagini del set CR01

Dal **2026-11-22**, su richiesta dell'utente, [public/img/cards](../../cinder-rift/public/img/cards/) contiene 30 illustrazioni WebP lossless estratte dalle carte originali, con un file distinto per carta. La build copia gli asset in `dist/img/cards/`; il componente esistente usa gli URL ricevuti dalle API. Associazione alle arti `STANDARD`, aggiornamento dei dati e conservazione degli identificativi seguono il [contratto BackEnd F04](../BackEnd/collezione-carte.md#immagini-del-set-cr01).

Per distribuire questa modifica servono il backend con V10 e la ricostruzione del frontend. Nessun URL usa percorsi del filesystem o un host locale fisso. Il segnaposto resta disponibile per immagini assenti o non caricabili e per altre carte senza URL.

Verifica locale del **2026-11-22**, copia di lavoro di `test-connessione`: esaminati visivamente tutti i 30 ritagli; verificata l'identità dei pixel fra WebP decodificati e ritagli PNG originali, senza ridimensionamento. `npm test` supera 33 test e `npm run build` riesce. Tutti i 30 file di `public` e `dist` coincidono byte per byte; ogni URL della migrazione restituisce `200` e `Content-Type: image/webp` dal build servito con Vite preview. I test Node e l'avvio preview hanno richiesto l'esecuzione fuori dal sandbox dopo il rifiuto Windows `spawn EPERM`. Verifica del database nel [contratto BackEnd](../BackEnd/collezione-carte.md#immagini-del-set-cr01). Nessuna prova browser o distribuzione sullo stack applicativo.

## Integrazioni da completare

- Selettore visivo nel deck builder F05: resta l'editor di ID carta/variante, con validazione del server.
- Aggiornamento dopo apertura pacchetti F03 o scambi F06, quando i rispettivi flussi saranno disponibili.
- Metadati completi dei filtri ed eventuali nuovi codici, da coordinare con F10.

## Collegamenti

- [Contratto API F04](../BackEnd/collezione-carte.md)
- [Autenticazione F01](../BackEnd/accesso-utenti.md)
- [Mazzi F05](gestione-mazzi.md)
- [Pacchetti F03](../BackEnd/acquisto-pacchetti.md)
- [Scambi F06](scambi-mercato.md)
- [Frontend: avvio e verifiche](../../cinder-rift/README.md)
- [Indice FrontEnd](README.md)
