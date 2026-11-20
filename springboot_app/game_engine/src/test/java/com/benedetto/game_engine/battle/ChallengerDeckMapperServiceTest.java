package com.benedetto.game_engine.battle;

import com.benedetto.game_engine.SeedDeckFixtures;
import com.benedetto.game_engine.battle.dto.ChallengerDeckRequest;
import com.benedetto.game_engine.battle.exception.InvalidDeckException;
import com.benedetto.game_engine.model.Creature;
import com.benedetto.game_engine.model.PlayerDeck;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ChallengerDeckMapperServiceTest {
    private final ChallengerDeckMapperService mapper = new ChallengerDeckMapperService(new com.benedetto.game_engine.effect.EffectEngine());

    @Test
    void mapsNumericIdsAndSuppliedStatisticsWithoutConsultingACatalog() throws Exception {
        var entries = SeedDeckFixtures.entries();
        entries.getFirst().setCardId(90001);
        entries.getFirst().setName("App snapshot");
        entries.getFirst().setBaseAtk(42);
        entries.getFirst().setEnergyCost(5);
        entries.getFirst().setPowerPoints(99);
        var request = new ChallengerDeckRequest(entries);
        var json = new ObjectMapper().valueToTree(request);
        assertTrue(json.at("/entries/0/cardId").isIntegralNumber());

        PlayerDeck mapped = mapper.mapToPlayerDeck(new ObjectMapper().treeToValue(json, ChallengerDeckRequest.class));
        Creature creature = mapped.getCreatureRounds().get(1).getFirst();
        assertEquals(90001, creature.getId());
        assertEquals("App snapshot", creature.getName());
        assertEquals(42, creature.getBaseAtk());
        assertEquals(99, creature.getPowerPoints());
        assertEquals(5, creature.getEnergyCost());
        assertEquals(2, mapped.getEquipmentDeck().get(2).getEffects().getFirst().parameters().get("amount"));
    }

    @Test
    void rejectsMissingSnapshotFieldsInsteadOfDroppingCards() {
        var entries = SeedDeckFixtures.entries();
        entries.getFirst().setBaseAtk(null);
        assertThrows(InvalidDeckException.class, () -> mapper.mapToPlayerDeck(new ChallengerDeckRequest(entries)));
    }

    @Test
    void rejectsMissingAndNonPositiveCardIds() {
        for (Integer id : new Integer[] {null, 0, -1}) {
            var entries = SeedDeckFixtures.entries();
            entries.getFirst().setCardId(id);
            assertThrows(InvalidDeckException.class, () -> mapper.mapToPlayerDeck(new ChallengerDeckRequest(entries)));
        }
    }

    @Test
    void mapsIndependentCopiesForRepeatedCardIds() {
        var entries = SeedDeckFixtures.entries();
        entries.set(1, entries.getFirst());
        PlayerDeck deck = mapper.mapToPlayerDeck(new ChallengerDeckRequest(entries));
        var creatures = deck.getCreatureRounds().get(1);
        assertNotSame(creatures.get(0), creatures.get(1));
        creatures.get(0).takeDamage(3);
        assertEquals(creatures.get(1).getInitialDef(), creatures.get(1).getCurrentDef());
    }
}
