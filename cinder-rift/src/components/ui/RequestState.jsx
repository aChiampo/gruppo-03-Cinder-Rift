import { AlertTriangle, Loader2, PackageOpen } from 'lucide-react'
export default function RequestState({ status, error, retry, empty, emptyMessage = 'Nessun risultato.' }) {
  if (status === 'loading') return <div className="request-state" role="status"><Loader2 className="spinner" size={26} /><p>Caricamento…</p></div>
  if (status === 'error' || error) return <div className="request-state error" role="alert"><AlertTriangle size={28} /><p>{error?.message || 'Operazione non riuscita.'}</p>{error?.status > 0 && <small>HTTP {error.status}</small>}{Array.isArray(error?.data?.violations) && <ul>{error.data.violations.map((v, i) => <li key={i}>{v}</li>)}</ul>}{retry && <button className="btn-outline" onClick={retry}>RIPROVA</button>}</div>
  if (empty) return <div className="request-state"><PackageOpen size={28} /><p>{emptyMessage}</p></div>
  return null
}
