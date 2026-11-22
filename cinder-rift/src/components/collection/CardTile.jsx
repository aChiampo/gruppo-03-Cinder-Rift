import { Swords, Shield } from 'lucide-react'
import Hex from '../ui/Hex.jsx'
import FactionIcon from '../ui/FactionIcon.jsx'
import CardArt from './CardArt.jsx'
import { cardAppearance, cardKey } from '../../models/cards.js'

export default function CardTile({ card, selected, onSelect, disabled = false, draggable = false, onDragStart, actionLabel, compact = false }) {
  const { color, factionId, factionLabel, rarityLabel, rarityColor } = cardAppearance(card)
  const isCreature = card.type?.code === 'CREATURE'
  const Tile = onSelect ? 'button' : 'div'
  const interactionProps = onSelect ? {
    type: 'button', disabled, draggable: draggable && !disabled, onDragStart,
    onClick: () => onSelect(cardKey(card)), 'aria-pressed': selected,
  } : {}
  return <Tile className={`card-tile ${selected ? 'selected' : ''}`} style={{ '--c': color }} role={onSelect ? undefined : 'group'} {...interactionProps}
    aria-label={actionLabel ?? `${card.name ?? 'Carta senza nome'}, ${rarityLabel}, energia ${card.energyCost ?? 'non disponibile'}`}>
    <span className="tile-top">
      <CardArt card={card} />
      <span className="tile-energy"><Hex color={color} size={compact ? 22 : 30} filled><b>{card.energyCost ?? '—'}</b></Hex></span>
    </span>
    <span className="tile-body">
      <span className="tile-name" title={card.name ?? undefined}>{card.name ?? 'Carta senza nome'}</span>
      <span className="tile-faction" style={{ color }}>
        {factionId && <FactionIcon faction={factionId} size={16} />}{factionLabel}
      </span>
      <span className="tile-rarity" style={{ color: rarityColor }}>{rarityLabel}</span>
      {card.cardArtId !== undefined && <span className="tile-copies">Variante {card.artCode} · Copie: {card.ownedReplica}</span>}
    </span>
    {isCreature && <span className="tile-stats">
      <span><Swords size={14} color={color} />{card.attack ?? '—'}</span>
      <span><Shield size={14} />{card.defense ?? '—'}</span>
    </span>}
  </Tile>
}
