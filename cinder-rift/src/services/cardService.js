import { apiFetch, ApiError } from './api.js'
import { ENDPOINTS } from './endpoints.js'
import { USE_MOCK } from './authService.js'
import { ENERGY_RANGE, STAT_RANGE, PAGE_SIZE, isNarrowed, rarityById } from '../models/cards.js'
import { factionById } from '../data/factions.js'
import { allCards, mockCards } from '../data/mockCards.js'

const rarityOrder = ['Comune', 'Rara', 'Epica', 'Leggendaria']

export const getAllCards = async () => mockCards

export const getCardById = async (id) => allCards.find((card) => card.id === id) ?? null

export const getCreatures = async () => mockCards.creatures

export const getEquipments = async () => mockCards.equipments

export const getCardsByFaction = async (faction) => {
  if (!faction || faction === 'Tutte') return allCards
  return allCards.filter((card) => card.faction === faction)
}

export const getCardsByRarity = async (rarity) => {
  if (!rarity || rarity === 'Tutte') return allCards
  return allCards.filter((card) => card.rarity === rarity)
}

export const getCardsByType = (type) => {
  if (type === 'creature') return [...mockCards.creatures]
  if (type === 'equipment') return [...mockCards.equipments]
  return [...allCards]
}

export const sortCards = (cards) => [...cards].sort((a, b) => {
  const rarityDiff = rarityOrder.indexOf(a.rarity) - rarityOrder.indexOf(b.rarity)
  if (rarityDiff !== 0) return rarityDiff
  return a.name.localeCompare(b.name)
})

// ===================================================================================================
// COLLEZIONE DEL GIOCATORE — GET /cards  (contratto: docs/API-COLLEZIONE.md)
// Le carte arrivano già filtrate, ordinate e paginate dal backend. Il deck builder (sopra) usa ancora i dati di esempio.
// ===================================================================================================

// Valore del parametro `sort` per ogni ordinamento dell'interfaccia. Se il backend usa altri nomi si cambia solo qui.
const SORT_PARAM = { rarita: 'rarita', nome: 'nome', energia: 'energia', attacco: 'attacco', difesa: 'difesa', fazione: 'fazione' }

const strip = (v) => String(v ?? '').normalize('NFD').replace(/[\u0300-\u036f]/g, '').toLowerCase().trim()
const num = (v, fallback = null) => (v === null || v === undefined || v === '' || Number.isNaN(Number(v)) ? fallback : Number(v))
const TYPES = { creature: 'creature', creatura: 'creature', equipment: 'equipment', equipaggiamento: 'equipment' }

/**
 * Unico punto in cui i campi della carta del backend diventano i campi usati dall'interfaccia
 * (come normalizeUser per l'utente). Accetta qualche sinonimo; scarta (con un avviso in console)
 * le carte a cui manca un campo indispensabile, così una carta malformata non rompe la pagina.
 */
export function normalizeCard(raw = {}) {
  const type = TYPES[strip(raw.type)]
  const rarity = strip(raw.rarity)
  const faction = raw.faction == null ? null : strip(raw.faction)
  const energy = num(raw.energy ?? raw.cost)
  const valid = raw.id != null && raw.name && type && rarityById[rarity] && energy != null && (type === 'equipment' || factionById[faction])
  if (!valid) {
    console.warn('[collezione] carta scartata: campi mancanti o non validi', raw)
    return null
  }
  const isCreature = type === 'creature'
  return {
    id: String(raw.id),
    type,
    name: String(raw.name),
    rarity,
    faction: isCreature ? faction : null,
    energy,
    power: num(raw.power, 0),
    attack: isCreature ? num(raw.attack ?? raw.atk, 0) : null,    // gli equipaggiamenti non hanno attacco/difesa
    defense: isCreature ? num(raw.defense ?? raw.def, 0) : null,
    flavor: raw.flavor ?? raw.flavorText ?? '',
    lore: raw.lore ?? null,
    effect: raw.effect ?? '',
    duration: raw.duration ?? 'Round',
    accent: raw.accentColor ?? rarityById[rarity].color,
    // imageUrl assoluto (http…) o relativo alla cartella public/ del frontend. imageCrop/icon: opzionali, solo per il mock.
    art: { src: raw.imageUrl ?? raw.image ?? null, ...(raw.imageCrop ?? {}), icon: raw.icon },
  }
}

/** Filtri + ordinamento + pagina dell'interfaccia -> parametri della richiesta. Gli intervalli partono solo se stretti. */
export function buildCollectionParams({ filters, sort, page, pageSize = PAGE_SIZE }) {
  const p = {}
  if (filters.type !== 'all') p.type = filters.type
  if (filters.factions.length) p.faction = filters.factions.join(',')
  if (filters.rarities.length) p.rarity = filters.rarities.join(',')
  if (isNarrowed(filters.energy, ENERGY_RANGE)) Object.assign(p, { energyMin: filters.energy[0], energyMax: filters.energy[1] })
  if (isNarrowed(filters.attack, STAT_RANGE)) Object.assign(p, { attackMin: filters.attack[0], attackMax: filters.attack[1] })
  if (isNarrowed(filters.defense, STAT_RANGE)) Object.assign(p, { defenseMin: filters.defense[0], defenseMax: filters.defense[1] })
  if (filters.query.trim()) p.q = filters.query.trim()
  return { ...p, sort: SORT_PARAM[sort] ?? sort, page, pageSize }
}

/**
 * GET /cards (Bearer) -> { items, page, pageSize, total, counts? }
 * Restituisce { items (normalizzate), total, page, pageCount, counts | null }.
 * Lancia ApiError: status 0 = server non raggiungibile, 401 = sessione scaduta (gestita da api.js), 5xx/502 = errore del server.
 */
export async function getCollection(params, { token, signal } = {}) {
  const query = Object.fromEntries(Object.entries(params).map(([k, v]) => [k, String(v)]))
  let data

  if (USE_MOCK) {
    // Solo con VITE_USE_MOCK=true: dati dimostrativi caricati al volo (non finiscono nel bundle normale).
    const { collectionFor, queryCards } = await import('../mocks/cardsMock.js')
    await new Promise((r) => setTimeout(r, 300)) // latenza finta
    try { data = queryCards(collectionFor(1), query) } catch (err) { throw new ApiError(err.message, err.status ?? 400) }
  } else {
    data = await apiFetch(`${ENDPOINTS.cards.collection}?${new URLSearchParams(query)}`, { token, signal })
  }

  if (!Array.isArray(data?.items)) throw new ApiError('Risposta del server non valida.', 502, data)
  const items = data.items.map(normalizeCard).filter(Boolean)
  const total = num(data.total, items.length)
  const pageSize = num(data.pageSize, params.pageSize) || PAGE_SIZE
  return {
    items,
    total,
    page: num(data.page, params.page),
    pageCount: Math.max(1, Math.ceil(total / pageSize)),
    // `counts` è consigliato (numeri sulle tab); se il backend non lo manda le tab mostrano solo il nome.
    counts: data.counts ? { all: num(data.counts.all, 0), creature: num(data.counts.creature, 0), equipment: num(data.counts.equipment, 0) } : null,
  }
}
