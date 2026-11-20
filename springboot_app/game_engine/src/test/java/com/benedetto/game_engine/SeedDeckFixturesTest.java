package com.benedetto.game_engine;

import com.benedetto.game_engine.model.Creature;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;

class SeedDeckFixturesTest {
    private static final Set<Integer> RARE = Set.of(14, 19, 22, 24, 26, 27);
    private static final Set<Integer> EPIC = Set.of(16, 18);

    @Test
    void mirrorUsesExactlyTheThirtyCardsOwnedByTheSeededPlayer() {
        var deck = SeedDeckFixtures.load("mirror");
        Set<Integer> ids = deck.getCreatureRounds().values().stream().flatMap(List::stream)
                .map(Creature::getId).collect(Collectors.toSet());
        deck.getEquipmentDeck().forEach(card -> assertTrue(ids.add(card.getId())));
        assertEquals(IntStream.rangeClosed(1, 30).boxed().collect(Collectors.toSet()), ids);
    }

    @Test
    void decksRespectEnergyPowerRarityAndCopyLimits() {
        for (String name : List.of("mirror", "demo")) {
            var deck = SeedDeckFixtures.load(name);
            assertEquals(name, deck.getPlayerId());
            assertEquals(Set.of(1, 2, 3, 4, 5), deck.getCreatureRounds().keySet());
            Map<Integer, Integer> copies = new HashMap<>();
            int totalPower = 0, common = 0, rare = 0, epic = 0, legendary = 0;
            for (var round : deck.getCreatureRounds().entrySet()) {
                assertEquals(5, round.getValue().size());
                int power = 0;
                for (Creature card : round.getValue()) {
                    assertTrue(card.getEnergyCost() <= round.getKey());
                    power += card.getPowerPoints();
                    copies.merge(card.getId(), 1, Integer::sum);
                    if (RARE.contains(card.getId())) rare++;
                    else if (EPIC.contains(card.getId())) epic++;
                    else if (card.getId() == 23) legendary++;
                    else common++;
                }
                assertTrue(power <= 14);
                totalPower += power;
            }
            assertTrue(totalPower <= 65);
            assertTrue(common >= 13 && rare <= 8 && epic <= 3 && legendary <= 1);
            assertTrue(copies.values().stream().allMatch(count -> count <= 2));
            assertEquals(5, deck.getEquipmentDeck().size());
            var equipmentIds = deck.getEquipmentDeck().stream().map(card -> card.getId()).toList();
            assertEquals(Set.of(6, 7, 28, 29, 30), new HashSet<>(equipmentIds));
            // V5 assigns power 1, 1, 2, 3, 3: three commons and two rares.
            assertEquals(10, equipmentIds.stream().mapToInt(id -> Map.of(6, 1, 7, 1, 28, 2, 29, 3, 30, 3).get(id)).sum());
        }
    }
}
