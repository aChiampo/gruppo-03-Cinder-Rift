package com.benedetto.game_engine;

import com.benedetto.game_engine.model.*;
import com.benedetto.game_engine.effect.EffectEngine;
import static com.benedetto.game_engine.SeedDeckFixtures.equipment;
import com.benedetto.game_engine.service.CombatEngine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class CombatEngineTest {

    private CombatEngine combatEngine;

    @BeforeEach
    void setUp() {
        combatEngine = new CombatEngine(new EffectEngine());
    }

    @Test
    void testVantaggioFazioneCalcolatoCorrettamente() {
        Creature attacker = new Creature(1, "Cyborg", Faction.CYBORG, 1, 2, 4, 10, 10, null);
        Creature defender = new Creature(2, "Mostro", Faction.MOSTRI_RADIOATTIVI, 1, 2, 3, 10, 10, null);

        activate(attacker, defender);
        int effAtk = combatEngine.calculateEffectiveAtk(attacker, defender);
        assertEquals(6, effAtk);
    }

    @Test
    void testTrasferimentoEquipaggiamentoEPassaggioAllaSuccessiva() {
        Equipment spadone = equipment(3, "Spadone", 2, 0);
        Creature c1 = new Creature(1, "Creatura 1", Faction.SAPIENS, 1, 2, 2, 2, 2, spadone);
        Creature c2 = new Creature(4, "Creatura 2", Faction.SAPIENS, 1, 2, 4, 5, 5, null);

        PlayerDeck deck = new PlayerDeck();
        Map<Integer, List<Creature>> map = new HashMap<>();
        map.put(1, List.of(c1, c2));
        deck.setCreatureRounds(map);

        Player player = new Player("Test", deck);
        player.prepareForRound(1, spadone);

        List<String> log = new ArrayList<>();
        player.handleCreatureDeath(log);

        assertNotNull(player.getActiveCreature());
        assertEquals("Creatura 2", player.getActiveCreature().getName());
        assertEquals(spadone, player.getActiveCreature().getEquipment());
    }

    @Test
    void testProtezioneLoopInfinito() {
        Creature c1 = new Creature(5, "Zero 1", Faction.CYBORG, 1, 1, 0, 10, 10, null);
        Creature c2 = new Creature(6, "Zero 2", Faction.CYBORG, 1, 1, 0, 10, 10, null);

        PlayerDeck d1 = new PlayerDeck();
        d1.setCreatureRounds(Map.of(1, List.of(c1)));
        PlayerDeck d2 = new PlayerDeck();
        d2.setCreatureRounds(Map.of(1, List.of(c2)));

        Player p1 = new Player("P1", d1);
        Player p2 = new Player("P2", d2);
        p1.prepareForRound(1, equipment(7, "Nessuno", 0, 0));
        p2.prepareForRound(1, equipment(7, "Nessuno", 0, 0));

        RoundResult result = combatEngine.runRound(p1, p2);

        assertTrue(result.getLog().stream().anyMatch(line -> line.contains("loop infinito interrotto")));
    }

    @Test
    void testNessunBonusFazioneTraStessaFazione() {
        Creature c1 = new Creature(1, "Cyborg 1", Faction.CYBORG, 1, 2, 5, 10, 10, null);
        Creature c2 = new Creature(4, "Cyborg 2", Faction.CYBORG, 1, 2, 3, 10, 10, null);

        int effAtk = combatEngine.calculateEffectiveAtk(c1, c2);
        assertEquals(5, effAtk);
    }

    @Test
    void testMorteSimultaneaESostituzione() {
        Creature c1 = new Creature(1, "Attaccante A", Faction.SAPIENS, 1, 2, 5, 3, 3, null);
        Creature c2 = new Creature(4, "Attaccante B", Faction.SAPIENS, 1, 2, 5, 3, 3, null);

        PlayerDeck d1 = new PlayerDeck();
        d1.setCreatureRounds(Map.of(1, List.of(c1)));
        PlayerDeck d2 = new PlayerDeck();
        d2.setCreatureRounds(Map.of(1, List.of(c2)));

        Player p1 = new Player("P1", d1);
        Player p2 = new Player("P2", d2);
        p1.prepareForRound(1, equipment(7, "Nessuno", 0, 0));
        p2.prepareForRound(1, equipment(7, "Nessuno", 0, 0));

        RoundResult result = combatEngine.runRound(p1, p2);

        assertNotNull(result);
        assertNotNull(result.getLog());
        assertFalse(result.getLog().isEmpty());
    }

    @Test
    void testStackingBonusFazioneEEquipaggiamento() {
        Equipment spadone = equipment(3, "Spadone", 3, 0);
        Creature attacker = new Creature(1, "Cyborg", Faction.CYBORG, 1, 2, 4, 10, 10, spadone);
        Creature defender = new Creature(2, "Mostro", Faction.MOSTRI_RADIOATTIVI, 1, 2, 3, 10, 10, null);

        activate(attacker, defender);
        int effAtk = combatEngine.calculateEffectiveAtk(attacker, defender);
        assertEquals(9, effAtk);
    }

    @Test
    void testPassaggioEquipaggiamentoInCatenaSuPiuCreature() {
        Equipment elmo = equipment(3, "Elmo", 1, 2);
        Creature c1 = new Creature(1, "Creatura 1", Faction.SAPIENS, 1, 1, 1, 1, 1, elmo);
        Creature c2 = new Creature(4, "Creatura 2", Faction.SAPIENS, 1, 1, 2, 5, 5, null);
        Creature c3 = new Creature(8, "Creatura 3", Faction.SAPIENS, 1, 1, 3, 5, 5, null);

        PlayerDeck deck = new PlayerDeck();
        deck.setCreatureRounds(Map.of(1, List.of(c1, c2, c3)));

        Player player = new Player("Test", deck);
        player.prepareForRound(1, elmo);
        List<String> log = new ArrayList<>();

        player.handleCreatureDeath(log);
        assertEquals("Creatura 2", player.getActiveCreature().getName());
        assertEquals(elmo, player.getActiveCreature().getEquipment());

        player.handleCreatureDeath(log);
        assertEquals("Creatura 3", player.getActiveCreature().getName());
        assertEquals(elmo, player.getActiveCreature().getEquipment());
    }

    @Test
    void testMorteUltimaCreaturaNonLanciaNullPointerExceptionConEquipaggiamento() {
        Equipment spadone = equipment(3, "Spadone", 2, 0);
        Creature ultima = new Creature(1, "Ultima Creatura", Faction.SAPIENS, 1, 1, 1, 1, 1, spadone);

        PlayerDeck deck = new PlayerDeck();
        deck.setCreatureRounds(Map.of(1, List.of(ultima)));

        Player player = new Player("Test", deck);
        player.prepareForRound(1, spadone);
        List<String> log = new ArrayList<>();

        assertDoesNotThrow(() -> player.handleCreatureDeath(log));
        assertNull(player.getActiveCreature());
    }

    @Test
    void testScontroConScudoEquipaggiato() {
        Equipment scudo = equipment(9, "Scudo Spaziale", 0, 3);

        Creature c1 = new Creature(10, "Attaccante", Faction.SAPIENS, 1, 2, 5, 2, 2, null);
        Creature c2 = new Creature(11, "Difensore", Faction.SAPIENS, 1, 2, 2, 4, 4, scudo);

        PlayerDeck d1 = new PlayerDeck();
        d1.setCreatureRounds(Map.of(1, List.of(c1)));
        PlayerDeck d2 = new PlayerDeck();
        d2.setCreatureRounds(Map.of(1, List.of(c2)));

        Player p1 = new Player("P1", d1);
        Player p2 = new Player("P2", d2);
        p1.prepareForRound(1, equipment(7, "Nessuno", 0, 0));
        p2.prepareForRound(1, scudo);

        RoundResult result = combatEngine.runRound(p1, p2);

        assertNotNull(result);
        assertFalse(result.getLog().isEmpty());
    }

    @Test
    void testAttaccoEffettivoInvariatoConCaloDifesaResidua() {
        Creature cyborg = new Creature(1, "Cyborg", Faction.CYBORG, 1, 2, 3, 10, 10, null);
        Creature mostro = new Creature(2, "Mostro", Faction.MOSTRI_RADIOATTIVI, 1, 2, 2, 8, 8, null);

        int atkIniziale = combatEngine.calculateEffectiveAtk(cyborg, mostro);

        Creature cyborgDanneggiato = new Creature(1, "Cyborg", Faction.CYBORG, 1, 2, 3, 2, 10, null);
        int atkDopoDanno = combatEngine.calculateEffectiveAtk(cyborgDanneggiato, mostro);

        assertEquals(5, atkDopoDanno);
    }

    @Test
    void startingDefenseBonusTransfersWithoutCarryingDamage() {
        PlayerDeck deck = new PlayerDeck();
        deck.setCreatureRounds(Map.of(1, List.of(
                new Creature(1, "First", Faction.SAPIENS, 1, 2, 3, 5, 5, null),
                new Creature(2, "Second", Faction.SAPIENS, 1, 2, 3, 4, 4, null))));
        Player player = new Player("Player", deck);
        player.prepareForRound(1, equipment(7, "Recovery Armor", 0, 2));
        new EffectEngine().activateEntries(player, playerWithCreatures("Opponent", 1, 20, 1), new ArrayList<>());
        assertEquals(7, player.getActiveCreature().getCurrentDef());
        player.getActiveCreature().takeDamage(8);
        player.handleCreatureDeath(new ArrayList<>());
        new EffectEngine().activateEntries(player, playerWithCreatures("Opponent", 1, 20, 1), new ArrayList<>());
        assertEquals(6, player.getActiveCreature().getCurrentDef());
        assertEquals(4, deck.getCreatureRounds().get(1).get(1).getInitialDef());
    }

    @Test
    void overdriveDefensePenaltyHasAMinimumOfOne() {
        PlayerDeck deck = new PlayerDeck();
        deck.setCreatureRounds(Map.of(1, List.of(
                new Creature(1, "Fragile", Faction.SAPIENS, 1, 2, 3, 1, 1, null))));
        Player player = new Player("Player", deck);
        player.prepareForRound(1, equipment(29, "Overdrive Harness", 2, -1));
        new EffectEngine().activateEntries(player, playerWithCreatures("Opponent", 1, 20, 1), new ArrayList<>());
        assertEquals(1, player.getActiveCreature().getCurrentDef());
    }

    @Test
    void breachCapacitorBonusExpiresAfterTheFirstExchange() {
        Player attacker = playerWithCreatures("Attacker", 2, 20, 1);
        Player defender = playerWithCreatures("Defender", 2, 20, 1);
        attacker.prepareForRound(1, equipment(28, "Breach Capacitor", 0, 0, 2));
        defender.prepareForRound(1, null);
        var log = combatEngine.runRound(attacker, defender).getLog();
        var exchanges = log.stream().filter(line -> line.startsWith("Scambio ")).toList();
        assertTrue(exchanges.get(0).contains("Attacker (Atk: 4, Def: 20)"));
        assertTrue(exchanges.get(1).contains("Attacker (Atk: 2, Def: 18)"));
    }

    @Test
    void breachCapacitorAppliesToEachCopyOnItsFirstExchange() {
        Player attacker = playerWithCreatures("Attacker", 2, 3, 2);
        Player defender = playerWithCreatures("Defender", 3, 20, 1);
        attacker.prepareForRound(1, equipment(28, "Breach Capacitor", 0, 0, 2));
        defender.prepareForRound(1, null);
        var log = combatEngine.runRound(attacker, defender).getLog();
        assertEquals(2, log.stream().filter(line -> line.contains("Attacker (Atk: 4, Def: 3)")).count());
    }

    private void activate(Creature attacker, Creature defender) {
        Player owner = new Player("Owner", new PlayerDeck());
        owner.setActiveCreature(attacker);
        Player opponent = new Player("Opponent", new PlayerDeck());
        opponent.setActiveCreature(defender);
        new EffectEngine().activateEntries(owner, opponent, new ArrayList<>());
    }

    private Player playerWithCreatures(String name, int attack, int defense, int count) {
        PlayerDeck deck = new PlayerDeck();
        List<Creature> creatures = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            creatures.add(new Creature(1, name, Faction.SAPIENS, 1, 2, attack, defense, defense, null));
        }
        deck.setCreatureRounds(Map.of(1, creatures));
        return new Player(name, deck);
    }
}
