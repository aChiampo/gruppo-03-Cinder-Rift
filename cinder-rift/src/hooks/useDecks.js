import { useCallback, useEffect, useState } from 'react'
import { listDecks } from '../services/deckService.js'
import { useAuth } from '../context/AuthContext.jsx'
export function useDecks() {
  const { token } = useAuth()
  const [revision, setRevision] = useState(0)
  const [result, setResult] = useState({ status: 'loading', data: null, error: null })
  const retry = useCallback(() => setRevision((n) => n + 1), [])
  useEffect(() => {
    const controller = new AbortController()
    setResult((r) => ({ ...r, status: 'loading', error: null }))
    listDecks(token, controller.signal).then((data) => {
      if (!controller.signal.aborted) setResult({ status: 'ready', data, error: null })
    }).catch((error) => { if (!controller.signal.aborted) setResult((r) => ({ ...r, status: 'error', error })) })
    return () => controller.abort()
  }, [token, revision])
  return { ...result, retry }
}
