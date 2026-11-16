import { apiFetch, ApiError } from './api.js'
import { ENDPOINTS } from './endpoints.js'
import { user as mockUser } from '../data/user.js'

// true = nessuna chiamata di rete (login finto per sviluppare la grafica). Default: chiamata vera al backend.
export const USE_MOCK = import.meta.env.VITE_USE_MOCK === 'true'

const DEFAULT_AVATAR = '/img/avatar-cyborg.jpg'

/** Unico punto in cui i campi del backend diventano i campi usati dall'interfaccia. */
export function normalizeUser(raw = {}) {
  return {
    id: raw.id ?? null,
    name: raw.name ?? raw.username ?? 'Giocatore',
    level: raw.level ?? 1,
    credits: raw.credits ?? 0,
    avatar: raw.avatar ?? DEFAULT_AVATAR,
  }
}

/**
 * POST /auth/login   body: { identifier, password }
 *   200 -> { token: string, user: { id, name, level, credits, avatar } }
 *   400 -> { message }   campi mancanti
 *   401 -> { message: 'Credenziali non valide' }
 */
export async function login({ identifier, password }) {
  if (USE_MOCK) {
    await new Promise((r) => setTimeout(r, 500))
    if (password.length < 4) throw new ApiError('Credenziali non valide', 401)
    return { token: 'mock-token', user: { ...mockUser, name: identifier.split('@')[0] } }
  }
  const data = await apiFetch(ENDPOINTS.auth.login, { method: 'POST', body: { identifier, password } })
  if (!data?.token) throw new ApiError('Risposta del server non valida.', 502, data)
  return { token: data.token, user: normalizeUser(data.user) }
}

/** GET /auth/me (Bearer) -> { user }. Serve a verificare che la sessione salvata sia ancora valida. */
export async function getCurrentUser(token) {
  const data = await apiFetch(ENDPOINTS.auth.me, { token })
  return normalizeUser(data?.user ?? data)
}

/** POST /auth/logout (Bearer) -> 204. Best effort: il logout locale avviene comunque. */
export async function logout(token) {
  if (USE_MOCK || !token) return
  try {
    await apiFetch(ENDPOINTS.auth.logout, { method: 'POST', token })
  } catch {
    /* ignorato: la sessione locale viene chiusa in ogni caso */
  }
}
