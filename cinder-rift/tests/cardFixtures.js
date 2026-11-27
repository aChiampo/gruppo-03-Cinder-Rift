// Canonical F04 payloads, confined to tests.
export const catalogCard = {
  id: 101, name: 'Ash & Fire',
  faction: { id: 1, code: 'SAP', name: 'Sapiens' },
  rarity: { id: 1, code: 'COMMON', name: 'Common' },
  type: { id: 1, code: 'CREATURE', name: null },
  expansion: { id: 1, code: 'CR01', name: 'Test expansion' },
  energyCost: 0, attack: 3, defense: null, power: 2,
  artworks: [
    { cardArtId: 201, artCode: 'STANDARD', imageUrl: null },
    { cardArtId: 202, artCode: 'ALT', imageUrl: 'https://example.invalid/alt.png' },
  ],
}
const { id, artworks, ...details } = catalogCard
export const collectionCard = { ...details, cardId: id, ...artworks[0], ownedReplica: 3, tradableReplica: 1 }
export const pageOf = (items, extra = {}) => ({ items, page: 0, size: 15, totalElements: items.length, totalPages: items.length ? 1 : 0, ...extra })
