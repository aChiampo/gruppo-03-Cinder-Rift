/** Esagono con bordo colorato (usato per energia, fazioni e rarità). `filled` = interno tinto. */
export default function Hex({ color, size = 28, filled = false, glow = true, children, className = '' }) {
  return (
    <span className={`hex ${glow ? 'hex-glow' : ''} ${className}`} style={{ '--c': color, '--s': `${size}px` }}>
      <span className="hex-edge">
        <span className={`hex-in ${filled ? 'hex-in-filled' : ''}`}>{children}</span>
      </span>
    </span>
  )
}
