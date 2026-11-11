import { useState } from 'react'
import { Layers, Search, ChevronDown } from 'lucide-react'
import FilterPanel from '../components/collection/FilterPanel.jsx'
import CardTile from '../components/collection/CardTile.jsx'
import CardDetail from '../components/collection/CardDetail.jsx'
import Pagination from '../components/collection/Pagination.jsx'
import { useCardCollection } from '../hooks/useCardCollection.js'
import { cards, DEFAULT_SELECTED_ID } from '../data/cards.js'
import { SORT_OPTIONS } from '../models/cards.js'

export default function CollectionScreen() {
  const c = useCardCollection(cards)
  const [selectedId, setSelectedId] = useState(DEFAULT_SELECTED_ID)
  const selected = cards.find((x) => x.id === selectedId) ?? cards[0]

  const tabs = [
    { id: 'all', label: 'Tutte', n: c.counts.all },
    { id: 'creature', label: 'Creature', n: c.counts.creature },
    { id: 'equipment', label: 'Equipaggiamenti', n: c.counts.equipment },
  ]

  return (
    <main className="collection">
      <aside className="panel col-filters" aria-label="Filtri">
        <FilterPanel filters={c.filters} toggle={c.toggle} patch={c.patch} reset={c.reset} isFiltered={c.isFiltered} />
      </aside>

      <section className="panel col-cards" aria-labelledby="collection-title">
        <header className="cards-head">
          <div>
            <h1 id="collection-title"><Layers size={30} /> COLLEZIONE CARTE</h1>
            <p>Esplora la tua collezione di creature ed equipaggiamenti. Usa i filtri per trovare le carte che ti servono.</p>
          </div>
          <label className="search">
            <Search size={16} />
            <input
              type="search" placeholder="Cerca una carta..." value={c.filters.query}
              onChange={(e) => c.patch({ query: e.target.value })} aria-label="Cerca una carta"
            />
          </label>
        </header>

        <div className="tabs" role="tablist" aria-label="Tipo di carta">
          {tabs.map((t) => (
            <button key={t.id} role="tab" aria-selected={c.filters.type === t.id}
              className={c.filters.type === t.id ? 'active' : ''} onClick={() => c.patch({ type: t.id })}>
              {t.label} ({t.n})
            </button>
          ))}
        </div>

        {c.visible.length ? (
          <ul className="card-grid">
            {c.visible.map((card) => (
              <li key={card.id}><CardTile card={card} selected={card.id === selected.id} onSelect={setSelectedId} /></li>
            ))}
          </ul>
        ) : (
          <div className="empty">
            <p>Nessuna carta corrisponde ai filtri.</p>
            <button className="btn-outline" onClick={c.reset}>AZZERA FILTRI</button>
          </div>
        )}

        <Pagination page={c.page} pageCount={c.pageCount} onChange={c.setPage} />
      </section>

      <aside className="col-detail" aria-label="Dettaglio carta">
        <label className="sort">
          <select value={c.sort} onChange={(e) => c.changeSort(e.target.value)} aria-label="Ordina per">
            {SORT_OPTIONS.map((o) => <option key={o.id} value={o.id}>Ordina per: {o.label}</option>)}
          </select>
          <ChevronDown size={16} />
        </label>
        <div className="panel detail-panel"><CardDetail card={selected} /></div>
      </aside>
    </main>
  )
}
