import { useEffect, useMemo, useState } from "react";
import { defaultFilters } from "../models/cards.js";
import {
  buildCollectionParams,
  getCollection,
} from "../services/cardService.js";
import { useAuth } from "../context/AuthContext.jsx";
import { useDebouncedValue } from "./useDebouncedValue.js";

/**
 * Stato della Collezione. Filtri, ordinamento e pagina vengono mandati al backend (GET /cards):
 * qui si tiene lo stato dell'interfaccia e l'esito della richiesta.
 *   status: 'loading' | 'ready' | 'error'   (durante un nuovo caricamento `visible` mostra ancora i risultati precedenti)
 */
export function useCardCollection() {
  const { token } = useAuth();
  const [filters, setFilters] = useState(defaultFilters);
  const [sort, setSort] = useState("rarita");
  const [page, setPage] = useState(1);
  const [reload, setReload] = useState(0);
  const [result, setResult] = useState({
    status: "loading",
    data: null,
    error: null,
  });

  // Ogni cambio di filtro riporta alla prima pagina.
  const patch = (changes) => {
    setFilters((f) => ({ ...f, ...changes }));
    setPage(1);
  };
  const toggle = (key, value) =>
    patch({
      [key]: filters[key].includes(value)
        ? filters[key].filter((v) => v !== value)
        : [...filters[key], value],
    });
  const reset = () => {
    setFilters(defaultFilters);
    setPage(1);
  };
  const changeSort = (value) => {
    setSort(value);
    setPage(1);
  };
  const retry = () => setReload((n) => n + 1);

  // Parametri della richiesta, ritardati un attimo: slider e ricerca non devono fare una chiamata a ogni movimento.
  const params = useMemo(
    () => buildCollectionParams({ filters, sort, page }),
    [filters, sort, page],
  );
  const settled = useDebouncedValue(params, 200);

  useEffect(() => {
    const controller = new AbortController();
    setResult((r) => ({ ...r, status: "loading", error: null }));
    getCollection(settled, { token, signal: controller.signal })
      .then((data) => setResult({ status: "ready", data, error: null }))
      .catch((error) => {
        if (controller.signal.aborted) return; // richiesta superata da una più recente
        setResult((r) => ({ ...r, status: "error", error }));
      });
    return () => controller.abort();
  }, [settled, token, reload]);

  const { status, data, error } = result;

  // Se il backend risponde con una pagina vuota ma ci sono risultati (es. si sono ridotti i filtri), si torna alla prima.
  useEffect(() => {
    if (
      status === "ready" &&
      data &&
      data.items.length === 0 &&
      data.total > 0 &&
      page > 1
    )
      setPage(1);
  }, [status, data, page]);

  const isFiltered = JSON.stringify(filters) !== JSON.stringify(defaultFilters);

  return {
    filters,
    patch,
    toggle,
    reset,
    isFiltered,
    sort,
    changeSort,
    status,
    error,
    retry,
    loaded: data !== null,
    visible: data?.items ?? [],
    total: data?.total ?? 0,
    counts: data?.counts ?? null,
    page: data?.page ?? page,
    pageCount: data?.pageCount ?? 1,
    setPage,
  };
}
