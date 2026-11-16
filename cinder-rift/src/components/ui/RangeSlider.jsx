/** Slider a due maniglie (minimo e massimo) fatto con due <input type="range"> sovrapposti. */
export default function RangeSlider({ min, max, step = 1, value, onChange, label }) {
  const [lo, hi] = value
  const pct = (v) => ((v - min) / (max - min)) * 100
  return (
    <div className="range">
      <div className="range-track"><div className="range-fill" style={{ left: `${pct(lo)}%`, width: `${pct(hi) - pct(lo)}%` }} /></div>
      <input type="range" min={min} max={max} step={step} value={lo} aria-label={`${label}: minimo`}
        style={{ zIndex: lo > min + (max - min) / 2 ? 4 : 2 }}
        onChange={(e) => onChange([Math.min(Number(e.target.value), hi), hi])} />
      <input type="range" min={min} max={max} step={step} value={hi} aria-label={`${label}: massimo`}
        style={{ zIndex: 3 }}
        onChange={(e) => onChange([lo, Math.max(Number(e.target.value), lo)])} />
    </div>
  )
}
