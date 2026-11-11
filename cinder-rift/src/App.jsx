import { useState } from "react";
import Header from "./components/layout/Header.jsx";
import Toast from "./components/ui/Toast.jsx";
import HomeScreen from "./screens/HomeScreen.jsx";
import CollectionScreen from "./screens/CollectionScreen.jsx";
import DeckBuilder from "./screens/Deck-Builder.jsx";
import PlaceholderScreen from "./screens/PlaceholderScreen.jsx";
import { useToast } from "./hooks/useToast.js";
import { user } from "./data/user.js";

export default function App() {
  const [page, setPage] = useState("home");
  const [message, toast] = useToast();

  return (
    <div className="app">
      <Header page={page} setPage={setPage} credits={user.credits} />
      {page === "home" && <HomeScreen go={setPage} toast={toast} />}
      {page === "collezione" && <CollectionScreen />}
      {page === "deck-builder" && <DeckBuilder setPage={setPage} />}
      {!["home", "collezione", "deck-builder"].includes(page) && (
        <PlaceholderScreen id={page} back={() => setPage("home")} />
      )}
      <Toast message={message} />
    </div>
  );
}
