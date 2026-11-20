package com.benedetto.game_engine.battle;

import com.benedetto.game_engine.battle.dto.ChallengerDeckEntryRequest;
import com.benedetto.game_engine.battle.dto.ChallengerDeckRequest;
import com.benedetto.game_engine.battle.dto.ChallengerInfoRequest;
import com.benedetto.game_engine.battle.exception.InvalidDeckException;
import com.benedetto.game_engine.model.Creature;
import com.benedetto.game_engine.model.Equipment;
import com.benedetto.game_engine.model.PlayerDeck;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternUtils;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ChallengerDeckMapperService {

    private static final int REQUIRED_EQUIPMENT_COUNT = 5;
    private static final int MAX_CARDS_PER_ROUND = 5;
    private static final int MAX_TOTAL_DECK_CARDS = 30;

    private final ResourceLoader resourceLoader;
    private final ObjectMapper objectMapper;

    private final Map<String, Creature> creaturePool = new ConcurrentHashMap<>();
    private final Map<String, Equipment> equipmentPool = new ConcurrentHashMap<>();

    public ChallengerDeckMapperService(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
        this.objectMapper = new ObjectMapper();
    }

    @PostConstruct
    public void init() {
        loadPools();
    }

    public synchronized void loadPools() {
        creaturePool.clear();
        equipmentPool.clear();

        try {
            ResourcePatternResolver resolver = ResourcePatternUtils.getResourcePatternResolver(resourceLoader);
            Resource[] resources = resolver.getResources("classpath*:mocks/pool_*.json");
            if (resources == null || resources.length == 0) {
                resources = resolver.getResources("classpath:mocks/pool_*.json");
            }

            List<Resource> resourceList = new ArrayList<>();
            if (resources != null && resources.length > 0) {
                resourceList.addAll(Arrays.asList(resources));
            } else {
                List<String> fallbackPaths = List.of(
                        "mocks/pool_alieni.json",
                        "mocks/pool_cultisti.json",
                        "mocks/pool_cyborg.json",
                        "mocks/pool_equipments.json",
                        "mocks/pool_mostri_radioattivi.json",
                        "mocks/pool_sapiens.json"
                );
                for (String path : fallbackPaths) {
                    Resource r = resourceLoader.getResource("classpath:" + path);
                    if (r.exists()) {
                        resourceList.add(r);
                    }
                }
            }

            for (Resource resource : resourceList) {
                String filename = resource.getFilename();
                if (filename == null) {
                    continue;
                }
                if (filename.toLowerCase().contains("equipment")) {
                    List<Equipment> equipments = objectMapper.readValue(
                            resource.getInputStream(),
                            new TypeReference<List<Equipment>>() {}
                    );
                    if (equipments != null) {
                        for (Equipment eq : equipments) {
                            if (eq != null && eq.getId() != null) {
                                equipmentPool.put(eq.getId(), eq);
                            }
                        }
                    }
                } else {
                    List<Creature> creatures = objectMapper.readValue(
                            resource.getInputStream(),
                            new TypeReference<List<Creature>>() {}
                    );
                    if (creatures != null) {
                        for (Creature c : creatures) {
                            if (c != null && c.getId() != null) {
                                creaturePool.put(c.getId(), c);
                            }
                        }
                    }
                }
            }
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load mock card pools from classpath:mocks/pool_*.json", e);
        }
    }

    public void validateDeck(ChallengerDeckRequest deckRequest) {
        if (creaturePool.isEmpty() && equipmentPool.isEmpty()) {
            loadPools();
        }

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

            String cardId = entry.getCardId() != null ? entry.getCardId().trim() : null;
            if (cardId == null || cardId.isEmpty()) {
                violations.add("Deck entry at index " + i + " has a missing or empty cardId");
                continue;
            }

            boolean isCreature = creaturePool.containsKey(cardId);
            boolean isEquipment = equipmentPool.containsKey(cardId);

            if (!isCreature && !isEquipment) {
                violations.add("Card with ID '" + cardId + "' does not exist in any loaded card pool");
            }

            String destination = entry.getDestination() != null ? entry.getDestination().trim().toUpperCase() : null;
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

            if (isRoundDest && isEquipment) {
                violations.add("Card with ID '" + cardId
                        + "' is an Equipment and cannot be assigned to creature destination '" + destination + "'");
            }
            if (isEquipDest && isCreature) {
                violations.add("Card with ID '" + cardId
                        + "' is a Creature and cannot be assigned to destination 'EQUIPMENT'");
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
            String dest = entry.getDestination().trim().toUpperCase();
            String cardId = entry.getCardId().trim();

            if (dest.startsWith("ROUND")) {
                int roundNum = Integer.parseInt(dest.substring(5));
                Creature template = creaturePool.get(cardId);
                if (template != null) {
                    creatureRounds.get(roundNum).add(template.cloneForBattle());
                }
            } else if ("EQUIPMENT".equals(dest)) {
                Equipment template = equipmentPool.get(cardId);
                if (template != null) {
                    equipmentDeck.add(new Equipment(
                            template.getId(),
                            template.getName(),
                            template.getAtkBonus(),
                            template.getDefBonus()
                    ));
                }
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

    public Map<String, Creature> getCreaturePool() {
        if (creaturePool.isEmpty()) {
            loadPools();
        }
        return Collections.unmodifiableMap(creaturePool);
    }

    public Map<String, Equipment> getEquipmentPool() {
        if (equipmentPool.isEmpty()) {
            loadPools();
        }
        return Collections.unmodifiableMap(equipmentPool);
    }

    public Optional<Creature> findCreature(String id) {
        if (creaturePool.isEmpty()) {
            loadPools();
        }
        return Optional.ofNullable(creaturePool.get(id));
    }

    public Optional<Equipment> findEquipment(String id) {
        if (equipmentPool.isEmpty()) {
            loadPools();
        }
        return Optional.ofNullable(equipmentPool.get(id));
    }
}
