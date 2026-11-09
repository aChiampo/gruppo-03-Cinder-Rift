import { ChevronRight } from 'lucide-react'

export default function Panel({ icon: Icon, title, link, onLink, children, className = '' }) {
  return (
    <section className={`panel ${className}`}>
      <div className="panel-head">
        <h2><Icon size={26} /> {title}</h2>
        {link && (
          <button className="link" onClick={onLink}>{link} <ChevronRight size={14} /></button>
        )}
      </div>
      {children}
    </section>
  )
}
