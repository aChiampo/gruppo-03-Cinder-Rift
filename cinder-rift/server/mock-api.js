// Backend di PROVA senza dipendenze: rispetta il contratto F04 del documento condiviso.
// Serve solo a sviluppare il frontend finché non c'è il backend vero. Da eliminare quando arriva.
// Avvio: npm run mock-api   ->   http://localhost:3000/api
import { createServer } from "node:http";
import { randomUUID } from "node:crypto";
import { collectionFor, queryCards } from "../src/mocks/cardsMock.js";

const PORT = process.env.PORT || 3000;
const DELAY = Number(process.env.MOCK_DELAY) || 0;
const wait = (ms) => new Promise((r) => setTimeout(r, ms));

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
const sessions = new Map();

function send(res, status, body) {
  res.writeHead(status, {
    "Content-Type": "application/json; charset=utf-8",
    "Access-Control-Allow-Origin": "http://localhost:8082",
    "Access-Control-Allow-Credentials": "true",
    "Access-Control-Allow-Headers": "Content-Type, Authorization, X-XSRF-TOKEN",
    "Access-Control-Allow-Methods": "GET, POST, PUT, PATCH, DELETE, OPTIONS",
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
      try { resolve(JSON.parse(raw || "{}")); } catch { resolve(null); }
    });
  });

const userFromToken = (req) => {
  const token = (req.headers.authorization || "").replace(/^Bearer /, "");
  return { token, user: USERS.find((u) => sessions.get(token) === u.id) };
};

createServer(async (req, res) => {
  const { pathname } = new URL(req.url, `http://${req.headers.host}`);
  console.log(`${req.method} ${pathname}`);

  if (req.method === "OPTIONS") return send(res, 204, null);
  if (req.method === "GET" && pathname === "/api/health") return send(res, 200, { status: "ok" });

  if (req.method === "GET" && pathname === "/api/auth/csrf-tokens") {
    return send(res, 200, { headerName: "X-XSRF-TOKEN", token: "mock-csrf-token" });
  }

  if (req.method === "POST" && pathname === "/api/auth/sessions") {
    const body = await readBody(req);
    if (!body?.email || !body?.password) {
      return send(res, 400, { message: "Inserisci email e password." });
    }
    const email = String(body.email).trim().toLowerCase();
    const user = USERS.find((u) => u.email === email);
    if (!user || user.password !== body.password) {
      return send(res, 401, { message: "Credenziali non valide" });
    }
    const token = randomUUID();
    sessions.set(token, user.id);
    return send(res, 200, {
      accessToken: token,
      tokenType: "Bearer",
      expiresIn: 900,
      user: {
        id: user.id,
        nickname: user.name,
        email: user.email,
        role: "USER",
        rankingPoints: user.credits,
        iconUrl: user.avatar,
      },
    });
  }

  if (req.method === "GET" && (pathname === "/api/auth/users/me" || pathname === "/api/auth/user/me" || pathname === "/api/auth/me")) {
    const { user } = userFromToken(req);
    if (!user) return send(res, 401, { message: "Sessione non valida" });
    return send(res, 200, {
      id: user.id,
      nickname: user.name,
      email: user.email,
      role: "USER",
      rankingPoints: user.credits,
      iconUrl: user.avatar,
    });
  }

  if (req.method === "POST" && (pathname === "/api/auth/session-revocations" || pathname === "/api/auth/logout")) {
    const token = (req.headers.authorization || "").replace(/^Bearer /, "");
    if (token) sessions.delete(token);
    return send(res, 204, null);
  }

  if (req.method === "GET" && (pathname === "/api/users/me/collections" || pathname === "/api/cards")) {
    const { user } = userFromToken(req);
    if (!user) return send(res, 401, { message: "Sessione non valida" });
    if (DELAY) await wait(DELAY);
    try {
      const params = Object.fromEntries(new URL(req.url, `http://${req.headers.host}`).searchParams);
      return send(res, 200, queryCards(collectionFor(user.id), params));
    } catch (err) {
      return send(res, err.status || 500, { message: err.message });
    }
  }

  send(res, 404, { message: "Endpoint non trovato" });
}).listen(PORT, () => console.log(`Mock API su http://localhost:${PORT}/api  (utenti: mirko / cinder123 · nuovo / cinder123 = collezione vuota)`));
