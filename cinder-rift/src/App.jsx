import { useEffect, useState } from 'react'
import Header from './components/layout/Header.jsx'
import LoginScreen from './screens/LoginScreen.jsx'
import HomeScreen from './screens/HomeScreen.jsx'
import CollectionScreen from './screens/CollectionScreen.jsx'
import DeckScreen from './screens/DeckScreen.jsx'
import BattleScreen from './screens/BattleScreen.jsx'
import ShopScreen from './screens/ShopScreen.jsx'
import ProfileScreen from './screens/ProfileScreen.jsx'
import { useAuth } from './context/AuthContext.jsx'
import RequestState from './components/ui/RequestState.jsx'

const pages = new Set(['home', 'catalogo', 'collezione', 'deck', 'gioca', 'negozio', 'profilo', 'impostazioni'])
const currentPage = () => { const page = window.location.hash.slice(1); return pages.has(page) ? page : 'home' }
export default function App() {
  const { user, checking, sessionError, restore } = useAuth()
  const [page, setPage] = useState(currentPage)
  const [authMode, setAuthMode] = useState('login')
  const [battle, setBattle] = useState(null)
  const go = (next) => { if (pages.has(next)) { window.location.hash = next; setPage(next); window.scrollTo(0, 0) } }
  useEffect(() => {
    const change = () => setPage(currentPage())
    window.addEventListener('hashchange', change)
    return () => window.removeEventListener('hashchange', change)
  }, [])
  useEffect(() => { if (!user) setBattle(null) }, [user?.id])
  useEffect(() => { document.title = `${page.charAt(0).toUpperCase() + page.slice(1)} — Cinder-Rift` }, [page])
  if (checking) return <div className="session-check" role="status">Verifica sessione…</div>
  if (!user) return <LoginScreen key={authMode} mode={authMode} onMode={setAuthMode} />
  return <div className="app">
    <Header page={page} setPage={go} />
    {sessionError && <RequestState status="error" error={sessionError} retry={restore} />}
    {page === 'home' && <HomeScreen go={go} />}
    {(page === 'catalogo' || page === 'collezione') && <CollectionScreen key={page} go={go} catalog={page === 'catalogo'} />}
    {page === 'deck' && <DeckScreen go={go} />}
    {page === 'gioca' && <BattleScreen result={battle} onResult={setBattle} go={go} />}
    {page === 'negozio' && <ShopScreen go={go} />}
    {(page === 'profilo' || page === 'impostazioni') && <ProfileScreen key={page} settings={page === 'impostazioni'} />}
  </div>
}
