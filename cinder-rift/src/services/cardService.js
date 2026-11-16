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
const SORT_PARAM = { rarita: 'rarity', nome: 'name', energia: 'energy', attacco: 'attack', difesa: 'defense', fazione: 'faction' }

const strip = (v) => String(v ?? '').normalize('NFD').replace(/[\u0300-\u036f]/g, '').toLowerCase().trim()
const num = (v, fallback = null) => (v === null || v === undefined || v === '' || Number.isNaN(Number(v)) ? fallback : Number(v))

const normalizeCode = (value) => {
  if (value === null || value === undefined || value === '') return null
  if (typeof value === 'object') return normalizeCode(value.code ?? value.name ?? value.id)
  return String(value).trim()
}

const normalizeType = (value) => {
  const code = normalizeCode(value)
  if (!code) return null
  const normalized = strip(code)
  const map = { creature: 'creature', equipment: 'equipment', equipaggiamento: 'equipment' }
  return map[normalized] ?? null
}

const normalizeFaction = (value) => {
  const code = normalizeCode(value)
  if (!code) return null
  const normalized = strip(code)
  const map = {
    cyborg: 'cyborg',
    sapiens: 'sapiens',
    cultisti: 'cultisti',
    alieni: 'alieni',
    radioattivi: 'radioattivi',
    radioattivo: 'radioattivi',
    monsters: 'radioattivi',
  }
  return map[normalized] ?? normalized
}

const normalizeRarity = (value) => {
  const code = normalizeCode(value)
  if (!code) return null
  const normalized = strip(code)
  const map = {
    comune: 'comune', common: 'comune',
    rare: 'rara', raro: 'rara', rar: 'rara', uncommon: 'rara',
    epic: 'epica', epica: 'epica',
    legendary: 'leggendaria', leggenda: 'leggendaria', leggendaria: 'leggendaria',
  }
  return map[normalized] ?? normalized
}

const normalizeTypeForRequest = (value) => {
  if (!value) return undefined
  const normalized = String(value).trim().toLowerCase()
  if (normalized === 'creature' || normalized === 'creatura') return 'CREATURE'
  if (normalized === 'equipment' || normalized === 'equipaggiamento') return 'EQUIPMENT'
  return undefined
}

const normalizeFactionForRequest = (value) => {
  if (!value) return undefined
  const normalized = String(value).trim().toLowerCase()
  const map = { cyborg: 'CYBORG', sapiens: 'SAPIENS', cultisti: 'CULTISTS', alieni: 'ALIENS', radioattivi: 'RADIOATTIVI', radioattivo: 'RADIOATTIVI' }
  return map[normalized] ?? normalized.toUpperCase()
}

const normalizeRarityForRequest = (value) => {
  if (!value) return undefined
  const normalized = String(value).trim().toLowerCase()
  const map = { comune: 'COMMON', rara: 'RARE', epica: 'EPIC', leggendaria: 'LEGENDARY' }
  return map[normalized] ?? normalized.toUpperCase()
}

/**
 * Unico punto in cui i campi della carta del backend diventano i campi usati dall'interfaccia.
 * Supporta sia il contratto old-style (campi flat) sia il nuovo contratto F04 (reference objects e code).
 */
export function normalizeCard(raw = {}) {
  const type = normalizeType(raw.type?.code ?? raw.type ?? raw.typeCode ?? raw.cardType)
  const rarity = normalizeRarity(raw.rarity?.code ?? raw.rarity ?? raw.rarityCode)
  const faction = raw.faction == null ? null : normalizeFaction(raw.faction?.code ?? raw.faction ?? raw.factionCode)
  const energy = num(raw.energyCost ?? raw.energy ?? raw.cost)
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
    power: num(raw.power ?? raw.powerPoints, 0),
    attack: isCreature ? num(raw.attack ?? raw.atk ?? raw.attackPoints, 0) : null,
    defense: isCreature ? num(raw.defense ?? raw.def ?? raw.defensePoints, 0) : null,
    flavor: raw.flavor ?? raw.flavorText ?? '',
    lore: raw.lore ?? null,
    effect: raw.effect ?? '',
    duration: raw.duration ?? 'Round',
    accent: raw.accentColor ?? rarityById[rarity].color,
    art: {
      src: raw.imageUrl ?? raw.image ?? raw.artwork?.imageUrl ?? null,
      ...(raw.imageCrop ?? {}),
      icon: raw.icon ?? raw.artwork?.icon ?? null,
    },
  }
}

/** Filtri + ordinamento + pagina dell'interfaccia -> parametri della richiesta. Gli intervalli partono solo se stretti. */
export function buildCollectionParams({ filters, sort, page, pageSize = PAGE_SIZE }) {
  const p = {}
  if (filters.type && filters.type !== 'all') p.type = normalizeTypeForRequest(filters.type)
  if (filters.factions.length) p.faction = filters.factions.map(normalizeFactionForRequest).join(',')
  if (filters.rarities.length) p.rarity = filters.rarities.map(normalizeRarityForRequest).join(',')
  if (isNarrowed(filters.energy, ENERGY_RANGE)) Object.assign(p, { energyMin: filters.energy[0], energyMax: filters.energy[1] })
  if (isNarrowed(filters.attack, STAT_RANGE)) Object.assign(p, { attackMin: filters.attack[0], attackMax: filters.attack[1] })
  if (isNarrowed(filters.defense, STAT_RANGE)) Object.assign(p, { defenseMin: filters.defense[0], defenseMax: filters.defense[1] })
  if (filters.query.trim()) p.name = filters.query.trim()
  const zeroBasedPage = Math.max(0, Number(page) - 1)
  const serverSort = SORT_PARAM[sort] ?? 'name'
  const direction = (serverSort === 'name' || serverSort === 'rarity' || serverSort === 'faction') ? 'asc' : 'desc'
  return { ...p, sort: serverSort, direction, page: zeroBasedPage, size: pageSize }
}

/**
 * GET /api/users/me/collections (Bearer) -> { items, page, size, totalElements, totalPages, counts? }
 * Restituisce { items (normalizzate), total, page, pageCount, counts | null }.
 * Lancia ApiError: status 0 = server non raggiungibile, 401 = sessione scaduta (gestita da api.js), 5xx/502 = errore del server.
 */
export async function getCollection(params, { token, signal } = {}) {
  const query = Object.fromEntries(Object.entries(params).map(([k, v]) => [k, String(v)]))
  let data

  if (USE_MOCK) {
    const { collectionFor, queryCards } = await import('../mocks/cardsMock.js')
    await new Promise((r) => setTimeout(r, 300))
    try { data = queryCards(collectionFor(1), query) } catch (err) { throw new ApiError(err.message, err.status ?? 400) }
  } else {
    data = await apiFetch(`${ENDPOINTS.cards.collection}?${new URLSearchParams(query)}`, { token, signal })
  }

  if (!Array.isArray(data?.items)) throw new ApiError('Risposta del server non valida.', 502, data)
  const items = data.items.map(normalizeCard).filter(Boolean)
  const total = num(data.totalElements ?? data.total, items.length)
  const size = num(data.size ?? data.pageSize, params.size) || PAGE_SIZE
  const page = num(data.page, params.page ?? 0) || 0
  return {
    items,
    total,
    page: page + 1,
    pageCount: Math.max(1, Math.ceil(total / size)),
    counts: data.counts ? { all: num(data.counts.all, 0), creature: num(data.counts.creature, 0), equipment: num(data.counts.equipment, 0) } : null,
  }
}
