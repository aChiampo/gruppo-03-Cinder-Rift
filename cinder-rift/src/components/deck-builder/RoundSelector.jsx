export default function RoundSelector({ activeDestination, onChange }) {
  const destinations = [
    ...[1, 2, 3, 4, 5].map((round) => ({ id: `ROUND${round}`, label: `Round ${round}` })),
    { id: 'EQUIPMENT', label: 'Equipaggiamenti' },
  ]

  return (
    <div className="round-selector" aria-label="Selettore round">
      {destinations.map(({ id, label }) => (
        <button
          key={id}
          type="button"
          className={`round-selector__button ${activeDestination === id ? 'active' : ''}`}
          aria-pressed={activeDestination === id}
          onClick={() => onChange(id)}
        >
          {label}
        </button>
      ))}
    </div>
  )
}
