import { ApiError } from './api.js'

// F04 has no agreed HTTP route or response schema. Never substitute fixture data.
export async function getCollection() {
  throw new ApiError('La collezione non è disponibile: il servizio carte non è ancora pubblicato.', 501)
}
export function buildCollectionParams({ filters, sort, page, pageSize = 15 }) {
  return { filters, sort, page, pageSize }
}
