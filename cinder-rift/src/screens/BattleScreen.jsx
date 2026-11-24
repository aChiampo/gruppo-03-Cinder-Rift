import { useRef, useState } from 'react'
import { Swords, ScrollText } from 'lucide-react'
import Panel from '../components/ui/Panel.jsx'
import RequestState from '../components/ui/RequestState.jsx'
import Unavailable from '../components/ui/Unavailable.jsx'
import { useDecks } from '../hooks/useDecks.js'
import { useAuth } from '../context/AuthContext.jsx'
import { playPve } from '../services/battleService.js'

export default function BattleScreen({ result, onResult, go }) {
  const decks = useDecks()
  const { token } = useAuth()
  const [tab, setTab] = useState('campagna')
  const [deckId, setDeckId] = useState('')
  const [stage, setStage] = useState('')
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState(null)
  const lock = useRef(false)
  const submit = async (event) => {
    event.preventDefault()
    if (lock.current) return
    lock.current = true
    setBusy(true); setError(null)
    try { onResult(await playPve(stage, deckId, token)) }
    catch (err) { setError(err) }
    finally { setBusy(false); lock.current = false }
  }
  return <main className="page-stack">
    <div className="page-heading"><h1><Swords /> GIOCA</h1><p>Scegli un mazzo e affronta la campagna.</p></div>
    <div className="tabs">{[['campagna', 'Campagna PvE'], ['ricevute', 'Sfide ricevute'], ['inviate', 'Sfide inviate'], ['storico', 'Storico']].map(([id, label]) => <button key={id} className={tab === id ? 'active' : ''} onClick={() => setTab(id)}>{label}</button>)}</div>
    {tab !== 'campagna' ? <section className="panel"><Unavailable feature={tab === 'storico' ? 'Le partite precedenti' : 'Le sfide tra giocatori'} /></section> : <>
      <Panel icon={Swords} title="NUOVA BATTAGLIA PvE">
        <RequestState {...decks} empty={decks.status === 'ready' && !decks.data?.length} emptyMessage="Crea un mazzo prima di iniziare una battaglia." />
        {decks.status === 'ready' && !!decks.data?.length && <form className="form-grid" onSubmit={submit}>
          <label>Mazzo<select required value={deckId} onChange={(e) => setDeckId(e.target.value)} disabled={busy}><option value="">Seleziona un mazzo</option>{decks.data.map((deck) => <option key={deck.id} value={deck.id}>Mazzo #{deck.id} · {deck.cardCount} carte</option>)}</select></label>
          <label>Stage<input required value={stage} onChange={(e) => setStage(e.target.value)} placeholder="stage_01" disabled={busy} /><small>Inserisci il codice dello stage. L’elenco della campagna non è ancora disponibile.</small></label>
          <button className="btn-primary" disabled={busy || !deckId}>{busy ? 'SIMULAZIONE IN CORSO…' : 'INIZIA BATTAGLIA'}</button>
        </form>}
        {error && <RequestState status="error" error={error} />}
        {error && <p className="hint">Controlla lo stato e il mazzo prima di avviare una nuova battaglia. Le richieste non vengono ripetute automaticamente dopo errori di rete.</p>}
        <button className="btn-ghost" onClick={() => go('deck')}>GESTISCI I MAZZI</button>
      </Panel>
      {result && <section className="panel battle-latest"><h2><ScrollText size={22} /> Ultimo risultato della sessione</h2><p>{result.stageTitle}</p><button className="btn-ghost" onClick={() => go('risultato-battaglia')}>VEDI RISULTATO E ROUND</button></section>}
    </>}
  </main>
}
