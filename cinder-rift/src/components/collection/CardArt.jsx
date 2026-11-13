import { useState } from "react";
import { Cpu, Bomb, Ghost, Package } from "lucide-react";

const icons = { Cpu, Bomb, Ghost };

/**
 * Illustrazione di una carta. `art.x/y/zoom/flip` ritagliano la stessa immagine in modi diversi.
 * Senza `src` (o se l'immagine non si carica) mostra un segnaposto con icona: la pagina non resta mai "rotta".
 */
export default function CardArt({ card, className = "" }) {
  const { src, x = 50, y = 50, zoom = 1, flip = false, icon } = card.art;
  const [failed, setFailed] = useState(false);

  if (!src || failed) {
    const Icon = icons[icon] ?? Package;
    return (
      <span className={`art art-placeholder ${className}`} aria-hidden="true">
        <Icon strokeWidth={1.2} />
      </span>
    );
  }
  return (
    <span className={`art ${className}`} aria-hidden="true">
      <span
        className="art-flip"
        style={flip ? { transform: "scaleX(-1)" } : undefined}
      >
        <img
          src={src}
          alt=""
          loading="lazy"
          draggable="false"
          onError={() => setFailed(true)}
          style={{
            width: `${zoom * 100}%`,
            height: `${zoom * 100}%`,
            left: `${-(zoom - 1) * x}%`,
            top: `${-(zoom - 1) * y}%`,
          }}
        />
      </span>
    </span>
  );
}
