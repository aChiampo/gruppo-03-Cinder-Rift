import test from 'node:test'
import assert from 'node:assert/strict'
import { battleLocation, battleSelection, moveBattleSelection, parseBattleReport } from '../src/models/battleReport.js'
import { battleResult } from './battleFixtures.js'

test('current engine log becomes ordered rounds with equipment and simultaneous exchanges without changing its payload', () => {
  const input = structuredClone(battleResult)
  const report = parseBattleReport(input)
  assert.deepEqual(input, battleResult)
  assert.deepEqual(report.score, [1, 0])
  assert.equal(report.exchangeCount, 3)
  assert.deepEqual(report.rounds.map((round) => [round.number, round.outcome]), [[1, 'VICTORY'], [2, 'DRAW']])
  assert.deepEqual(report.rounds[0].equipment, ['Pulse Blade', 'Recovery Armor'])
  const [first, second] = report.rounds[0].frames
  assert.deepEqual(first.creatures, [{ name: 'Scout', attack: 7, defense: 5 }, { name: 'Guard', attack: 3, defense: 4 }])
  assert.deepEqual(first.deaths, [false, true])
  assert.deepEqual(second.deaths, [true, true])
  assert.deepEqual(first.hp, [null, 27])
  assert.deepEqual(second.hp, [null, 27])
  assert.ok(first.lines.some((line) => line.startsWith('[EFFECT]')))
  assert.ok(first.lines.some((line) => line.includes('entra in campo')))
  // HP reset values are not recorded; they must not leak from the previous round.
  assert.deepEqual(report.rounds[1].frames[0].hp, [null, null])
  assert.equal(second.creatures[0].defense, 2)
  assert.equal(second.defenseAfter, undefined)
})

test('equal display names do not identify a winner or HP owner; positional scores identify round wins', () => {
  const result = {
    ...battleResult, outcome: 'DEFEAT', challenger: { displayName: 'Same' }, enemyName: 'Same',
    battleLog: ['--- INIZIO ROUND 1 ---', 'Equip Same: Gear', 'Scambio 1: A (Atk: 4, Def: 2) vs B (Atk: 1, Def: 2)', 'Danno in eccesso: 2 inflitto a Same (HP residui: 28)', '--> ROUND VINTO DA: Same', 'PUNTEGGIO PARZIALE: Same 0 - 1 Same', 'RISULTATO FINALE: Same 0 - 1 Same'],
  }
  const report = parseBattleReport(result)
  assert.equal(result.outcome, 'DEFEAT')
  assert.equal(report.rounds[0].outcome, 'DEFEAT')
  assert.deepEqual(report.rounds[0].hp, [null, null])
  assert.deepEqual(report.rounds[0].equipment, [null, null])
  const withoutScore = parseBattleReport({ ...result, battleLog: result.battleLog.slice(0, -2) })
  assert.equal(withoutScore.rounds[0].outcome, 'UNKNOWN')
})

test('unknown, multiline, partial and empty reports remain safe without invented rounds or stats', () => {
  for (const battleLog of [[], ['unfamiliar log'], ['RISULTATO FINALE: Player [A]. 3 - 1 Enemy (B)']]) {
    const report = parseBattleReport({ ...battleResult, battleLog })
    assert.equal(report.rounds.length, 0)
    assert.equal(report.exchangeCount, 0)
  }
  const report = parseBattleReport({ ...battleResult, battleLog: ['--- INIZIO ROUND 1 ---\nEquip Player [A].: Gear\n  Unknown effect\nScambio 1: Unsupported format\n--> ROUND VINTO DA: Enemy (B)'] })
  assert.equal(report.rounds[0].frames.length, 0)
  assert.equal(report.rounds[0].outcome, 'DEFEAT')
  assert.ok(report.rounds[0].lines.includes('  Unknown effect'))
})

test('exact markers and names containing regex punctuation are used rather than loose number or winner guesses', () => {
  const report = parseBattleReport({ ...battleResult, battleLog: ['story about ROUND 1 and 999 HP', '--- INIZIO ROUND 1 ---', 'Scambio 1: A (Atk: 0, Def: -2) vs B (Atk: 3, Def: 0)', 'Danno in eccesso: 4 inflitto a Player [A]. (HP residui: -1)', '--> ROUND VINTO DA: Enemy (B)', 'RISULTATO FINALE: Player [A]. 0 - 1 Enemy (B)'] })
  assert.deepEqual(report.score, [0, 1])
  assert.deepEqual(report.rounds[0].hp, [-1, null])
  assert.equal(report.rounds[0].frames[0].creatures[0].attack, 0)
  assert.equal(report.rounds[0].frames[0].creatures[0].defense, -2)
})

