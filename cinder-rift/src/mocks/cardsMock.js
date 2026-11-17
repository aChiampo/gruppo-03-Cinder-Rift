// DATI E LOGICA DI PROVA DEL BACKEND — da eliminare quando le API di Gabriele sono pronte.
// Usato da due parti:
//   1. server/mock-api.js  -> risponde a GET /api/cards (npm run mock-api)
//   2. cardService.js      -> solo con VITE_USE_MOCK=true (nessuna rete), caricato con import() dinamico
// Il frontend NON importa questo file in produzione: non c'è nessun dato dimostrativo nel bundle.
//
// Forma delle carte = CONTRATTO API (vedi docs/API-COLLEZIONE.md). Il frontend la converte con normalizeCard().
//
// ILLUSTRAZIONI: nel set dimostrativo c'è 1 illustrazione per fazione + 2 per gli equipaggiamenti.
// Le altre carte riusano la stessa immagine con un ritaglio diverso (imageCrop, solo per il mock).
// EFFETTI EQUIPAGGIAMENTI: bozze da validare, il regolamento (par. 15) li lascia da definire.

// Ritagli (in % del riquadro): x/y = punto di messa a fuoco, zoom, flip = specchiato.
const VIEWS = [
  { x: 50, y: 50, zoom: 1 },
  { x: 40, y: 0, zoom: 1.45 },
  { x: 72, y: 25, zoom: 1.4 },
  { x: 50, y: 50, zoom: 1.05, flip: true },
  { x: 60, y: 0, zoom: 1.4, flip: true },
]
const equipIcon = (icon) => ({ icon })

const slug = (name) => name.toLowerCase().normalize('NFD').replace(/[\u0300-\u036f]/g, '').replace(/[^a-z0-9]+/g, '-').replace(/(^-|-$)/g, '')

const creature = (faction, rarity, name, energy, power, attack, defense, flavor, artView, extra = {}) => ({
  id: slug(name), type: 'creature', faction, rarity, name, energy, power, attack, defense, flavor,
  imageUrl: `/img/cards/${faction}.jpg`, imageCrop: VIEWS[artView], ...extra,
})

const equipment = (rarity, name, energy, power, accent, effect, flavor, artDef) => ({
  id: slug(name), type: 'equipment', faction: null, rarity, name, energy, power, attack: null, defense: null,
  flavor, effect, duration: 'Round', accentColor: accent,
  imageUrl: artDef.src ?? null,
  ...(artDef.src ? { imageCrop: { x: artDef.x, y: artDef.y, zoom: artDef.zoom } } : { icon: artDef.icon }),
})

