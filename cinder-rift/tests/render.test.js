import test from 'node:test'
import assert from 'node:assert/strict'
import Module from 'node:module'
import { build } from 'esbuild'
import { fileURLToPath } from 'node:url'
import { defaultFilters } from '../src/models/cards.js'
import { catalogCard, collectionCard } from './cardFixtures.js'
import { battleResult } from './battleFixtures.js'

// Render real page components with isolated session/resource state. No fixtures enter src or dist.
const root = fileURLToPath(new URL('../', import.meta.url))
const result = await build({
  absWorkingDir: root, write: false, bundle: true, platform: 'node', format: 'cjs', jsx: 'automatic', define: { 'import.meta.env': '{}' },
  stdin: { resolveDir: root, contents: `
    import React from 'react'
    import { renderToStaticMarkup } from 'react-dom/server'
    import LoginScreen from './src/screens/LoginScreen.jsx'
    import HomeScreen from './src/screens/HomeScreen.jsx'
    import CollectionScreen from './src/screens/CollectionScreen.jsx'
    import DeckScreen from './src/screens/DeckScreen.jsx'
    import BattleScreen from './src/screens/BattleScreen.jsx'
    import BattleResultScreen from './src/screens/BattleResultScreen.jsx'
    import ShopScreen from './src/screens/ShopScreen.jsx'
    import ProfileScreen from './src/screens/ProfileScreen.jsx'
    import DeckValidation from './src/components/deck-builder/DeckValidation.jsx'
    import RequestState from './src/components/ui/RequestState.jsx'
    import CollectionPicker from './src/components/deck-builder/CollectionPicker.jsx'
    import DeckSlots from './src/components/deck-builder/DeckSlots.jsx'
    import RoundSelector from './src/components/deck-builder/RoundSelector.jsx'
    export { RoundSelector }
    const components = { LoginScreen, HomeScreen, CollectionScreen, DeckScreen, BattleScreen, BattleResultScreen, ShopScreen, ProfileScreen, DeckValidation, RequestState, CollectionPicker, DeckSlots, RoundSelector }
    export const render = (name, props) => renderToStaticMarkup(React.createElement(components[name], props))
  ` },
  plugins: [{
    name: 'isolated-render-state',
    setup(build) {
      build.onLoad({ filter: /[\\/]context[\\/]AuthContext\.jsx$/ }, () => ({ loader: 'js', contents: 'export const useAuth = () => globalThis.__renderAuth' }))
      build.onLoad({ filter: /[\\/]hooks[\\/]useDecks\.js$/ }, () => ({ loader: 'js', contents: 'export const useDecks = () => globalThis.__renderDecks' }))
      build.onLoad({ filter: /[\\/]hooks[\\/]useDeckCollection\.js$/ }, () => ({ loader: 'js', contents: 'export const useDeckCollection = () => globalThis.__renderDeckCollection' }))
      build.onLoad({ filter: /[\\/]hooks[\\/]useCardCollection\.js$/ }, () => ({ loader: 'js', contents: 'export const useCardCollection = (options) => { globalThis.__renderCollectionOptions = options; return globalThis.__renderCollection }' }))
    },
  }],
})
const compiled = new Module(root + 'tests/rendered-components.cjs')
compiled.filename = root + 'tests/rendered-components.cjs'
compiled.paths = Module._nodeModulePaths(root)
compiled._compile(result.outputFiles[0].text, compiled.filename)
const { render, RoundSelector } = compiled.exports
globalThis.__renderAuth = {
  user: { id: 42, nickname: 'Test player', name: 'Test player', email: 'test@example.invalid', role: 'USER', rankingPoints: 10, iconUrl: null },
  token: 'test-token', login() {}, register() {}, logout() {}, updateUser() {},
}
globalThis.__renderDecks = { status: 'ready', data: [], error: null, retry() {} }
globalThis.__renderDeckCollection = { status: 'ready', data: [], error: null, retry() {} }
const go = () => {}
const collectionState = (changes = {}) => ({
  filters: defaultFilters, status: 'ready', error: null, visible: [], total: 0, loaded: true,
  isFiltered: false, page: 1, pageCount: 1, sort: 'name', direction: 'asc',
  patch: go, reset: go, retry: go, changeSort: go, changeDirection: go, setPage: go, ...changes,
})

