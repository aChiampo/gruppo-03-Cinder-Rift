package com.benedetto.game_engine.effect;

import com.benedetto.game_engine.model.Creature;
import com.benedetto.game_engine.model.Equipment;
import com.benedetto.game_engine.model.Faction;
import com.benedetto.game_engine.model.Player;
import com.benedetto.game_engine.model.PlayerDeck;
import com.benedetto.game_engine.service.CombatEngine;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class EffectEngineTest {
    private final EffectEngine effects = new EffectEngine();

    private CardEffect attack(int amount) {
        return new CardEffect(501, "ATTACK_BONUS", Map.of("amount", amount));
    }

    private CardEffect defense(int amount) {
        return new CardEffect(502, "STARTING_DEFENSE_MODIFIER", Map.of("amount", amount, "minimum", 1));
    }

    private Player player(String name, List<CardEffect> equipment, List<CardEffect> creature) {
        Creature card = new Creature(701, name, Faction.SAPIENS, 1, 2, 3, 2, 2, null);
        card.setEffects(creature);
        PlayerDeck deck = new PlayerDeck();
        deck.setCreatureRounds(Map.of(1, List.of(card, card), 2, List.of(card)));
        Player player = new Player(name, deck);
        player.prepareForRound(1, new Equipment(900, "Equipment " + name, equipment));
        return player;
    }

    @Test
    void activatesOnEntryAndEquipmentResolvesBeforeCreatureEffects() {
        Player first = player("First", List.of(defense(-3)), List.of(defense(2)));
        Player second = player("Second", List.of(attack(1)), List.of(attack(2)));
        List<String> log = new ArrayList<>();
        assertEquals(2, first.getActiveCreature().getCurrentDef());
        effects.activateEntries(first, second, log);
        // Equipment lowers defense to 1, then the creature adds 2. Reversed order gives 1.
        assertEquals(3, first.getActiveCreature().getCurrentDef());
        assertEquals(3, second.getActiveCreature().getAttackModifier());
        assertTrue(log.get(0).contains("Equipment First ->"));
        assertTrue(log.get(1).contains("Equipment Second ->"));
        assertTrue(log.get(2).contains("First -> First:"));
        assertTrue(log.get(3).contains("Second -> Second:"));
        int count = log.size();
        effects.activateEntries(first, second, log);
        assertEquals(count, log.size());
        assertEquals(3, first.getActiveCreature().getCurrentDef());
    }

    @Test
    void preservesArrayOrderWithinEachSource() {
        Player first = player("First", List.of(defense(-5), defense(2)), List.of());
        Player second = player("Second", List.of(), List.of());
        effects.activateEntries(first, second, new ArrayList<>());
        assertEquals(3, first.getActiveCreature().getCurrentDef());
    }

    @Test
    void expiresOnlyTemporaryModifiersAndLeavesPermanentEffectsActive() {
        CardEffect opening = new CardEffect(503, "FIRST_EXCHANGE_ATTACK_BONUS", Map.of("amount", 2));
        Player first = player("First", List.of(attack(1), opening), List.of(attack(3), opening));
        Player second = player("Second", List.of(), List.of());
        List<String> log = new ArrayList<>();
        effects.activateEntries(first, second, log);
        assertEquals(8, first.getActiveCreature().getAttackModifier());
        effects.afterExchange(first, second, log);
        assertEquals(4, first.getActiveCreature().getAttackModifier());
        effects.afterExchange(first, second, log);
        assertEquals(4, first.getActiveCreature().getAttackModifier());
        assertEquals(2, log.stream().filter(line -> line.contains("expired")).count());
    }

    @Test
    void replacementAndNextRoundStartWithFreshStateWithoutChangingSnapshots() {
        CardEffect opening = new CardEffect(503, "FIRST_EXCHANGE_ATTACK_BONUS", Map.of("amount", 2));
        Player first = player("First", List.of(opening), List.of(attack(3)));
        Player second = player("Second", List.of(), List.of());
        List<String> log = new ArrayList<>();
        effects.activateEntries(first, second, log);
        effects.afterExchange(first, second, log);
        first.getActiveCreature().takeDamage(10);
        first.handleCreatureDeath(log);
        effects.activateEntries(first, second, log);
        assertEquals(5, first.getActiveCreature().getAttackModifier());
        assertEquals(0, first.getActiveCreature().getExchangesCompleted());
        assertEquals(2, first.getActiveCreature().getCurrentDef());
        first.prepareForRound(2, new Equipment(901, "Empty", List.of()));
        effects.activateEntries(first, second, log);
        assertEquals(3, first.getActiveCreature().getAttackModifier());
        assertEquals(3, first.getDeck().getCreatureRounds().get(1).getFirst().getBaseAtk());
        assertEquals(0, first.getDeck().getCreatureRounds().get(1).getFirst().getAttackModifier());
    }

    @Test
    void rejectsUnsupportedEffectsAndMalformedParameters() {
        var invalid = Arrays.asList(
                new CardEffect(1, "UNKNOWN", Map.of()),
                new CardEffect(null, "ATTACK_BONUS", Map.of("amount", 1)),
                new CardEffect(0, "ATTACK_BONUS", Map.of("amount", 1)),
                new CardEffect(1, null, Map.of()),
                new CardEffect(1, "ATTACK_BONUS", null),
                new CardEffect(1, "ATTACK_BONUS", Map.of()),
                new CardEffect(1, "ATTACK_BONUS", Map.of("amount", 1, "extra", 2)),
                new CardEffect(1, "ATTACK_BONUS", Map.of("amount", 1.5)),
                new CardEffect(1, "ATTACK_BONUS", Map.of("amount", "2")),
                new CardEffect(1, "STARTING_DEFENSE_MODIFIER", Map.of("amount", -1, "minimum", 0)));
        for (CardEffect effect : invalid) {
            assertThrows(IllegalArgumentException.class, () -> effects.validate(List.of(effect)));
        }
        assertThrows(IllegalArgumentException.class, () -> effects.validate(null));
        assertThrows(IllegalArgumentException.class, () -> effects.validate(Arrays.asList((CardEffect) null)));
    }

    @Test
    void simultaneousLethalAttacksStillResolveForBothCreatures() {
        Player first = player("First", List.of(attack(10)), List.of());
        Player second = player("Second", List.of(), List.of(attack(10)));
        var result = new CombatEngine(effects).runRound(first, second);
        assertNull(result.getWinner());
        assertFalse(first.hasActiveCreature());
        assertFalse(second.hasActiveCreature());
        assertEquals(30, first.getHp());
        assertEquals(30, second.getHp());
    }

    @Test
    void parameterSnapshotsAreImmutableAndDoNotShareRuntimeState() {
        Map<String, Object> parameters = new java.util.HashMap<>();
        parameters.put("amount", 2);
        CardEffect effect = new CardEffect(9001, "ATTACK_BONUS", parameters);
        parameters.put("amount", 99);
        assertEquals(2, effect.parameters().get("amount"));
        assertThrows(UnsupportedOperationException.class, () -> effect.parameters().put("amount", 4));
        assertDoesNotThrow(() -> effects.validate(List.of(effect)));
    }

    @Test
    void doesNotDeployAReplacementOrActivateItsEffectsAfterTheRoundEnds() {
        Player first = player("First", List.of(attack(10)), List.of());
        Player second = player("Second", List.of(attack(10)), List.of());
        second.getCurrentRoundCreatures().clear();
        var result = new CombatEngine(effects).runRound(first, second);
        assertSame(first, result.getWinner());
        assertEquals(1, first.getCurrentRoundCreatures().size());
        assertEquals(2, result.getLog().stream().filter(line -> line.startsWith("[EFFECT]")).count());
    }
}
