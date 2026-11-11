export default function DeckValidation({ validation }) {
  const errors = validation?.errors ?? []
  const warnings = validation?.warnings ?? []

  return (
    <div className="deck-validation">
      <h4>Validazione mazzo</h4>

      {errors.length === 0 && warnings.length === 0 ? (
        <div className="deck-validation__status success">Mazzo conforme ai vincoli.</div>
      ) : null}

      {errors.length > 0 && (
        <ul className="deck-validation__list deck-validation__list--error">
          {errors.map((message, index) => (
            <li key={`error-${index}`}>{message}</li>
          ))}
        </ul>
      )}

      {warnings.length > 0 && (
        <ul className="deck-validation__list deck-validation__list--warning">
          {warnings.map((message, index) => (
            <li key={`warning-${index}`}>{message}</li>
          ))}
        </ul>
      )}
    </div>
  )
}
