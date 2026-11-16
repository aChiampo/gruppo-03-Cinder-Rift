import { Swords, ArrowRight } from 'lucide-react'

export default function NewChallengeBanner({ onClick }) {
  return (
    <section className="cta-banner">
      <img className="cta-l" src="/img/art-cyborg.jpg" alt="" />
      <img className="cta-r" src="/img/art-radioattivi.jpg" alt="" />
      <div className="hero-shade" />
      <div className="cta-body">
        <h2>PRONTO A UNA NUOVA SFIDA?</h2>
        <p>Metti alla prova il tuo mazzo contro altri giocatori e scala la classifica!</p>
      </div>
      <button className="btn-primary" onClick={onClick}><Swords size={18} /> NUOVA SFIDA <ArrowRight size={18} /></button>
    </section>
  )
}
