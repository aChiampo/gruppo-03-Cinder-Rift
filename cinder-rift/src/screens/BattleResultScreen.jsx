import { useEffect, useMemo, useState } from 'react'
import { ChevronLeft, ChevronRight, Flag, Heart, Home, Pause, Play, RotateCcw, ScrollText, Shield, SkipBack, SkipForward, Swords, Trophy, UserRound } from 'lucide-react'
import { useAuth } from '../context/AuthContext.jsx'
import BattleCreature from '../components/battle/BattleCreature.jsx'
import { battleLocation, battleSelection, moveBattleSelection, parseBattleReport } from '../models/battleReport.js'

const labels = { VICTORY: 'Vittoria', DEFEAT: 'Sconfitta', DRAW: 'Pareggio', UNKNOWN: 'Non riportato' }
const headlines = { VICTORY: 'VITTORIA!', DEFEAT: 'SCONFITTA', DRAW: 'PAREGGIO' }
const subtitles = { VICTORY: 'Hai conquistato la vittoria nella sfida!', DEFEAT: 'La sfida si conclude a favore del tuo avversario.', DRAW: 'La sfida si conclude senza un vincitore.' }
const sameSelection = (first, second) => first.roundIndex === second.roundIndex && first.frameIndex === second.frameIndex

function Participant({ name, score, hp, enemy, avatar }) {
  const [failedAvatar, setFailedAvatar] = useState(null)
  return <div className={`battle-participant ${enemy ? 'battle-participant--enemy' : ''}`}>
    <div className="battle-portrait" aria-hidden="true">{avatar && failedAvatar !== avatar
      ? <img src={avatar} alt="" onError={() => setFailedAvatar(avatar)} />
      : enemy ? <Swords strokeWidth={1} /> : <UserRound strokeWidth={1} />}</div>
    <div className="battle-participant__info"><strong>{name}</strong>
      <span className="battle-score"><Trophy size={19} /> {score === null ? '—' : score} round vinti</span>
      <span className="battle-hp"><Heart size={23} fill="currentColor" /><span><small>Ultimi PV riportati</small><b>{hp ?? '—'}</b></span></span>
    </div>
  </div>
}

export default function BattleResultScreen({ result, go }) {
  if (!result) return <main className="page-stack"><section className="panel request-state">
    <ScrollText size={36} /><h1>Nessun risultato disponibile</h1><p>Il risultato resta disponibile durante questa sessione. Avvia una battaglia per consultarne il resoconto.</p>
    <button className="btn-outline" onClick={() => go('gioca')}>VAI ALLA CAMPAGNA</button>
  </section></main>
  return <BattleReport result={result} go={go} />
}

