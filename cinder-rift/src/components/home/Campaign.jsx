import { Play, ArrowRight, Cog } from 'lucide-react'

export default function Campaign({ onStart }) {
  return (
    <section className="hero">
      <img className="hero-bg" src="/img/art-cyborg.jpg" alt="" />
      <div className="hero-shade" />
      <div className="hero-body">
        <span className="badge-offline"><Cog size={14} /> CAMPAGNA PvE</span>
        <h1>CAMPAGNA</h1>
        <p>Affronta la campagna di Cinder-Rift. Scegli il tuo mazzo e mettilo alla prova in una battaglia simulata sul server.</p>
        <button className="btn-primary" onClick={onStart}>
          <Play size={18} fill="currentColor" /> INIZIA CAMPAGNA <ArrowRight size={18} />
        </button>
      </div>
    </section>
  )
}
