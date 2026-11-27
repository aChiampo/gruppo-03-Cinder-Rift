import test, { afterEach } from 'node:test'
import assert from 'node:assert/strict'
import { apiFetch, ApiError, setSessionHandlers } from '../src/services/api.js'
import * as auth from '../src/services/authService.js'
import { listDecks, getDeck, createDeck, saveDeck, deleteDeck, deckPayload } from '../src/services/deckService.js'
import { playPve } from '../src/services/battleService.js'
import { buildCollectionParams, getCatalog, getCollection } from '../src/services/cardService.js'
import { cardKey, defaultFilters } from '../src/models/cards.js'
import { catalogCard, collectionCard, pageOf } from './cardFixtures.js'

const originalFetch = globalThis.fetch
afterEach(() => { globalThis.fetch = originalFetch; setSessionHandlers() })
const json = (body, status = 200) => new Response(JSON.stringify(body), { status, headers: { 'Content-Type': 'application/json' } })
// Fixtures are confined to tests and are never imported by the application.
const profile = { id: 42, nickname: 'Test player', email: 'test@example.invalid', role: 'USER', rankingPoints: 150, iconUrl: null }
const session = { accessToken: 'test-access-token', expiresIn: 900, tokenType: 'Bearer', user: profile }
const validation = { valid: true, errors: [], warnings: [], summary: { totalCards: 0 } }
const deck = { id: 8, ownerId: 42, isChallengeable: false, entries: [], validation }

