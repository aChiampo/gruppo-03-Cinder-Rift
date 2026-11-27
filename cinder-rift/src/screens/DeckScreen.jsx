import { useEffect, useRef, useState } from 'react'
import { BookCopy, Plus, Save, Trash2 } from 'lucide-react'
import RequestState from '../components/ui/RequestState.jsx'
import RoundSelector from '../components/deck-builder/RoundSelector.jsx'
import CollectionPicker from '../components/deck-builder/CollectionPicker.jsx'
import DeckSlots from '../components/deck-builder/DeckSlots.jsx'
import DeckSummary from '../components/deck-builder/DeckSummary.jsx'
import DeckValidation from '../components/deck-builder/DeckValidation.jsx'
import { useAuth } from '../context/AuthContext.jsx'
import { useDecks } from '../hooks/useDecks.js'
import { useDeckCollection } from '../hooks/useDeckCollection.js'
import { cardKey } from '../models/cards.js'
import { assignSlots, placeCard } from '../models/deckBuilder.js'
import { createDeck, getDeck, saveDeck, deleteDeck } from '../services/deckService.js'

export default function DeckScreen({ go }) {
  const { token } = useAuth()
  const tokenRef = useRef(token)
  tokenRef.current = token
  const decks = useDecks()
  const collection = useDeckCollection()
  const [selectedId, setSelectedId] = useState(null)
  const [deck, setDeck] = useState(null)
  const [entries, setEntries] = useState([])
  const [challengeable, setChallengeable] = useState(false)
  const [destination, setDestination] = useState('ROUND1')
  const [status, setStatus] = useState('idle')
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState(null)
  const [dirty, setDirty] = useState(false)
  const [notice, setNotice] = useState('')
  const [revision, setRevision] = useState(0)
  const actionLock = useRef(false)
  const editingDisabled = busy || status !== 'ready' || !deck || selectedId !== deck.id
  const apply = (value) => {
    setDeck(value); setEntries(assignSlots(value.entries)); setChallengeable(value.isChallengeable); setDirty(false)
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
  const add = (card, slot) => {
    if (editingDisabled || actionLock.current || collection.status !== 'ready') return
    setError(null)
    try {
      const next = placeCard(entries, card, destination, slot)
      setEntries(next); setDirty(true); setNotice(`${card.name ?? 'Carta'} aggiunta a ${destination}, slot ${next.at(-1).slot + 1}.`)
    } catch (err) { setError(err) }
  }
  return <main className="deck-builder-layout live-deck-layout">
    <aside className="panel">
      <div className="section-heading"><h2>I TUOI MAZZI</h2><button className="icon-btn" aria-label="Crea mazzo" onClick={create} disabled={busy || status === 'loading'}><Plus /></button></div>
      <RequestState {...decks} empty={decks.status === 'ready' && !decks.data?.length} emptyMessage="Non hai ancora creato un mazzo." />
      {decks.status === 'ready' && <ul className="decks">{decks.data.map((item) => <li key={item.id}><button className={`deck ${selectedId === item.id ? 'active' : ''}`} onClick={() => select(item.id)} disabled={busy}><BookCopy /><span className="grow"><strong>Mazzo #{item.id}</strong><small>{item.cardCount} carte · {item.isChallengeable ? 'Sfidabile' : 'Bozza'}</small></span></button></li>)}</ul>}
      <button className="btn-outline" onClick={create} disabled={busy || status === 'loading'}>CREA MAZZO</button>
      <hr />
      <CollectionPicker collection={collection} entries={entries} destination={destination} disabled={editingDisabled} onAdd={add} />
      <button className="btn-ghost" onClick={() => go('collezione')}>APRI COLLEZIONE</button>
    </aside>
    <section className="panel deck-editor">
      <h1>DECK BUILDER</h1><p className="hint">Organizza le copie nei cinque round e nel gruppo equipaggiamenti.</p>
      {status === 'loading' && <RequestState status="loading" />}
      {error && <RequestState status="error" error={error} retry={status === 'error' ? () => setRevision((n) => n + 1) : undefined} />}
      {!selectedId && <p className="request-state">Seleziona un mazzo o crea una nuova bozza.</p>}
      {deck && status === 'ready' && <fieldset disabled={busy}>
        <div className="section-heading"><h2>Mazzo #{deck.id}</h2><span>{dirty ? 'Modifiche non salvate' : 'Salvato'}</span></div>
        <RoundSelector activeDestination={destination} onChange={setDestination} />
        <h3>{destination === 'EQUIPMENT' ? 'Equipaggiamenti' : `Round ${destination.slice(-1)}`}</h3>
        <DeckSlots entries={entries} cards={collection.data} destination={destination} disabled={editingDisabled}
          onDropCard={(key, slot) => add(collection.data.find((card) => cardKey(card) === key), slot)}
          onRemove={(entry) => { setEntries((list) => list.filter((item) => item !== entry)); setDirty(true); setNotice('Carta rimossa.'); setError(null) }} />
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
