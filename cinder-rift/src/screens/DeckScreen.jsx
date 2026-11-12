import { useEffect, useMemo, useState } from 'react'
import CardFilters from '../components/deck-builder/CardFilters.jsx'
import CardCollection from '../components/deck-builder/CardCollection.jsx'
import RoundSelector from '../components/deck-builder/RoundSelector.jsx'
import RoundPanel from '../components/deck-builder/RoundPanel.jsx'
import SelectedEquipment from '../components/deck-builder/SelectedEquipment.jsx'
import AvailableEquipment from '../components/deck-builder/AvailableEquipment.jsx'
import DeckSummary from '../components/deck-builder/DeckSummary.jsx'
import DeckValidation from '../components/deck-builder/DeckValidation.jsx'
import { getCreatures, getEquipments } from '../services/cardService.js'
import { validateDeck, validateDeckWithBackend } from '../services/deckService.js'

const EMPTY_ROUNDS = { 1: [], 2: [], 3: [], 4: [], 5: [] }
const factionOptions = ['Tutte', 'Cyborg', 'Mutante', 'Alieno', 'Cultista', 'Sapiens']
const rarityOptions = ['Tutte', 'Comune', 'Rara', 'Epica', 'Leggendaria']
const energyOptions = ['Tutte', '1', '2', '3', '4', '5']

