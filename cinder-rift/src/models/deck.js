import { DECK_SIZE, MAX_POWER } from './constants.js'

/** Un mazzo è valido se ha 30 carte e non supera il budget di punti potenza. */
export const isDeckValid = (deck) => deck.cards === DECK_SIZE && deck.power <= MAX_POWER
