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
import { cardKey, PAGE_SIZE, SORT_OPTIONS } from "../models/cards.js";

export default function CollectionScreen({ go, catalog = false }) {
  const c = useCardCollection({ catalog });
  const [chosenId, setChosenId] = useState(null);

  // Carta mostrata nel dettaglio: quella scelta se è nella pagina corrente, altrimenti la prima (mai una carta non più in elenco).
  const selected =
    c.visible.find((x) => cardKey(x) === chosenId) ?? c.visible[0] ?? null;

  const tabs = [
    { id: "", label: "Tutte" },
    { id: "CREATURE", label: "Creature" },
    { id: "EQUIPMENT", label: "Equipaggiamenti" },
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
              <Layers size={30} /> {catalog ? 'CATALOGO CARTE' : 'LA TUA COLLEZIONE'}
            </h1>
            <p>
              {catalog ? 'Esplora il catalogo di creature ed equipaggiamenti.' : 'Esplora le creature e gli equipaggiamenti che possiedi.'} Usa i
              filtri per trovare le carte che ti servono.
            </p>
          </div>
          <label className="search">
            <Search size={16} />
            <input
              type="search"
              placeholder="Cerca una carta..."
              maxLength={100}
              value={c.filters.name}
              onChange={(e) => c.patch({ name: e.target.value })}
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
            <p>{c.error?.message || (catalog ? "Impossibile caricare il catalogo." : "Impossibile caricare la collezione.")}</p>
            <button className="btn-outline" onClick={c.retry}>
              RIPROVA
            </button>
          </div>
        )}

        {emptyCollection && (
          <div className="collection-state">
            <PackageOpen size={34} />
            <p>{catalog ? "Il catalogo non contiene ancora carte." : "La tua collezione è vuota: non possiedi ancora nessuna carta."}</p>
            {!catalog && go && (
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
              <li key={cardKey(card)}>
                <CardTile
                  card={card}
                  selected={selected && cardKey(selected) === cardKey(card)}
                  onSelect={setChosenId}
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
        <label className="sort">
          <select value={c.direction} onChange={(e) => c.changeDirection(e.target.value)} aria-label="Direzione ordinamento">
            <option value="asc">Crescente</option>
            <option value="desc">Decrescente</option>
          </select>
          <ChevronDown size={16} />
        </label>
        <div className="panel detail-panel">
          {selected && !failed ? (
            <CardDetail key={cardKey(selected)} card={selected} />
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
