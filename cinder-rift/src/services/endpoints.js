// Paths are relative to the API base, which already includes /api.
export const ENDPOINTS = {
  auth: {
    csrfTokens: '/auth/csrf-tokens', signin: '/auth/signin', login: '/auth/login',
    me: '/auth/user/me', renewal: '/auth/session-renewals', logout: '/auth/logout',
  },
  decks: '/users/me/decks',
  deck: (id) => `/decks/${encodeURIComponent(id)}`,
  pve: (stageId) => `/battle/pve/${encodeURIComponent(stageId)}`,
  profile: '/users/me',
}
