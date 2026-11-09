import { Play, ArrowRight, Cog } from 'lucide-react'

export default function Campaign({ onStart }) {
  return (
    <section className="hero">
      <img className="hero-bg" src="/img/art-cyborg.jpg" alt="" />
      <div className="hero-shade" />
      <div className="hero-body">
        <span className="badge-offline"><Cog size={14} /> MODALITÀ OFFLINE</span>
        <h1>CAMPAGNA</h1>
        <p>Affronta la storia di Cinder-Rift in modalità single player. Scopri i segreti del mondo, colleziona nuove carte e migliora il tuo mazzo.</p>
        <button className="btn-primary" onClick={onStart}>
          <Play size={18} fill="currentColor" /> INIZIA CAMPAGNA <ArrowRight size={18} />
        </button>
      </div>
    </section>
  )
}
