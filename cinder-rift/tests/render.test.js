import test from 'node:test'
import assert from 'node:assert/strict'
import Module from 'node:module'
import { build } from 'esbuild'
import { fileURLToPath } from 'node:url'

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
    import ShopScreen from './src/screens/ShopScreen.jsx'
    import ProfileScreen from './src/screens/ProfileScreen.jsx'
    import DeckValidation from './src/components/deck-builder/DeckValidation.jsx'
    import RequestState from './src/components/ui/RequestState.jsx'
    const components = { LoginScreen, HomeScreen, CollectionScreen, DeckScreen, BattleScreen, ShopScreen, ProfileScreen, DeckValidation, RequestState }
    export const render = (name, props) => renderToStaticMarkup(React.createElement(components[name], props))
  ` },
  plugins: [{
    name: 'isolated-render-state',
    setup(build) {
      build.onLoad({ filter: /[\\/]context[\\/]AuthContext\.jsx$/ }, () => ({ loader: 'js', contents: 'export const useAuth = () => globalThis.__renderAuth' }))
      build.onLoad({ filter: /[\\/]hooks[\\/]useDecks\.js$/ }, () => ({ loader: 'js', contents: 'export const useDecks = () => globalThis.__renderDecks' }))
      build.onLoad({ filter: /[\\/]hooks[\\/]useCardCollection\.js$/ }, () => ({ loader: 'js', contents: 'export const useCardCollection = () => globalThis.__renderCollection' }))
    },
  }],
})
const compiled = new Module(root + 'tests/rendered-components.cjs')
compiled.filename = root + 'tests/rendered-components.cjs'
compiled.paths = Module._nodeModulePaths(root)
compiled._compile(result.outputFiles[0].text, compiled.filename)
const { render } = compiled.exports
globalThis.__renderAuth = {
  user: { id: 42, nickname: 'Test player', name: 'Test player', email: 'test@example.invalid', role: 'USER', rankingPoints: 10, iconUrl: null },
  token: 'test-token', login() {}, register() {}, logout() {}, updateUser() {},
}
globalThis.__renderDecks = { status: 'ready', data: [], error: null, retry() {} }
const go = () => {}

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
  globalThis.__renderCollection = {
    filters: { query: '', type: 'all', factions: [], rarities: [], energy: [1, 5], attack: [0, 10], defense: [0, 10] },
    status: 'error', error: { status: 501, message: 'La collezione non è disponibile' },
    visible: [], counts: null, total: 0, loaded: false, isFiltered: false, page: 1, pageCount: 1,
    sort: 'rarita', patch: go, toggle: go, reset: go, retry: go, changeSort: go, setPage: go,
  }
  const collection = render('CollectionScreen', { go })
  assert.match(collection, /LA TUA COLLEZIONE/)
  assert.match(collection, /role="alert"/)
  assert.doesNotMatch(collection, /La tua collezione è vuota/)
  const catalog = render('CollectionScreen', { catalog: true, go })
  assert.match(catalog, /CATALOGO CARTE/)
  assert.match(catalog, /Il catalogo non è disponibile/)
})
test('deck page renders empty state and creation without claiming validation success', () => {
  const html = render('DeckScreen', { go })
  assert.match(html, /DECK BUILDER/)
  assert.match(html, /CREA MAZZO/)
  assert.doesNotMatch(html, /Validazione superata/)
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
  const html = render('BattleScreen', { result, onResult: go, go })
  assert.match(html, /SCONFITTA/)
  assert.match(html, /  Line one\nLine two/)
  assert.match(html, /&lt;script&gt;unsafe&lt;\/script&gt;/)
  assert.doesNotMatch(html, /\+500|Replay|Vita residua/)
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
