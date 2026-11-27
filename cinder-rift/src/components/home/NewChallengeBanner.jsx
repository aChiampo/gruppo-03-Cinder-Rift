import { Swords, ArrowRight } from 'lucide-react'

export default function NewChallengeBanner({ onClick }) {
  return (
    <section className="cta-banner">
      <img className="cta-l" src="/img/art-cyborg.jpg" alt="" />
      <img className="cta-r" src="/img/art-radioattivi.jpg" alt="" />
      <div className="hero-shade" />
      <div className="cta-body">
        <h2>PRONTO A UNA NUOVA BATTAGLIA?</h2>
        <p>Scegli il tuo mazzo e affronta la campagna.</p>
      </div>
      <button className="btn-primary" onClick={onClick}><Swords size={18} /> GIOCA <ArrowRight size={18} /></button>
    </section>
  )
}
