import { useState } from 'react'
import { Plus, Trash2 } from 'lucide-react'
import CardTile from '../collection/CardTile.jsx'
import { cardKey } from '../../models/cards.js'
import { CARD_DRAG_TYPE, SLOTS_PER_GROUP } from '../../models/deckBuilder.js'

export default function DeckSlots({ entries, cards, destination, disabled, onDropCard, onRemove }) {
  const [hovered, setHovered] = useState(null)
  const group = entries.filter((entry) => entry.destination === destination)
  const count = Math.max(SLOTS_PER_GROUP, ...group.map((entry) => entry.slot + 1))
  return <ul className="entry-slots deck-card-grid" aria-label={`Slot ${destination}`}>
    {Array.from({ length: count }, (_, slot) => {
      const entry = group.find((entry) => entry.slot === slot)
      const card = entry && cards.find((card) => cardKey(card) === cardKey(entry))
      return <li key={`${destination}-${slot}`} className={`entry-slot ${entry ? '' : 'entry-slot--empty'} ${hovered === slot ? 'entry-slot--over' : ''}`}
        aria-label={`Slot ${slot + 1}: ${entry ? card?.name ?? `Carta #${entry.cardId}` : 'vuoto'}`}
        onDragOver={(event) => {
          if (!disabled && !entry && event.dataTransfer.types.includes(CARD_DRAG_TYPE)) {
            event.preventDefault(); event.dataTransfer.dropEffect = 'copy'; setHovered(slot)
          }
        }}
        onDragLeave={() => setHovered(null)}
        onDrop={(event) => {
          event.preventDefault(); setHovered(null)
          if (!disabled && event.dataTransfer.types.includes(CARD_DRAG_TYPE)) onDropCard(event.dataTransfer.getData(CARD_DRAG_TYPE), slot)
        }}>
        <small>Slot {slot + 1}</small>
        {entry ? <>
          <CardTile card={card ?? { name: `Carta #${entry.cardId}`, cardId: entry.cardId, cardArtId: entry.cardArtId, artCode: `#${entry.cardArtId}` }} compact />
          <button type="button" disabled={disabled} className="icon-btn" aria-label={`Rimuovi ${card?.name ?? 'carta'} dallo slot ${slot + 1}`} onClick={() => onRemove(entry)}><Trash2 size={18} /></button>
        </> : <><Plus aria-hidden="true" /><span>{destination === 'EQUIPMENT' ? 'Equipaggiamento' : 'Creatura'}</span></>}
      </li>
    })}
  </ul>
}
