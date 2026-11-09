import { Eye, ArrowRight, CalendarClock, Clock, Trophy } from 'lucide-react'
import Panel from '../ui/Panel.jsx'

export default function LastMatch({ match: m, onDetails }) {
  return (
    <Panel icon={Trophy} title="ULTIMA SFIDA">
      <div className="match">
        <div className="player">
          <img className="avatar lg cyan" src={m.me.avatar} alt="" />
          <div><strong>{m.me.name}</strong><span className="win">{m.result}</span></div>
        </div>
        <div className="score"><b>VS</b><em>{m.score}</em><small>({m.rounds} round)</small></div>
        <div className="player right">
          <div><strong>{m.opponent.name}</strong></div>
          <img className="avatar lg red" src={m.opponent.avatar} alt="" />
        </div>
      </div>
      <button className="btn-ghost" onClick={onDetails}><Eye size={18} /> VEDI DETTAGLI <ArrowRight size={16} /></button>
      <div className="meta">
        <div><CalendarClock size={20} /><span><small>Data</small>{m.date}</span></div>
        <div><Clock size={20} /><span><small>Durata</small>{m.duration}</span></div>
        <div><Trophy size={20} /><span><small>Modalità</small>{m.mode}</span></div>
      </div>
    </Panel>
  )
}
