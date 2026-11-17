// Tutti gli endpoint del backend in un unico posto (percorsi relativi a VITE_API_URL).
// Quando il backend sarà pronto, se i percorsi sono diversi si cambia solo questo file.
export const ENDPOINTS = {
  auth: {
    login: "/api/auth/login", // POST  { email, password } -> { accessToken, tokenType, expiresIn, user }
    me: "/api/auth/user/me", // GET   (Bearer token) -> { id, nickname, email, role, rankingPoints, iconUrl }
    logout: "/api/auth/logout", // POST  (Bearer token) -> 204
  },
  cards: {
    // GET (Bearer token) ?type&faction&rarity&energyMin&energyMax&attackMin&attackMax&defenseMin&defenseMax&q&sort&page&pageSize
    //   -> { items, page, pageSize, total, counts }   (solo le carte possedute dall'utente: docs/API-COLLEZIONE.md)
    collection: "/api/cards",
  },
};
