import { apiFetch, ApiError } from './api.js'
import { ENDPOINTS } from './endpoints.js'

export const DESTINATIONS = ['ROUND1', 'ROUND2', 'ROUND3', 'ROUND4', 'ROUND5', 'EQUIPMENT']
export function positiveId(value) {
  const number = Number(value)
  if (!Number.isInteger(number) || number < 1 || number > 2147483647) throw new ApiError('Inserisci un identificativo intero positivo valido.', 400)
  return number
}
/** Only request fields from F05 are sent; server-generated entryId is omitted. */
export function deckPayload(entries, isChallengeable = false) {
  return { entries: entries.map(({ cardId, cardArtId, destination }) => {
    if (!DESTINATIONS.includes(destination)) throw new ApiError('Destinazione non valida.', 400)
    return { cardId: positiveId(cardId), cardArtId: positiveId(cardArtId), destination }
  }), isChallengeable }
}
function details(data) {
  if (!Number.isInteger(data?.id) || !Array.isArray(data.entries) || !data.validation || !Array.isArray(data.validation.errors) || !Array.isArray(data.validation.warnings)) throw new ApiError('Risposta mazzo non valida.', 502)
  return data
}
export async function listDecks(token, signal) {
  const data = await apiFetch(ENDPOINTS.decks, { token, signal })
  if (!Array.isArray(data) || data.some((d) => !Number.isInteger(d.id) || !Number.isInteger(d.cardCount) || !d.validation)) throw new ApiError('Elenco mazzi non valido.', 502)
  return data
}
export const getDeck = async (id, token, signal) => details(await apiFetch(ENDPOINTS.deck(positiveId(id)), { token, signal }))
export const createDeck = async (token) => details(await apiFetch(ENDPOINTS.decks, { token, method: 'POST', body: { entries: [] } }))
export const saveDeck = async (id, entries, isChallengeable, token) => details(await apiFetch(ENDPOINTS.deck(positiveId(id)), { method: 'PUT', token, body: deckPayload(entries, isChallengeable) }))
export const deleteDeck = (id, token) => apiFetch(ENDPOINTS.deck(positiveId(id)), { method: 'DELETE', token })
