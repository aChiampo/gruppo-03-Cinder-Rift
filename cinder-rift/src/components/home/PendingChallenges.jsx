import { Swords } from 'lucide-react'
import Panel from '../ui/Panel.jsx'

export default function PendingChallenges({ items, onAccept, onAll }) {
  return (
    <Panel icon={Swords} title="SFIDE IN ATTESA DI RISPOSTA" link="VAI ALLE SFIDE" onLink={onAll}>
      <ul className="rows">
        {items.length === 0 && <li className="empty">Nessuna sfida in attesa.</li>}
        {items.map((c) => (
          <li key={c.id} className="row">
            <img className="avatar" src={c.avatar} alt="" />
            <div className="grow"><strong>{c.from}</strong><small>{c.type} • {c.ago}</small></div>
            <button className="btn-outline" onClick={() => onAccept(c)}>ACCETTA</button>
          </li>
        ))}
      </ul>
    </Panel>
  )
}
