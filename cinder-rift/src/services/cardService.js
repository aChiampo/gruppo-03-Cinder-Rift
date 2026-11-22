import { apiFetch, ApiError } from './api.js'
import { ENDPOINTS } from './endpoints.js'
import { defaultFilters, PAGE_SIZE } from '../models/cards.js'

/** Translate the UI's one-based page to F04 query parameters. Empty bounds stay unfiltered. */
export function buildCollectionParams({ filters = defaultFilters, sort = 'name', direction = 'asc', page = 1, pageSize = PAGE_SIZE } = {}) {
  const params = { sort, direction, page: page - 1, size: pageSize }
  for (const key of Object.keys(defaultFilters)) {
    const value = typeof filters[key] === 'string' ? filters[key].trim() : filters[key]
    if (value !== '' && value !== undefined && value !== null) params[key] = value
  }
  return params
}

const positiveId = (value) => Number.isInteger(value) && value > 0
const count = (value) => Number.isSafeInteger(value) && value >= 0
const text = (value) => value === null || typeof value === 'string'
const reference = (value) => value === null || (positiveId(value?.id) && text(value.code) && text(value.name))
const artwork = (value) => positiveId(value?.cardArtId) && typeof value.artCode === 'string' && text(value.imageUrl)

/** Validate the canonical CardPage without changing its fields, references, variants or counts. */
function cardPage(data, catalog) {
  const valid = data && Array.isArray(data.items) && count(data.page) && positiveId(data.size) && data.size <= 100 &&
    count(data.totalElements) && count(data.totalPages) && data.totalPages === Math.ceil(data.totalElements / data.size) &&
    data.items.length <= data.size && data.items.length <= data.totalElements && data.items.every((item) => {
      if (!item || !text(item.name) || !['faction', 'rarity', 'type', 'expansion'].every((key) => reference(item[key])) ||
        !['energyCost', 'attack', 'defense', 'power'].every((key) => item[key] === null || count(item[key]))) return false
      return catalog
        ? positiveId(item.id) && Array.isArray(item.artworks) && item.artworks.every(artwork)
        : positiveId(item.cardId) && artwork(item) && positiveId(item.ownedReplica) && count(item.tradableReplica) && item.tradableReplica <= item.ownedReplica
    })
  if (!valid) throw new ApiError('Risposta carte del server non valida.', 502)
  return data
}

async function readPage(path, params, options, catalog) {
  const query = new URLSearchParams(params).toString()
  return cardPage(await apiFetch(`${path}${query ? `?${query}` : ''}`, options), catalog)
}

/** GET /api/cards, authenticated through the shared F01 client. */
export const getCatalog = (params = {}, options = {}) => readPage(ENDPOINTS.cards, params, options, true)
/** GET /api/users/me/collections; the owner is always determined by the Bearer token. */
export const getCollection = (params = {}, options = {}) => readPage(ENDPOINTS.collection, params, options, false)
