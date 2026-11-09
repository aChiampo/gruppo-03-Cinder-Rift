# Cinder-Rift — Dashboard Home

## Avvio
1. Installa Node.js 18+ (https://nodejs.org)
2. `npm install`
3. `npm run dev` → http://localhost:5173
Build: `npm run build` (output in `dist/`).

## Struttura
```
src/
├── main.jsx            punto di ingresso
├── App.jsx             navigazione tra le schermate
├── screens/            schermate complete (HomeScreen, PlaceholderScreen)
├── components/
│   ├── layout/         Header
│   ├── ui/             elementi riusabili (Panel, Toast)
│   └── home/           blocchi della Home (Campaign, News, Decks, ...)
├── models/             regole di gioco (costanti, validazione mazzo)
├── data/               dati di esempio, da sostituire con le API
├── hooks/              hook React (useToast)
└── styles/             CSS diviso per area (base, header, hero, ...)
public/img/             logo e immagini
```