export default function DeckScreen() {
  const [selectedType, setSelectedType] = useState('creature')
  const [search, setSearch] = useState('')
  const [factionFilter, setFactionFilter] = useState('Tutte')
  const [rarityFilter, setRarityFilter] = useState('Tutte')
  const [energyFilter, setEnergyFilter] = useState('Tutte')
  const [activeRound, setActiveRound] = useState(1)
  const [pendingSlot, setPendingSlot] = useState(null)
  const [creatures, setCreatures] = useState([])
  const [equipments, setEquipments] = useState([])
  const [roundCreatures, setRoundCreatures] = useState(EMPTY_ROUNDS)
  const [selectedEquipment, setSelectedEquipment] = useState([])
  const [verified, setVerified] = useState(false)

  useEffect(() => {
    const previousTitle = document.title
    document.title = 'Deck — Cinder-Rift'

    let active = true

    getCreatures().then((cards) => {
      if (active) setCreatures(cards)
    })

    getEquipments().then((cards) => {
      if (active) setEquipments(cards)
    })

    return () => {
      active = false
      document.title = previousTitle
    }
  }, [])

  const filteredCards = useMemo(() => {
    const source = selectedType === 'creature' ? creatures : equipments

    return source.filter((card) => {
      const matchesName = card.name.toLowerCase().includes(search.toLowerCase())
      const matchesFaction = factionFilter === 'Tutte' || card.faction === factionFilter
      const matchesRarity = rarityFilter === 'Tutte' || card.rarity === rarityFilter
      const matchesEnergy = energyFilter === 'Tutte' || String(card.energy) === energyFilter
      return matchesName && matchesFaction && matchesRarity && matchesEnergy
    })
  }, [selectedType, creatures, equipments, search, factionFilter, rarityFilter, energyFilter])

  const validation = useMemo(
    () => validateDeck({ roundCreatures, selectedEquipment }),
    [roundCreatures, selectedEquipment],
  )

  const currentRoundCards = roundCreatures[activeRound] ?? []

  const invalidCreatureIds = useMemo(
    () =>
      currentRoundCards
        .filter((card) => Number(card.energy) > activeRound || Number(card.power) > 14)
        .map((card) => card.id),
    [currentRoundCards, activeRound],
  )

  const addCreatureToRound = (card, roundNumber = activeRound, targetIndex = null) => {
    setRoundCreatures((current) => {
      const roundCards = [...(current[roundNumber] ?? [])]

      if (targetIndex !== null) {
        roundCards[targetIndex] = card
      } else {
        if (roundCards.length >= 5) return current
        roundCards.push(card)
      }

      return {
        ...current,
        [roundNumber]: roundCards,
      }
    })
  }

  const removeCreatureFromRound = (roundNumber, index) => {
    setRoundCreatures((current) => ({
      ...current,
      [roundNumber]: (current[roundNumber] ?? []).filter((_, slotIndex) => slotIndex !== index),
    }))
  }

  const addEquipmentCard = (card) => {
    setSelectedEquipment((current) => {
      if (current.length >= 5) return current
      if (current.some((item) => item.id === card.id)) return current
      return [...current, card]
    })
  }

  const removeEquipmentAt = (index) => {
    setSelectedEquipment((current) => current.filter((_, slotIndex) => slotIndex !== index))
  }

  const handleSelectCard = (card) => {
    if (card.type === 'creature') {
      if (pendingSlot) {
        addCreatureToRound(card, pendingSlot.round, pendingSlot.slotIndex)
        setPendingSlot(null)
        return
      }

      addCreatureToRound(card, activeRound)
      return
    }

    addEquipmentCard(card)
  }

  const handleVerify = async () => {
    setVerified(true)
    await validateDeckWithBackend({ roundCreatures, selectedEquipment })
  }

  const resetDeck = () => {
    setRoundCreatures(EMPTY_ROUNDS)
    setSelectedEquipment([])
    setPendingSlot(null)
    setVerified(false)
  }

  return (
    <div className="deck-builder-page">
      <main className="deck-builder-layout">
        <section className="deck-builder-panel deck-builder-panel--left">
          <div className="panel-header">
            <h2>COLLEZIONE CARTE</h2>
          </div>

          <div className="collection-tabs">
            <button
              type="button"
              className={selectedType === 'creature' ? 'active' : ''}
              onClick={() => setSelectedType('creature')}
            >
              Creature
            </button>
            <button
              type="button"
              className={selectedType === 'equipment' ? 'active' : ''}
              onClick={() => setSelectedType('equipment')}
            >
              Equipaggiamenti
            </button>
          </div>

          <CardFilters
            search={search}
            onSearchChange={setSearch}
            faction={factionFilter}
            onFactionChange={setFactionFilter}
            rarity={rarityFilter}
            onRarityChange={setRarityFilter}
            energy={energyFilter}
            onEnergyChange={setEnergyFilter}
            factionOptions={factionOptions}
            rarityOptions={rarityOptions}
            energyOptions={energyOptions}
          />

          <CardCollection
            cards={filteredCards}
            selectedType={selectedType}
            onSelectCard={handleSelectCard}
            selectedIds={currentRoundCards.map((card) => card.id)}
          />

          <div className="collection-total">{filteredCards.length} carte disponibili</div>
        </section>

        <section className="deck-builder-panel deck-builder-panel--center">
          <div className="panel-header">
            <h2>DECK BUILDER</h2>
          </div>

          <p className="deck-builder-panel__subtitle">
            Componi il tuo mazzo per i cinque round di battaglia.
          </p>

          <RoundSelector activeRound={activeRound} onChange={setActiveRound} />

          <RoundPanel
            cards={roundCreatures[activeRound] ?? []}
            roundNumber={activeRound}
            active
            invalidCardIds={invalidCreatureIds}
            onAddSlot={(slotIndex) => {
              setPendingSlot({ round: activeRound, slotIndex })
              setSelectedType('creature')
            }}
            onRemoveCard={(slotIndex) => removeCreatureFromRound(activeRound, slotIndex)}
          />

          <SelectedEquipment
            equipment={selectedEquipment}
            onRemove={removeEquipmentAt}
            onAddSlot={() => setSelectedType('equipment')}
          />

          <AvailableEquipment
            equipment={equipments}
            onSelect={(card) => {
              setSelectedType('equipment')
              addEquipmentCard(card)
            }}
          />
        </section>

        <section className="deck-builder-panel deck-builder-panel--right">
          <DeckSummary validation={validation} onVerify={handleVerify} />
          <DeckValidation validation={validation} />
          {verified && <div className="verification-badge">Verifica completata</div>}
        </section>
      </main>
    </div>
  )
}
