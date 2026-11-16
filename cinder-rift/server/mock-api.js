// Backend di PROVA senza dipendenze: rispetta il contratto API descritto nel README.
// Serve solo a sviluppare il frontend finché non c'è il backend vero. Da eliminare (cartella /server) quando arriva.
// Avvio: npm run mock-api   ->   http://localhost:3000/api
import { createServer } from "node:http";
import { randomUUID } from "node:crypto";
import { collectionFor, queryCards } from "../src/mocks/cardsMock.js";

const PORT = process.env.PORT || 3000;
// Latenza artificiale (ms) per vedere gli stati di caricamento: MOCK_DELAY=1200 npm run mock-api
const DELAY = Number(process.env.MOCK_DELAY) || 0;
const wait = (ms) => new Promise((r) => setTimeout(r, ms));

// Utenti di esempio (password in chiaro: SOLO per questo mock)
const USERS = [
  {
    id: 1,
    username: "mirko",
    email: "mirko@cinder-rift.dev",
    password: "cinder123",
    name: "Mirko",
    level: 3,
    credits: 2450,
    avatar: "/img/avatar-cyborg.jpg",
  },
  // Giocatore appena registrato: nessuna carta (per provare la "collezione vuota")
  {
    id: 2,
    username: "nuovo",
    email: "nuovo@cinder-rift.dev",
    password: "cinder123",
    name: "Nuovo",
    level: 1,
    credits: 0,
    avatar: "/img/avatar-sapiens.jpg",
  },
];
const sessions = new Map(); // token -> id utente

const publicUser = ({ password, username, email, ...rest }) => rest;

function send(res, status, body) {
  res.writeHead(status, {
    "Content-Type": "application/json; charset=utf-8",
    // CORS: il frontend (porta 5173) e questo server (porta 3000) sono origini diverse
    "Access-Control-Allow-Origin": "*",
    "Access-Control-Allow-Headers": "Content-Type, Authorization",
    "Access-Control-Allow-Methods": "GET, POST, OPTIONS",
  });
  res.end(status === 204 ? undefined : JSON.stringify(body));
}

const readBody = (req) =>
  new Promise((resolve) => {
    let raw = "";
    req.on("data", (c) => {
      raw += c;
      if (raw.length > 1e5) req.destroy();
    });
    req.on("end", () => {
      try {
        resolve(JSON.parse(raw || "{}"));
      } catch {
        resolve(null);
      }
    });
  });

const userFromToken = (req) => {
  const token = (req.headers.authorization || "").replace(/^Bearer /, "");
  const id = sessions.get(token);
  return { token, user: USERS.find((u) => u.id === id) };
};

createServer(async (req, res) => {
  const { pathname } = new URL(req.url, `http://${req.headers.host}`);
  console.log(`${req.method} ${pathname}`);

  if (req.method === "OPTIONS") return send(res, 204);
  if (req.method === "GET" && pathname === "/api/health")
    return send(res, 200, { status: "ok" });

  // POST /api/auth/login  { identifier, password } -> 200 { token, user } | 400 | 401
  if (req.method === "POST" && pathname === "/api/auth/login") {
    const body = await readBody(req);
    if (!body?.identifier || !body?.password)
      return send(res, 400, {
        message: "Inserisci username/email e password.",
      });
    const id = String(body.identifier).trim().toLowerCase();
    const user = USERS.find((u) => u.username === id || u.email === id);
    if (!user || user.password !== body.password)
      return send(res, 401, { message: "Credenziali non valide" });
    const token = randomUUID();
    sessions.set(token, user.id);
    return send(res, 200, { token, user: publicUser(user) });
  }

  // GET /api/auth/me  (Bearer) -> 200 { user } | 401
  if (req.method === "GET" && pathname === "/api/auth/me") {
    const { user } = userFromToken(req);
    return user
      ? send(res, 200, { user: publicUser(user) })
      : send(res, 401, { message: "Sessione non valida" });
  }

  // POST /api/auth/logout  (Bearer) -> 204
  if (req.method === "POST" && pathname === "/api/auth/logout") {
    sessions.delete(userFromToken(req).token);
    return send(res, 204);
  }

  // GET /api/cards?type&faction&rarity&energyMin&energyMax&attackMin&attackMax&defenseMin&defenseMax&q&sort&page&pageSize
  //   (Bearer) -> 200 { items, page, pageSize, total, counts: { all, creature, equipment } } | 400 { message } | 401
  // Restituisce SOLO le carte possedute dall'utente del token. Dettagli in docs/API-COLLEZIONE.md.
  if (req.method === "GET" && pathname === "/api/cards") {
    const { user } = userFromToken(req);
    if (!user) return send(res, 401, { message: "Sessione non valida" });
    if (DELAY) await wait(DELAY);
    try {
      const params = Object.fromEntries(
        new URL(req.url, `http://${req.headers.host}`).searchParams,
      );
      return send(res, 200, queryCards(collectionFor(user.id), params));
    } catch (err) {
      return send(res, err.status || 500, { message: err.message });
    }
  }

  send(res, 404, { message: "Endpoint non trovato" });
}).listen(PORT, () =>
  console.log(
    `Mock API su http://localhost:${PORT}/api  (utenti: mirko / cinder123 · nuovo / cinder123 = collezione vuota)`,
  ),
);
