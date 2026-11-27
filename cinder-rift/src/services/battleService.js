import { apiFetch, ApiError } from './api.js'
import { ENDPOINTS } from './endpoints.js'
import { positiveId } from './deckService.js'
export async function playPve(stageId, deckId, token) {
  if (!stageId.trim()) throw new ApiError('Inserisci lo stage della campagna.', 400)
  const data = await apiFetch(ENDPOINTS.pve(stageId.trim()), { method: 'POST', token, body: { deckId: positiveId(deckId) } })
  if (!['VICTORY', 'DEFEAT', 'DRAW'].includes(data?.outcome) || !Array.isArray(data.battleLog) || data.battleLog.some((line) => typeof line !== 'string') || !data.challenger?.displayName || typeof data.enemyName !== 'string' || typeof data.stageTitle !== 'string') throw new ApiError('Risposta battaglia non valida.', 502)
  return data
}
