import { useState } from 'react'
import { Filter, ChevronUp, RefreshCw } from 'lucide-react'
import { CARD_FACTIONS, RARITIES } from '../../models/cards.js'

function Section({ title, children }) {
  const [open, setOpen] = useState(true)
  return <section className={`filter-box ${open ? '' : 'closed'}`}>
    <button className="filter-title" onClick={() => setOpen(!open)} aria-expanded={open}>
      {title}<ChevronUp size={14} className="chev" />
    </button>
    {open && <div className="filter-content">{children}</div>}
  </section>
}

/** Optional, inclusive bounds match F04; blank values do not exclude nullable statistics. */
function StatRange({ label, field, filters, patch }) {
  return <Section title={label}>
    <div className="filter-range">
      {['Min', 'Max'].map((bound) => <label key={bound}>
        {bound === 'Min' ? 'Minimo' : 'Massimo'}
        <input type="number" min="0" max="2147483647" step="1"
          aria-label={`${label}: ${bound === 'Min' ? 'minimo' : 'massimo'}`}
          value={filters[`${field}${bound}`]}
          onChange={(event) => patch({ [`${field}${bound}`]: event.target.value })} />
      </label>)}
    </div>
  </Section>
}

export default function FilterPanel({ filters, patch, reset, isFiltered }) {
  const [open, setOpen] = useState(false)
  return <div className={`filters ${open ? 'open' : ''}`}>
    <h2 className="filters-head"><Filter size={20} /> FILTRI
      <button className="filters-toggle" onClick={() => setOpen(!open)} aria-expanded={open}>
        {open ? 'NASCONDI' : 'MOSTRA'}{isFiltered && !open ? ' •' : ''}
      </button>
    </h2>
    <div className="filters-body">
      <Section title="FAZIONE">
        <select aria-label="Fazione" value={filters.faction} onChange={(event) => patch({ faction: event.target.value })}>
          <option value="">Tutte le fazioni</option>
          {CARD_FACTIONS.map((entry) => <option key={entry.code} value={entry.code}>{entry.label}</option>)}
        </select>
      </Section>
      <Section title="RARITÀ">
        <select aria-label="Rarità" value={filters.rarity} onChange={(event) => patch({ rarity: event.target.value })}>
          <option value="">Tutte le rarità</option>
          {RARITIES.map((entry) => <option key={entry.code} value={entry.code}>{entry.label}</option>)}
        </select>
      </Section>
      <Section title="ESPANSIONE">
        <input aria-label="Codice espansione" placeholder="Codice espansione" maxLength={100}
          value={filters.expansion} onChange={(event) => patch({ expansion: event.target.value })} />
      </Section>
      <StatRange label="COSTO ENERGIA" field="energy" filters={filters} patch={patch} />
      <StatRange label="ATTACCO" field="attack" filters={filters} patch={patch} />
      <StatRange label="DIFESA" field="defense" filters={filters} patch={patch} />
      <StatRange label="PUNTI POTENZA" field="power" filters={filters} patch={patch} />
      <button className="btn-ghost reset" onClick={reset} disabled={!isFiltered}><RefreshCw size={15} /> AZZERA FILTRI</button>
    </div>
  </div>
}
