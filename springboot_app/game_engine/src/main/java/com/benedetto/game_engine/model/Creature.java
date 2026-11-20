package com.benedetto.game_engine.model;

import com.benedetto.game_engine.effect.CardEffect;
import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.List;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class Creature {
    private Integer id;
    private String name;
    private Faction faction;
    private int energyCost;
    private int powerPoints;
    private int baseAtk;
    private int initialDef;
    private int currentDef;
    private Equipment equipment;

    private List<CardEffect> effects = List.of();
    @JsonIgnore private int attackModifier;
    @JsonIgnore private int exchangesCompleted;
    @JsonIgnore private boolean effectsActivated;

    public Creature(Integer id, String name, Faction faction, int energyCost, int powerPoints,
                    int baseAtk, int initialDef, int currentDef, Equipment equipment) {
        this.id = id;
        this.name = name;
        this.faction = faction;
        this.energyCost = energyCost;
        this.powerPoints = powerPoints;
        this.baseAtk = baseAtk;
        this.initialDef = initialDef;
        this.currentDef = currentDef;
        this.equipment = equipment;
    }

    public void initCombatStats() {
        this.currentDef = this.initialDef;
        this.attackModifier = 0;
        this.exchangesCompleted = 0;
        this.effectsActivated = false;
    }

    public void takeDamage(int damage) {
        this.currentDef -= damage;
    }

    public boolean isDead() {
        return this.currentDef <= 0;
    }

    public Creature cloneForBattle() {
        Creature copy = new Creature(id, name, faction, energyCost, powerPoints, baseAtk, initialDef, initialDef, null);
        copy.setEffects(List.copyOf(effects));
        return copy;
    }
}
