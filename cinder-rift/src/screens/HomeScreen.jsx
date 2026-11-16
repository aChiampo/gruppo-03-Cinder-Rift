import { useState } from 'react'
import Campaign from '../components/home/Campaign.jsx'
import PendingChallenges from '../components/home/PendingChallenges.jsx'
import LastMatch from '../components/home/LastMatch.jsx'
import NewChallengeBanner from '../components/home/NewChallengeBanner.jsx'
import News from '../components/home/News.jsx'
import Decks from '../components/home/Decks.jsx'
import { initialChallenges, lastMatch } from '../data/challenges.js'
import { decks } from '../data/decks.js'

export default function HomeScreen({ go, toast }) {
  const [challenges, setChallenges] = useState(initialChallenges)
  const [activeDeck, setActiveDeck] = useState('principale')
  const deckName = decks.find((d) => d.id === activeDeck).name

  const accept = (c) => {
    setChallenges((l) => l.filter((x) => x.id !== c.id))
    toast(`Sfida di ${c.from} accettata con ${deckName}.`)
  }

  return (
    <main className="grid">
      <div className="col-main">
        <Campaign onStart={() => toast('Campagna in arrivo: parte dal tutorial di SENTINEL.')} />
        <div className="two">
          <PendingChallenges items={challenges} onAccept={accept} onAll={() => go('gioca')} />
          <LastMatch match={lastMatch} onDetails={() => go('gioca')} />
        </div>
        <NewChallengeBanner onClick={() => go('gioca')} />
      </div>
      <aside className="col-side">
        <News onAll={() => toast('Pagina novità non ancora disponibile.')} />
        <Decks activeId={activeDeck} setActive={setActiveDeck} onManage={() => go('deck')} />
      </aside>
    </main>
  )
}
