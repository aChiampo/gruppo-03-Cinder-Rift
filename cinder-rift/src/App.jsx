import { useState } from 'react'
import Header from './components/layout/Header.jsx'
import Toast from './components/ui/Toast.jsx'
import HomeScreen from './screens/HomeScreen.jsx'
import PlaceholderScreen from './screens/PlaceholderScreen.jsx'
import { useToast } from './hooks/useToast.js'
import { user } from './data/user.js'

export default function App() {
  const [page, setPage] = useState('home')
  const [message, toast] = useToast()

  return (
    <div className="app">
      <Header page={page} setPage={setPage} credits={user.credits} />
      {page === 'home'
        ? <HomeScreen go={setPage} toast={toast} />
        : <PlaceholderScreen id={page} back={() => setPage('home')} />}
      <Toast message={message} />
    </div>
  )
}