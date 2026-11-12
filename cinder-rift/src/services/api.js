// Wrapper unico per le chiamate al backend con fetch API.
const BASE_URL = (import.meta.env.VITE_API_URL ?? 'http://localhost:3000/api').replace(/\/$/, '')
const TIMEOUT_MS = 10000

export class ApiError extends Error {
  constructor(message, status, data) {
    super(message)
    this.name = 'ApiError'
    this.status = status // 0 = nessuna risposta dal server
    this.data = data
  }
}

// Chiamato quando il backend risponde 401 a una richiesta autenticata (token scaduto o non valido).
let onUnauthorized = null
export const setUnauthorizedHandler = (fn) => { onUnauthorized = fn }

export async function apiFetch(path, { method = 'GET', body, token, headers = {}, signal } = {}) {
  const controller = new AbortController()
  const timer = setTimeout(() => controller.abort(), TIMEOUT_MS)
  signal?.addEventListener('abort', () => controller.abort())

  let res
  try {
    res = await fetch(`${BASE_URL}${path}`, {
      method,
      signal: controller.signal,
      headers: {
        'Content-Type': 'application/json',
        ...(token ? { Authorization: `Bearer ${token}` } : {}),
        ...headers,
      },
      body: body ? JSON.stringify(body) : undefined,
    })
  } catch (err) {
    throw new ApiError(
      err.name === 'AbortError' ? 'Il server non risponde. Riprova tra poco.' : 'Server non raggiungibile. Riprova più tardi.',
      0,
    )
  } finally {
    clearTimeout(timer)
  }

  const data = await res.json().catch(() => null) // 204 / corpo vuoto -> null
  if (!res.ok) {
    if (res.status === 401 && token) onUnauthorized?.()
    throw new ApiError(data?.message || `Errore ${res.status}`, res.status, data)
  }
  return data
}