test('duplicate exchange/round numbers do not create extra replay steps and unknown lines remain in the round', () => {
  const report = parseBattleReport({ ...battleResult, battleLog: ['--- INIZIO ROUND 1 ---', 'Scambio 1: A (Atk: 1, Def: 2) vs B (Atk: 1, Def: 2)', 'Scambio 1: A (Atk: 1, Def: 2) vs B (Atk: 1, Def: 2)', 'Future event', '--- INIZIO ROUND 1 ---'] })
  assert.equal(report.rounds.length, 1)
  assert.equal(report.rounds[0].frames.length, 1)
  assert.ok(report.rounds[0].lines.includes('Future event'))
})

test('navigation supports bookmarks, forward/back across rounds, missing indices and empty rounds', () => {
  const report = parseBattleReport(battleResult)
  const first = { roundIndex: 0, frameIndex: 0 }
  const second = { roundIndex: 0, frameIndex: 1 }
  const last = { roundIndex: 1, frameIndex: 0 }
  assert.deepEqual(battleSelection(report, '#risultato-battaglia?round=2&scambio=1'), last)
  assert.deepEqual(battleSelection(report, '#risultato-battaglia?round=99&scambio=-1'), first)
  assert.deepEqual(moveBattleSelection(report, first, -1), first)
  assert.deepEqual(moveBattleSelection(report, first, 1), second)
  assert.deepEqual(moveBattleSelection(report, second, 1), last)
  assert.deepEqual(moveBattleSelection(report, last, -1), second)
  assert.deepEqual(moveBattleSelection(report, last, 1), last)
  assert.equal(battleLocation(report, second), 'risultato-battaglia?round=1&scambio=2')
  const emptyRound = parseBattleReport({ ...battleResult, battleLog: ['--- INIZIO ROUND 1 ---', '--- INIZIO ROUND 3 ---'] })
  assert.deepEqual(moveBattleSelection(emptyRound, first, 1), last)
  assert.equal(battleLocation(emptyRound, last), 'risultato-battaglia?round=3')
  assert.deepEqual(moveBattleSelection({ rounds: [] }, first, 1), first)
})

test('all five rounds and an early three-win finish retain the recorded score without fabricating unplayed rounds', () => {
  const complete = (outcomes) => {
    const score = [0, 0]
    const battleLog = outcomes.flatMap((outcome, index) => {
      if (outcome !== null) score[outcome]++
      return [`--- INIZIO ROUND ${index + 1} ---`, 'Scambio 1: A (Atk: 1, Def: 1) vs B (Atk: 1, Def: 1)', '-> Entrambe le creature sono state eliminate nello stesso scambio!',
        outcome === null ? '--> ROUND TERMINATO IN PAREGGIO!' : `--> ROUND VINTO DA: ${outcome ? battleResult.enemyName : battleResult.challenger.displayName}`,
        `PUNTEGGIO PARZIALE: ${battleResult.challenger.displayName} ${score[0]} - ${score[1]} ${battleResult.enemyName}`]
    })
    battleLog.push(`RISULTATO FINALE: ${battleResult.challenger.displayName} ${score[0]} - ${score[1]} ${battleResult.enemyName}`)
    return parseBattleReport({ ...battleResult, battleLog })
  }
  const five = complete([0, 1, null, 0, 1])
  assert.deepEqual(five.score, [2, 2])
  assert.deepEqual(five.rounds.map((round) => round.outcome), ['VICTORY', 'DEFEAT', 'DRAW', 'VICTORY', 'DEFEAT'])
  assert.equal(five.rounds.length, 5)
  const early = complete([0, 0, 0])
  assert.equal(early.rounds.length, 3)
  assert.deepEqual(early.score, [3, 0])
  assert.equal(early.exchangeCount, 3)
})

test('an unrecognized exchange boundary does not attach its outcomes to the preceding recognized exchange', () => {
  const report = parseBattleReport({ ...battleResult, battleLog: ['--- INIZIO ROUND 1 ---', 'Scambio 1: A (Atk: 1, Def: 2) vs B (Atk: 1, Def: 2)', 'Scambio 2: future format', '-> Entrambe le creature sono state eliminate nello stesso scambio!'] })
  assert.deepEqual(report.rounds[0].frames[0].deaths, [false, false])
  assert.ok(report.rounds[0].lines.includes('Scambio 2: future format'))
})
