import { useEffect, useRef, useState } from 'react'
import { BookCopy, Layers, Plus, Save, Trash2 } from 'lucide-react'
import Panel from '../components/ui/Panel.jsx'
import RequestState from '../components/ui/RequestState.jsx'
import RoundSelector from '../components/deck-builder/RoundSelector.jsx'
import DeckSummary from '../components/deck-builder/DeckSummary.jsx'
import DeckValidation from '../components/deck-builder/DeckValidation.jsx'
import { useAuth } from '../context/AuthContext.jsx'
import { useDecks } from '../hooks/useDecks.js'
import { createDeck, getDeck, saveDeck, deleteDeck, positiveId } from '../services/deckService.js'

export default function DeckScreen({ go }) {
  const { token } = useAuth()
  const tokenRef = useRef(token)
  tokenRef.current = token
  const decks = useDecks()
  const [selectedId, setSelectedId] = useState(null)
  const [deck, setDeck] = useState(null)
  const [entries, setEntries] = useState([])
  const [challengeable, setChallengeable] = useState(false)
  const [activeRound, setActiveRound] = useState(1)
  const [group, setGroup] = useState('creature')
  const [cardId, setCardId] = useState('')
  const [artId, setArtId] = useState('')
  const [status, setStatus] = useState('idle')
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState(null)
  const [dirty, setDirty] = useState(false)
  const [notice, setNotice] = useState('')
  const [revision, setRevision] = useState(0)
  const actionLock = useRef(false)
  const destination = group === 'equipment' ? 'EQUIPMENT' : `ROUND${activeRound}`
  const apply = (value) => {
    setDeck(value); setEntries(value.entries); setChallengeable(value.isChallengeable); setDirty(false)
  }
  useEffect(() => {
    if (!selectedId) return
    const controller = new AbortController()
    setStatus('loading'); setError(null); setDeck(null); setNotice('')
    getDeck(selectedId, tokenRef.current, controller.signal).then((value) => {
      if (!controller.signal.aborted) { apply(value); setStatus('ready') }
    }).catch((err) => { if (!controller.signal.aborted) { setError(err); setStatus('error') } })
    return () => controller.abort()
  }, [selectedId, revision])

  const run = async (operation) => {
    if (actionLock.current) return
    actionLock.current = true; setBusy(true); setError(null); setNotice('')
    try { await operation() }
    catch (err) { setError(err) }
    finally { actionLock.current = false; setBusy(false) }
  }
  const select = (id) => {
    if (dirty && !window.confirm('Scartare le modifiche non salvate?')) return
    setDirty(false); setSelectedId(id)
  }
  const create = () => run(async () => {
    if (dirty && !window.confirm('Scartare le modifiche non salvate?')) return
    const value = await createDeck(tokenRef.current)
    setDirty(false); setSelectedId(value.id); decks.retry()
  })
  const save = () => run(async () => {
    const value = await saveDeck(deck.id, entries, challengeable, tokenRef.current)
    apply(value); setNotice('Mazzo salvato e validato dal server.'); decks.retry()
  })
  const remove = () => {
    if (!window.confirm(`Eliminare definitivamente il mazzo #${deck.id}?`)) return
    run(async () => { await deleteDeck(deck.id, tokenRef.current); setSelectedId(null); setDeck(null); setEntries([]); setDirty(false); setStatus('idle'); setNotice('Mazzo eliminato.'); decks.retry() })
  }
  const add = (event) => {
    event.preventDefault(); setError(null)
    try {
      const entry = { cardId: positiveId(cardId), cardArtId: positiveId(artId), destination }
      setEntries((list) => [...list, entry]); setDirty(true); setNotice('')
    } catch (err) { setError(err) }
  }
  return <main className="deck-builder-layout live-deck-layout">
    <aside className="panel">
      <h2><Layers size={22} /> COLLEZIONE CARTE</h2>
      <p className="hint">Consulta le tue carte nella collezione. La selezione visiva per il mazzo resta da integrare.</p>
      <button className="btn-ghost" onClick={() => go('collezione')}>APRI COLLEZIONE</button>
      <hr />
      <div className="section-heading"><h2>I TUOI MAZZI</h2><button className="icon-btn" aria-label="Crea mazzo" onClick={create} disabled={busy || status === 'loading'}><Plus /></button></div>
      <RequestState {...decks} empty={decks.status === 'ready' && !decks.data?.length} emptyMessage="Non hai ancora creato un mazzo." />
      {decks.status === 'ready' && <ul className="decks">{decks.data.map((item) => <li key={item.id}><button className={`deck ${selectedId === item.id ? 'active' : ''}`} onClick={() => select(item.id)} disabled={busy}><BookCopy /><span className="grow"><strong>Mazzo #{item.id}</strong><small>{item.cardCount} carte · {item.isChallengeable ? 'Sfidabile' : 'Bozza'}</small></span></button></li>)}</ul>}
      <button className="btn-outline" onClick={create} disabled={busy || status === 'loading'}>CREA MAZZO</button>
    </aside>
    <section className="panel deck-editor">
      <h1>DECK BUILDER</h1><p className="hint">Organizza le copie nei cinque round e nel gruppo equipaggiamenti.</p>
      {status === 'loading' && <RequestState status="loading" />}
      {error && <RequestState status="error" error={error} retry={status === 'error' ? () => setRevision((n) => n + 1) : undefined} />}
      {!selectedId && <p className="request-state">Seleziona un mazzo o crea una nuova bozza.</p>}
      {deck && status === 'ready' && <fieldset disabled={busy}>
        <div className="section-heading"><h2>Mazzo #{deck.id}</h2><span>{dirty ? 'Modifiche non salvate' : 'Salvato'}</span></div>
        <RoundSelector activeRound={activeRound} onChange={(round) => { setActiveRound(round); setGroup('creature') }} />
        <div className="tabs"><button type="button" className={group === 'creature' ? 'active' : ''} onClick={() => setGroup('creature')}>Creature</button><button type="button" className={group === 'equipment' ? 'active' : ''} onClick={() => setGroup('equipment')}>Equipaggiamenti</button></div>
        <h3>{group === 'equipment' ? 'Equipaggiamenti' : `Round ${activeRound}`}</h3>
        <ul className="entry-slots">{entries.map((entry, index) => entry.destination === destination && <li key={index} className="entry-slot"><BookCopy /><strong>Carta #{entry.cardId}</strong><small>Variante #{entry.cardArtId}</small><button className="icon-btn" aria-label={`Rimuovi copia ${index + 1}`} onClick={() => { setEntries((list) => list.filter((_, i) => i !== index)); setDirty(true); setNotice('') }}><Trash2 size={18} /></button></li>)}</ul>
        {!entries.some((entry) => entry.destination === destination) && <p className="hint">Nessuna copia assegnata a questo gruppo.</p>}
        <details className="manual-entry"><summary>Aggiungi una copia tramite identificativi</summary><p className="hint">La selezione visiva delle carte non è ancora disponibile. Se conosci gli identificativi di una carta e della sua variante posseduta, puoi aggiungerli qui; il server verifica il possesso al salvataggio.</p>
          <form className="form-grid" onSubmit={add}><label>ID carta<input type="number" min="1" max="2147483647" step="1" required value={cardId} onChange={(e) => setCardId(e.target.value)} /></label><label>ID variante<input type="number" min="1" max="2147483647" step="1" required value={artId} onChange={(e) => setArtId(e.target.value)} /></label><button className="btn-outline">AGGIUNGI A {destination}</button></form>
        </details>
        <label className="check-label"><input type="checkbox" checked={challengeable} onChange={(e) => { setChallengeable(e.target.checked); setDirty(true); setNotice('') }} />Rendi il mazzo sfidabile</label>
        <div className="action-row"><button type="button" className="btn-primary" onClick={save}><Save size={18} />{busy ? 'SALVATAGGIO…' : 'SALVA E VERIFICA'}</button><button type="button" className="btn-outline danger" onClick={remove}><Trash2 size={18} /> ELIMINA</button></div>
      </fieldset>}
      {notice && <p role="status" className="success-message">{notice}</p>}
    </section>
    <aside className="panel">
      <DeckSummary validation={deck?.validation} dirty={dirty} />
      <DeckValidation validation={error?.data?.errors ? error.data : deck?.validation} dirty={dirty && !error?.data?.errors} />
    </aside>
  </main>
}
