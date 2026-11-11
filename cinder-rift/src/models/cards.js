import { factions, factionById } from '../data/factions.js'

/**
 * Rarità. `power` = intervallo di punti potenza per carta (regolamento v2.0, par. 3).
 * `rank` serve solo all'ordinamento.
 */
export const RARITIES = [
  { id: 'comune',      label: 'Comune',      rank: 1, color: '#b6c2d0', power: [1, 2] },
  { id: 'rara',        label: 'Rara',        rank: 2, color: '#3d9bff', power: [2, 3] },
  { id: 'epica',       label: 'Epica',       rank: 3, color: '#b34dff', power: [3, 4] },
  { id: 'leggendaria', label: 'Leggendaria', rank: 4, color: '#ffb629', power: [5, 5] },
]
export const rarityById = Object.fromEntries(RARITIES.map((r) => [r.id, r]))

export const ENERGY_RANGE = [1, 5]   // soglie energetiche dei 5 round
export const STAT_RANGE = [0, 10]    // intervallo dei filtri Attacco / Difesa
export const PAGE_SIZE = 15          // 5 colonne x 3 righe

export const defaultFilters = {
  query: '',
  type: 'all',            // 'all' | 'creature' | 'equipment'
  factions: [],           // vuoto = tutte
  rarities: [],           // vuoto = tutte
  energy: [...ENERGY_RANGE],
  attack: [...STAT_RANGE],
  defense: [...STAT_RANGE],
}

export const SORT_OPTIONS = [
  { id: 'rarita',  label: 'Rarità' },
  { id: 'nome',    label: 'Nome' },
  { id: 'energia', label: 'Energia' },
  { id: 'attacco', label: 'Attacco' },
  { id: 'difesa',  label: 'Difesa' },
  { id: 'fazione', label: 'Fazione' },
]

const norm = (s) => s.normalize('NFD').replace(/[\u0300-\u036f]/g, '').toLowerCase()
const inRange = (v, [lo, hi]) => v >= lo && v <= hi
const isNarrowed = (range, full) => range[0] !== full[0] || range[1] !== full[1]

/** `ignoreType` serve per contare le carte di ogni tab senza considerare la tab attiva. */
export function matchesFilters(card, f, { ignoreType = false } = {}) {
  if (!ignoreType && f.type !== 'all' && card.type !== f.type) return false
  if (f.query) {
    const hay = norm(`${card.name} ${factionById[card.faction]?.label ?? ''} ${card.type === 'equipment' ? 'equipaggiamento' : ''}`)
    if (!hay.includes(norm(f.query.trim()))) return false
  }
  if (f.factions.length && !f.factions.includes(card.faction)) return false
  if (f.rarities.length && !f.rarities.includes(card.rarity)) return false
  if (!inRange(card.energy, f.energy)) return false
  // Gli equipaggiamenti non hanno attacco/difesa: spariscono solo se il filtro è stretto.
  if (isNarrowed(f.attack, STAT_RANGE) && !(card.attack != null && inRange(card.attack, f.attack))) return false
  if (isNarrowed(f.defense, STAT_RANGE) && !(card.defense != null && inRange(card.defense, f.defense))) return false
  return true
}

const factionOrder = Object.fromEntries(factions.map((f, i) => [f.id, i]))
const byOrder = (a, b) => a.order - b.order
// Valori mancanti (equipaggiamenti senza statistiche) sempre in fondo.
const desc = (key) => (a, b) => (b[key] ?? -1) - (a[key] ?? -1) || byOrder(a, b)

const sorters = {
  rarita:  (a, b) => rarityById[b.rarity].rank - rarityById[a.rarity].rank || byOrder(a, b), // dalla più rara
  nome:    (a, b) => a.name.localeCompare(b.name, 'it'),
  energia: (a, b) => a.energy - b.energy || rarityById[b.rarity].rank - rarityById[a.rarity].rank || byOrder(a, b),
  attacco: desc('attack'),
  difesa:  desc('defense'),
  fazione: (a, b) => (factionOrder[a.faction] ?? 99) - (factionOrder[b.faction] ?? 99) || byOrder(a, b),
}

export const sortCards = (cards, key) => [...cards].sort(sorters[key] ?? sorters.rarita)

/** Numeri di pagina da mostrare: tutti se pochi, altrimenti una finestra con puntini. */
export function pageWindow(current, total) {
  if (total <= 7) return Array.from({ length: total }, (_, i) => i + 1)
  const pages = new Set([1, total, current - 1, current, current + 1])
  const list = [...pages].filter((p) => p >= 1 && p <= total).sort((a, b) => a - b)
  return list.flatMap((p, i) => (i && p - list[i - 1] > 1 ? ['…', p] : [p]))
}