test('API base has one /api prefix; Bearer and cookies are included', async () => {
  globalThis.fetch = async (url, options) => {
    assert.equal(url, '/api/users/me/decks')
    assert.equal(options.headers.Authorization, 'Bearer test-token')
    assert.equal(options.credentials, 'include')
    return json([])
  }
  assert.deepEqual(await listDecks('test-token'), [])
})
test('Problem Details retains validation issues and handles errors without JSON', async () => {
  const issue = { code: 'OWNERSHIP', message: 'Copy not owned' }
  globalThis.fetch = async () => json({ detail: 'Deck validation failed', errors: [issue] }, 400)
  await assert.rejects(apiFetch('/decks/8'), (err) => err.status === 400 && err.message === 'Deck validation failed' && err.data.errors[0].code === 'OWNERSHIP')
  globalThis.fetch = async () => new Response('<html>Gateway unavailable</html>', { status: 503 })
  await assert.rejects(apiFetch('/decks/8'), (err) => err.status === 503 && err.message.includes('503'))
})
test('204 is accepted and malformed successful payload is rejected', async () => {
  globalThis.fetch = async () => new Response(null, { status: 204 })
  assert.equal(await apiFetch('/decks/8', { method: 'DELETE' }), null)
  globalThis.fetch = async () => new Response('<html>SPA fallback</html>')
  await assert.rejects(apiFetch('/decks/8'), { status: 502 })
})
test('401 renews once and the retry uses the replacement token', async () => {
  const tokens = []
  let renewals = 0, clears = 0
  setSessionHandlers({ renew: async () => { renewals++; return 'new-token' }, clear: () => clears++ })
  globalThis.fetch = async (_, options) => { tokens.push(options.headers.Authorization); return tokens.length === 1 ? new Response(null, { status: 401 }) : json({ ok: true }) }
  assert.deepEqual(await apiFetch('/decks/8', { token: 'old-token' }), { ok: true })
  assert.deepEqual(tokens, ['Bearer old-token', 'Bearer new-token'])
  assert.equal(renewals, 1); assert.equal(clears, 0)
})
test('rejected retry clears the session without an infinite renewal loop', async () => {
  let renewals = 0, clears = 0, requests = 0
  setSessionHandlers({ renew: async () => { renewals++; return 'new-token' }, clear: () => clears++ })
  globalThis.fetch = async () => { requests++; return new Response(null, { status: 401 }) }
  await assert.rejects(apiFetch('/decks/8', { token: 'old-token' }), { status: 401 })
  assert.equal(requests, 2); assert.equal(renewals, 1); assert.equal(clears, 1)
})
test('network failure on a mutation is not retried', async () => {
  let requests = 0
  globalThis.fetch = async () => { requests++; throw new TypeError('Network failure') }
  await assert.rejects(apiFetch('/decks/8', { method: 'PUT', body: {} }), { status: 0 })
  assert.equal(requests, 1)
})
test('external cancellation is preserved as AbortError', async () => {
  const controller = new AbortController(); controller.abort()
  globalThis.fetch = async (_, options) => { options.signal.throwIfAborted() }
  await assert.rejects(apiFetch('/decks/8', { signal: controller.signal }), { name: 'AbortError' })
})
test('registration obtains CSRF and sends canonical fields only', async () => {
  const calls = []
  globalThis.fetch = async (url, options) => {
    calls.push(url)
    if (url.endsWith('/csrf-tokens')) return json({ headerName: 'X-XSRF-TOKEN', token: 'test-csrf' })
    assert.equal(url, '/api/auth/signin')
    assert.equal(options.headers['X-XSRF-TOKEN'], 'test-csrf')
    assert.equal(options.headers.Authorization, undefined)
    assert.deepEqual(JSON.parse(options.body), { nickname: 'Test', email: 'test@example.invalid', password: 'test-only-password' })
    return json(session, 201)
  }
  const result = await auth.register({ nickname: ' Test ', email: ' test@example.invalid ', password: 'test-only-password', confirmation: 'ignored' })
  assert.equal(result.user.name, profile.nickname)
  assert.equal(result.user.avatar, null)
  assert.equal(result.user.credits, undefined)
  assert.deepEqual(calls, ['/api/auth/csrf-tokens', '/api/auth/signin'])
})
test('renewal and logout are serialized and omit Bearer authorization', async () => {
  const calls = []
  globalThis.fetch = async (url, options) => {
    calls.push(url)
    assert.equal(options.headers.Authorization, undefined)
    if (url.endsWith('/csrf-tokens')) return json({ headerName: 'X-XSRF-TOKEN', token: 'test-csrf' })
    if (url.endsWith('/session-renewals')) return json(session)
    return new Response(null, { status: 204 })
  }
  await Promise.all([auth.renewSession(), auth.logout()])
  assert.deepEqual(calls, ['/api/auth/csrf-tokens', '/api/auth/session-renewals', '/api/auth/csrf-tokens', '/api/auth/logout'])
})
test('invalid CSRF/session/profile responses cannot create a user', async () => {
  globalThis.fetch = async () => json({})
  await assert.rejects(auth.login({ email: 'test@example.invalid', password: 'test-only' }), { status: 502 })
  assert.throws(() => auth.normalizeUser({ id: 42, nickname: 'Test', role: 'UNKNOWN' }), { status: 502 })
})
test('deck CRUD preserves variants and destinations and omits server entry IDs', async () => {
  const calls = []
  globalThis.fetch = async (url, options) => {
    calls.push([url, options.method])
    if (options.method === 'PUT') assert.deepEqual(JSON.parse(options.body), { entries: [{ cardId: 17, cardArtId: 41, destination: 'ROUND1' }], isChallengeable: true })
    if (options.method === 'POST') assert.deepEqual(JSON.parse(options.body), { entries: [] })
    return options.method === 'DELETE' ? new Response(null, { status: 204 }) : json(deck)
  }
  await createDeck('token'); await getDeck(8, 'token')
  await saveDeck(8, [{ entryId: 101, cardId: 17, cardArtId: 41, destination: 'ROUND1' }], true, 'token')
  await deleteDeck(8, 'token')
  assert.deepEqual(calls, [['/api/users/me/decks', 'POST'], ['/api/decks/8', 'GET'], ['/api/decks/8', 'PUT'], ['/api/decks/8', 'DELETE']])
})
test('deck payload rejects invalid IDs/destinations without a network request', () => {
  for (const cardId of [0, -1, 1.5, 2147483648, 'invalid']) assert.throws(() => deckPayload([{ cardId, cardArtId: 1, destination: 'ROUND1' }]), ApiError)
  assert.throws(() => deckPayload([{ cardId: 1, cardArtId: 1, destination: 'ROUND6' }]), ApiError)
})
test('PvE sends only deckId and preserves authoritative outcome and log whitespace', async () => {
  const result = { stageId: 'stage_01', stageTitle: 'Test stage', challenger: { displayName: 'Same name' }, enemyName: 'Same name', outcome: 'DEFEAT', winner: 'Same name', battleLog: ['  Preserve whitespace\nSecond line'] }
  globalThis.fetch = async (url, options) => {
    assert.equal(url, '/api/battle/pve/stage_01')
    assert.deepEqual(JSON.parse(options.body), { deckId: 8 })
    return json(result)
  }
  assert.deepEqual(await playPve('stage_01', '8', 'token'), result)
})
test('PvE violations remain available for 422 and unknown responses fail closed', async () => {
  globalThis.fetch = async () => json({ detail: 'Deck invalid', violations: ['Missing creatures'] }, 422)
  await assert.rejects(playPve('stage_01', 8, 'token'), (err) => err.status === 422 && err.data.violations[0] === 'Missing creatures')
  globalThis.fetch = async () => json({ outcome: 'UNKNOWN', battleLog: [] })
  await assert.rejects(playPve('stage_01', 8, 'token'), { status: 502 })
})
test('card query uses canonical codes, inclusive bounds and zero-based pagination without an owner', () => {
  assert.deepEqual(buildCollectionParams(), { sort: 'name', direction: 'asc', page: 0, size: 15 })
  assert.deepEqual(buildCollectionParams({ filters: { ...defaultFilters, name: ' Ash & Fire ', faction: 'SAP', rarity: 'RARE', type: 'CREATURE', expansion: 'CR01', energyMin: '0', energyMax: '3', attackMin: 0, defenseMax: '8', powerMin: '2', powerMax: '5', userId: 999 }, sort: 'power', direction: 'desc', page: 3, pageSize: 20 }), {
    sort: 'power', direction: 'desc', page: 2, size: 20, name: 'Ash & Fire', faction: 'SAP', rarity: 'RARE', type: 'CREATURE', expansion: 'CR01', energyMin: '0', energyMax: '3', attackMin: 0, defenseMax: '8', powerMin: '2', powerMax: '5',
  })
})