const catalog = [
  // Cyborg
  creature('cyborg', 'comune', 'Cyborg Scout', 1, 1, 2, 2, 'Un occhio in ogni ombra, un ordine in ogni passo.', 1),
  creature('cyborg', 'rara', 'Cyborg Berserker', 3, 2, 4, 4, 'Quando il protocollo si spezza, resta solo la furia.', 2),
  creature('cyborg', 'comune', 'Sentinella K-17', 2, 2, 3, 5, 'La città è caduta. I suoi ordini di restare di guardia no.', 0),
  creature('cyborg', 'comune', 'Riparatore R-9', 1, 1, 1, 3, 'Ripara tutto, tranne sé stesso.', 3),
  creature('cyborg', 'comune', 'Unità Frammento', 3, 2, 4, 3, 'Un pezzo di una macchina più grande, ancora in marcia.', 4),
  // Mostri Radioattivi
  creature('radioattivi', 'rara', 'Toxik Mutant', 2, 2, 3, 3, 'Il veleno è l’unica cosa che gli è rimasta di umano.', 1),
  creature('radioattivi', 'epica', 'Mutante Alfa', 4, 4, 6, 5, 'Il branco segue chi ha resistito di più alle radiazioni.', 2),
  creature('radioattivi', 'comune', 'Cane di Scoria', 3, 2, 4, 4, 'Dove la terra brucia, lui trova una pista.', 0),
  creature('radioattivi', 'comune', 'Larva Tossica', 1, 1, 1, 2, 'Piccola, lenta, ovunque.', 3),
  creature('radioattivi', 'comune', 'Brulicante', 2, 1, 3, 2, 'Non è mai solo. Non è mai uno.', 4),
  // Sapiens
  creature('sapiens', 'epica', 'Sapiens Guardiano', 4, 4, 5, 5, 'La conoscenza è la vera arma.', 1, {
    lore: 'I Sapiens non combattono per distruggere, ma per costruire un domani migliore.',
  }),
  creature('sapiens', 'comune', 'Sapiens Medic', 2, 1, 3, 3, 'Ogni ferita curata è un giorno guadagnato.', 2),
  creature('sapiens', 'comune', 'Esploratore Rovine', 2, 2, 3, 5, 'Ogni rovina nasconde una via d’uscita.', 0),
  creature('sapiens', 'comune', 'Recuperante', 1, 1, 2, 2, 'Quello che per gli altri è rottame, per lui è un inizio.', 3),
  creature('sapiens', 'comune', 'Arciere del Ponte', 3, 2, 4, 3, 'Dal ponte vede arrivare tutto, molto prima degli altri.', 4),
  // Cultisti
  creature('cultisti', 'comune', 'Cultista Ombra', 1, 1, 1, 2, 'Chi non si vede non può essere fermato.', 1),
  creature('cultisti', 'rara', 'Cultista Sacerdote', 3, 3, 4, 4, 'Predica alla Frattura. E la Frattura ascolta.', 2),
  creature('cultisti', 'comune', 'Accolito Frattura', 3, 2, 4, 4, 'Nel silenzio della Frattura, qualcuno risponde.', 0),
  creature('cultisti', 'comune', 'Novizio del Velo', 2, 1, 2, 3, 'Ha giurato fedeltà prima ancora di capire a cosa.', 3),
  creature('cultisti', 'comune', 'Fanatico Cobalt', 2, 2, 3, 3, 'Il Cobalto lo chiama. Lui risponde sempre.', 4),
  // Alieni
  creature('alieni', 'rara', 'Alieno Drone', 3, 3, 4, 3, 'Obbedisce a una voce che nessun altro sente.', 1),
  creature('alieni', 'leggendaria', 'Alieno Predatore', 5, 5, 7, 6, 'Non cerca una preda. Cerca un degno avversario.', 2),
  creature('alieni', 'comune', 'Segugio di Orione', 3, 2, 5, 3, 'Per lui, questo mondo è appena cominciato.', 0),
  creature('alieni', 'comune', 'Larva Aliena', 1, 1, 2, 1, 'Cresce in fretta. Impara ancora più in fretta.', 3),
  creature('alieni', 'comune', 'Esploratore Xeno', 2, 1, 3, 2, 'Ogni mondo è un campione da analizzare.', 4),

  // Equipaggiamenti
  equipment('comune', 'Modulo Energetico', 1, 1, '#ff8a1f',
    'La creatura equipaggiata ottiene +1 attacco.',
    'Un frammento di tecnologia. Un vantaggio tangibile.',
    { src: '/img/cards/equip-lama.jpg', x: 50, y: 50, zoom: 1 }),
  equipment('rara', 'Armatura Tattica', 2, 2, '#3d9bff',
    'Ogni creatura entra con +2 difesa iniziale. Non cura la creatura attiva.',
    'Nulla va perso finché può proteggere.',
    { src: '/img/cards/equip-armatura.jpg', x: 50, y: 50, zoom: 1 }),
  equipment('epica', 'Chip Neurale', 3, 3, '#b34dff',
    'La creatura equipaggiata ottiene +1 attacco e +1 difesa.',
    'Pensa più in fretta di quanto riesca a sopportare.',
    equipIcon('Cpu')),
  equipment('rara', 'Bomba a Nulla', 4, 3, '#5dff3a',
    'All’ingresso, infligge 2 danni alla creatura avversaria attiva.',
    'Dove cade, non resta nemmeno il silenzio.',
    equipIcon('Bomb')),
  // NB: il PDF economico prevede nessun equipaggiamento leggendario nel primo set.
  // Qui resta perché presente nel mockup: per allinearsi al PDF basta cambiare la rarità.
  equipment('leggendaria', 'Velo di Ombra', 5, 5, '#c04dff',
    'La creatura equipaggiata ottiene +3 difesa.',
    'Chi lo indossa smette di essere un bersaglio.',
    equipIcon('Ghost')),
]

// ---------------------------------------------------------------------------------------------
// Collezioni di prova per utente (id utente di server/mock-api.js -> id carte possedute)
export const MOCK_CATALOG = catalog
export const collectionFor = (userId) => (userId === 2 ? [] : catalog) // utente 2 = collezione vuota

// ---------------------------------------------------------------------------------------------
// "Backend": filtri, ricerca, ordinamento e paginazione di GET /cards. Valori dei parametri = stringhe
// (come arrivano in una query string). Vedi docs/API-COLLEZIONE.md.
export const SORT_KEYS = ['rarita', 'nome', 'energia', 'attacco', 'difesa', 'fazione']
export const MAX_PAGE_SIZE = 100