function BattleReport({ result, go }) {
  const { user } = useAuth()
  const report = useMemo(() => parseBattleReport(result), [result])
  const [selection, setSelection] = useState(() => battleSelection(report, typeof window === 'undefined' ? '' : window.location.hash))
  const [playing, setPlaying] = useState(false)
  const { roundIndex, frameIndex } = selection
  const round = report.rounds[roundIndex]
  const frame = round?.frames[frameIndex]
  const previous = moveBattleSelection(report, selection, -1)
  const next = moveBattleSelection(report, selection, 1)
  const atStart = sameSelection(previous, selection)
  const atEnd = sameSelection(next, selection)
  const lastHp = report.rounds.at(-1)?.hp ?? [null, null]
  const navigate = (target, automatic = false) => {
    if (!automatic) setPlaying(false)
    setSelection(target)
    const hash = `#${battleLocation(report, target)}`
    if (automatic) window.history.replaceState(null, '', hash)
    else if (window.location.hash !== hash) window.location.hash = hash
  }
  useEffect(() => {
    const change = () => { setSelection(battleSelection(report, window.location.hash)); setPlaying(false) }
    window.addEventListener('hashchange', change)
    return () => window.removeEventListener('hashchange', change)
  }, [report])
  useEffect(() => {
    if (!playing) return
    if (atEnd) { setPlaying(false); return }
    const timer = window.setTimeout(() => navigate(next, true), 2200)
    return () => window.clearTimeout(timer)
  }, [playing, roundIndex, frameIndex, report])
  useEffect(() => {
    const visibility = () => { if (document.hidden) setPlaying(false) }
    document.addEventListener('visibilitychange', visibility)
    return () => document.removeEventListener('visibilitychange', visibility)
  }, [])
  const replay = () => {
    if (playing) { setPlaying(false); return }
    if (atEnd) navigate({ roundIndex: 0, frameIndex: 0 }, true)
    setPlaying(true)
  }
  const roundState = round?.outcome ?? 'UNKNOWN'
  return <main className={`battle-page outcome-${result.outcome.toLowerCase()}`}>
    <section className="battle-banner" aria-labelledby="battle-title">
      <div className="battle-banner__heading"><h1 id="battle-title">{headlines[result.outcome]}</h1><p>{subtitles[result.outcome]}</p><span>{result.stageTitle}</span></div>
      <div className="battle-versus">
        <Participant name={report.names[0]} score={report.score?.[0] ?? null} hp={lastHp[0]} avatar={user?.avatar} />
        <span className="battle-vs" aria-hidden="true">VS</span>
        <Participant name={report.names[1]} score={report.score?.[1] ?? null} hp={lastHp[1]} enemy />
      </div>
      <button className="battle-button battle-home" onClick={() => go('home')}><Home size={18} /> Torna alla Home</button>
    </section>

    <section className="battle-details" aria-labelledby="battle-details-title">
      <header className="battle-details__header"><h2 id="battle-details-title"><Swords size={23} /> Dettagli della Sfida</h2>
        <button className="battle-button" onClick={replay} disabled={!report.exchangeCount} aria-pressed={playing}>{playing ? <Pause size={16} /> : <Play size={16} fill="currentColor" />} {playing ? 'Pausa replay' : 'Replay Partita'}</button>
      </header>
      {round ? <div className="battle-round-layout">
        <div className="battle-round-content">
          <div className="battle-arena" role="region" aria-label={`Round ${round.number}${frame ? `, scambio ${frame.number}` : ''}`}>
            <div className="battle-arena__heading"><span>Il tuo mazzo</span><h3>Round {round.number}</h3><span>Mazzo avversario</span></div>
            <div className="battle-arena__combat">
              <BattleCreature creature={frame?.creatures[0]} defeated={frame?.deaths[0]} />
              <div className={`battle-round-verdict battle-status--${roundState.toLowerCase()}`}><Swords size={32} />
                <strong>{labels[roundState]}</strong><small>Esito del round</small>
                {round.score && <b>{round.score[0]} : {round.score[1]}</b>}
              </div>
              <BattleCreature creature={frame?.creatures[1]} defeated={frame?.deaths[1]} enemy />
            </div>
            <div className="battle-equipment">{round.equipment.map((equipment, slot) => <div key={slot}><Shield size={17} /><span><small>Equipaggiamento {slot ? 'avversario' : 'alleato'}</small>{equipment ?? 'Non riportato'}</span></div>)}</div>
          </div>
          <div className="battle-playback">
            <div className="battle-playback__position" role="status" aria-live={playing ? 'off' : 'polite'}><span>{frame ? `Scambio ${frame.number}` : 'Riepilogo del round'}</span><small>{frame ? `${frameIndex + 1} di ${round.frames.length} · Attacchi simultanei` : 'Nessuno scambio riconosciuto'}</small></div>
            <div className="battle-playback__buttons">
              <button className="battle-control" aria-label="Inizio partita" disabled={atStart} onClick={() => navigate({ roundIndex: 0, frameIndex: 0 })}><SkipBack size={18} /></button>
              <button className="battle-control" aria-label="Scambio precedente" disabled={atStart} onClick={() => navigate(previous)}><ChevronLeft size={20} /></button>
              <button className="battle-control battle-control--play" aria-label={playing ? 'Pausa replay' : 'Riproduci scambi'} disabled={!report.exchangeCount} aria-pressed={playing} onClick={replay}>{playing ? <Pause size={19} /> : <Play size={19} />}</button>
              <button className="battle-control" aria-label="Scambio successivo" disabled={atEnd} onClick={() => navigate(next)}><ChevronRight size={20} /></button>
              <button className="battle-control" aria-label="Fine partita" disabled={atEnd} onClick={() => navigate({ roundIndex: report.rounds.length - 1, frameIndex: Math.max(0, report.rounds.at(-1).frames.length - 1) })}><SkipForward size={18} /></button>
            </div>
            {round.frames.length > 1 && <label className="battle-seek"><span className="sr-only">Seleziona scambio del round</span><input aria-label="Seleziona scambio del round" aria-valuetext={`Scambio ${frame.number}`} type="range" min="0" max={round.frames.length - 1} value={frameIndex} onChange={(event) => navigate({ roundIndex, frameIndex: Number(event.target.value) })} /></label>}
          </div>
          <section className="battle-events" aria-label="Eventi dello scambio">
            <h3><ScrollText size={18} /> {frame ? 'Cosa è successo' : 'Resoconto del round'}</h3>
            {frame && <p className="battle-events__hint">Le statistiche mostrano l’inizio dello scambio. Eliminazioni, effetti e danni sono riportati qui sotto.</p>}
            <ol>{(frame?.lines ?? round.lines).map((line, index) => <li key={index} className={line.includes('[EFFECT]') ? 'battle-event--effect' : ''}>{line}</li>)}</ol>
            {frame && <div className="battle-events__hp">{frame.hp.map((hp, slot) => <span key={slot}><Heart size={16} /> {slot ? 'Avversario' : 'Tu'}: <b>{hp ?? '—'} PV</b></span>)}</div>}
          </section>
        </div>
        <nav className="battle-round-nav" aria-label="Round della battaglia">{Array.from({ length: 5 }, (_, index) => {
          const target = report.rounds.findIndex((item) => item.number === index + 1)
          const item = report.rounds[target]
          return <button key={index} className={`battle-round-link ${target === roundIndex ? 'active' : ''}`} disabled={!item} aria-current={target === roundIndex ? 'step' : undefined} onClick={() => navigate({ roundIndex: target, frameIndex: 0 })}>
            <span className="battle-round-dot" /><span>Round {index + 1}</span><strong className={`battle-status--${item?.outcome.toLowerCase() ?? 'unknown'}`}>{item ? labels[item.outcome] : 'Non nel report'}</strong>
          </button>
        })}<p>Seleziona un round per esplorare gli scambi.</p></nav>
      </div> : <div className="battle-empty"><ScrollText size={32} /><h3>Dettaglio dei round non disponibile</h3><p>Questo resoconto non contiene round riconoscibili. Puoi consultare il log completo qui sotto.</p></div>}
      <footer className="battle-summary">
        <div><Trophy size={25} /><span><small>Round nel report</small><strong>{report.rounds.length}</strong></span></div>
        <div><Swords size={25} /><span><small>Scambi riconosciuti</small><strong>{report.exchangeCount}</strong></span></div>
        <div><Flag size={25} /><span><small>Modalità</small><strong>Campagna PvE</strong></span></div>
      </footer>
    </section>
    <details className="battle-raw"><summary><ScrollText size={19} /> Resoconto completo <span>{result.battleLog.length} voci</span></summary><ol className="battle-log">{result.battleLog.map((line, index) => <li key={index}>{line}</li>)}</ol></details>
    <div className="battle-page__footer"><p>Il risultato resta disponibile durante questa sessione. I valori non riportati sono indicati con —.</p><button className="battle-button" onClick={() => go('gioca')}><RotateCcw size={16} /> Nuova battaglia</button></div>
  </main>
}
