export const navItems = [
  { id: 'home', label: 'HOME', icon: 'Home' },
  { id: 'gioca', label: 'GIOCA', icon: 'Swords' },
  { id: 'deck-builder', label: 'DECK BUILDER', icon: 'BookCopy' },
  { id: 'collezione', label: 'COLLEZIONE', icon: 'Layers' },
  { id: 'profilo', label: 'PROFILO', icon: 'UserRound' },
  { id: 'notifiche', label: 'NOTIFICHE', icon: 'Bell' },
]

// Sezioni non ancora realizzate: contenuti previsti dal documento di progetto
export const placeholders = {
  collezione: { title: 'Collezione', items: ['Elenco carte (filtri: fazione, rarità, costo, attacco, difesa)', 'Dettaglio carta: statistiche, copie possedute, matchup di fazione'] },
  mazzi: { title: 'Mazzi', items: ['Elenco mazzi (Bozza / Valido / Non valido)', 'Deck builder: crea e modifica mazzo', 'Carte usate per round 1–5 (creature ed equipaggiamenti)'] },
  sfide: { title: 'Sfide', items: ['Tab Ricevute, Inviate, Storico', 'Nuova sfida (invito tramite nome profilo)', 'Dettaglio sfida: accetta con mazzo / rifiuta', 'Resoconto partite'] },
  negozio: { title: 'Negozio', items: ['Pacchetti di carte (nome, costo, numero carte)', 'Conferma acquisto e apertura pacchetto', 'Risorse / valuta'] },
  profilo: { title: 'Profilo', items: ['Nome profilo e icona', 'Statistiche: partite, vittorie, sconfitte, pareggi', 'Progresso verso le 30 carte per le classificate'] },
  impostazioni: { title: 'Impostazioni', items: ['Account, preferenze (lingua, audio)', 'Ripeti tutorial', 'Regolamento, termini e privacy, logout'] },
}
