package com.benedetto.game_engine.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Creature {
    private String id;
    private String name;
    private Faction faction;
    private int energyCost;
    private int powerPoints;
    private int baseAtk;
    private int initialDef;
    private int currentDef;
    private Equipment equipment;

    public void initCombatStats() {
        this.currentDef = this.initialDef;
    }

    public void takeDamage(int damage) {
        this.currentDef -= damage;
    }

    public boolean isDead() {
        return this.currentDef <= 0;
    }

    public Creature cloneForBattle() {
        return new Creature(id, name, faction, energyCost, powerPoints, baseAtk, initialDef, initialDef, null);
    }
}