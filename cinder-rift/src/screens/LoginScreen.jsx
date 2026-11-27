import { useState } from 'react'
import { useAuth } from '../context/AuthContext.jsx'
import { User, Mail, Lock, Eye, EyeOff, LogIn, ChevronRight, UserRound } from 'lucide-react'

export default function LoginScreen({ mode = 'login', onMode }) {
  const { login, register, sessionError, restore } = useAuth()
  const signingUp = mode === 'register'
  const [values, setValues] = useState({ nickname: '', email: '', password: '', confirmation: '' })
  const [showPwd, setShowPwd] = useState(false)
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)
  const field = (key) => ({ value: values[key], onChange: (e) => setValues((v) => ({ ...v, [key]: e.target.value })) })
  const submit = async (event) => {
    event.preventDefault()
    setError('')
    if (!values.password.trim() || new TextEncoder().encode(values.password).length > 72) return setError('La password non può essere vuota o superare 72 byte UTF-8.')
    if (signingUp && !values.nickname.trim()) return setError('Inserisci un nome profilo.')
    if (signingUp && values.password !== values.confirmation) return setError('Le password non coincidono.')
    setLoading(true)
    try { await (signingUp ? register : login)(values) }
    catch (err) { setError(err.message || 'Accesso non riuscito.') }
    finally { setLoading(false) }
  }
  return <div className="login">
    <img className="login-logo" src="/img/logo-horizontal.png" alt="Cinder-Rift — The Cobalt Addiction" />
    <div className="login-wrap"><div className="login-card">
      <form className="login-inner" onSubmit={submit}>
        <h1 className="login-title"><UserRound size={36} /> {signingUp ? 'REGISTRAZIONE' : 'ACCEDI'}</h1>
        <div className="login-divider"><i /></div>
        <p className="login-sub">{signingUp ? 'Hai già un profilo?' : 'Non hai un profilo?'} <button type="button" className="login-link" disabled={loading} onClick={() => onMode(signingUp ? 'login' : 'register')}>{signingUp ? 'Accedi' : 'Registrati'}</button></p>
        {signingUp && <label className="field"><User size={22} /><input aria-label="Nome profilo" placeholder="Nome profilo" autoComplete="nickname" maxLength={50} required {...field('nickname')} /></label>}
        <label className="field"><Mail size={22} /><input type="email" aria-label="Email" placeholder="Email" autoComplete="email" maxLength={254} required {...field('email')} /></label>
        <label className="field"><Lock size={22} /><input type={showPwd ? 'text' : 'password'} aria-label="Password" placeholder="Password" autoComplete={signingUp ? 'new-password' : 'current-password'} required {...field('password')} /><button type="button" className="eye" aria-label={showPwd ? 'Nascondi password' : 'Mostra password'} onClick={() => setShowPwd((v) => !v)}>{showPwd ? <Eye size={22} /> : <EyeOff size={22} />}</button></label>
        {signingUp && <label className="field"><Lock size={22} /><input type={showPwd ? 'text' : 'password'} aria-label="Conferma password" placeholder="Conferma password" autoComplete="new-password" required {...field('confirmation')} /></label>}
        {error && <p className="login-error" role="alert">{error}</p>}
        {sessionError && <div className="login-error" role="alert"><p>{sessionError.message}</p><button type="button" className="login-link" onClick={restore} disabled={loading}>Riprova connessione</button></div>}
        <button type="submit" className="btn-primary login-btn" disabled={loading}><LogIn size={22} /><span>{loading ? 'ATTENDI…' : signingUp ? 'REGISTRATI' : 'ACCEDI'}</span><ChevronRight size={22} /></button>
      </form>
    </div></div>
  </div>
}
