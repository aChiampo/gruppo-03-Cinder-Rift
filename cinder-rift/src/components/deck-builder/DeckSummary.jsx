import { DESTINATIONS } from '../../services/deckService.js'

const destinationEntries = (values) => Object.entries(values ?? {})
  .sort(([left], [right]) => DESTINATIONS.indexOf(left) - DESTINATIONS.indexOf(right))

export default function DeckSummary({ validation, dirty }) {
  const summary = validation?.summary
  return <section className="deck-summary"><h2>RIEPILOGO MAZZO</h2>
    <p className="hint">{dirty ? 'Valori dell’ultimo salvataggio. Salva per verificare le modifiche.' : 'Validazione fornita dal server.'}</p>
    {!summary ? <p className="request-state">Seleziona un mazzo per visualizzare il riepilogo.</p> : <>
      <div className="deck-summary__metrics">{[['Totale carte', summary.totalCards], ['Creature', summary.creatureCount], ['Equipaggiamenti', summary.equipmentCount]].map(([label, value]) => <div key={label}><span>{label}</span><strong>{value ?? '—'}</strong></div>)}</div>
      <h3>Copie per destinazione</h3><dl className="summary-list">{destinationEntries(summary.cardCountByDestination).map(([key, count]) => <div key={key}><dt>{key}</dt><dd>{count}</dd></div>)}</dl>
      <h3>Energia totale per destinazione</h3><dl className="summary-list">{destinationEntries(summary.energyByDestination).map(([key, energy]) => <div key={key}><dt>{key}</dt><dd>{energy}</dd></div>)}</dl>
    </>}
  </section>
}
