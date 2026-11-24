import { BookCopy, ArrowRight } from 'lucide-react'
import Panel from '../ui/Panel.jsx'
import RequestState from '../ui/RequestState.jsx'
export default function Decks({ items = [], status, error, retry, onManage }) {
  return <Panel icon={BookCopy} title="I TUOI MAZZI" link="TUTTI I MAZZI" onLink={onManage}>
    <RequestState status={status} error={error} retry={retry} empty={items.length === 0} emptyMessage="Non hai ancora creato un mazzo." />
    {status === 'ready' && <ul className="decks">{items.map((deck) => <li key={deck.id}><button className="deck" onClick={onManage}><BookCopy size={28} /><span className="grow"><strong>Mazzo #{deck.id}</strong><small>{deck.cardCount} carte · {deck.isChallengeable ? 'Sfidabile' : 'Bozza'}</small></span></button></li>)}</ul>}
    <button className="btn-ghost" onClick={onManage}>GESTISCI I MAZZI <ArrowRight size={16} /></button>
  </Panel>
}
