import { useState } from "react";
import {
  Layers,
  Search,
  ChevronDown,
  WifiOff,
  PackageOpen,
} from "lucide-react";
import FilterPanel from "../components/collection/FilterPanel.jsx";
import CardTile from "../components/collection/CardTile.jsx";
import CardDetail from "../components/collection/CardDetail.jsx";
import Pagination from "../components/collection/Pagination.jsx";
import { useCardCollection } from "../hooks/useCardCollection.js";
import { PAGE_SIZE, SORT_OPTIONS } from "../models/cards.js";

export default function CollectionScreen({ go }) {
  const c = useCardCollection();
  const [chosenId, setChosenId] = useState(null);

  // Carta mostrata nel dettaglio: quella scelta se è nella pagina corrente, altrimenti la prima (mai una carta non più in elenco).
  const selected =
    c.visible.find((x) => x.id === chosenId) ?? c.visible[0] ?? null;

  const tabs = [
    { id: "all", label: "Tutte" },
    { id: "creature", label: "Creature" },
    { id: "equipment", label: "Equipaggiamenti" },
  ];

  const initialLoading = !c.loaded && c.status === "loading";
  const failed = c.status === "error";
  const emptyCollection =
    c.status === "ready" && c.total === 0 && !c.isFiltered;
  const noResults = c.status === "ready" && c.total === 0 && c.isFiltered;
  const busy = c.status === "loading" && c.loaded;

  return (
    <main className="collection">
      <aside className="panel col-filters" aria-label="Filtri">
        <FilterPanel
          filters={c.filters}
          toggle={c.toggle}
          patch={c.patch}
          reset={c.reset}
          isFiltered={c.isFiltered}
        />
      </aside>

      <section
        className="panel col-cards"
        aria-labelledby="collection-title"
        aria-busy={initialLoading || busy}
      >
        <header className="cards-head">
          <div>
            <h1 id="collection-title">
              <Layers size={30} /> COLLEZIONE CARTE
            </h1>
            <p>
              Esplora la tua collezione di creature ed equipaggiamenti. Usa i
              filtri per trovare le carte che ti servono.
            </p>
          </div>
          <label className="search">
            <Search size={16} />
            <input
              type="search"
              placeholder="Cerca una carta..."
              value={c.filters.query}
              onChange={(e) => c.patch({ query: e.target.value })}
              aria-label="Cerca una carta"
            />
          </label>
        </header>

        <div className="tabs" role="tablist" aria-label="Tipo di carta">
          {tabs.map((t) => (
            <button
              key={t.id}
              role="tab"
              aria-selected={c.filters.type === t.id}
              className={c.filters.type === t.id ? "active" : ""}
              onClick={() => c.patch({ type: t.id })}
            >
              {t.label}
              {c.counts ? ` (${c.counts[t.id]})` : ""}
            </button>
          ))}
        </div>

        {initialLoading && (
          <ul
            className="card-grid"
            role="status"
            aria-label="Caricamento delle carte"
          >
            {Array.from({ length: PAGE_SIZE }, (_, i) => (
              <li key={i} className="card-skeleton" aria-hidden="true" />
            ))}
          </ul>
        )}

        {failed && (
          <div className="collection-state error" role="alert">
            <WifiOff size={34} />
            <p>{c.error?.message || "Impossibile caricare la collezione."}</p>
            <button className="btn-outline" onClick={c.retry}>
              RIPROVA
            </button>
          </div>
        )}

        {emptyCollection && (
          <div className="collection-state">
            <PackageOpen size={34} />
            <p>La tua collezione è vuota: non possiedi ancora nessuna carta.</p>
            {go && (
              <button className="btn-outline" onClick={() => go("negozio")}>
                VAI AL NEGOZIO
              </button>
            )}
          </div>
        )}

        {noResults && (
          <div className="collection-state">
            <p>Nessuna carta corrisponde ai filtri.</p>
            <button className="btn-outline" onClick={c.reset}>
              AZZERA FILTRI
            </button>
          </div>
        )}

        {!failed && c.visible.length > 0 && (
          <ul className={`card-grid ${busy ? "busy" : ""}`}>
            {c.visible.map((card) => (
              <li key={card.id}>
                <CardTile
                  card={card}
                  selected={selected?.id === card.id}
                  onSelect={() => setChosenId(card.id)}
                />
              </li>
            ))}
          </ul>
        )}

        {!failed && (
          <Pagination
            page={c.page}
            pageCount={c.pageCount}
            onChange={c.setPage}
          />
        )}
      </section>

      <aside className="col-detail" aria-label="Dettaglio carta">
        <label className="sort">
          <select
            value={c.sort}
            onChange={(e) => c.changeSort(e.target.value)}
            aria-label="Ordina per"
          >
            {SORT_OPTIONS.map((o) => (
              <option key={o.id} value={o.id}>
                Ordina per: {o.label}
              </option>
            ))}
          </select>
          <ChevronDown size={16} />
        </label>
        <div className="panel detail-panel">
          {selected ? (
            <CardDetail card={selected} />
          ) : (
            <p className="detail-empty">
              Seleziona una carta per vederne i dettagli.
            </p>
          )}
        </div>
      </aside>
    </main>
  );
}
