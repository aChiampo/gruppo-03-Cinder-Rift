import { useEffect, useState } from "react";
import Header from "./components/layout/Header.jsx";
import Toast from "./components/ui/Toast.jsx";
import LoginScreen from "./screens/LoginScreen.jsx";
import HomeScreen from "./screens/HomeScreen.jsx";
import CollectionScreen from "./screens/CollectionScreen.jsx";
import DeckScreen from "./screens/DeckScreen.jsx";
import PlaceholderScreen from "./screens/PlaceholderScreen.jsx";
import { useToast } from "./hooks/useToast.js";
import { useAuth } from "./context/AuthContext.jsx";

export default function App() {
  const { user, checking } = useAuth(); // user null = non autenticato
  const [page, setPage] = useState("home");
  const [message, toast] = useToast();

  // Dopo il logout, al prossimo accesso si riparte dalla Home
  useEffect(() => {
    if (!user) setPage("home");
  }, [user]);

  // Ricaricamento della pagina: attende la verifica della sessione col backend (GET /auth/me)
  if (checking)
    return (
      <div className="session-check" role="status">
        Verifica sessione…
      </div>
    );

  // La prima schermata è il login: dopo l'accesso (user valorizzato) si vede la Home
  if (!user) {
    return (
      <>
        <LoginScreen
          onRegister={() =>
            toast("Registrazione in arrivo: sarà la prossima pagina.")
          }
        />
        <Toast message={message} />
      </>
    );
  }

  return (
    <div className="app">
      <Header page={page} setPage={setPage} credits={user.credits} />
      {page === "home" && <HomeScreen go={setPage} toast={toast} />}
      {page === "collezione" && <CollectionScreen go={setPage} />}
      {page === "deck" && <DeckScreen />}
      {!["home", "collezione", "deck"].includes(page) && (
        <PlaceholderScreen id={page} back={() => setPage("home")} />
      )}
      <Toast message={message} />
    </div>
  );
}
