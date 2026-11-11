import { Bot, Radiation, Sun, Eye, Orbit } from 'lucide-react'
import Hex from './Hex.jsx'
import { factionById } from '../../data/factions.js'

const glyphs = { Bot, Radiation, Sun, Eye, Orbit }

export default function FactionIcon({ faction, size = 22 }) {
  const f = factionById[faction]
  const Glyph = glyphs[f.icon]
  return (
    <Hex color={f.color} size={size} glow={false}>
      <Glyph size={Math.round(size * 0.5)} color={f.color} strokeWidth={2.4} />
    </Hex>
  )
}
