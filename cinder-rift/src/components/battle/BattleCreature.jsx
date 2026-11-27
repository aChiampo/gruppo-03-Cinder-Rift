import { useEffect, useState } from 'react'
import { Shield, Skull, Swords } from 'lucide-react'
import CardArt from '../collection/CardArt.jsx'
import { getCatalog } from '../../services/cardService.js'
import { useAuth } from '../../context/AuthContext.jsx'

/** Catalog art is cosmetic; every displayed combat stat comes from the battle log. */
export default function BattleCreature({ creature, defeated, enemy = false }) {
  const { token } = useAuth()
  const [illustration, setIllustration] = useState(null)
  const name = creature?.name
  useEffect(() => {
    setIllustration(null)
    if (!name) return
    const controller = new AbortController()
    getCatalog({ name, page: 0, size: 100 }, { token, signal: controller.signal }).then((page) => {
      const matches = page.items.filter((card) => card.name === name)
      if (!controller.signal.aborted && page.totalPages <= 1 && matches.length === 1) setIllustration({ name, card: matches[0] })
    }).catch(() => { /* Cosmetic enrichment must not prevent report navigation. */ })
    return () => controller.abort()
  }, [name, token])
  const card = illustration?.name === name ? illustration.card : null
  return <div className={`battle-creature ${enemy ? 'battle-creature--enemy' : ''}`}>
    <figure className={`battle-card ${defeated ? 'battle-card--defeated' : ''}`}>
      <CardArt card={card ?? { artworks: [] }} />
      <figcaption>{name ?? 'Creatura non riportata'}</figcaption>
      {defeated && <span className="battle-card__defeated"><Skull size={16} /> Eliminata</span>}
    </figure>
    <div className="battle-creature__stats">
      <span><Swords size={18} /> Attacco <strong>{creature?.attack ?? '—'}</strong></span>
      <span><Shield size={18} /> Difesa <strong>{creature?.defense ?? '—'}</strong></span>
      <small>Prima dello scambio</small>
      {card?.artworks?.some((art) => art.imageUrl) && <small>Arte dal catalogo attuale</small>}
    </div>
  </div>
}
