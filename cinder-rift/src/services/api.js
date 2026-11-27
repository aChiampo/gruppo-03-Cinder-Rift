const BASE_URL = (import.meta.env?.VITE_API_URL || '/api').replace(/\/$/, '')
const TIMEOUT_MS = 10000

export class ApiError extends Error {
  constructor(message, status = 0, data = null) {
    super(message)
    this.name = 'ApiError'
    this.status = status
    this.data = data
  }
}

let sessionHandlers = {}
export const setSessionHandlers = (handlers = {}) => { sessionHandlers = handlers }

/** Sends a documented request and retries a rejected Bearer once after renewal. */
export async function apiFetch(path, { method = 'GET', body, token, headers = {}, signal, retry = true } = {}) {
  const controller = new AbortController()
  const abort = () => controller.abort()
  if (signal?.aborted) abort()
  signal?.addEventListener('abort', abort, { once: true })
  const timer = setTimeout(abort, TIMEOUT_MS)
  let res, data
  try {
    res = await fetch(`${BASE_URL}${path}`, {
      method, signal: controller.signal, credentials: 'include',
      headers: { Accept: 'application/json, application/problem+json', ...(body !== undefined ? { 'Content-Type': 'application/json' } : {}), ...(token ? { Authorization: `Bearer ${token}` } : {}), ...headers },
      body: body !== undefined ? JSON.stringify(body) : undefined,
    })
    const content = await res.text()
    try { data = content ? JSON.parse(content) : null } catch { data = null }
    if (res.ok && res.status !== 204 && data === null) throw new ApiError('Risposta del server non valida.', 502)
  } catch (error) {
    if (signal?.aborted) throw new DOMException('Request cancelled', 'AbortError')
    if (error instanceof ApiError) throw error
    throw new ApiError(controller.signal.aborted ? 'Tempo di attesa esaurito. Verifica lo stato prima di riprovare.' : 'Server non raggiungibile. Riprova più tardi.')
  } finally {
    clearTimeout(timer)
    signal?.removeEventListener('abort', abort)
  }
  if (res.status === 401 && token && retry && sessionHandlers.renew) {
    const nextToken = await sessionHandlers.renew(token)
    if (signal?.aborted) throw new DOMException('Request cancelled', 'AbortError')
    return apiFetch(path, { method, body, token: nextToken, headers, signal, retry: false })
  }
  if (!res.ok) {
    if (res.status === 401 && token) sessionHandlers.clear?.()
    throw new ApiError(data?.detail || data?.message || data?.title || `Richiesta non riuscita (HTTP ${res.status}).`, res.status, data)
  }
  return data
}
