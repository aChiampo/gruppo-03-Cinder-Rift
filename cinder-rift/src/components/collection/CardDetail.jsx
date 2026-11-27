import { useState } from 'react'
import { Swords, Shield, Gem, Zap, Sparkles } from 'lucide-react'
import Hex from '../ui/Hex.jsx'
import FactionIcon from '../ui/FactionIcon.jsx'
import CardArt from './CardArt.jsx'
import { cardAppearance, cardArtworks, referenceLabel } from '../../models/cards.js'

function Row({ icon, label, children }) {
  return <li><span className="row-label">{icon} {label}</span><b className="row-value">{children}</b></li>
}

export default function CardDetail({ card }) {
  const { color, factionId, factionLabel, rarityLabel, rarityColor } = cardAppearance(card)
  const artworks = cardArtworks(card)
  const [chosenArt, setChosenArt] = useState(null)
  const artwork = artworks.find((entry) => entry.cardArtId === chosenArt) ?? artworks[0]
  const isCreature = card.type?.code === 'CREATURE'
  return <div className="detail" style={{ '--c': color }}>
    <div className="detail-card">
      <div className="detail-art">
        <CardArt card={card} artwork={artwork} />
        <span className="detail-energy"><Hex color={color} size={44} filled><b>{card.energyCost ?? '—'}</b></Hex></span>
      </div>
      <div className="detail-plate">
        <h3>{card.name ?? 'Carta senza nome'}</h3>
        <span className="detail-faction" style={{ color }}>
          {factionId && <FactionIcon faction={factionId} size={18} />}{factionLabel}
        </span>
        <span className="detail-rarity" style={{ color: rarityColor }}>{rarityLabel}</span>
      </div>
      {isCreature && <div className="detail-stats">
        <span><Swords size={22} color={color} /><b>{card.attack ?? '—'}</b></span>
        <span><Shield size={22} /><b>{card.defense ?? '—'}</b></span>
      </div>}
    </div>
    {artworks.length > 1 && <label className="artwork-choice">Variante grafica
      <select aria-label="Variante grafica" value={artwork.cardArtId} onChange={(event) => setChosenArt(Number(event.target.value))}>
        {artworks.map((entry) => <option key={entry.cardArtId} value={entry.cardArtId}>{entry.artCode}</option>)}
      </select>
    </label>}
    <h3 className="detail-title">DETTAGLI CARTA</h3>
    <ul className="detail-rows">
      <Row label="Tipo">{referenceLabel(card.type)}</Row>
      <Row label="Espansione">{referenceLabel(card.expansion)}</Row>
      <Row label="Variante">{artwork?.artCode ?? 'Non disponibile'}</Row>
      <Row icon={factionId && <FactionIcon faction={factionId} size={20} />} label="Fazione">{factionLabel}</Row>
      <Row icon={<Gem size={17} />} label="Rarità"><span style={{ color: rarityColor }}>{rarityLabel}</span></Row>
      <Row icon={<Zap size={17} />} label="Costo energia">{card.energyCost ?? '—'}</Row>
      <Row icon={<Sparkles size={17} />} label="Punti potenza">{card.power ?? '—'}</Row>
      {isCreature && <>
        <Row icon={<Swords size={17} />} label="Attacco">{card.attack ?? '—'}</Row>
        <Row icon={<Shield size={17} />} label="Difesa">{card.defense ?? '—'}</Row>
      </>}
      {card.ownedReplica !== undefined && <>
        <Row label="Copie possedute">{card.ownedReplica}</Row>
        <Row label="Copie scambiabili registrate">{card.tradableReplica}</Row>
      </>}
    </ul>
    {card.ownedReplica !== undefined && <p className="hint">Le copie scambiabili registrate non sottraggono le prenotazioni degli scambi.</p>}
  </div>
}
