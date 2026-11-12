import { useState } from 'react'
import { useAuth } from '../context/AuthContext.jsx'
import { User, Lock, Eye, EyeOff, LogIn, ChevronRight, UserRound } from 'lucide-react'

export default function LoginScreen({ onRegister }) {
  const { login } = useAuth()
  const [identifier, setIdentifier] = useState('')
  const [password, setPassword] = useState('')
  const [showPwd, setShowPwd] = useState(false)
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)

  const submit = async (e) => {
    e.preventDefault()
    if (!identifier.trim() || !password) {
      setError('Inserisci username/email e password.')
      return
    }
    setError('')
    setLoading(true)
    try {
      await login({ identifier: identifier.trim(), password }) // poi App mostra la Home
    } catch (err) {
      setError(err.message || 'Accesso non riuscito. Riprova.')
      setLoading(false)
    }
  }

  return (
    <div className="login">
      <img className="login-logo" src="/img/logo-horizontal.png" alt="Cinder-Rift — The Cobalt Addiction" />

      <div className="login-wrap">
        <div className="login-card">
          <form className="login-inner" onSubmit={submit} noValidate>
            <h1 className="login-title"><UserRound size={44} strokeWidth={1.6} /> ACCEDI</h1>
            <div className="login-divider"><i /></div>

            <p className="login-sub">
              Non hai un profilo?{' '}
              <button type="button" className="login-link" onClick={onRegister}>Registrati</button>
            </p>

            <label className="field">
              <User size={22} fill="currentColor" />
              <input
                type="text"
                placeholder="Username o Email"
                autoComplete="username"
                value={identifier}
                onChange={(e) => setIdentifier(e.target.value)}
                aria-label="Username o Email"
              />
            </label>

            <label className="field">
              <Lock size={22} fill="currentColor" />
              <input
                type={showPwd ? 'text' : 'password'}
                placeholder="Password"
                autoComplete="current-password"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                aria-label="Password"
              />
              <button type="button" className="eye" onClick={() => setShowPwd((v) => !v)} aria-label={showPwd ? 'Nascondi password' : 'Mostra password'}>
                {showPwd ? <Eye size={22} /> : <EyeOff size={22} />}
              </button>
            </label>

            {error && <p className="login-error" role="alert">{error}</p>}

            <button type="submit" className="btn-primary login-btn" disabled={loading}>
              <LogIn size={22} /> <span>{loading ? 'ACCESSO IN CORSO…' : 'ACCEDI'}</span> <ChevronRight size={22} />
            </button>
          </form>
        </div>
      </div>
    </div>
  )
}
