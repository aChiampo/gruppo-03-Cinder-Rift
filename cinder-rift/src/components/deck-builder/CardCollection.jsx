import CreatureCard from '../cards/CreatureCard.jsx'
import EquipmentCard from '../cards/EquipmentCard.jsx'

export default function CardCollection({
  cards,
  selectedType,
  onSelectCard,
  selectedIds = [],
}) {
  return (
    <div className="card-collection">
      <div className="card-collection__grid">
        {cards.map((card) => {
          const CardComponent = selectedType === 'equipment' ? EquipmentCard : CreatureCard
          const isSelected = selectedIds.includes(card.id)

          return (
            <div key={card.id} className="card-collection__item">
              <CardComponent
                card={card}
                selected={isSelected}
                onSelect={() => onSelectCard(card)}
                compact
              />
            </div>
          )
        })}
      </div>
    </div>
  )
}
