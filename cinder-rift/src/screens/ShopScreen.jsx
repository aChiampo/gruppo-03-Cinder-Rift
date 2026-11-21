import { useEffect, useRef, useState } from 'react'
import { ShoppingBag, Package, History, X } from 'lucide-react'
import Unavailable from '../components/ui/Unavailable.jsx'
export default function ShopScreen({ go }) {
  const [tab, setTab] = useState('offerte')
  const [detail, setDetail] = useState(false)
  const dialog = useRef(null)
  useEffect(() => {
    if (detail) dialog.current?.showModal()
    else dialog.current?.close()
  }, [detail])
  return <main className="shop-layout">
    <aside className="panel shop-sidebar" aria-label="Sezioni negozio">{[['offerte', 'Negozio', ShoppingBag], ['pacchetti', 'I miei pacchetti', Package], ['cronologia', 'Cronologia acquisti', History]].map(([id, label, Icon]) => <button className={tab === id ? 'active' : ''} key={id} onClick={() => setTab(id)} aria-current={tab === id ? 'page' : undefined}><Icon size={20} />{label}</button>)}</aside>
    <section className="page-stack">
      <div className="page-heading"><h1>{tab === 'offerte' ? 'NEGOZIO' : tab === 'pacchetti' ? 'I MIEI PACCHETTI' : 'CRONOLOGIA ACQUISTI'}</h1><p>Espandi la tua collezione di Cinder-Rift.</p></div>
      <div className="resource-grid">{['Scaglie', 'Nuclei Rift', 'Rotami'].map((resource) => <div className="panel" key={resource}><small>{resource}</small><strong aria-label="Saldo non disponibile">—</strong></div>)}</div>
      <section className="panel shop-empty"><Unavailable feature={tab === 'offerte' ? 'Le offerte dei pacchetti' : tab === 'pacchetti' ? 'I pacchetti posseduti' : 'Le informazioni sugli acquisti'} />
        {tab === 'offerte' && <button className="btn-outline" onClick={() => setDetail(true)}>DETTAGLI PACCHETTO</button>}
        {tab === 'pacchetti' && <button className="btn-outline" disabled>APRI PACCHETTO</button>}
      </section>
      <button className="btn-ghost" onClick={() => go('collezione')}>VAI ALLA COLLEZIONE</button>
    </section>
    <dialog ref={dialog} className="pack-dialog panel" onCancel={() => setDetail(false)} onClose={() => setDetail(false)} aria-labelledby="pack-title">
      <button className="dialog-close icon-btn" aria-label="Chiudi dettagli" onClick={() => setDetail(false)}><X /></button>
      <h2 id="pack-title">DETTAGLI DEL PACCHETTO</h2>
      <div className="pack-detail-grid"><div className="pack-visual"><Package size={100} /><p>Pacchetto non disponibile</p></div><div><h3>COSA PUOI TROVARE</h3><Unavailable feature="Le carte e le probabilità del pacchetto" /></div><div className="pack-facts"><h3>DETTAGLI DEL PACCHETTO</h3><p>Contenuto —</p><p>Prezzo —</p><p>Risorsa richiesta —</p><p>Garanzia mercy —</p><button className="btn-primary" disabled>ACQUISTA</button></div></div>
    </dialog>
  </main>
}
