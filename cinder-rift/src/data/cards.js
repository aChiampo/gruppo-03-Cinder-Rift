// Collezione iniziale: 30 carte = 25 creature + 5 equipaggiamenti (regolamento v2.0).
// Dati di esempio da sostituire con le API.
//
// ILLUSTRAZIONI: nel set dimostrativo c'è 1 illustrazione per fazione + 2 per gli equipaggiamenti.
// Le altre carte riusano la stessa immagine con un ritaglio diverso (art.view) finché non
// arrivano le illustrazioni definitive: basta cambiare `art.src` (cartella public/img/cards/).
// Gli equipaggiamenti senza illustrazione mostrano un'icona (art.icon).
//
// EFFETTI EQUIPAGGIAMENTI: bozze da validare, il regolamento (par. 15) li lascia da definire.

// Ritagli (in % del riquadro): x/y = punto di messa a fuoco, zoom, flip = specchiato.
const VIEWS = [
  { x: 50, y: 50, zoom: 1 },
  { x: 40, y: 0, zoom: 1.45 },
  { x: 72, y: 25, zoom: 1.4 },
  { x: 50, y: 50, zoom: 1.05, flip: true },
  { x: 60, y: 0, zoom: 1.4, flip: true },
]
const art = (src, i = 0) => ({ src: `/img/cards/${src}.jpg`, ...VIEWS[i] })
const equipIcon = (icon) => ({ icon })

const creature = (faction, rarity, name, energy, power, attack, defense, flavor, artView, extra = {}) => ({
  id: name.toLowerCase().normalize('NFD').replace(/[\u0300-\u036f]/g, '').replace(/[^a-z0-9]+/g, '-').replace(/(^-|-$)/g, ''),
  type: 'creature', faction, rarity, name, energy, power, attack, defense, flavor,
  art: art(faction, artView), ...extra,
})

const equipment = (rarity, name, energy, power, accent, effect, flavor, artDef) => ({
  id: name.toLowerCase().replace(/[^a-z0-9]+/g, '-'),
  type: 'equipment', faction: null, rarity, name, energy, power, accent, effect, flavor, duration: 'Round', art: artDef,
})

