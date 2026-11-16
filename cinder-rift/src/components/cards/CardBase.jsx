// Colori rarità: palette del progetto (#CCD7DE, #44C3F4, #165DD9); la leggendaria resta oro per distinguerla.
const rarityColors = {
  Comune: '#CCD7DE',
  Rara: '#44C3F4',
  Epica: '#165DD9',
  Leggendaria: '#ffd166',
}

export default function CardBase({
  card,
  selected = false,
  error = false,
  onSelect,
  onRemove,
  compact = false,
  children,
}) {
  const borderColor = error ? '#f04455' : rarityColors[card?.rarity] || '#CCD7DE'

  return (
    <div
      className={`card-base ${selected ? 'selected' : ''} ${error ? 'error' : ''} ${compact ? 'compact' : ''}`}
      style={{ '--card-border': borderColor }}
    >
      <button
        type="button"
        className="card-base__button"
        onClick={() => onSelect?.(card)}
        aria-label={card?.name || 'Carta'}
      >
        <div className="card-base__image-wrap">
          <img src={card?.image || '/img/logo-horizontal.png'} alt={card?.name || 'Carta'} />
          {card?.energy !== undefined && <span className="card-base__energy">{card.energy}</span>}
        </div>

        <div className="card-base__body">
          <div className="card-base__title-row">
            <strong>{card?.name}</strong>
          </div>

          <div className="card-base__meta">
            <span>{card?.faction || 'Generica'}</span>
            <span>{card?.rarity || 'Comune'}</span>
          </div>

          <div className="card-base__stats">{children}</div>
        </div>
      </button>

      {onRemove && (
        <button
          type="button"
          className="card-base__remove"
          onClick={() => onRemove(card)}
          aria-label={`Rimuovi ${card?.name}`}
        >
          ×
        </button>
      )}
    </div>
  )
}
