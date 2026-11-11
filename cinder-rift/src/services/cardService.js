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
