import { apiFetch, ApiError } from './api.js'
import { ENDPOINTS } from './endpoints.js'
import { user as mockUser } from '../data/user.js'

// true = nessuna chiamata di rete (login finto per sviluppare la grafica). Default: chiamata vera al backend.
export const USE_MOCK = import.meta.env.VITE_USE_MOCK === 'true'

const DEFAULT_AVATAR = '/img/avatar-cyborg.jpg'

/** Unico punto in cui i campi del backend diventano i campi usati dall'interfaccia. */
export function normalizeUser(raw = {}) {
  const safeRaw = raw ?? {}
  const name = safeRaw.name ?? safeRaw.nickname ?? safeRaw.username ?? 'Giocatore'
  const credits = safeRaw.credits ?? safeRaw.rankingPoints ?? 0
  const avatar = safeRaw.avatar ?? safeRaw.iconUrl ?? DEFAULT_AVATAR

  return {
    id: safeRaw.id ?? null,
    name,
    email: safeRaw.email ?? null,
    role: safeRaw.role ?? 'USER',
    level: safeRaw.level ?? 1,
    credits,
    avatar,
  }
}

/**
 * POST /api/auth/login   body: { email, password }
 *   200 -> { accessToken: string, user: { ... } }
 *   400 -> { message }   campi mancanti
 *   401 -> { message: 'Credenziali non valide' }
 */
export async function login({ email, password }) {
  const normalizedEmail = (email ?? '').trim()

  if (USE_MOCK) {
    await new Promise((r) => setTimeout(r, 500))
    if (!normalizedEmail || password.length < 4) throw new ApiError('Credenziali non valide', 401)
    return { token: 'mock-token', user: { ...mockUser, name: normalizedEmail.split('@')[0], email: normalizedEmail } }
  }

  const data = await apiFetch(ENDPOINTS.auth.login, { method: 'POST', body: { email: normalizedEmail, password } })
  const token = data?.accessToken ?? data?.token
  if (!token) throw new ApiError('Risposta del server non valida.', 502, data)
  return { token, user: normalizeUser(data.user) }
}

/** GET /api/auth/user/me (Bearer) -> { user }. Serve a verificare che la sessione salvata sia ancora valida. */
export async function getCurrentUser(token) {
  const data = await apiFetch(ENDPOINTS.auth.me, { token })
  return normalizeUser(data?.user ?? data)
}

/** POST /api/auth/logout (Bearer) -> 204. Best effort: il logout locale avviene comunque. */
export async function logout(token) {
  if (USE_MOCK || !token) return
  try {
    await apiFetch(ENDPOINTS.auth.logout, { method: 'POST', token })
  } catch {
    /* ignorato: la sessione locale viene chiusa in ogni caso */
  }
}
