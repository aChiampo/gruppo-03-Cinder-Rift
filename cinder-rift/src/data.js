// Dati di esempio. In produzione arriveranno dal backend (API).
// Regole dal regolamento: mazzo = 30 carte (25 creature + 5 equipaggiamenti), budget max 75 PT.
export const DECK_SIZE = 30
export const MAX_POWER = 75

export const user = {
  name: 'Mirko',
  level: 3,
  credits: 2450,
  avatar: '/img/avatar-cyborg.jpg',
}

export const navItems = [
  { id: 'home', label: 'HOME', icon: 'Home' },
  { id: 'collezione', label: 'COLLEZIONE', icon: 'Layers' },
  { id: 'mazzi', label: 'MAZZI', icon: 'BookCopy' },
  { id: 'sfide', label: 'SFIDE', icon: 'Swords' },
  { id: 'negozio', label: 'NEGOZIO', icon: 'ShoppingBag' },
]

export const initialChallenges = [
  { id: 1, from: 'Luca_Storm', avatar: '/img/avatar-cyborg.jpg', type: 'Sfida classificata', ago: '2 ore fa' },
  { id: 2, from: 'ShadowPlayer', avatar: '/img/avatar-cultisti.jpg', type: 'Sfida amichevole', ago: '5 ore fa' },
  { id: 3, from: 'ZeroMind', avatar: '/img/avatar-alieni.jpg', type: 'Sfida classificata', ago: '1 giorno fa' },
]

export const lastMatch = {
  me: { name: 'Mirko', avatar: '/img/avatar-cyborg.jpg' },
  opponent: { name: 'Luca_Storm', avatar: '/img/avatar-cyborg.jpg' },
  result: 'VITTORIA',
  score: '2 - 1',
  rounds: 3,
  date: '24 ott 2026',
  duration: '12 min',
  mode: 'Classificata',
}

export const news = [
  { id: 1, tag: 'NEGOZIO', tone: 'blue', title: 'Nuove buste disponibili!', text: 'Sono arrivate le nuove buste Leggendarie nel negozio. Non farti scappare le novità!', date: '24 ott 2026', visual: 'packs' },
  { id: 2, tag: 'EVENTO', tone: 'orange', title: 'Evento speciale: La Frattura', text: "Partecipa all'evento limitato e ottieni ricompense esclusive.", date: '22 ott 2026', visual: '/img/art-radioattivi.jpg' },
  { id: 3, tag: 'PATCH NOTES', tone: 'purple', title: 'Patch 1.1.0', text: 'Bilanciamenti carte, correzioni bug e miglioramenti generali.', date: '20 ott 2026', visual: 'symbol' },
]

export const decks = [
  { id: 'principale', name: 'Mazzo Principale', cards: 30, power: 75, img: '/img/deck-sapiens.jpg', faction: 'mixed' },
  { id: 'radioattivi', name: 'Mazzo Radioattivi', cards: 30, power: 72, img: '/img/deck-radioattivi.jpg', faction: 'radioattivi' },
  { id: 'sapiens', name: 'Mazzo Sapiens', cards: 30, power: 68, img: '/img/deck-sapiens.jpg', faction: 'sapiens' },
  { id: 'cultisti', name: 'Mazzo Cultisti', cards: 30, power: 70, img: '/img/deck-cultisti.jpg', faction: 'cultisti' },
  { id: 'alieni', name: 'Mazzo Alieni', cards: 30, power: 66, img: '/img/deck-alieni.jpg', faction: 'alieni' },
]

// Colore guida per fazione (palette dal documento di lore)
export const factionColors = {
  mixed: '#ffb020',
  cyborg: '#ffd400',
  radioattivi: '#9bff3d',
  cultisti: '#d24dff',
  sapiens: '#ff7a2f',
  alieni: '#3dffd8',
}

// Sezioni non ancora realizzate: struttura presa dal documento di progetto
export const placeholders = {
  collezione: { title: 'Collezione', items: ['Elenco carte (filtri: fazione, rarità, costo, attacco, difesa)', 'Dettaglio carta: statistiche, copie possedute, matchup di fazione'] },
  mazzi: { title: 'Mazzi', items: ['Elenco mazzi (Bozza / Valido / Non valido)', 'Deck builder: crea e modifica mazzo', 'Carte usate per round 1–5 (creature ed equipaggiamenti)'] },
  sfide: { title: 'Sfide', items: ['Tab Ricevute, Inviate, Storico', 'Nuova sfida (invito tramite nome profilo)', 'Dettaglio sfida: accetta con mazzo / rifiuta', 'Resoconto partite'] },
  negozio: { title: 'Negozio', items: ['Pacchetti di carte (nome, costo, numero carte)', 'Conferma acquisto e apertura pacchetto', 'Risorse / valuta'] },
  profilo: { title: 'Profilo', items: ['Nome profilo e icona', 'Statistiche: partite, vittorie, sconfitte, pareggi', 'Progresso verso le 30 carte per le classificate'] },
  impostazioni: { title: 'Impostazioni', items: ['Account, preferenze (lingua, audio)', 'Ripeti tutorial', 'Regolamento, termini e privacy, logout'] },
}
