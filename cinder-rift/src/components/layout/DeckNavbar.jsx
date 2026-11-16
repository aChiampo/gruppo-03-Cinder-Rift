import { Bell, Home, Layers, Swords, UserRound } from 'lucide-react'

const navItems = [
  { id: 'home', label: 'Home', icon: Home },
  { id: 'gioca', label: 'Gioca', icon: Swords },
  { id: 'deck-builder', label: 'Deck Builder', icon: Layers },
  { id: 'collezione', label: 'Collezione', icon: Layers },
  { id: 'profilo', label: 'Profilo', icon: UserRound },
  { id: 'notifiche', label: 'Notifiche', icon: Bell },
]

export default function DeckNavbar({ page, setPage }) {
  return (
    <header className="deck-navbar">
      <div className="deck-navbar__brand" role="button" tabIndex={0} onClick={() => setPage('home')} onKeyDown={(event) => { if (event.key === 'Enter') setPage('home') }}>
        <div className="deck-navbar__logo">C</div>
        <span>CINDER-RIFT</span>
      </div>

      <nav className="deck-navbar__nav" aria-label="Navigazione principale">
        {navItems.map((item) => {
          const Icon = item.icon
          const isActive = item.id === page

          return (
            <button
              key={item.id}
              type="button"
              className={`deck-navbar__item ${isActive ? 'active' : ''}`}
              onClick={() => setPage(item.id)}
            >
              <Icon size={16} />
              <span>{item.label}</span>
            </button>
          )
        })}
      </nav>

      <div className="deck-navbar__profile">
        <div className="deck-navbar__avatar">M</div>
        <span>Mirko</span>
      </div>
    </header>
  )
}
