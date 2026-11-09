import { useCallback, useRef, useState } from 'react'

/** Messaggio temporaneo a schermo: restituisce [messaggio, mostra]. */
export function useToast(duration = 3200) {
  const [msg, setMsg] = useState(null)
  const timer = useRef(null)
  const show = useCallback((text) => {
    setMsg(text)
    clearTimeout(timer.current)
    timer.current = setTimeout(() => setMsg(null), duration)
  }, [duration])
  return [msg, show]
}
