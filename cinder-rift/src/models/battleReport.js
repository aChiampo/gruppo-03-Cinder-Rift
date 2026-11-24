const integer = '-?\\d+'
const exchangePattern = new RegExp(`^Scambio (\\d+): (.+) \\(Atk: (${integer}), Def: (${integer})\\) vs (.+) \\(Atk: (${integer}), Def: (${integer})\\)$`)

function readScore(line, prefix, names) {
  const start = `${prefix}: ${names[0]} `
  const end = ` ${names[1]}`
  if (!line.startsWith(start) || !line.endsWith(end)) return null
  const match = line.slice(start.length, -end.length).match(/^(\d+) - (\d+)$/)
  return match ? match.slice(1).map(Number) : null
}

/** A presentation adapter for the current engine's text, never a combat simulation.
 * Unknown lines stay available verbatim. Missing historical values remain null.
 * The public outcome is deliberately not calculated from this adapter.
 */
export function parseBattleReport(result) {
  const names = [result.challenger.displayName, result.enemyName]
  const rounds = []
  let round = null
  let frame = null
  let score = null
  let previousScore = [0, 0]
  let previousScoreRound = 0
  let previousNumber = 0
  for (const raw of result.battleLog) {
    for (const original of raw.split(/\r?\n/)) {
      const line = original.trim()
      const start = line.match(/^--- INIZIO ROUND ([1-5]) ---$/)
      if (start) {
        const number = Number(start[1])
        // Repeated/out-of-order boundaries are not safe to turn into navigation.
        if (number <= previousNumber) { round = null; frame = null; continue }
        round = { number, equipment: [null, null], frames: [], lines: [], score: null, outcome: 'UNKNOWN', hp: [null, null] }
        rounds.push(round)
        previousNumber = number
        frame = null
        continue
      }
      const finalScore = readScore(line, 'RISULTATO FINALE', names)
      if (finalScore) { score = finalScore; round = null; frame = null; continue }
      if (!round) continue
      round.lines.push(original)
      const exchange = line.match(exchangePattern)
      if (!exchange && /^Scambio \d+:/.test(line)) frame = null
      if (exchange) {
        const number = Number(exchange[1])
        if (number > (round.frames.at(-1)?.number ?? 0)) {
          frame = {
            number,
            creatures: [
              { name: exchange[2], attack: Number(exchange[3]), defense: Number(exchange[4]) },
              { name: exchange[5], attack: Number(exchange[6]), defense: Number(exchange[7]) },
            ],
            hp: [...round.hp], deaths: [false, false], lines: round.frames.length ? [] : round.lines.slice(0, -1),
          }
          round.frames.push(frame)
        } else frame = null
      }
      if (frame) frame.lines.push(original)
      names.forEach((name, slot) => {
        const equipPrefix = `Equip ${name}: `
        // Identical names make ownership ambiguous in the text format.
        if (names[0] !== names[1] && line.startsWith(equipPrefix) && !round.frames.length) {
          round.equipment[slot] = line.slice(equipPrefix.length)
        }
        const hp = line.match(/^Danno in eccesso: (\d+) inflitto a (.+) \(HP residui: (-?\d+)\)$/)
        if (names[0] !== names[1] && hp?.[2] === name) {
          round.hp[slot] = Number(hp[3])
          if (frame) frame.hp[slot] = Number(hp[3])
        }
        if (frame && line === `-> ${frame.creatures[slot].name} di ${name} è stata distrutta!` && names[0] !== names[1]) frame.deaths[slot] = true
      })
      if (frame && line === '-> Entrambe le creature sono state eliminate nello stesso scambio!') frame.deaths = [true, true]
      const partial = readScore(line, 'PUNTEGGIO PARZIALE', names)
      if (partial) {
        round.score = partial
        // A positional score delta also disambiguates players with equal names.
        if (previousScore && round.number === previousScoreRound + 1) {
          const delta = partial.map((value, slot) => value - previousScore[slot])
          if (delta[0] === 1 && delta[1] === 0) round.outcome = 'VICTORY'
          if (delta[0] === 0 && delta[1] === 1) round.outcome = 'DEFEAT'
          if (delta.every((value) => value === 0) && round.lines.some((entry) => entry.trim() === '--> ROUND TERMINATO IN PAREGGIO!')) round.outcome = 'DRAW'
        }
        previousScore = partial
        previousScoreRound = round.number
      }
      if (line === '--> ROUND TERMINATO IN PAREGGIO!') round.outcome = 'DRAW'
      if (names[0] !== names[1] && line === `--> ROUND VINTO DA: ${names[0]}`) round.outcome = 'VICTORY'
      if (names[0] !== names[1] && line === `--> ROUND VINTO DA: ${names[1]}`) round.outcome = 'DEFEAT'
    }
  }
  return { names, rounds, score, exchangeCount: rounds.reduce((sum, item) => sum + item.frames.length, 0) }
}

/** Resolve a bookmark only to a round/exchange actually present in this report. */
export function battleSelection(report, hash = '') {
  const params = new URLSearchParams(hash.split('?')[1] ?? '')
  const roundIndex = Math.max(0, report.rounds.findIndex((round) => round.number === Number(params.get('round'))))
  const frames = report.rounds[roundIndex]?.frames ?? []
  const frameIndex = Math.max(0, frames.findIndex((frame) => frame.number === Number(params.get('scambio'))))
  return { roundIndex, frameIndex }
}

/** Step through exchanges and through rounds without exchanges, clamping at both ends. */
export function moveBattleSelection(report, selection, direction) {
  const steps = report.rounds.flatMap((round, roundIndex) => Array.from({ length: Math.max(1, round.frames.length) }, (_, frameIndex) => ({ roundIndex, frameIndex })))
  if (!steps.length) return selection
  const current = steps.findIndex((step) => step.roundIndex === selection.roundIndex && step.frameIndex === selection.frameIndex)
  return steps[Math.max(0, Math.min(steps.length - 1, current + direction))]
}

export function battleLocation(report, { roundIndex, frameIndex }) {
  const round = report.rounds[roundIndex]
  if (!round) return 'risultato-battaglia'
  const frame = round.frames[frameIndex]
  return `risultato-battaglia?round=${round.number}${frame ? `&scambio=${frame.number}` : ''}`
}
