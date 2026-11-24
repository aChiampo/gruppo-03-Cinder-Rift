import test, { afterEach } from 'node:test'
import assert from 'node:assert/strict'
import { assignSlots, availableCopies, cardFitsDestination, placeCard } from '../src/models/deckBuilder.js'
import { deckPayload } from '../src/services/deckService.js'
import { getDeckCollection } from '../src/services/cardService.js'
import { collectionCard } from './cardFixtures.js'

const originalFetch = globalThis.fetch
afterEach(() => { globalThis.fetch = originalFetch })

test('click fills the first empty slot after a targeted placement or removal', () => {
  let entries = placeCard([], collectionCard, 'ROUND1', 3)
  entries = placeCard(entries, collectionCard, 'ROUND1')
  assert.deepEqual(entries.map((entry) => entry.slot), [3, 0])
  entries = entries.filter((entry) => entry.slot !== 0)
  assert.equal(placeCard(entries, collectionCard, 'ROUND1').at(-1).slot, 0)
  assert.equal(placeCard(entries, collectionCard, 'ROUND2').at(-1).slot, 0)
})

test('drops reject occupied slots, incompatible types, unknown cards and full groups', () => {
  const entries = placeCard([], collectionCard, 'ROUND1', 2)
  assert.throws(() => placeCard(entries, collectionCard, 'ROUND1', 2), /occupato/)
  assert.throws(() => placeCard([], collectionCard, 'EQUIPMENT'), /tipo corretto/)
  assert.throws(() => placeCard([], undefined, 'ROUND1'), /tipo corretto/)
  assert.throws(() => placeCard([], collectionCard, 'ROUND1', -1), /completo/)
  const full = assignSlots(Array.from({ length: 5 }, (_, cardId) => ({ cardId, cardArtId: 999, destination: 'ROUND1' })))
  assert.throws(() => placeCard(full, collectionCard, 'ROUND1'), /completo/)
  const equipment = { ...collectionCard, type: { code: 'EQUIPMENT' } }
  assert.equal(placeCard([], equipment, 'EQUIPMENT', 4)[0].slot, 4)
  assert.throws(() => placeCard([], equipment, 'ROUND5'), /tipo corretto/)
})

test('ownership is counted across rounds per artwork and removal restores availability', () => {
  const singleCopy = { ...collectionCard, ownedReplica: 1 }
  const entries = placeCard([], singleCopy, 'ROUND1')
  assert.equal(availableCopies(singleCopy, entries), 0)
  assert.throws(() => placeCard(entries, singleCopy, 'ROUND2'), /tutte le copie/)
  assert.equal(availableCopies(singleCopy, []), 1)
  const alternate = { ...singleCopy, cardArtId: 202 }
  assert.equal(placeCard(entries, alternate, 'ROUND2').length, 2)
})

test('all five creature rounds include equal and lower energy and reject higher or missing costs', () => {
  for (let round = 1; round <= 5; round++) {
    const destination = `ROUND${round}`
    for (const energyCost of [0, round - 1, round]) {
      const card = { ...collectionCard, energyCost }
      assert.equal(cardFitsDestination(card, destination), true)
      assert.equal(placeCard([], card, destination)[0].destination, destination)
    }
    for (const energyCost of [round + 1, null, undefined, -1, '1']) {
      const card = { ...collectionCard, energyCost }
      assert.equal(cardFitsDestination(card, destination), false)
      assert.throws(() => placeCard([], card, destination), /energia/)
      assert.throws(() => placeCard([], card, destination, 3), /energia/)
    }
  }
})

test('equipment selection remains separate from creature round energy filtering', () => {
  const equipment = { ...collectionCard, type: { code: 'EQUIPMENT' }, energyCost: 6 }
  assert.equal(cardFitsDestination(equipment, 'EQUIPMENT'), true)
  assert.equal(placeCard([], equipment, 'EQUIPMENT', 4)[0].slot, 4)
  assert.equal(cardFitsDestination(equipment, 'ROUND5'), false)
  assert.equal(cardFitsDestination(collectionCard, 'EQUIPMENT'), false)
  assert.equal(cardFitsDestination(collectionCard, 'ROUND6'), false)
})

test('saved entries load into independent groups and local slots never enter the F05 payload', () => {
  const entries = assignSlots([
    { entryId: 1, cardId: 101, cardArtId: 201, destination: 'ROUND1' },
    { entryId: 2, cardId: 102, cardArtId: 202, destination: 'ROUND2' },
    { entryId: 3, cardId: 101, cardArtId: 201, destination: 'ROUND1' },
  ])
  assert.deepEqual(entries.map((entry) => entry.slot), [0, 0, 1])
  const payload = deckPayload(entries, true)
  assert.equal(payload.isChallengeable, true)
  assert.deepEqual(Object.keys(payload.entries[0]), ['cardId', 'cardArtId', 'destination'])
  assert.equal(payload.entries.length, 3)
})

test('deck picker reads all owned collection pages with authentication and cancellation', async () => {
  const signal = new AbortController().signal
  const pages = []
  globalThis.fetch = async (url, options) => {
    const query = new URL(url, 'https://example.invalid').searchParams
    const page = Number(query.get('page'))
    pages.push(page)
    assert.equal(query.get('size'), '100')
    assert.equal(options.headers.Authorization, 'Bearer test-token')
    assert.ok(options.signal)
    const items = page === 0
      ? Array.from({ length: 100 }, (_, i) => ({ ...collectionCard, cardId: i + 1 }))
      : [{ ...collectionCard, cardId: 101 }]
    return new Response(JSON.stringify({ items, page, size: 100, totalElements: 101, totalPages: 2 }), { headers: { 'Content-Type': 'application/json' } })
  }
  const cards = await getDeckCollection({ token: 'test-token', signal })
  assert.deepEqual(pages, [0, 1])
  assert.equal(cards.length, 101)
  assert.equal(cards.at(-1).cardId, 101)
})