const list = [
  // Cyborg
  creature('cyborg', 'comune', 'Cyborg Scout', 1, 1, 2, 2, 'Un occhio in ogni ombra, un ordine in ogni passo.', 1),
  creature('cyborg', 'rara', 'Cyborg Berserker', 3, 2, 4, 4, 'Quando il protocollo si spezza, resta solo la furia.', 2),
  creature('cyborg', 'comune', 'Sentinella K-17', 2, 2, 3, 5, 'La città è caduta. I suoi ordini di restare di guardia no.', 0),
  creature('cyborg', 'comune', 'Riparatore R-9', 1, 1, 1, 3, 'Ripara tutto, tranne sé stesso.', 3),
  creature('cyborg', 'comune', 'Unità Frammento', 3, 2, 4, 3, 'Un pezzo di una macchina più grande, ancora in marcia.', 4),
  // Mostri Radioattivi
  creature('radioattivi', 'rara', 'Toxik Mutant', 2, 2, 3, 3, 'Il veleno è l’unica cosa che gli è rimasta di umano.', 1),
  creature('radioattivi', 'epica', 'Mutante Alfa', 4, 4, 6, 5, 'Il branco segue chi ha resistito di più alle radiazioni.', 2),
  creature('radioattivi', 'comune', 'Cane di Scoria', 3, 2, 4, 4, 'Dove la terra brucia, lui trova una pista.', 0),
  creature('radioattivi', 'comune', 'Larva Tossica', 1, 1, 1, 2, 'Piccola, lenta, ovunque.', 3),
  creature('radioattivi', 'comune', 'Brulicante', 2, 1, 3, 2, 'Non è mai solo. Non è mai uno.', 4),
  // Sapiens
  creature('sapiens', 'epica', 'Sapiens Guardiano', 4, 4, 5, 5, 'La conoscenza è la vera arma.', 1, {
    lore: 'I Sapiens non combattono per distruggere, ma per costruire un domani migliore.',
  }),
  creature('sapiens', 'comune', 'Sapiens Medic', 2, 1, 3, 3, 'Ogni ferita curata è un giorno guadagnato.', 2),
  creature('sapiens', 'comune', 'Esploratore Rovine', 2, 2, 3, 5, 'Ogni rovina nasconde una via d’uscita.', 0),
  creature('sapiens', 'comune', 'Recuperante', 1, 1, 2, 2, 'Quello che per gli altri è rottame, per lui è un inizio.', 3),
  creature('sapiens', 'comune', 'Arciere del Ponte', 3, 2, 4, 3, 'Dal ponte vede arrivare tutto, molto prima degli altri.', 4),
  // Cultisti
  creature('cultisti', 'comune', 'Cultista Ombra', 1, 1, 1, 2, 'Chi non si vede non può essere fermato.', 1),
  creature('cultisti', 'rara', 'Cultista Sacerdote', 3, 3, 4, 4, 'Predica alla Frattura. E la Frattura ascolta.', 2),
  creature('cultisti', 'comune', 'Accolito Frattura', 3, 2, 4, 4, 'Nel silenzio della Frattura, qualcuno risponde.', 0),
  creature('cultisti', 'comune', 'Novizio del Velo', 2, 1, 2, 3, 'Ha giurato fedeltà prima ancora di capire a cosa.', 3),
  creature('cultisti', 'comune', 'Fanatico Cobalt', 2, 2, 3, 3, 'Il Cobalto lo chiama. Lui risponde sempre.', 4),
  // Alieni
  creature('alieni', 'rara', 'Alieno Drone', 3, 3, 4, 3, 'Obbedisce a una voce che nessun altro sente.', 1),
  creature('alieni', 'leggendaria', 'Alieno Predatore', 5, 5, 7, 6, 'Non cerca una preda. Cerca un degno avversario.', 2),
  creature('alieni', 'comune', 'Segugio di Orione', 3, 2, 5, 3, 'Per lui, questo mondo è appena cominciato.', 0),
  creature('alieni', 'comune', 'Larva Aliena', 1, 1, 2, 1, 'Cresce in fretta. Impara ancora più in fretta.', 3),
  creature('alieni', 'comune', 'Esploratore Xeno', 2, 1, 3, 2, 'Ogni mondo è un campione da analizzare.', 4),

  // Equipaggiamenti
  equipment('comune', 'Modulo Energetico', 1, 1, '#ff8a1f',
    'La creatura equipaggiata ottiene +1 attacco.',
    'Un frammento di tecnologia. Un vantaggio tangibile.',
    { src: '/img/cards/equip-lama.jpg', x: 50, y: 50, zoom: 1 }),
  equipment('rara', 'Armatura Tattica', 2, 2, '#3d9bff',
    'Ogni creatura entra con +2 difesa iniziale. Non cura la creatura attiva.',
    'Nulla va perso finché può proteggere.',
    { src: '/img/cards/equip-armatura.jpg', x: 50, y: 50, zoom: 1 }),
  equipment('epica', 'Chip Neurale', 3, 3, '#b34dff',
    'La creatura equipaggiata ottiene +1 attacco e +1 difesa.',
    'Pensa più in fretta di quanto riesca a sopportare.',
    equipIcon('Cpu')),
  equipment('rara', 'Bomba a Nulla', 4, 3, '#5dff3a',
    'All’ingresso, infligge 2 danni alla creatura avversaria attiva.',
    'Dove cade, non resta nemmeno il silenzio.',
    equipIcon('Bomb')),
  // NB: il PDF economico prevede nessun equipaggiamento leggendario nel primo set.
  // Qui resta perché presente nel mockup: per allinearsi al PDF basta cambiare la rarità.
  equipment('leggendaria', 'Velo di Ombra', 5, 5, '#c04dff',
    'La creatura equipaggiata ottiene +3 difesa.',
    'Chi lo indossa smette di essere un bersaglio.',
    equipIcon('Ghost')),
]

export const cards = list.map((c, order) => ({ ...c, order }))
export const DEFAULT_SELECTED_ID = 'sapiens-guardiano'
