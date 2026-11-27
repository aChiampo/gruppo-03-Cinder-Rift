import { CloudOff } from 'lucide-react'
export default function Unavailable({ feature, children }) {
  return <div className="request-state unavailable" role="status"><CloudOff size={30} /><h3>Servizio non disponibile</h3><p>{feature} non sono ancora disponibili. Riprova più tardi.</p>{children}</div>
}
