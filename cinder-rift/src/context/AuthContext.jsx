import { createContext, useContext, useState, useEffect, useCallback, useRef } from 'react'
import * as auth from '../services/authService.js'
import { ApiError, setSessionHandlers } from '../services/api.js'

const AuthContext = createContext(null)

export function AuthProvider({ children }) {
  const [session, setSession] = useState(null)
  const [checking, setChecking] = useState(true)
  const [sessionError, setSessionError] = useState(null)
  const current = useRef(null)
  const renewal = useRef(null)
  const sessionTokens = useRef(new Set())
  const generation = useRef(0)
  const closing = useRef(false)
  const store = useCallback((value) => { current.current = value; setSession(value) }, [])
  const clear = useCallback(() => { generation.current++; sessionTokens.current.clear(); store(null) }, [store])
  const renew = useCallback((rejectedToken) => {
    if (closing.current) return Promise.reject(new ApiError('Sessione terminata.', 401))
    if (rejectedToken && !sessionTokens.current.has(rejectedToken)) return Promise.reject(new ApiError('Sessione terminata.', 401))
    if (rejectedToken && current.current?.token && current.current.token !== rejectedToken) return Promise.resolve(current.current.token)
    if (!renewal.current) {
      const version = generation.current
      renewal.current = auth.renewSession().then((value) => {
        if (version !== generation.current || closing.current) throw new ApiError('Sessione terminata.', 401)
        sessionTokens.current.add(value.token)
        store(value)
        setSessionError(null)
        return value.token
      }).catch((error) => {
        if (error.status === 401 && version === generation.current) clear()
        throw error
      }).finally(() => { renewal.current = null })
    }
    return renewal.current
  }, [store, clear])

  const restore = useCallback(async () => {
    setChecking(true)
    setSessionError(null)
    try { await renew() } catch (error) { if (error.status !== 401) setSessionError(error) }
    finally { setChecking(false) }
  }, [renew])

  useEffect(() => {
    // Remove credentials left by the earlier prototype; new credentials stay in memory.
    try { sessionStorage.removeItem('cinder-rift-session') } catch { /* Storage may be disabled. */ }
    setSessionHandlers({ renew, clear })
    restore()
    return () => setSessionHandlers()
  }, [renew, clear, restore])

  useEffect(() => {
    if (!session) return
    const timer = setTimeout(() => renew().catch(setSessionError), Math.max(1000, session.expiresAt - Date.now() - 30000))
    return () => clearTimeout(timer)
  }, [session, renew])

  const authenticate = async (operation, values) => {
    generation.current++
    sessionTokens.current.clear()
    const value = await operation(values)
    sessionTokens.current.add(value.token)
    store(value)
    setSessionError(null)
  }
  const logout = async () => {
    closing.current = true
    clear()
    try { await auth.logout(); setSessionError(null) }
    catch (error) { setSessionError(new ApiError(`Sessione locale chiusa; uscita dal server non confermata. ${error.message}`, error.status)) }
    finally { closing.current = false }
  }
  return <AuthContext.Provider value={{ user: session?.user ?? null, token: session?.token ?? null, checking, sessionError, restore, renew, logout,
    login: (values) => authenticate(auth.login, values), register: (values) => authenticate(auth.register, values),
    updateUser: (user) => { if (current.current) store({ ...current.current, user }) },
  }}>{children}</AuthContext.Provider>
}

export const useAuth = () => useContext(AuthContext)
