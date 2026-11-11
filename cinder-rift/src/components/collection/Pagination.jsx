import { ChevronLeft, ChevronRight } from 'lucide-react'
import { pageWindow } from '../../models/cards.js'

export default function Pagination({ page, pageCount, onChange }) {
  if (pageCount <= 1) return null
  return (
    <nav className="pagination" aria-label="Pagine della collezione">
      <button onClick={() => onChange(page - 1)} disabled={page === 1} aria-label="Pagina precedente"><ChevronLeft size={16} /></button>
      {pageWindow(page, pageCount).map((p, i) =>
        p === '…'
          ? <span key={`gap${i}`} className="gap">…</span>
          : <button key={p} className={p === page ? 'current' : ''} onClick={() => onChange(p)} aria-current={p === page ? 'page' : undefined}>{p}</button>
      )}
      <button onClick={() => onChange(page + 1)} disabled={page === pageCount} aria-label="Pagina successiva"><ChevronRight size={16} /></button>
    </nav>
  )
}