test('login and registration render accessible fields and real submit actions', () => {
  const login = render('LoginScreen', { onMode: go })
  assert.match(login, /aria-label="Email"/)
  assert.match(login, /autoComplete="current-password"/i)
  const registration = render('LoginScreen', { mode: 'register', onMode: go })
  assert.match(registration, /REGISTRAZIONE/)
  assert.match(registration, /aria-label="Nome profilo"/)
  assert.match(registration, /aria-label="Conferma password"/)
})
test('home renders unavailable services without pretending that requests returned empty lists', () => {
  const html = render('HomeScreen', { go })
  assert.match(html, /CAMPAGNA PvE/)
  assert.match(html, /Servizio non disponibile/)
  assert.doesNotMatch(html, /Nessuna sfida in attesa/)
  assert.match(html, /Non hai ancora creato un mazzo/)
})
test('catalog and user collection render distinct headings and accessible errors', () => {
  globalThis.__renderCollection = collectionState({ status: 'error', error: { status: 500, message: 'Errore del server' }, loaded: false })
  const collection = render('CollectionScreen', { go })
  assert.deepEqual(globalThis.__renderCollectionOptions, { catalog: false })
  assert.match(collection, /LA TUA COLLEZIONE/)
  assert.match(collection, /role="alert"/)
  assert.doesNotMatch(collection, /La tua collezione è vuota/)
  const catalog = render('CollectionScreen', { catalog: true, go })
  assert.deepEqual(globalThis.__renderCollectionOptions, { catalog: true })
  assert.match(catalog, /CATALOGO CARTE/)
  assert.match(catalog, /Errore del server/)
  assert.doesNotMatch(catalog, /Il catalogo non contiene ancora carte/)
})

test('catalog renders canonical cards, nullable stats, every artwork option and real pagination', () => {
  globalThis.__renderCollection = collectionState({ visible: [catalogCard], total: 16, pageCount: 2 })
  const html = render('CollectionScreen', { catalog: true, go })
  assert.match(html, /Ash &amp; Fire/)
  assert.match(html, /energia 0/)
  assert.match(html, /Variante grafica/)
  assert.match(html, /value="201"/)
  assert.match(html, /value="202"/)
  assert.match(html, /ALT/)
  assert.match(html, /art-placeholder/)
  assert.match(html, /Pagina successiva/)
  assert.match(html, /Direzione ordinamento/)
  assert.match(html, /PUNTI POTENZA/)
  assert.doesNotMatch(html, /Copie possedute|DURATA|undefined|NaN/)
})

test('collection renders variant rows and persisted copy counts', () => {
  const alternate = { ...collectionCard, cardArtId: 202, artCode: 'ALT', ownedReplica: 1, tradableReplica: 0 }
  globalThis.__renderCollection = collectionState({ visible: [collectionCard, alternate], total: 2 })
  const html = render('CollectionScreen', { go })
  assert.match(html, /Variante STANDARD · Copie: 3/)
  assert.match(html, /Variante ALT · Copie: 1/)
  assert.match(html, /Copie possedute/)
  assert.match(html, /Copie scambiabili registrate/)
  assert.match(html, /non sottraggono le prenotazioni/)
  assert.doesNotMatch(html, /Variante grafica/)
})

test('equipment and unfamiliar or missing references render without invented duration or statistics', () => {
  const equipment = { ...catalogCard, type: { id: 2, code: 'EQUIPMENT', name: 'Equipment' }, faction: null, rarity: null, energyCost: null, artworks: [] }
  globalThis.__renderCollection = collectionState({ visible: [equipment], total: 1 })
  const html = render('CollectionScreen', { catalog: true, go })
  assert.match(html, /Equipment/)
  assert.match(html, /Non disponibile/)
  assert.doesNotMatch(html, /DURATA|undefined|NaN/)
  const unknown = { ...equipment, faction: { id: 99, code: 'FUTURE', name: 'Future faction' }, rarity: { id: 99, code: 'FUTURE', name: null } }
  globalThis.__renderCollection = collectionState({ visible: [unknown], total: 1 })
  assert.match(render('CollectionScreen', { catalog: true, go }), /Future faction/)
})

