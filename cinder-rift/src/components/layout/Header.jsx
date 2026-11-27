import { Home, Layers, Library, BookCopy, Swords, ShoppingBag, Settings, UserRound, LogOut } from 'lucide-react'
import { useAuth } from '../../context/AuthContext.jsx'
import { navItems } from '../../data/navigation.js'
const icons = { Home, Layers, Library, BookCopy, Swords, ShoppingBag }

export default function Header({ page, setPage }) {
  const { user, logout } = useAuth()
  return <header className="header">
    <button className="brand" onClick={() => setPage('home')} aria-label="Cinder-Rift home"><img src="/img/logo-horizontal.png" alt="Cinder-Rift" /></button>
    <nav className="nav" aria-label="Navigazione principale">{navItems.map((item) => {
      const Icon = icons[item.icon]
      return <button key={item.id} className={`nav-item ${page === item.id ? 'active' : ''}`} aria-current={page === item.id ? 'page' : undefined} onClick={() => setPage(item.id)}><Icon size={18} /><span>{item.label}</span></button>
    })}</nav>
    <div className="userbox">
      <button className="profile" onClick={() => setPage('profilo')} aria-label="Apri profilo">{user.avatar ? <img src={user.avatar} alt="" onError={(e) => { e.currentTarget.style.display = 'none' }} /> : <UserRound size={28} />}<span><strong>{user.name}</strong><small>{user.rankingPoints ?? '—'} punti classifica</small></span></button>
      <button className="icon-btn" onClick={() => setPage('impostazioni')} aria-label="Impostazioni"><Settings size={22} /></button>
      <button className="icon-btn" onClick={logout} aria-label="Esci"><LogOut size={22} /></button>
    </div>
  </header>
}
