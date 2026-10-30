package com.benedetto.game_engine.service;

import com.benedetto.game_engine.model.*;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.List;

@Service
public class MatchEngine {

    private final CombatEngine combatEngine;

    public MatchEngine(CombatEngine combatEngine) {
        this.combatEngine = combatEngine;
    }

    public List<String> runMatch(Player p1, Player p2) {
        List<String> matchLog = new ArrayList<>();
        matchLog.add("==========================================");
        matchLog.add("     INIZIO PARTITA AUTOMATICA TCG       ");
        matchLog.add("==========================================");

        p1.getDeck().shuffleEquipments();
        p2.getDeck().shuffleEquipments();

        for (int round = 1; round <= 5; round++) { // Partita su 5 round[cite: 1]
            p1.getDeck().shuffleRoundCreatures(round);
            p2.getDeck().shuffleRoundCreatures(round);

            Equipment equipP1 = p1.getDeck().getEquipmentDeck().get(round - 1);
            Equipment equipP2 = p2.getDeck().getEquipmentDeck().get(round - 1);

            p1.prepareForRound(round, equipP1);
            p2.prepareForRound(round, equipP2);

            matchLog.add(String.format("\n--- INIZIO ROUND %d ---", round));
            matchLog.add(String.format("Equip %s: %s (+%d ATK, +%d DEF)", p1.getName(), equipP1.getName(), equipP1.getAtkBonus(), equipP1.getDefBonus()));
            matchLog.add(String.format("Equip %s: %s (+%d ATK, +%d DEF)", p2.getName(), equipP2.getName(), equipP2.getAtkBonus(), equipP2.getDefBonus()));

            RoundResult result = combatEngine.runRound(p1, p2);
            matchLog.addAll(result.getLog());

            if (result.getWinner() == p1) {
                p1.addRoundWin();
                matchLog.add("--> ROUND VINTO DA: " + p1.getName());
            } else if (result.getWinner() == p2) {
                p2.addRoundWin();
                matchLog.add("--> ROUND VINTO DA: " + p2.getName());
            } else {
                matchLog.add("--> ROUND TERMINATO IN PAREGGIO!");
            }

            matchLog.add(String.format("PUNTEGGIO PARZIALE: %s %d - %d %s", p1.getName(), p1.getRoundsWon(), p2.getRoundsWon(), p2.getName()));

            if (p1.getRoundsWon() == 3 || p2.getRoundsWon() == 3) { // Vittoria anticipata a 3 round vinti[cite: 1]
                matchLog.add("\n==========================================");
                matchLog.add("VITTORIA ANTICIPATA RAGGIUNTA!");
                break;
            }
        }

        matchLog.add("\n==========================================");
        matchLog.add(String.format("RISULTATO FINALE: %s %d - %d %s", p1.getName(), p1.getRoundsWon(), p2.getRoundsWon(), p2.getName()));
        if (p1.getRoundsWon() > p2.getRoundsWon()) {
            matchLog.add("VINCITORE PARTITA: " + p1.getName());
        } else if (p2.getRoundsWon() > p1.getRoundsWon()) {
            matchLog.add("VINCITORE PARTITA: " + p2.getName());
        } else {
            matchLog.add("LA PARTITA TERMINA IN PAREGGIO!");
        }
        matchLog.add("==========================================");

        return matchLog;
    }
}