const RARITY_RANK = { comune: 1, rara: 2, epica: 3, leggendaria: 4 }
const FACTION_ORDER = ['cyborg', 'radioattivi', 'sapiens', 'cultisti', 'alieni']
const FACTION_LABEL = { cyborg: 'Cyborg', radioattivi: 'Mostri Radioattivi', sapiens: 'Sapiens', cultisti: 'Cultisti', alieni: 'Alieni' }

const norm = (s) => String(s).normalize('NFD').replace(/[\u0300-\u036f]/g, '').toLowerCase()
const csv = (v) => (v ? String(v).split(',').map((s) => s.trim()).filter(Boolean) : [])
const int = (v, fallback) => (v === undefined || v === '' || Number.isNaN(Number(v)) ? fallback : Math.trunc(Number(v)))

/** Lancia { status: 400, message } se un parametro non è valido. */
export function queryCards(all, p = {}) {
  const sort = p.sort || 'rarita'
  if (!SORT_KEYS.includes(sort)) throw Object.assign(new Error(`Ordinamento non valido: ${sort}`), { status: 400 })
  const type = p.type || 'all'
  if (!['all', 'creature', 'equipment'].includes(type)) throw Object.assign(new Error(`Tipo non valido: ${type}`), { status: 400 })

  const pageSize = Math.min(Math.max(int(p.pageSize, 15), 1), MAX_PAGE_SIZE)
  const wantedPage = Math.max(int(p.page, 1), 1)
  const factions = csv(p.faction)
  const rarities = csv(p.rarity)
  const [eMin, eMax] = [int(p.energyMin, -Infinity), int(p.energyMax, Infinity)]
  const [aMin, aMax] = [int(p.attackMin, null), int(p.attackMax, null)]
  const [dMin, dMax] = [int(p.defenseMin, null), int(p.defenseMax, null)]
  const q = norm((p.q ?? '').trim())

  const inRange = (v, lo, hi) => (lo == null || v >= lo) && (hi == null || v <= hi)
  // Con un filtro su attacco/difesa attivo, gli equipaggiamenti (senza statistiche) sono esclusi.
  const stat = (v, lo, hi) => (lo == null && hi == null) || (v != null && inRange(v, lo, hi))

  const matches = (c) => {
    if (q && !norm(`${c.name} ${FACTION_LABEL[c.faction] ?? ''} ${c.type === 'equipment' ? 'equipaggiamento' : ''}`).includes(q)) return false
    if (factions.length && !factions.includes(c.faction)) return false
    if (rarities.length && !rarities.includes(c.rarity)) return false
    return inRange(c.energy, eMin, eMax) && stat(c.attack, aMin, aMax) && stat(c.defense, dMin, dMax)
  }

  const order = new Map(all.map((c, i) => [c.id, i])) // ordine di catalogo = spareggio stabile
  const byOrder = (a, b) => order.get(a.id) - order.get(b.id)
  const desc = (key) => (a, b) => (b[key] ?? -1) - (a[key] ?? -1) || byOrder(a, b) // senza statistica = in fondo
  const sorters = {
    rarita: (a, b) => RARITY_RANK[b.rarity] - RARITY_RANK[a.rarity] || byOrder(a, b), // dalla più rara
    nome: (a, b) => a.name.localeCompare(b.name, 'it'),
    energia: (a, b) => a.energy - b.energy || RARITY_RANK[b.rarity] - RARITY_RANK[a.rarity] || byOrder(a, b),
    attacco: desc('attack'),
    difesa: desc('defense'),
    fazione: (a, b) => (FACTION_ORDER.indexOf(a.faction) + 1 || 99) - (FACTION_ORDER.indexOf(b.faction) + 1 || 99) || byOrder(a, b),
  }

  const base = all.filter(matches) // tutti i filtri tranne il tipo: servono ai contatori delle tab
  const counts = {
    all: base.length,
    creature: base.filter((c) => c.type === 'creature').length,
    equipment: base.filter((c) => c.type === 'equipment').length,
  }
  const sorted = base.filter((c) => type === 'all' || c.type === type).sort(sorters[sort])
  const pageCount = Math.max(1, Math.ceil(sorted.length / pageSize))
  const page = Math.min(wantedPage, pageCount)
  return { items: sorted.slice((page - 1) * pageSize, page * pageSize), page, pageSize, total: sorted.length, counts }
}
