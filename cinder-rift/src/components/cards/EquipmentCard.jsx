import CardBase from './CardBase.jsx'

export default function EquipmentCard({ card, selected = false, error = false, onSelect, onRemove, compact = false }) {
  return (
    <CardBase card={card} selected={selected} error={error} onSelect={onSelect} onRemove={onRemove} compact={compact}>
      <span>Power {card?.power ?? '—'}</span>
      <span>{card?.rarity || '—'}</span>
    </CardBase>
  )
}
