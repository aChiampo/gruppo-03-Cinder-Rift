import { BookCopy, ChevronRight, Hexagon, ArrowRight } from 'lucide-react'
import Panel from '../ui/Panel.jsx'
import { decks } from '../../data/decks.js'
import { factionColors } from '../../data/factions.js'
import { isDeckValid } from '../../models/deck.js'

export default function Decks({ activeId, setActive, onManage }) {
  return (
    <Panel icon={BookCopy} title="I TUOI MAZZI" link="TUTTI I MAZZI" onLink={onManage}>
      <ul className="decks">
        {decks.map((d) => {
          const active = d.id === activeId
          return (
            <li key={d.id}>
              <button className={`deck ${active ? 'active' : ''}`} onClick={() => setActive(d.id)} title={isDeckValid(d) ? 'Mazzo valido' : 'Mazzo non valido'}>
                <img className="deck-img" src={d.img} alt="" />
                <Hexagon size={26} color={factionColors[d.faction]} />
                <span className="grow"><strong>{d.name}</strong><small>{d.cards} carte • {d.power} PT</small></span>
                {active ? <span className="in-use">IN USO</span> : <ChevronRight size={18} className="chev" />}
              </button>
            </li>
          )
        })}
      </ul>
      <button className="btn-ghost" onClick={onManage}>GESTISCI I MAZZI <ArrowRight size={16} /></button>
    </Panel>
  )
}
