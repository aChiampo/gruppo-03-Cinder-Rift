export default function RoundSelector({ activeRound, onChange }) {
  const rounds = [1, 2, 3, 4, 5]

  return (
    <div className="round-selector" aria-label="Selettore round">
      {rounds.map((round) => (
        <button
          key={round}
          type="button"
          className={`round-selector__button ${activeRound === round ? 'active' : ''}`}
          onClick={() => onChange(round)}
        >
          Round {round}
        </button>
      ))}
    </div>
  )
}
