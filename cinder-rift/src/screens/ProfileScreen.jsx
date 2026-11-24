import { useState } from 'react'
import { UserRound } from 'lucide-react'
import { useAuth } from '../context/AuthContext.jsx'
import { updateProfile } from '../services/authService.js'
import Panel from '../components/ui/Panel.jsx'
import RequestState from '../components/ui/RequestState.jsx'
export default function ProfileScreen({ settings = false }) {
  const { user, token, updateUser, logout } = useAuth()
  const [values, setValues] = useState({ nickname: user.nickname, email: user.email, iconUrl: user.iconUrl ?? '' })
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState(null)
  const [saved, setSaved] = useState(false)
  const submit = async (event) => {
    event.preventDefault(); setBusy(true); setError(null); setSaved(false)
    try { updateUser(await updateProfile(values, token)); setSaved(true) }
    catch (err) { setError(err) }
    finally { setBusy(false) }
  }
  return <main className="profile-page"><Panel icon={UserRound} title={settings ? 'IMPOSTAZIONI ACCOUNT' : 'IL TUO PROFILO'}>
    <p className="hint">{user.rankingPoints ?? '—'} punti classifica · {user.role}</p>
    <form className="form-grid" onSubmit={submit}>
      {['nickname', 'email', 'iconUrl'].map((key) => <label key={key}>{({ nickname: 'Nome profilo', email: 'Email', iconUrl: 'URL immagine profilo' })[key]}<input type={key === 'email' ? 'email' : 'text'} required={key !== 'iconUrl'} maxLength={key === 'nickname' ? 50 : key === 'email' ? 254 : undefined} value={values[key]} onChange={(e) => { setSaved(false); setValues((v) => ({ ...v, [key]: e.target.value })) }} disabled={busy} /></label>)}
      <button className="btn-primary" disabled={busy}>{busy ? 'SALVATAGGIO…' : 'SALVA PROFILO'}</button>
    </form>
    {error && <RequestState status="error" error={error} />}
    {saved && <p role="status" className="success-message">Profilo aggiornato.</p>}
    <button className="btn-ghost" onClick={logout}>ESCI</button>
  </Panel></main>
}
