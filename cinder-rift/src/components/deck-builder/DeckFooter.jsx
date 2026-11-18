export default function DeckFooter({ onVerify, onReset }) {
  return (
    <div className="deck-footer">
      <button type="button" className="deck-footer__button secondary" onClick={onReset}>Reset</button>
      <button type="button" className="deck-footer__button primary" onClick={onVerify}>Verifica mazzo</button>
    </div>
  )
}
