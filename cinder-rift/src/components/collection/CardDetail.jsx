import { Swords, Shield, Gem, Zap, Sparkles, Timer } from 'lucide-react'
import Hex from '../ui/Hex.jsx'
import FactionIcon from '../ui/FactionIcon.jsx'
import CardArt from './CardArt.jsx'
import { cardColor } from './CardTile.jsx'
import { factionById, NEUTRAL } from '../../data/factions.js'
import { rarityById } from '../../models/cards.js'

function Row({ icon, label, children }) {
  return (
    <li><span className="row-label">{icon} {label}</span><b className="row-value">{children}</b></li>
  )
}

export default function CardDetail({ card }) {
  const color = cardColor(card)
  const rarity = rarityById[card.rarity]
  const isCreature = card.type === 'creature'
  const faction = isCreature ? factionById[card.faction] : NEUTRAL

  return (
    <div className="detail" style={{ '--c': color }}>
      <div className="detail-card">
        <div className="detail-art">
          <CardArt card={card} />
          <span className="detail-energy"><Hex color={color} size={44} filled><b>{card.energy}</b></Hex></span>
        </div>
        <div className="detail-plate">
          <h3>{card.name}</h3>
          <span className="detail-faction" style={isCreature ? { color } : undefined}>
            {isCreature && <FactionIcon faction={card.faction} size={18} />} {faction.singular}
          </span>
          <span className="detail-rarity" style={{ color: rarity.color }}>{rarity.label}</span>
        </div>
        {isCreature ? (
          <div className="detail-stats">
            <span><Swords size={22} color={color} /> <b>{card.attack}</b></span>
            <span><Shield size={22} /> <b>{card.defense}</b></span>
          </div>
        ) : (
          <div className="detail-stats single"><Timer size={18} /> DURATA: {card.duration.toUpperCase()}</div>
        )}
      </div>

      <h3 className="detail-title">DETTAGLI CARTA</h3>
      <ul className="detail-rows">
        <Row icon={isCreature ? <FactionIcon faction={card.faction} size={20} /> : <Hex color={color} size={18} glow={false} />} label="Fazione">
          <span style={isCreature ? { color } : undefined}>{faction.label}</span>
        </Row>
        <Row icon={<Gem size={17} />} label="Rarità"><span style={{ color: rarity.color }}>{rarity.label}</span></Row>
        <Row icon={<Zap size={17} />} label="Costo energia">{card.energy}</Row>
        <Row icon={<Sparkles size={17} />} label="Punti potenza">{card.power}</Row>
        {isCreature ? (
          <>
            <Row icon={<Swords size={17} />} label="Attacco">{card.attack}</Row>
            <Row icon={<Shield size={17} />} label="Difesa">{card.defense}</Row>
          </>
        ) : (
          <Row icon={<Timer size={17} />} label="Durata">{card.duration}</Row>
        )}
      </ul>

      <blockquote className="detail-quote">
        {isCreature ? (
          <>
            <p>{card.flavor}</p>
            {card.lore && <p className="lore">{card.lore}</p>}
          </>
        ) : (
          <>
            <p className="effect">{card.effect}</p>
            <p className="lore">{card.flavor}</p>
          </>
        )}
      </blockquote>
    </div>
  )
}
