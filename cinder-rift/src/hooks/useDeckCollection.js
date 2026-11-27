import { useEffect, useState } from 'react'
import { useAuth } from '../context/AuthContext.jsx'
import { getDeckCollection } from '../services/cardService.js'

/** Load every owned variant so saved entries can resolve artwork across collection pages. */
export function useDeckCollection() {
  const { token } = useAuth()
  const [revision, setRevision] = useState(0)
  const [result, setResult] = useState({ status: 'loading', data: [], error: null })
  useEffect(() => {
    const controller = new AbortController()
    setResult({ status: 'loading', data: [], error: null })
    getDeckCollection({ token, signal: controller.signal }).then((data) => {
      if (!controller.signal.aborted) setResult({ status: 'ready', data, error: null })
    }).catch((error) => {
      if (!controller.signal.aborted) setResult({ status: 'error', data: [], error })
    })
    return () => controller.abort()
  }, [token, revision])
  return { ...result, retry: () => setRevision((value) => value + 1) }
}
