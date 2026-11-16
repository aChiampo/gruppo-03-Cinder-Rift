import CreatureCard from './CreatureCard.jsx'
import EquipmentCard from './EquipmentCard.jsx'

export default function CardSlot({
  type = 'creature',
  card,
  onAdd,
  onRemove,
  onClick,
  error = false,
  compact = false,
}) {
  if (card) {
    const CardComponent = type === 'equipment' ? EquipmentCard : CreatureCard
    return (
      <div className="card-slot card-slot--filled" data-error={error}>
        <CardComponent card={card} error={error} compact={compact} onSelect={onClick} onRemove={onRemove} />
      </div>
    )
  }

  return (
    <button type="button" className="card-slot card-slot--empty" onClick={onAdd}>
      <span className="card-slot__plus">+</span>
      <span className="card-slot__label">{type === 'equipment' ? 'Equip.' : 'Creatura'}</span>
    </button>
  )
}
