import { useMemo, useState } from 'react'
import { PAGE_SIZE, defaultFilters, matchesFilters, sortCards } from '../models/cards.js'

/** Stato della Collezione: filtri, ordinamento, pagina e risultati già calcolati. */
export function useCardCollection(cards) {
  const [filters, setFilters] = useState(defaultFilters)
  const [sort, setSort] = useState('rarita')
  const [page, setPage] = useState(1)

  // Ogni cambio di filtro riporta alla prima pagina.
  const patch = (changes) => { setFilters((f) => ({ ...f, ...changes })); setPage(1) }
  const toggle = (key, value) =>
    patch({ [key]: filters[key].includes(value) ? filters[key].filter((v) => v !== value) : [...filters[key], value] })
  const reset = () => { setFilters(defaultFilters); setPage(1) }
  const changeSort = (value) => { setSort(value); setPage(1) }

  const results = useMemo(() => sortCards(cards.filter((c) => matchesFilters(c, filters)), sort), [cards, filters, sort])

  // Conteggi delle tab: tengono conto di ricerca e filtri, ma non della tab scelta.
  const counts = useMemo(() => {
    const base = cards.filter((c) => matchesFilters(c, filters, { ignoreType: true }))
    return {
      all: base.length,
      creature: base.filter((c) => c.type === 'creature').length,
      equipment: base.filter((c) => c.type === 'equipment').length,
    }
  }, [cards, filters])

  const pageCount = Math.max(1, Math.ceil(results.length / PAGE_SIZE))
  const current = Math.min(page, pageCount)
  const visible = results.slice((current - 1) * PAGE_SIZE, current * PAGE_SIZE)

  const isFiltered = JSON.stringify(filters) !== JSON.stringify(defaultFilters)

  return { filters, patch, toggle, reset, sort, changeSort, results, visible, counts, page: current, pageCount, setPage, isFiltered }
}
