import { apiFetch, ApiError } from './api.js'
import { ENDPOINTS } from './endpoints.js'

let authQueue = Promise.resolve()
function serialize(operation) {
  const result = authQueue.then(operation, operation)
  authQueue = result.catch(() => {})
  return result
}

async function csrfWrite(path, body) {
  const csrf = await apiFetch(ENDPOINTS.auth.csrfTokens)
  if (typeof csrf?.headerName !== 'string' || typeof csrf?.token !== 'string' || !csrf.token) throw new ApiError('Token CSRF non disponibile.', 502)
  return apiFetch(path, { method: 'POST', body, headers: { [csrf.headerName]: csrf.token } })
}

export function normalizeUser(raw) {
  if (!Number.isInteger(raw?.id) || !raw.nickname || !['USER', 'ADMIN'].includes(raw.role)) throw new ApiError('Profilo del server non valido.', 502)
  return { ...raw, name: raw.nickname, avatar: raw.iconUrl || null }
}

function normalizeSession(data) {
  if (!data?.accessToken || !Number.isFinite(data.expiresIn) || data.expiresIn <= 0) throw new ApiError('Sessione del server non valida.', 502)
  return { token: data.accessToken, user: normalizeUser(data.user), expiresAt: Date.now() + data.expiresIn * 1000 }
}

export const login = ({ email, password }) => serialize(async () => normalizeSession(await csrfWrite(ENDPOINTS.auth.login, { email: email.trim(), password })))
export const register = ({ nickname, email, password }) => serialize(async () => normalizeSession(await csrfWrite(ENDPOINTS.auth.signin, { nickname: nickname.trim(), email: email.trim(), password })))
export const renewSession = () => serialize(async () => normalizeSession(await csrfWrite(ENDPOINTS.auth.renewal)))
export const logout = () => serialize(() => csrfWrite(ENDPOINTS.auth.logout))
export const getCurrentUser = async (token) => normalizeUser(await apiFetch(ENDPOINTS.auth.me, { token }))
export const updateProfile = async (body, token) => normalizeUser(await apiFetch(ENDPOINTS.profile, { method: 'PATCH', body, token }))
