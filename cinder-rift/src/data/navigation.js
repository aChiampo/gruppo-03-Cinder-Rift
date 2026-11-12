export const navItems = [
  { id: 'home', label: 'HOME', icon: 'Home' },
  { id: 'collezione', label: 'COLLEZIONE', icon: 'Layers' },
  { id: 'deck', label: 'DECK', icon: 'BookCopy' },
  { id: 'gioca', label: 'GIOCA', icon: 'Swords' },
  { id: 'negozio', label: 'NEGOZIO', icon: 'ShoppingBag' },
]

// Sezioni non ancora realizzate: contenuti previsti dal documento di progetto
export const placeholders = {
  gioca: { title: 'Gioca', items: ['Tab Ricevute, Inviate, Storico', 'Nuova sfida (invito tramite nome profilo)', 'Dettaglio sfida: accetta con mazzo / rifiuta', 'Resoconto partite'] },
  negozio: { title: 'Negozio', items: ['Pacchetti di carte (nome, costo, numero carte)', 'Conferma acquisto e apertura pacchetto', 'Risorse / valuta'] },
  profilo: { title: 'Profilo', items: ['Nome profilo e icona', 'Statistiche: partite, vittorie, sconfitte, pareggi', 'Progresso verso le 30 carte per le classificate'] },
  impostazioni: { title: 'Impostazioni', items: ['Account, preferenze (lingua, audio)', 'Ripeti tutorial', 'Regolamento, termini e privacy, logout'] },
}
