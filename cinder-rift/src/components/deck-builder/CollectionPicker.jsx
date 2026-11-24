import { useState } from 'react'
import CardTile from '../collection/CardTile.jsx'
import RequestState from '../ui/RequestState.jsx'
import { cardKey } from '../../models/cards.js'
import { availableCopies, cardFitsDestination, CARD_DRAG_TYPE, firstEmptySlot } from '../../models/deckBuilder.js'

export default function CollectionPicker({ collection, entries, destination, disabled, onAdd }) {
  const [search, setSearch] = useState('')
  const cards = collection.data.filter((card) => cardFitsDestination(card, destination) &&
    (card.name ?? '').toLocaleLowerCase().includes(search.trim().toLocaleLowerCase()))
  const full = firstEmptySlot(entries, destination) === undefined
  return <>
    <label className="deck-collection-search">Cerca nella collezione
      <input type="search" value={search} maxLength={100} onChange={(event) => setSearch(event.target.value)} />
    </label>
    <RequestState {...collection} empty={collection.status === 'ready' && cards.length === 0} emptyMessage="Nessuna carta disponibile per questo gruppo o questa ricerca." />
    {collection.status === 'ready' && <ul className="deck-collection-grid deck-card-grid" aria-label="Carte disponibili">
      {cards.map((card) => {
        const remaining = availableCopies(card, entries)
        return <li key={cardKey(card)}>
          <CardTile card={card} compact disabled={disabled || full || remaining === 0} onSelect={() => onAdd(card)}
            actionLabel={`Aggiungi ${card.name ?? 'carta'} (${card.artCode}), ${remaining} copie disponibili`}
            draggable onDragStart={(event) => {
              if (disabled || full || remaining === 0) { event.preventDefault(); return }
              event.dataTransfer.effectAllowed = 'copy'
              event.dataTransfer.setData(CARD_DRAG_TYPE, cardKey(card))
            }} />
          <small className="deck-copy-count">Disponibili: {remaining} / {card.ownedReplica}</small>
        </li>
      })}
    </ul>}
  </>
}
