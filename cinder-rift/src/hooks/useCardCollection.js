import { useEffect, useMemo, useState } from 'react'
import { defaultFilters } from '../models/cards.js'
import { buildCollectionParams, getCatalog, getCollection } from '../services/cardService.js'
import { useAuth } from '../context/AuthContext.jsx'
import { useDebouncedValue } from './useDebouncedValue.js'

/** F04 state shared by the catalog and private collection, with cancellable server filtering. */
export function useCardCollection({ catalog = false } = {}) {
  const { token } = useAuth()
  const [filters, setFilters] = useState(defaultFilters)
  const [sort, setSort] = useState('name')
  const [direction, setDirection] = useState('asc')
  const [page, setPage] = useState(1)
  const [reload, setReload] = useState(0)
  const [result, setResult] = useState({ status: 'loading', data: null, error: null })
  const patch = (changes) => { setFilters((value) => ({ ...value, ...changes })); setPage(1) }
  const reset = () => { setFilters(defaultFilters); setPage(1) }
  const changeSort = (value) => { setSort(value); setPage(1) }
  const changeDirection = (value) => { setDirection(value); setPage(1) }
  const retry = () => setReload((value) => value + 1)
  const params = useMemo(() => buildCollectionParams({ filters, sort, direction, page }), [filters, sort, direction, page])
  const settled = useDebouncedValue(params, 200)

  useEffect(() => {
    const controller = new AbortController()
    setResult({ status: 'loading', data: null, error: null })
    const read = catalog ? getCatalog : getCollection
    read(settled, { token, signal: controller.signal }).then((data) => {
      if (!controller.signal.aborted) setResult({ status: 'ready', data, error: null })
    }).catch((error) => {
      if (!controller.signal.aborted) setResult({ status: 'error', data: null, error })
    })
    return () => controller.abort()
  }, [settled, token, reload, catalog])

  const { status, data, error } = result
  useEffect(() => {
    if (status === 'ready' && data.items.length === 0 && data.totalElements > 0 && page > 1) setPage(1)
  }, [status, data, page])

  return {
    filters, patch, reset, isFiltered: Object.values(filters).some((value) => value !== ''),
    sort, changeSort, direction, changeDirection, status, error, retry,
    loaded: data !== null, visible: data?.items ?? [], total: data?.totalElements ?? 0,
    page: data ? data.page + 1 : page, pageCount: Math.max(1, data?.totalPages ?? 0), setPage,
  }
}
