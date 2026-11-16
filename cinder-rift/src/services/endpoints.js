// Endpoint del backend allineati al contratto F04 e al backend Spring Boot.
export const ENDPOINTS = {
  auth: {
    csrf: "/api/auth/csrf-tokens",
    login: "/api/auth/sessions",
    me: "/api/auth/users/me",
    logout: "/api/auth/session-revocations",
  },
  cards: {
    // GET /api/users/me/collections?name&faction&rarity&type&energyMin&energyMax&attackMin&attackMax&defenseMin&defenseMax&sort&direction&page&size
    collection: "/api/users/me/collections",
  },
};
