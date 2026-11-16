export default function CardFilters({
  search,
  onSearchChange,
  faction,
  onFactionChange,
  rarity,
  onRarityChange,
  energy,
  onEnergyChange,
  factionOptions,
  rarityOptions,
  energyOptions,
}) {
  return (
    <div className="card-filters">
      <div className="filter-search">
        <input
          type="text"
          value={search}
          onChange={(event) => onSearchChange(event.target.value)}
          placeholder="Cerca carta"
        />
      </div>

      <div className="filter-row">
        <label>
          <span>Fazione</span>
          <select value={faction} onChange={(event) => onFactionChange(event.target.value)}>
            {factionOptions.map((option) => (
              <option key={option} value={option}>{option}</option>
            ))}
          </select>
        </label>

        <label>
          <span>Rarità</span>
          <select value={rarity} onChange={(event) => onRarityChange(event.target.value)}>
            {rarityOptions.map((option) => (
              <option key={option} value={option}>{option}</option>
            ))}
          </select>
        </label>

        <label>
          <span>Energia</span>
          <select value={energy} onChange={(event) => onEnergyChange(event.target.value)}>
            {energyOptions.map((option) => (
              <option key={option} value={option}>{option}</option>
            ))}
          </select>
        </label>
      </div>
    </div>
  )
}
