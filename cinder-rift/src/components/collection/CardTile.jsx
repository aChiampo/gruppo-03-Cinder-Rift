import { Swords, Shield } from 'lucide-react'
import Hex from '../ui/Hex.jsx'
import FactionIcon from '../ui/FactionIcon.jsx'
import CardArt from './CardArt.jsx'
import { factionById, NEUTRAL } from '../../data/factions.js'
import { rarityById } from '../../models/cards.js'

/** Colore guida di una carta: fazione per le creature, colore proprio per gli equipaggiamenti. */
export const cardColor = (card) => (card.type === 'equipment' ? card.accent : factionById[card.faction].color)

export default function CardTile({ card, selected, onSelect }) {
  const color = cardColor(card)
  const rarity = rarityById[card.rarity]
  const isCreature = card.type === 'creature'
  const faction = isCreature ? factionById[card.faction] : NEUTRAL

  return (
    <button
      className={`card-tile ${selected ? 'selected' : ''}`}
      style={{ '--c': color }}
      onClick={() => onSelect(card.id)}
      aria-pressed={selected}
      aria-label={`${card.name}, ${rarity.label}, energia ${card.energy}`}
    >
      <span className="tile-top">
        <CardArt card={card} />
        <span className="tile-energy"><Hex color={color} size={30} filled><b>{card.energy}</b></Hex></span>
      </span>
      <span className="tile-body">
        <span className="tile-name" title={card.name}>{card.name}</span>
        <span className="tile-faction" style={isCreature ? { color } : undefined}>
          {isCreature && <FactionIcon faction={card.faction} size={16} />}
          {faction.singular}
        </span>
        <span className="tile-rarity" style={{ color: rarity.color }}>{rarity.label}</span>
      </span>
      {isCreature && (
        <span className="tile-stats">
          <span><Swords size={14} color={color} /> {card.attack}</span>
          <span><Shield size={14} /> {card.defense}</span>
        </span>
      )}
    </button>
  )
}
