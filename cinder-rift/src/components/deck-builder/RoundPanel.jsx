import CardSlot from '../cards/CardSlot.jsx'

export default function RoundPanel({ cards, roundNumber, active, onAddSlot, onRemoveCard, invalidCardIds = [] }) {
  return (
    <div className={`round-panel ${active ? 'active' : ''}`}>
      <div className="round-panel__slots">
        {Array.from({ length: 5 }, (_, index) => {
          const card = cards[index]
          const hasError = card ? invalidCardIds.includes(card.id) : false

          return (
            <CardSlot
              key={`${roundNumber}-${index}`}
              type="creature"
              card={card}
              error={hasError}
              onAdd={() => onAddSlot(index)}
              onRemove={() => onRemoveCard(index)}
            />
          )
        })}
      </div>
    </div>
  )
}
