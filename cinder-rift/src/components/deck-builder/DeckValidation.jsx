export default function DeckValidation({ validation, dirty }) {
  if (!validation) return null
  return <section className="deck-validation" aria-live="polite"><h3>ESITO DELLA VALIDAZIONE</h3>
    {dirty && <p className="hint">Esito precedente alle modifiche correnti.</p>}
    <p className={validation.valid && !validation.warnings?.length ? 'success-message' : 'hint'}>{validation.valid ? validation.warnings?.length ? 'Bozza salvabile, composizione da completare.' : 'Validazione superata.' : 'Il mazzo presenta errori.'}</p>
    {['errors', 'warnings'].map((kind) => <ul key={kind} className={`deck-validation__list deck-validation__list--${kind === 'errors' ? 'error' : 'warning'}`}>{(validation[kind] ?? []).map((issue, index) => <li key={index}><strong>{issue.destination ? `${issue.destination}: ` : ''}{issue.message}</strong><small>{issue.code}</small></li>)}</ul>)}
  </section>
}
