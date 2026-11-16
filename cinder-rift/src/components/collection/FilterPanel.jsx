import { useState } from "react";
import { Filter, ChevronUp, RefreshCw } from "lucide-react";
import Hex from "../ui/Hex.jsx";
import FactionIcon from "../ui/FactionIcon.jsx";
import RangeSlider from "../ui/RangeSlider.jsx";
import { factions } from "../../data/factions.js";
import { RARITIES, ENERGY_RANGE, STAT_RANGE } from "../../models/cards.js";

/** Blocco di filtri richiudibile. */
function Section({ title, children }) {
  const [open, setOpen] = useState(true);
  return (
    <section className={`filter-box ${open ? "" : "closed"}`}>
      <button
        className="filter-title"
        onClick={() => setOpen(!open)}
        aria-expanded={open}
      >
        {title} <ChevronUp size={14} className="chev" />
      </button>
      {open && <div className="filter-content">{children}</div>}
    </section>
  );
}

function Option({ active, onClick, children }) {
  return (
    <button
      className={`filter-option ${active ? "on" : ""}`}
      onClick={onClick}
      aria-pressed={active}
    >
      {children}
    </button>
  );
}

function StatRange({ label, value, onChange }) {
  return (
    <Section title={label}>
      <RangeSlider
        min={STAT_RANGE[0]}
        max={STAT_RANGE[1]}
        value={value}
        onChange={onChange}
        label={label}
      />
      <div className="range-values">
        <span>{value[0]}</span>
        <span>{value[1]}</span>
      </div>
    </Section>
  );
}

export default function FilterPanel({
  filters,
  toggle,
  patch,
  reset,
  isFiltered,
}) {
  const [open, setOpen] = useState(false);
  const [eMin, eMax] = filters.energy;
  const energyIsFull = eMin === ENERGY_RANGE[0] && eMax === ENERGY_RANGE[1];
  const pickEnergy = (n) =>
    patch({ energy: eMin === n && eMax === n ? [...ENERGY_RANGE] : [n, n] });
  const energyValues = Array.from(
    { length: ENERGY_RANGE[1] - ENERGY_RANGE[0] + 1 },
    (_, i) => ENERGY_RANGE[0] + i,
  );

  return (
    <div className={`filters ${open ? "open" : ""}`}>
      <h2 className="filters-head">
        <Filter size={20} /> FILTRI
        {/* Solo mobile: i filtri sono chiusi di default così le carte restano subito visibili */}
        <button
          className="filters-toggle"
          onClick={() => setOpen(!open)}
          aria-expanded={open}
        >
          {open ? "NASCONDI" : "MOSTRA"}
          {isFiltered && !open ? " •" : ""}
        </button>
      </h2>
      <div className="filters-body">
        <Section title="FAZIONE">
          {factions.map((f) => (
            <Option
              key={f.id}
              active={filters.factions.includes(f.id)}
              onClick={() => toggle("factions", f.id)}
            >
              <FactionIcon faction={f.id} size={22} /> {f.label}
            </Option>
          ))}
        </Section>

        <Section title="RARITÀ">
          {RARITIES.map((r) => (
            <Option
              key={r.id}
              active={filters.rarities.includes(r.id)}
              onClick={() => toggle("rarities", r.id)}
            >
              <Hex color={r.color} size={22} filled glow={false} /> {r.label}
            </Option>
          ))}
        </Section>

        <Section title="COSTO ENERGIA">
          <div className="energy-buttons">
            {energyValues.map((n) => (
              <button
                key={n}
                className={!energyIsFull && n >= eMin && n <= eMax ? "on" : ""}
                onClick={() => pickEnergy(n)}
                aria-label={`Solo energia ${n}`}
              >
                {n}
              </button>
            ))}
          </div>
          <RangeSlider
            min={ENERGY_RANGE[0]}
            max={ENERGY_RANGE[1]}
            value={filters.energy}
            onChange={(v) => patch({ energy: v })}
            label="Costo energia"
          />
          <div className="range-values center">
            <span>
              {eMin} - {eMax}
            </span>
          </div>
        </Section>

        <StatRange
          label="ATTACCO"
          value={filters.attack}
          onChange={(v) => patch({ attack: v })}
        />
        <StatRange
          label="DIFESA"
          value={filters.defense}
          onChange={(v) => patch({ defense: v })}
        />

        <button
          className="btn-ghost reset"
          onClick={reset}
          disabled={!isFiltered}
        >
          <RefreshCw size={15} /> AZZERA FILTRI
        </button>
      </div>
    </div>
  );
}
