package com.benedetto.game_engine.battle;

import com.benedetto.game_engine.battle.dto.ChallengerDeckEntryRequest;
import com.benedetto.game_engine.battle.dto.ChallengerDeckRequest;
import com.benedetto.game_engine.battle.dto.ChallengerInfoRequest;
import com.benedetto.game_engine.battle.exception.InvalidDeckException;
import com.benedetto.game_engine.model.Creature;
import com.benedetto.game_engine.model.Equipment;
import com.benedetto.game_engine.model.PlayerDeck;
import org.springframework.stereotype.Service;
import com.benedetto.game_engine.effect.EffectEngine;

import java.util.*;

@Service
public class ChallengerDeckMapperService {

    private static final int REQUIRED_EQUIPMENT_COUNT = 5;
    private static final int MAX_CARDS_PER_ROUND = 5;
    private static final int MAX_TOTAL_DECK_CARDS = 30;

    private final EffectEngine effectEngine;

    public ChallengerDeckMapperService(EffectEngine effectEngine) {
        this.effectEngine = effectEngine;
    }

    public void validateDeck(ChallengerDeckRequest deckRequest) {
        if (deckRequest == null || deckRequest.getEntries() == null || deckRequest.getEntries().isEmpty()) {
            throw new InvalidDeckException("Deck request entries must not be null or empty",
                    List.of("Deck entries must not be null or empty"));
        }

        List<String> violations = new ArrayList<>();
        Map<String, Integer> destinationCounts = new LinkedHashMap<>();

        if (deckRequest.getEntries().size() > MAX_TOTAL_DECK_CARDS) {
            violations.add("Deck exceeds total allowed limit of " + MAX_TOTAL_DECK_CARDS + " cards (found "
                    + deckRequest.getEntries().size() + ")");
        }

        for (int i = 0; i < deckRequest.getEntries().size(); i++) {
            ChallengerDeckEntryRequest entry = deckRequest.getEntries().get(i);
            if (entry == null) {
                violations.add("Deck entry at index " + i + " is null");
                continue;
            }

            Integer cardId = entry.getCardId();
            if (cardId == null || cardId <= 0) {
                violations.add("Deck entry at index " + i + " must have a positive numeric cardId");
                continue;
            }

            String destination = entry.getDestination() != null ? entry.getDestination().trim().toUpperCase(Locale.ROOT) : null;
            if (destination == null || destination.isEmpty()) {
                violations.add("Card with ID '" + cardId + "' has a missing or empty destination");
                continue;
            }

            boolean isRoundDest = destination.matches("^ROUND[1-5]$");
            boolean isEquipDest = "EQUIPMENT".equals(destination);

            if (!isRoundDest && !isEquipDest) {
                violations.add("Destination '" + destination + "' for card '" + cardId
                        + "' is invalid. Expected ROUND1..ROUND5 or EQUIPMENT");
                continue;
            }

            // The app backend validates catalog data and deck legality. Only require
            // the snapshot fields needed to construct battle objects here.
            if (entry.getName() == null || entry.getName().isBlank()) {
                violations.add("Card " + cardId + " must include its name");
            }
            if (isRoundDest && (entry.getFaction() == null || entry.getBaseAtk() == null
                    || entry.getInitialDef() == null || entry.getEnergyCost() == null
                    || entry.getPowerPoints() == null)) {
                violations.add("Creature " + cardId
                        + " must include faction, baseAtk, initialDef, energyCost and powerPoints");
            }
            try {
                effectEngine.validate(entry.getEffects());
            } catch (IllegalArgumentException ex) {
                violations.add("Card " + cardId + ": " + ex.getMessage());
            }

            destinationCounts.merge(destination, 1, Integer::sum);
        }

        for (int round = 1; round <= 5; round++) {
            String roundDest = "ROUND" + round;
            int count = destinationCounts.getOrDefault(roundDest, 0);
            if (count == 0) {
                violations.add("Destination '" + roundDest
                        + "' is not covered. Each round from ROUND1 to ROUND5 must contain at least 1 creature card");
            } else if (count > MAX_CARDS_PER_ROUND) {
                violations.add("Destination '" + roundDest + "' exceeds maximum allowed limit of "
                        + MAX_CARDS_PER_ROUND + " cards (found " + count + ")");
            }
        }

        int equipmentCount = destinationCounts.getOrDefault("EQUIPMENT", 0);
        if (equipmentCount == 0) {
            violations.add("Destination 'EQUIPMENT' is missing. Exactly " + REQUIRED_EQUIPMENT_COUNT
                    + " equipment cards are required");
        } else if (equipmentCount != REQUIRED_EQUIPMENT_COUNT) {
            violations.add("Destination 'EQUIPMENT' must contain exactly " + REQUIRED_EQUIPMENT_COUNT
                    + " equipment cards (found " + equipmentCount + ")");
        }

        if (!violations.isEmpty()) {
            throw new InvalidDeckException(violations);
        }
    }

    public PlayerDeck mapToPlayerDeck(ChallengerInfoRequest challenger, ChallengerDeckRequest deckRequest) {
        validateDeck(deckRequest);

        String playerId = "challenger";
        if (challenger != null) {
            if (challenger.getUserId() != null && !challenger.getUserId().isBlank()) {
                playerId = challenger.getUserId().trim();
            } else if (challenger.getUsername() != null && !challenger.getUsername().isBlank()) {
                playerId = challenger.getUsername().trim();
            } else if (challenger.getDisplayName() != null && !challenger.getDisplayName().isBlank()) {
                playerId = challenger.getDisplayName().trim();
            }
        }

        PlayerDeck playerDeck = new PlayerDeck();
        playerDeck.setPlayerId(playerId);
        playerDeck.setDeterministic(false);

        Map<Integer, List<Creature>> creatureRounds = new LinkedHashMap<>();
        for (int r = 1; r <= 5; r++) {
            creatureRounds.put(r, new ArrayList<>());
        }
        List<Equipment> equipmentDeck = new ArrayList<>();

        for (ChallengerDeckEntryRequest entry : deckRequest.getEntries()) {
            String dest = entry.getDestination().trim().toUpperCase(Locale.ROOT);
            if (dest.startsWith("ROUND")) {
                int roundNum = Integer.parseInt(dest.substring(5));
                Creature creature = new Creature(
                        entry.getCardId(), entry.getName(), entry.getFaction(), entry.getEnergyCost(),
                        entry.getPowerPoints(), entry.getBaseAtk(), entry.getInitialDef(), entry.getInitialDef(), null);
                creature.setEffects(List.copyOf(entry.getEffects()));
                creatureRounds.get(roundNum).add(creature);
            } else {
                equipmentDeck.add(new Equipment(entry.getCardId(), entry.getName(), List.copyOf(entry.getEffects())));
            }
        }

        playerDeck.setCreatureRounds(creatureRounds);
        playerDeck.setEquipmentDeck(equipmentDeck);
        return playerDeck;
    }

    public PlayerDeck mapToPlayerDeck(ChallengerDeckRequest deckRequest) {
        return mapToPlayerDeck(null, deckRequest);
    }

    public PlayerDeck mapToPlayerDeck(ChallengerDeckRequest deckRequest, String playerId) {
        return mapToPlayerDeck(ChallengerInfoRequest.builder().userId(playerId).build(), deckRequest);
    }

}
