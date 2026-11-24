import CardBase from './CardBase.jsx'

export default function CreatureCard({ card, selected = false, error = false, onSelect, onRemove, compact = false }) {
  return (
    <CardBase card={card} selected={selected} error={error} onSelect={onSelect} onRemove={onRemove} compact={compact}>
      <span>ATK {card?.attack ?? '—'}</span>
      <span>DEF {card?.defense ?? '—'}</span>
      <span>POW {card?.power ?? '—'}</span>
    </CardBase>
  )
}
