import { createContext, useContext, useState, useEffect, useCallback } from 'react'
import { login as loginRequest, logout as logoutRequest, getCurrentUser, USE_MOCK } from '../services/authService.js'
import { setUnauthorizedHandler } from '../services/api.js'

const AuthContext = createContext(null)
const STORAGE_KEY = 'cinder-rift-session'

function readSession() {
  try { return JSON.parse(sessionStorage.getItem(STORAGE_KEY)) } catch { return null }
}
function saveSession(session) {
  sessionStorage.setItem(STORAGE_KEY, JSON.stringify(session))
}

export function AuthProvider({ children }) {
  const [session, setSession] = useState(readSession)
  // true mentre si verifica col backend la sessione salvata (solo al ricaricamento della pagina)
  const [checking, setChecking] = useState(() => !USE_MOCK && !!readSession())

  const clear = useCallback(() => {
    sessionStorage.removeItem(STORAGE_KEY)
    setSession(null)
  }, [])

  const login = useCallback(async (credentials) => {
    const data = await loginRequest(credentials) // lancia ApiError se fallisce
    saveSession(data)
    setSession(data)
  }, [])

  const logout = useCallback(() => {
    logoutRequest(readSession()?.token) // avvisa il backend, senza aspettare la risposta
    clear()
  }, [clear])

  // Qualsiasi richiesta autenticata che riceve 401 riporta al login
  useEffect(() => {
    setUnauthorizedHandler(clear)
    return () => setUnauthorizedHandler(null)
  }, [clear])

  // Al ricaricamento: il token salvato è ancora valido? (GET /auth/me)
  useEffect(() => {
    const stored = readSession()
    if (!stored || USE_MOCK) return
    let active = true
    getCurrentUser(stored.token)
      .then((user) => {
        if (!active) return
        const next = { ...stored, user }
        saveSession(next)
        setSession(next)
      })
      .catch((err) => { if (active && err.status === 401) clear() }) // altri errori (rete): si resta dentro
      .finally(() => { if (active) setChecking(false) })
    return () => { active = false }
  }, [clear])

  return (
    <AuthContext.Provider value={{ user: session?.user ?? null, token: session?.token ?? null, checking, login, logout }}>
      {children}
    </AuthContext.Provider>
  )
}

export const useAuth = () => useContext(AuthContext)
