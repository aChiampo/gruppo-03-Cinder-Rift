import { Cpu, Bomb, Ghost } from 'lucide-react'

const icons = { Cpu, Bomb, Ghost }

/**
 * Illustrazione di una carta. `art.x/y/zoom/flip` ritagliano la stessa immagine in modi diversi;
 * se manca `src` mostra un segnaposto con icona (equipaggiamenti senza illustrazione).
 */
export default function CardArt({ card, className = '' }) {
  const { src, x = 50, y = 50, zoom = 1, flip = false, icon } = card.art
  if (!src) {
    const Icon = icons[icon]
    return (
      <span className={`art art-placeholder ${className}`} aria-hidden="true">
        {Icon && <Icon strokeWidth={1.2} />}
      </span>
    )
  }
  return (
    <span className={`art ${className}`} aria-hidden="true">
      <span className="art-flip" style={flip ? { transform: 'scaleX(-1)' } : undefined}>
        <img
          src={src} alt="" loading="lazy" draggable="false"
          style={{ width: `${zoom * 100}%`, height: `${zoom * 100}%`, left: `${-(zoom - 1) * x}%`, top: `${-(zoom - 1) * y}%` }}
        />
      </span>
    </span>
  )
}
