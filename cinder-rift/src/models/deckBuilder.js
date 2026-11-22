import { cardKey } from './cards.js'

export const SLOTS_PER_GROUP = 5
export const CARD_DRAG_TYPE = 'application/x-cinder-rift-card'
export const destinationType = (destination) => destination === 'EQUIPMENT' ? 'EQUIPMENT' : 'CREATURE'

/** Equipment is a separate group; numbered rounds require a known, nonnegative energy cost. */
export function cardFitsDestination(card, destination) {
  if (!card || card.type?.code !== destinationType(destination)) return false
  if (destination === 'EQUIPMENT') return true
  const round = /^ROUND([1-5])$/.exec(destination)
  return Boolean(round) && Number.isInteger(card.energyCost) && card.energyCost >= 0 && card.energyCost <= Number(round[1])
}

/** Slot positions are local editing state; F05 persists only copies and destinations. */
export function assignSlots(entries) {
  const next = {}
  return entries.map((entry) => ({ ...entry, slot: (next[entry.destination] = (next[entry.destination] ?? -1) + 1) }))
}

export const availableCopies = (card, entries) => Math.max(0, card.ownedReplica - entries.filter((entry) => cardKey(entry) === cardKey(card)).length)
export const firstEmptySlot = (entries, destination) => Array.from({ length: SLOTS_PER_GROUP }, (_, slot) => slot)
  .find((slot) => !entries.some((entry) => entry.destination === destination && entry.slot === slot))

/** Validate both click placement and targeted drops against the current draft. */
export function placeCard(entries, card, destination, slot = firstEmptySlot(entries, destination)) {
  if (!card || card.type?.code !== destinationType(destination)) throw new Error('Scegli una carta del tipo corretto per questo gruppo.')
  if (!cardFitsDestination(card, destination)) throw new Error('Scegli una creatura con energia disponibile e non superiore al numero del round.')
  if (!Number.isInteger(slot) || slot < 0 || slot >= SLOTS_PER_GROUP) throw new Error('Questo gruppo è completo: rimuovi una carta prima di aggiungerne un’altra.')
  if (entries.some((entry) => entry.destination === destination && entry.slot === slot)) throw new Error('Questo slot è già occupato. Scegli uno slot vuoto.')
  if (availableCopies(card, entries) < 1) throw new Error('Hai già assegnato tutte le copie possedute di questa variante.')
  return [...entries, { cardId: card.cardId, cardArtId: card.cardArtId, destination, slot }]
}
