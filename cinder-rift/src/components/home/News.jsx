import { Newspaper, ChevronRight } from 'lucide-react'
import Panel from '../ui/Panel.jsx'
import { news } from '../../data/news.js'

function NewsVisual({ v }) {
  if (v === 'packs')
    return (
      <div className="packs">
        <i style={{ '--c': '#165DD9' }} /><i style={{ '--c': '#44C3F4' }} /><i style={{ '--c': '#CCD7DE' }} />
      </div>
    )
  if (v === 'symbol') return <div className="patch"><img src="/img/symbol.png" alt="" /></div>
  return <img src={v} alt="" />
}

export default function News({ onAll }) {
  return (
    <Panel icon={Newspaper} title="NOVITÀ" link="TUTTE LE NOVITÀ" onLink={onAll}>
      <ul className="news">
        {news.map((n) => (
          <li key={n.id}>
            <button className="news-item">
              <div className="news-img"><NewsVisual v={n.visual} /></div>
              <div className="grow">
                <span className={`tag ${n.tone}`}>{n.tag}</span>
                <strong>{n.title}</strong>
                <p>{n.text}</p>
                <small>{n.date}</small>
              </div>
              <ChevronRight size={18} className="chev" />
            </button>
          </li>
        ))}
      </ul>
    </Panel>
  )
}
