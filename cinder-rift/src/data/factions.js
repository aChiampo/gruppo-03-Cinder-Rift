// Colore guida per fazione (palette dal documento di lore)
export const factionColors = {
  mixed: "#ffb020",
  cyborg: "#ffd400",
  radioattivi: "#9bff3d",
  cultisti: "#d24dff",
  sapiens: "#ff7a2f",
  alieni: "#3dffd8",
};

// Fazioni della Collezione: colori e nomi come da mockup della schermata.
// `singular` è il nome mostrato sulla carta ("Mostro Radioattivo"), `label` quello nei filtri.
export const factions = [
  {
    id: "cyborg",
    label: "Cyborg",
    singular: "Cyborg",
    color: "#3db4ff",
    icon: "Bot",
  },
  {
    id: "radioattivi",
    label: "Mostri Radioattivi",
    singular: "Mostro Radioattivo",
    color: "#5dff3a",
    icon: "Radiation",
  },
  {
    id: "sapiens",
    label: "Sapiens",
    singular: "Sapiens",
    color: "#ffa31a",
    icon: "Sun",
  },
  {
    id: "cultisti",
    label: "Cultisti",
    singular: "Cultisti",
    color: "#b04dff",
    icon: "Eye",
  },
  {
    id: "alieni",
    label: "Alieni",
    singular: "Alieni",
    color: "#ff3fa8",
    icon: "Orbit",
  },
];
export const factionById = Object.fromEntries(factions.map((f) => [f.id, f]));

// Gli equipaggiamenti non appartengono a nessuna fazione.
export const NEUTRAL = {
  id: "neutrale",
  label: "Neutrale",
  singular: "Equipaggiamento",
  color: "#cbd5e1",
};