test('catalog calls the authenticated backend and preserves its envelope and all artworks', async () => {
  const expected = pageOf([catalogCard], { page: 1, totalElements: 16, totalPages: 2 })
  globalThis.fetch = async (url, options) => {
    const request = new URL(url, 'https://example.invalid')
    assert.equal(request.pathname, '/api/cards')
    assert.equal(request.searchParams.get('name'), 'Ash & Fire')
    assert.equal(request.searchParams.get('page'), '1')
    assert.equal(request.searchParams.get('size'), '15')
    assert.equal(options.headers.Authorization, 'Bearer test-token')
    assert.equal(options.method, 'GET')
    assert.equal(options.body, undefined)
    return json(expected)
  }
  assert.deepEqual(await getCatalog(buildCollectionParams({ filters: { name: 'Ash & Fire' }, page: 2 }), { token: 'test-token' }), expected)
})

test('private collection preserves separate artwork rows and stored copy counts', async () => {
  const variant = { ...collectionCard, cardArtId: 202, artCode: 'ALT', ownedReplica: 1, tradableReplica: 0 }
  const expected = pageOf([collectionCard, variant])
  globalThis.fetch = async (url, options) => {
    assert.equal(url, '/api/users/me/collections?sort=name&direction=asc&page=0&size=15')
    assert.equal(options.headers.Authorization, 'Bearer owner-token')
    return json(expected)
  }
  const result = await getCollection(buildCollectionParams(), { token: 'owner-token' })
  assert.deepEqual(result, expected)
  assert.notEqual(cardKey(result.items[0]), cardKey(result.items[1]))
})

test('valid empty card pages stay distinct from malformed responses and failed requests', async () => {
  globalThis.fetch = async () => json(pageOf([]))
  assert.equal((await getCatalog()).totalElements, 0)
  assert.deepEqual((await getCollection()).items, [])
  for (const body of [{}, [], { ...pageOf([]), totalElements: -1 }, { ...pageOf([]), totalPages: 2 }, pageOf([{ ...catalogCard, artworks: null }]), pageOf([{ ...catalogCard, id: null }]), pageOf([{ ...catalogCard, type: 'CREATURE' }])]) {
    globalThis.fetch = async () => json(body)
    await assert.rejects(getCatalog(), { status: 502 })
  }
  globalThis.fetch = async () => json(pageOf([{ ...collectionCard, tradableReplica: collectionCard.ownedReplica + 1 }]))
  await assert.rejects(getCollection(), { status: 502 })
  for (const status of [400, 401, 500]) {
    globalThis.fetch = async () => json({ detail: 'Server error' }, status)
    await assert.rejects(getCollection(), { status, message: 'Server error' })
  }
})

test('nullable card fields and unfamiliar catalog codes remain authoritative', async () => {
  const card = { ...catalogCard, faction: { id: 99, code: 'FUTURE', name: 'Future faction' }, rarity: null, type: null, expansion: null, name: null, energyCost: null, attack: null, defense: null, power: null, artworks: [] }
  globalThis.fetch = async () => json(pageOf([card]))
  assert.deepEqual((await getCatalog()).items[0], card)
})

test('card requests preserve cancellation and renew rejected Bearer tokens once', async () => {
  const controller = new AbortController()
  controller.abort()
  globalThis.fetch = async (_, options) => options.signal.throwIfAborted()
  await assert.rejects(getCollection({}, { signal: controller.signal }), { name: 'AbortError' })
  const tokens = []
  setSessionHandlers({ renew: async () => 'new-token' })
  globalThis.fetch = async (_, options) => {
    tokens.push(options.headers.Authorization)
    return tokens.length === 1 ? json({}, 401) : json(pageOf([catalogCard]))
  }
  assert.equal((await getCatalog({}, { token: 'old-token' })).items[0].id, catalogCard.id)
  assert.deepEqual(tokens, ['Bearer old-token', 'Bearer new-token'])
})
