import { useEffect, useState } from 'react'

/** Restituisce `value` dopo che è rimasto invariato per `delay` ms (evita una richiesta a ogni tasto o scatto di slider). */
export function useDebouncedValue(value, delay = 250) {
  const [settled, setSettled] = useState(value)
  useEffect(() => {
    const t = setTimeout(() => setSettled(value), delay)
    return () => clearTimeout(t)
  }, [value, delay])
  return settled
}
