package com.benedetto.game_engine.service;

import com.benedetto.game_engine.model.*;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.List;
import com.benedetto.game_engine.effect.EffectEngine;

@Service
public class CombatEngine {

    private static final int MAX_EXCHANGES = 50;
    private final EffectEngine effectEngine;

    public CombatEngine(EffectEngine effectEngine) {
        this.effectEngine = effectEngine;
    }

    public RoundResult runRound(Player p1, Player p2) {
        List<String> log = new ArrayList<>();
        int exchanges = 0;

        while (p1.hasActiveCreature() && p2.hasActiveCreature() && p1.getHp() > 0 && p2.getHp() > 0 && exchanges < MAX_EXCHANGES) {
            effectEngine.activateEntries(p1, p2, log);
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

            effectEngine.afterExchange(p1, p2, log);
            effectEngine.afterExchange(p2, p1, log);

            boolean c1Dead = c1.isDead();
            boolean c2Dead = c2.isDead();

            if (c1Dead && c2Dead) {
                log.add("-> Entrambe le creature sono state eliminate nello stesso scambio!");
            } else if (c1Dead) {
                log.add(String.format("-> %s di %s è stata distrutta!", c1.getName(), p1.getName()));
                int excessDamage = Math.max(0, atk2 - defBefore1);
                if (excessDamage > 0) {
                    p1.takeHpDamage(excessDamage);
                    log.add(String.format("   Danno in eccesso: %d inflitto a %s (HP residui: %d)", excessDamage, p1.getName(), p1.getHp()));
                }
            } else if (c2Dead) {
                log.add(String.format("-> %s di %s è stata distrutta!", c2.getName(), p2.getName()));
                int excessDamage = Math.max(0, atk1 - defBefore2);
                if (excessDamage > 0) {
                    p2.takeHpDamage(excessDamage);
                    log.add(String.format("   Danno in eccesso: %d inflitto a %s (HP residui: %d)", excessDamage, p2.getName(), p2.getHp()));
                }
            }

            // A finished round must not deploy another host or activate new effects.
            if (p1.getHp() <= 0 || p2.getHp() <= 0
                    || !p1.hasCreaturesRemaining() || !p2.hasCreaturesRemaining()) break;
            if (c1Dead) p1.handleCreatureDeath(log);
            if (c2Dead) p2.handleCreatureDeath(log);
        }

        if (exchanges >= MAX_EXCHANGES) {
            log.add("ATTENZIONE: Raggiunto il limite massimo di 50 scambi! Possibile loop infinito interrotto.");
        }

        Player roundWinner = determineRoundWinner(p1, p2);
        return new RoundResult(log, roundWinner);
    }

    public int calculateEffectiveAtk(Creature attacker, Creature defender) {
        int factionBonus = attacker.getFaction().isStrongAgainst(defender.getFaction()) ? 2 : 0;
        return Math.max(0, Math.addExact(Math.addExact(attacker.getBaseAtk(), attacker.getAttackModifier()), factionBonus));
    }

    private Player determineRoundWinner(Player p1, Player p2) {
        if (p1.getHp() <= 0 && p2.getHp() <= 0) return null;
        if (p1.getHp() <= 0) return p2;
        if (p2.getHp() <= 0) return p1;

        if (!p1.hasCreaturesRemaining() && !p2.hasCreaturesRemaining()) {
            if (p1.getHp() > p2.getHp()) return p1;
            if (p2.getHp() > p1.getHp()) return p2;
            return null;
        }

        if (p1.hasCreaturesRemaining() && !p2.hasCreaturesRemaining()) return p1;
        if (p2.hasCreaturesRemaining() && !p1.hasCreaturesRemaining()) return p2;

        return null;
    }
}
