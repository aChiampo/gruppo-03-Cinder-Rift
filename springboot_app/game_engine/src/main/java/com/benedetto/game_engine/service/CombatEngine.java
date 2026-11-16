package com.benedetto.game_engine.service;

import com.benedetto.game_engine.model.*;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.List;

@Service
public class CombatEngine {

    private static final int MAX_EXCHANGES = 50;

    public RoundResult runRound(Player p1, Player p2) {
        List<String> log = new ArrayList<>();
        int exchanges = 0;

        while (p1.hasActiveCreature() && p2.hasActiveCreature() && p1.getHp() > 0 && p2.getHp() > 0 && exchanges < MAX_EXCHANGES) {
            exchanges++;
            Creature c1 = p1.getActiveCreature();
            Creature c2 = p2.getActiveCreature();

            int atk1 = calculateEffectiveAtk(c1, c2);
            int atk2 = calculateEffectiveAtk(c2, c1);

            int defBefore1 = c1.getCurrentDef();
            int defBefore2 = c2.getCurrentDef();

            log.add(String.format("Scambio %d: %s (Atk: %d, Def: %d) vs %s (Atk: %d, Def: %d)",
                    exchanges, c1.getName(), atk1, defBefore1, c2.getName(), atk2, defBefore2));

            c1.takeDamage(atk2);
            c2.takeDamage(atk1);

            boolean c1Dead = c1.isDead();
            boolean c2Dead = c2.isDead();

            if (c1Dead && c2Dead) {
                log.add("-> Entrambe le creature sono state eliminate nello stesso scambio!");
                p1.handleCreatureDeath(log);
                p2.handleCreatureDeath(log);
            } else if (c1Dead) {
                log.add(String.format("-> %s di %s è stata distrutta!", c1.getName(), p1.getName()));
                int excessDamage = Math.max(0, atk2 - defBefore1);
                if (excessDamage > 0) {
                    p1.takeHpDamage(excessDamage);
                    log.add(String.format("   Danno in eccesso: %d inflitto a %s (HP residui: %d)", excessDamage, p1.getName(), p1.getHp()));
                }
                p1.handleCreatureDeath(log);
            } else if (c2Dead) {
                log.add(String.format("-> %s di %s è stata distrutta!", c2.getName(), p2.getName()));
                int excessDamage = Math.max(0, atk1 - defBefore2);
                if (excessDamage > 0) {
                    p2.takeHpDamage(excessDamage);
                    log.add(String.format("   Danno in eccesso: %d inflitto a %s (HP residui: %d)", excessDamage, p2.getName(), p2.getHp()));
                }
                p2.handleCreatureDeath(log);
            }
        }

        if (exchanges >= MAX_EXCHANGES) {
            log.add("ATTENZIONE: Raggiunto il limite massimo di 50 scambi! Possibile loop infinito interrotto.");
        }

        Player roundWinner = determineRoundWinner(p1, p2);
        return new RoundResult(log, roundWinner);
    }

    public int calculateEffectiveAtk(Creature attacker, Creature defender) {
        int bonusFazione = attacker.getFaction().isStrongAgainst(defender.getFaction()) ? 2 : 0; // Bonus fazione +2
        int bonusEquip = (attacker.getEquipment() != null) ? attacker.getEquipment().getAtkBonus() : 0;
        return attacker.getBaseAtk() + bonusFazione + bonusEquip;
    }

    private Player determineRoundWinner(Player p1, Player p2) {
        if (p1.getHp() <= 0 && p2.getHp() <= 0) return null;
        if (p1.getHp() <= 0) return p2;
        if (p2.getHp() <= 0) return p1;

        if (!p1.hasActiveCreature() && !p2.hasActiveCreature()) {
            if (p1.getHp() > p2.getHp()) return p1;
            if (p2.getHp() > p1.getHp()) return p2;
            return null;
        }

        if (p1.hasActiveCreature() && !p2.hasActiveCreature()) return p1;
        if (p2.hasActiveCreature() && !p1.hasActiveCreature()) return p2;

        return null;
    }
}