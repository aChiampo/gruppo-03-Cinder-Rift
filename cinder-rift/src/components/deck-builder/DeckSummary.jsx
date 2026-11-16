export default function DeckSummary({ validation, onVerify }) {
  const totalCards = (validation?.creatureCount ?? 0) + (validation?.equipmentCount ?? 0)
  const goalPower = 250
  const progress = Math.min(100, ((validation?.totalPower ?? 0) / goalPower) * 100)

  return (
    <aside className="deck-summary">
      <div className="deck-summary__header">
        <h3>Riepilogo mazzo</h3>
      </div>

      <div className="deck-summary__power">
        <div className="deck-summary__ring">
          <span>{validation?.totalPower ?? 0}</span>
        </div>
      </div>

      <div className="deck-summary__progress">
        <div className="deck-summary__progress-bar">
          <span style={{ width: `${progress}%` }} />
        </div>
      </div>

      <div className="deck-summary__metrics">
        <div><span>Creature</span><strong>{validation?.creatureCount ?? 0}</strong></div>
        <div><span>Equipaggiamenti</span><strong>{validation?.equipmentCount ?? 0}</strong></div>
        <div><span>Totale carte</span><strong>{totalCards}</strong></div>
      </div>

      <div className="deck-summary__rarity">
        {Object.entries(validation?.rarityCounts ?? {}).map(([label, count]) => (
          <div key={label}><span>{label}</span><strong>{count}</strong></div>
        ))}
      </div>

      <div className="deck-summary__rounds">
        {(validation?.roundValidation ?? []).map((round) => (
          <div key={round.round} className="deck-summary__round">
            <span>Round {round.round}</span>
            <b>{round.creatures}/5</b>
          </div>
        ))}
      </div>

      <div className="deck-summary__equipment-rarity">
        {Object.entries(validation?.equipmentRarityCounts ?? {}).map(([label, count]) => (
          <div key={label}><span>{label}</span><strong>{count}</strong></div>
        ))}
      </div>

      <button type="button" className="deck-summary__verify" onClick={onVerify}>Verifica mazzo</button>
    </aside>
  )
}