test('catalog and collection have distinct empty states and filtered results stay distinct', () => {
  globalThis.__renderCollection = collectionState()
  const catalog = render('CollectionScreen', { catalog: true, go })
  assert.match(catalog, /Il catalogo non contiene ancora carte/)
  assert.doesNotMatch(catalog, /VAI AL NEGOZIO|non possiedi/)
  assert.match(render('CollectionScreen', { go }), /La tua collezione è vuota/)
  globalThis.__renderCollection = collectionState({ isFiltered: true })
  assert.match(render('CollectionScreen', { go }), /Nessuna carta corrisponde ai filtri/)
  globalThis.__renderCollection = collectionState({ status: 'loading', loaded: false })
  const loading = render('CollectionScreen', { go })
  assert.match(loading, /Caricamento delle carte/)
  assert.doesNotMatch(loading, /La tua collezione è vuota/)
})
test('deck page renders empty state and creation without claiming validation success', () => {
  const html = render('DeckScreen', { go })
  assert.match(html, /DECK BUILDER/)
  assert.match(html, /CREA MAZZO/)
  assert.doesNotMatch(html, /Validazione superata/)
  assert.doesNotMatch(html, /COLLEZIONE CARTE|Creature possedute|Clicca una carta/)
  assert.ok(html.indexOf('I TUOI MAZZI') < html.indexOf('Cerca nella collezione'))
})

test('deck collection displays only owned variants matching the active group', () => {
  const equipment = { ...collectionCard, cardId: 102, name: 'Equipment only', type: { code: 'EQUIPMENT' } }
  const collection = { status: 'ready', data: [collectionCard, equipment] }
  const html = render('CollectionPicker', { collection, entries: [], destination: 'ROUND2', onAdd: go })
  assert.match(html, /Ash &amp; Fire/)
  assert.match(html, /draggable="true"/)
  assert.match(html, /3 copie disponibili/)
  assert.doesNotMatch(html, /Equipment only/)
  const equipmentHtml = render('CollectionPicker', { collection, entries: [], destination: 'EQUIPMENT', onAdd: go })
  assert.match(equipmentHtml, /Equipment only/)
  assert.doesNotMatch(equipmentHtml, /Ash &amp; Fire/)
  const disabled = render('CollectionPicker', { collection, entries: [], destination: 'ROUND1', disabled: true, onAdd: go })
  assert.match(disabled, /disabled="" draggable="false"/)
})

