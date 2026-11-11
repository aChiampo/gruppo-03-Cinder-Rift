import { Home, Layers, BookCopy, Swords, ShoppingBag, Settings, Plus, Coins, Bell, UserRound } from 'lucide-react'
import { user } from '../../data/user.js'
import { navItems } from '../../data/navigation.js'

const icons = { Home, Layers, BookCopy, Swords, ShoppingBag, Bell, UserRound }

export default function Header({ page, setPage, credits }) {
  return (
    <header className="header">
      <button className="brand" onClick={() => setPage('home')} aria-label="Cinder-Rift home">
        <img src="/img/logo-horizontal.png" alt="Cinder-Rift — The Cobalt Addiction" />
      </button>

      <nav className="nav">
        {navItems.map((n) => {
          const Icon = icons[n.icon]
          return (
            <button key={n.id} className={`nav-item ${page === n.id ? 'active' : ''}`} onClick={() => setPage(n.id)}>
              <Icon size={20} /> <span>{n.label}</span>
            </button>
          )
        })}
      </nav>

      <div className="userbox">
        <button className="profile" onClick={() => setPage('profilo')}>
          <img src={user.avatar} alt="" />
          <span><strong>{user.name}</strong><small>Livello {user.level}</small></span>
        </button>
        <div className="credits" title="Saldo crediti (sempre visibile)">
          <Coins size={22} color="#44C3F4" />
          <b>{credits.toLocaleString('it-IT')}</b>
          <button className="plus" onClick={() => setPage('negozio')} aria-label="Acquista crediti"><Plus size={16} /></button>
        </div>
        <button className="icon-btn" onClick={() => setPage('impostazioni')} aria-label="Impostazioni"><Settings size={22} /></button>
      </div>
    </header>
  )
}
