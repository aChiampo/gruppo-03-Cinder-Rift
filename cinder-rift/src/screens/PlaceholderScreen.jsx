import { placeholders } from "../data/navigation.js";

export default function PlaceholderScreen({ id, back }) {
  const p = placeholders[id] ?? { title: id.toUpperCase(), items: [] };
  return (
    <main className="placeholder panel">
      <h1>{p.title}</h1>
      <p>
        Sezione da realizzare. Contenuti previsti dal documento di progetto:
      </p>
      <ul>
        {p.items.map((i) => (
          <li key={i}>{i}</li>
        ))}
      </ul>
      <button className="btn-outline" onClick={back}>
        TORNA ALLA HOME
      </button>
    </main>
  );
}