test('deck slots preserve targeted holes and display saved card metadata or a repair fallback', () => {
  const entries = [{ cardId: 101, cardArtId: 201, destination: 'ROUND1', slot: 3 }]
  const html = render('DeckSlots', { entries, cards: [collectionCard], destination: 'ROUND1' })
  assert.equal((html.match(/class="entry-slot /g) ?? []).length, 5)
  assert.match(html, /Slot 1: vuoto/)
  assert.match(html, /Slot 4: Ash &amp; Fire/)
  assert.match(html, /Rimuovi Ash &amp; Fire dallo slot 4/)
  assert.match(html, /class="tile-stats"/)
  assert.match(html, new RegExp(`</svg>${collectionCard.attack ?? '—'}</span>`))
  assert.match(html, new RegExp(`</svg>${collectionCard.defense ?? '—'}</span>`))
  assert.ok(html.indexOf('Rimuovi Ash &amp; Fire') > html.indexOf('class="tile-stats"'))
  assert.equal((html.match(/<button/g) ?? []).length, 1)
  assert.match(render('DeckSlots', { entries, cards: [], destination: 'ROUND1' }), /Carta #101/)
})

test('round selector includes an active equipment destination and sends its selection', () => {
  const html = render('RoundSelector', { activeDestination: 'EQUIPMENT', onChange: go })
  assert.equal((html.match(/<button/g) ?? []).length, 6)
  assert.match(html, /class="round-selector__button active" aria-pressed="true">Equipaggiamenti/)
  const changes = []
  const selector = RoundSelector({ activeDestination: 'ROUND1', onChange: (value) => changes.push(value) })
  selector.props.children[5].props.onClick()
  selector.props.children[1].props.onClick()
  assert.deepEqual(changes, ['EQUIPMENT', 'ROUND2'])
})

test('compact deck picker hides creatures exceeding the active round or lacking an energy cost', () => {
  const card = (cardId, name, energyCost) => ({ ...collectionCard, cardId, name, energyCost })
  const collection = { status: 'ready', data: [card(1, 'Below limit', 1), card(2, 'At limit', 2), card(3, 'Above limit', 3), card(4, 'Missing cost', null)] }
  const html = render('CollectionPicker', { collection, entries: [], destination: 'ROUND2', onAdd: go })
  assert.match(html, /Below limit/)
  assert.match(html, /At limit/)
  assert.doesNotMatch(html, /Above limit|Missing cost/)
  assert.doesNotMatch(html, /Creature possedute|Clicca una carta|deck-collection-help/)
  assert.match(html, /--s:22px/)
  const nextRound = render('CollectionPicker', { collection, entries: [], destination: 'ROUND3', onAdd: go })
  assert.match(nextRound, /Above limit/)
  assert.doesNotMatch(nextRound, /Missing cost/)
})
test('server deck violations render structured messages rather than React object children', () => {
  const html = render('DeckValidation', { validation: { valid: false, errors: [{ code: 'NOT_OWNED', destination: 'ROUND1', message: 'Card copy unavailable' }], warnings: [] } })
  assert.match(html, /ROUND1: Card copy unavailable/)
  assert.match(html, /NOT_OWNED/)
})
test('shop and pack detail render missing resources and disabled transactions', () => {
  const html = render('ShopScreen', { go })
  for (const resource of ['Scaglie', 'Nuclei Rift', 'Rotami']) assert.ok(html.includes(resource))
  assert.match(html, /aria-label="Saldo non disponibile"/)
  assert.match(html, /DETTAGLI DEL PACCHETTO/)
  assert.match(html, /disabled="">ACQUISTA/)
  assert.doesNotMatch(html, /2\.450|3\.000|50%|15%/)
})
test('battle result renders authoritative defeat despite identical player names', () => {
  const result = { outcome: 'DEFEAT', stageTitle: 'Test stage', challenger: { displayName: 'Same player' }, enemyName: 'Same player', winner: 'Same player', battleLog: ['  Line one\nLine two', '<script>unsafe</script>'] }
  const html = render('BattleResultScreen', { result, go })
  assert.match(html, /SCONFITTA/)
  assert.match(html, /  Line one\nLine two/)
  assert.match(html, /&lt;script&gt;unsafe&lt;\/script&gt;/)
  assert.match(html, /Dettaglio dei round non disponibile/)
  assert.doesNotMatch(html, /\+500|Vita residua/)
})

test('battle result renders real report rounds, missing HP, pre-exchange stats, replay and full raw log', () => {
  const html = render('BattleResultScreen', { result: battleResult, go })
  assert.match(html, /VITTORIA!/)
  assert.match(html, /aria-label="Round della battaglia"/)
  assert.match(html, /aria-current="step"/)
  assert.match(html, /aria-label="Scambio successivo"/)
  assert.match(html, /aria-label="Seleziona scambio del round"/)
  assert.match(html, /Prima dello scambio/)
  assert.match(html, /Pulse Blade/)
  assert.match(html, /Scambio 1/)
  assert.match(html, /Resoconto completo/)
  assert.match(html, /Non nel report/)
  assert.doesNotMatch(html, /\+500|crediti|NaN|undefined/)
})

test('result without in-memory battle offers recovery and home/campaign can reopen the current result', () => {
  assert.match(render('BattleResultScreen', { result: null, go }), /Nessun risultato disponibile/)
  const home = render('HomeScreen', { go, battle: battleResult })
  assert.match(home, /VEDI RISULTATO E ROUND/)
  assert.match(home, /Risultato della sessione/)
  const campaign = render('BattleScreen', { go, onResult: go, result: battleResult })
  assert.match(campaign, /VEDI RISULTATO E ROUND/)
  assert.doesNotMatch(campaign, /battle-banner/)
})
test('profile uses nickname and ranking points without inventing credit balances or levels', () => {
  const html = render('ProfileScreen', {})
  assert.match(html, /IL TUO PROFILO/)
  assert.match(html, /10 punti classifica/)
  assert.doesNotMatch(html, /Livello|crediti/)
})
test('PvE 422 errors render their violations visibly', () => {
  const html = render('RequestState', { status: 'error', error: { status: 422, message: 'Invalid deck', data: { violations: ['Missing equipment'] } } })
  assert.match(html, /HTTP 422/)
  assert.match(html, /Missing equipment/)
})
