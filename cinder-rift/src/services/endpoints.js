// Tutti gli endpoint del backend in un unico posto (percorsi relativi a VITE_API_URL).
// Quando il backend sarà pronto, se i percorsi sono diversi si cambia solo questo file.
export const ENDPOINTS = {
  auth: {
  csrfTokens: "/auth/csrf-tokens",
  login: "/auth/login",
  me: "/auth/user/me",
  logout: "/auth/logout",
},
  cards: {
    // GET (Bearer token) ?type&faction&rarity&energyMin&energyMax&attackMin&attackMax&defenseMin&defenseMax&q&sort&page&pageSize
    //   -> { items, page, pageSize, total, counts }   (solo le carte possedute dall'utente: docs/API-COLLEZIONE.md)
    collection: "/cards",
  },
};
