const rarityOrder = ['Comune', 'Rara', 'Epica', 'Leggendaria']

const rarityTotals = {
  Comune: 0,
  Rara: 0,
  Epica: 0,
  Leggendaria: 0,
}

const commonValidation = {
  creature: { min: 13, max: 999 },
  equipment: { min: 3, max: 999 }
}

const clampCounts = (deck) => {
  const selectedCreatures = Object.values(deck.roundCreatures ?? {}).flat()
  const selectedEquipment = deck.selectedEquipment ?? []

  return {
    selectedCreatures,
    selectedEquipment,
    totalPower: [...selectedCreatures, ...selectedEquipment].reduce((sum, card) => sum + (Number(card.power) || 0), 0),
    creatureCount: selectedCreatures.length,
    equipmentCount: selectedEquipment.length,
  }
}

export function validateDeck(deck = {}) {
  const { selectedCreatures, selectedEquipment, totalPower, creatureCount, equipmentCount } = clampCounts(deck)

  const errors = []
  const warnings = []
  const rarityCounts = { ...rarityTotals }
  const equipmentRarityCounts = { ...rarityTotals }

  selectedCreatures.forEach((card) => {
    if (card.rarity && rarityCounts[card.rarity] !== undefined) rarityCounts[card.rarity] += 1
  })

  selectedEquipment.forEach((card) => {
    if (card.rarity && equipmentRarityCounts[card.rarity] !== undefined) equipmentRarityCounts[card.rarity] += 1
  })

  const roundValidation = Array.from({ length: 5 }, (_, roundIndex) => {
    const roundNumber = roundIndex + 1
    const roundCreatures = deck.roundCreatures?.[roundNumber] ?? []
    const roundPower = roundCreatures.reduce((sum, card) => sum + (Number(card.power) || 0), 0)
    const roundEnergy = roundCreatures.reduce((sum, card) => sum + (Number(card.energy) || 0), 0)
    const roundErrors = []
    const roundWarnings = []

    if (roundCreatures.length < 5) {
      roundWarnings.push(`Round ${roundNumber}: devono essere inserite 5 creature.`)
    }

    if (roundPower > 14) {
      roundErrors.push(`Round ${roundNumber}: limite potenza massimo superato.`)
    }

    if (roundEnergy > roundNumber) {
      roundErrors.push(`Round ${roundNumber}: energia massima superata.`)
    }

    if (roundCreatures.some((card) => Number(card.energy) > roundNumber)) {
      roundErrors.push(`Non è possibile inserire una creatura con energia superiore al Round selezionato.`)
    }

    return {
      round: roundNumber,
      creatures: roundCreatures.length,
      power: roundPower,
      energy: roundEnergy,
      maxEnergy: roundNumber,
      maxPower: 14,
      valid: roundErrors.length === 0 && roundWarnings.length === 0,
      errors: roundErrors,
      warnings: roundWarnings,
    }
  })

  if (creatureCount < 25) {
    warnings.push(`Creatures totali: mancano ${25 - creatureCount} creature per completare il mazzo.`)
  }

  if (creatureCount > 25) {
    errors.push('Creatures totali: superato il limite massimo di 25 creature.')
  }

  if (equipmentCount < 5) {
    warnings.push(`Equipaggiamenti: mancano ${5 - equipmentCount} equipaggiamenti.`)
  }

  if (equipmentCount > 5) {
    errors.push('Equipaggiamenti: superato il limite massimo di 5 equipaggiamenti.')
  }

  if (rarityCounts.Comune < 13) {
    warnings.push(`Rarità comuni: mancano ${13 - rarityCounts.Comune} creature.`)
  }
  if (rarityCounts.Rara > 8) {
    errors.push(`Rarità rare: limite massimo di 8 superato.`)
  }
  if (rarityCounts.Epica > 3) {
    errors.push(`Rarità epiche: limite massimo di 3 superato.`)
  }
  if (rarityCounts.Leggendaria > 1) {
    errors.push(`Rarità leggendarie: limite massimo di 1 superato.`)
  }

  if (equipmentRarityCounts.Comune < 3) {
    warnings.push(`Equipaggiamenti comuni: mancano ${3 - equipmentRarityCounts.Comune} equipaggiamenti.`)
  }
  if (equipmentRarityCounts.Rara > 2) {
    errors.push('Equipaggiamento raro: limite massimo di 2 superato.')
  }
  if (equipmentRarityCounts.Epica > 1) {
    errors.push('Equipaggiamento epico: limite massimo di 1 superato.')
  }

  const duplicates = selectedEquipment.filter((card, index, cards) => cards.findIndex((item) => item.id === card.id) !== index)
  if (duplicates.length > 0) {
    errors.push('Equipaggiamento già selezionato.')
  }

  const isValid = errors.length === 0 && roundValidation.every((item) => item.errors.length === 0) && creatureCount === 25 && equipmentCount === 5

  return {
    isValid,
    errors,
    warnings,
    totalPower,
    creatureCount,
    equipmentCount,
    rarityCounts,
    equipmentRarityCounts,
    roundValidation,
  }
}

export const validateDeckWithBackend = async (deck) => validateDeck(deck)
