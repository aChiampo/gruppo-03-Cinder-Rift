import { Newspaper, Swords, Trophy } from 'lucide-react'
import Campaign from '../components/home/Campaign.jsx'
import NewChallengeBanner from '../components/home/NewChallengeBanner.jsx'
import Decks from '../components/home/Decks.jsx'
import Panel from '../components/ui/Panel.jsx'
import Unavailable from '../components/ui/Unavailable.jsx'
import { useDecks } from '../hooks/useDecks.js'
export default function HomeScreen({ go }) {
  const decks = useDecks()
  return <main className="grid">
    <div className="col-main">
      <Campaign onStart={() => go('gioca')} />
      <div className="two">
        <Panel icon={Swords} title="SFIDE IN ATTESA"><Unavailable feature="Le sfide tra giocatori" /></Panel>
        <Panel icon={Trophy} title="ULTIMA SFIDA"><Unavailable feature="Lo storico delle partite" /></Panel>
      </div>
      <NewChallengeBanner onClick={() => go('gioca')} />
    </div>
    <aside className="col-side">
      <Panel icon={Newspaper} title="NOVITÀ"><Unavailable feature="Le novità" /></Panel>
      <Decks items={decks.data ?? []} {...decks} onManage={() => go('deck')} />
    </aside>
  </main>
}
