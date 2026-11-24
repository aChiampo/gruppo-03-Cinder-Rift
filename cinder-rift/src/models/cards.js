import { factionById, NEUTRAL } from '../data/factions.js'

export const CARD_FACTIONS = [
  { code: 'CYB', ...factionById.cyborg },
  { code: 'RAD', ...factionById.radioattivi },
  { code: 'SAP', ...factionById.sapiens },
  { code: 'CLT', ...factionById.cultisti },
  { code: 'ALN', ...factionById.alieni },
]
export const RARITIES = [
  { code: 'COMMON', label: 'Comune', color: '#b6c2d0' },
  { code: 'RARE', label: 'Rara', color: '#3d9bff' },
  { code: 'EPIC', label: 'Epica', color: '#b34dff' },
  { code: 'LEGENDARY', label: 'Leggendaria', color: '#ffb629' },
]
export const PAGE_SIZE = 15
export const defaultFilters = {
  name: '', type: '', faction: '', rarity: '', expansion: '',
  energyMin: '', energyMax: '', attackMin: '', attackMax: '',
  defenseMin: '', defenseMax: '', powerMin: '', powerMax: '',
}
export const SORT_OPTIONS = [
  { id: 'name', label: 'Nome' }, { id: 'rarity', label: 'Rarità' },
  { id: 'energy', label: 'Energia' }, { id: 'attack', label: 'Attacco' },
  { id: 'defense', label: 'Difesa' }, { id: 'faction', label: 'Fazione' },
  { id: 'power', label: 'Punti potenza' },
]

export const referenceLabel = (value) => value?.name || value?.code || 'Non disponibile'
export const cardKey = (card) => card.cardId !== undefined ? `${card.cardId}:${card.cardArtId}` : String(card.id)
export const cardArtworks = (card) => card.artworks ?? [{ cardArtId: card.cardArtId, artCode: card.artCode, imageUrl: card.imageUrl }]

/** Known codes supply presentation only; unknown/null references keep their server label. */
export function cardAppearance(card) {
  const faction = CARD_FACTIONS.find((entry) => entry.code === card.faction?.code)
  const rarity = RARITIES.find((entry) => entry.code === card.rarity?.code)
  return {
    color: faction?.color ?? NEUTRAL.color,
    factionId: faction?.id,
    factionLabel: faction?.label ?? referenceLabel(card.faction),
    rarityLabel: rarity?.label ?? referenceLabel(card.rarity),
    rarityColor: rarity?.color ?? NEUTRAL.color,
  }
}

/** Compact one-based page navigation. */
export function pageWindow(current, total) {
  if (total <= 7) return Array.from({ length: total }, (_, i) => i + 1)
  const pages = new Set([1, total, current - 1, current, current + 1])
  const list = [...pages].filter((p) => p >= 1 && p <= total).sort((a, b) => a - b)
  return list.flatMap((p, i) => i && p - list[i - 1] > 1 ? ['…', p] : [p])
}
