import { useState } from 'react'
import { Package } from 'lucide-react'
import { cardArtworks } from '../../models/cards.js'

/** Missing or failed artwork uses a placeholder; a different URL can load independently. */
export default function CardArt({ card, artwork, className = '' }) {
  const src = (artwork ?? cardArtworks(card)[0])?.imageUrl
  const [failedSrc, setFailedSrc] = useState(null)
  if (!src || failedSrc === src) return <span className={`art art-placeholder ${className}`} aria-hidden="true"><Package strokeWidth={1.2} /></span>
  return <span className={`art ${className}`} aria-hidden="true">
    <span className="art-flip"><img src={src} alt="" loading="lazy" draggable="false"
      onError={() => setFailedSrc(src)} style={{ width: '100%', height: '100%', left: 0, top: 0 }} /></span>
  </span>
}
