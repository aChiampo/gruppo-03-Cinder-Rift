// Filtri e ordinamento li esegue il BACKEND (GET /cards): qui restano solo valori e opzioni dell'interfaccia.
/**
 * Rarità. `power` = intervallo di punti potenza per carta (regolamento v2.0, par. 3).
 * `rank` serve solo all'ordinamento.
 */
export const RARITIES = [
  { id: "comune", label: "Comune", rank: 1, color: "#b6c2d0", power: [1, 2] },
  { id: "rara", label: "Rara", rank: 2, color: "#3d9bff", power: [2, 3] },
  { id: "epica", label: "Epica", rank: 3, color: "#b34dff", power: [3, 4] },
  {
    id: "leggendaria",
    label: "Leggendaria",
    rank: 4,
    color: "#ffb629",
    power: [5, 5],
  },
];
export const rarityById = Object.fromEntries(RARITIES.map((r) => [r.id, r]));

export const ENERGY_RANGE = [1, 5]; // soglie energetiche dei 5 round
export const STAT_RANGE = [0, 10]; // intervallo dei filtri Attacco / Difesa
export const PAGE_SIZE = 15; // 5 colonne x 3 righe

export const defaultFilters = {
  query: "",
  type: "all", // 'all' | 'creature' | 'equipment'
  factions: [], // vuoto = tutte
  rarities: [], // vuoto = tutte
  energy: [...ENERGY_RANGE],
  attack: [...STAT_RANGE],
  defense: [...STAT_RANGE],
};

export const SORT_OPTIONS = [
  { id: "rarita", label: "Rarità" },
  { id: "nome", label: "Nome" },
  { id: "energia", label: "Energia" },
  { id: "attacco", label: "Attacco" },
  { id: "difesa", label: "Difesa" },
  { id: "fazione", label: "Fazione" },
];

/** Un intervallo è "attivo" solo se più stretto di quello completo: solo allora viene mandato al server. */
export const isNarrowed = (range, full) =>
  range[0] !== full[0] || range[1] !== full[1];

/** Numeri di pagina da mostrare: tutti se pochi, altrimenti una finestra con puntini. */
export function pageWindow(current, total) {
  if (total <= 7) return Array.from({ length: total }, (_, i) => i + 1);
  const pages = new Set([1, total, current - 1, current, current + 1]);
  const list = [...pages]
    .filter((p) => p >= 1 && p <= total)
    .sort((a, b) => a - b);
  return list.flatMap((p, i) => (i && p - list[i - 1] > 1 ? ["…", p] : [p]));
}
