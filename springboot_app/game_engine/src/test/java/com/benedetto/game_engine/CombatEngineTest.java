package com.benedetto.game_engine;

import com.benedetto.game_engine.model.*;
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
        combatEngine = new CombatEngine();
    }

    @Test
    void testVantaggioFazioneCalcolatoCorrettamente() {
        Creature attacker = new Creature("C1", "Cyborg", Faction.CYBORG, 1, 2, 4, 10, 10, null);
        Creature defender = new Creature("M1", "Mostro", Faction.MOSTRI_RADIOATTIVI, 1, 2, 3, 10, 10, null);

        int effAtk = combatEngine.calculateEffectiveAtk(attacker, defender);
        assertEquals(6, effAtk);
    }

    @Test
    void testTrasferimentoEquipaggiamentoEPassaggioAllaSuccessiva() {
        Equipment spadone = new Equipment("E1", "Spadone", 2, 0);
        Creature c1 = new Creature("C1", "Creatura 1", Faction.SAPIENS, 1, 2, 2, 2, 2, spadone);
        Creature c2 = new Creature("C2", "Creatura 2", Faction.SAPIENS, 1, 2, 4, 5, 5, null);

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
        Creature c1 = new Creature("Zero1", "Zero 1", Faction.CYBORG, 1, 1, 0, 10, 10, null);
        Creature c2 = new Creature("Zero2", "Zero 2", Faction.CYBORG, 1, 1, 0, 10, 10, null);

        PlayerDeck d1 = new PlayerDeck();
        d1.setCreatureRounds(Map.of(1, List.of(c1)));
        PlayerDeck d2 = new PlayerDeck();
        d2.setCreatureRounds(Map.of(1, List.of(c2)));

        Player p1 = new Player("P1", d1);
        Player p2 = new Player("P2", d2);
        p1.prepareForRound(1, new Equipment("Nessuno", "Nessuno", 0, 0));
        p2.prepareForRound(1, new Equipment("Nessuno", "Nessuno", 0, 0));

        RoundResult result = combatEngine.runRound(p1, p2);

        assertTrue(result.getLog().stream().anyMatch(line -> line.contains("loop infinito interrotto")));
    }

    @Test
    void testNessunBonusFazioneTraStessaFazione() {
        Creature c1 = new Creature("C1", "Cyborg 1", Faction.CYBORG, 1, 2, 5, 10, 10, null);
        Creature c2 = new Creature("C2", "Cyborg 2", Faction.CYBORG, 1, 2, 3, 10, 10, null);

        int effAtk = combatEngine.calculateEffectiveAtk(c1, c2);
        assertEquals(5, effAtk);
    }

    @Test
    void testMorteSimultaneaESostituzione() {
        Creature c1 = new Creature("C1", "Attaccante A", Faction.SAPIENS, 1, 2, 5, 3, 3, null);
        Creature c2 = new Creature("C2", "Attaccante B", Faction.SAPIENS, 1, 2, 5, 3, 3, null);

        PlayerDeck d1 = new PlayerDeck();
        d1.setCreatureRounds(Map.of(1, List.of(c1)));
        PlayerDeck d2 = new PlayerDeck();
        d2.setCreatureRounds(Map.of(1, List.of(c2)));

        Player p1 = new Player("P1", d1);
        Player p2 = new Player("P2", d2);
        p1.prepareForRound(1, new Equipment("Nessuno", "Nessuno", 0, 0));
        p2.prepareForRound(1, new Equipment("Nessuno", "Nessuno", 0, 0));

        RoundResult result = combatEngine.runRound(p1, p2);

        assertNotNull(result);
        assertNotNull(result.getLog());
        assertFalse(result.getLog().isEmpty());
    }

    @Test
    void testStackingBonusFazioneEEquipaggiamento() {
        Equipment spadone = new Equipment("E1", "Spadone", 3, 0);
        Creature attacker = new Creature("C1", "Cyborg", Faction.CYBORG, 1, 2, 4, 10, 10, spadone);
        Creature defender = new Creature("M1", "Mostro", Faction.MOSTRI_RADIOATTIVI, 1, 2, 3, 10, 10, null);

        int effAtk = combatEngine.calculateEffectiveAtk(attacker, defender);
        assertEquals(9, effAtk);
    }

    @Test
    void testPassaggioEquipaggiamentoInCatenaSuPiuCreature() {
        Equipment elmo = new Equipment("E1", "Elmo", 1, 2);
        Creature c1 = new Creature("C1", "Creatura 1", Faction.SAPIENS, 1, 1, 1, 1, 1, elmo);
        Creature c2 = new Creature("C2", "Creatura 2", Faction.SAPIENS, 1, 1, 2, 5, 5, null);
        Creature c3 = new Creature("C3", "Creatura 3", Faction.SAPIENS, 1, 1, 3, 5, 5, null);

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
        Equipment spadone = new Equipment("E1", "Spadone", 2, 0);
        Creature ultima = new Creature("C1", "Ultima Creatura", Faction.SAPIENS, 1, 1, 1, 1, 1, spadone);

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
        Equipment scudo = new Equipment("E2", "Scudo Spaziale", 0, 3);

        Creature c1 = new Creature("A1", "Attaccante", Faction.SAPIENS, 1, 2, 5, 2, 2, null);
        Creature c2 = new Creature("D1", "Difensore", Faction.SAPIENS, 1, 2, 2, 4, 4, scudo);

        PlayerDeck d1 = new PlayerDeck();
        d1.setCreatureRounds(Map.of(1, List.of(c1)));
        PlayerDeck d2 = new PlayerDeck();
        d2.setCreatureRounds(Map.of(1, List.of(c2)));

        Player p1 = new Player("P1", d1);
        Player p2 = new Player("P2", d2);
        p1.prepareForRound(1, new Equipment("Nessuno", "Nessuno", 0, 0));
        p2.prepareForRound(1, scudo);

        RoundResult result = combatEngine.runRound(p1, p2);

        assertNotNull(result);
        assertFalse(result.getLog().isEmpty());
    }

    @Test
    void testAttaccoEffettivoInvariatoConCaloDifesaResidua() {
        Creature cyborg = new Creature("C1", "Cyborg", Faction.CYBORG, 1, 2, 3, 10, 10, null);
        Creature mostro = new Creature("M1", "Mostro", Faction.MOSTRI_RADIOATTIVI, 1, 2, 2, 8, 8, null);

        int atkIniziale = combatEngine.calculateEffectiveAtk(cyborg, mostro);

        Creature cyborgDanneggiato = new Creature("C1", "Cyborg", Faction.CYBORG, 1, 2, 3, 2, 10, null);
        int atkDopoDanno = combatEngine.calculateEffectiveAtk(cyborgDanneggiato, mostro);

        assertEquals(5, atkDopoDanno);
    }
}