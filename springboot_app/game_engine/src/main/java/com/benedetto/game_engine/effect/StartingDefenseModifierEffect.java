package com.benedetto.game_engine.effect;

import java.util.Set;

public final class StartingDefenseModifierEffect implements CardEffectHandler {
    public String code() { return "STARTING_DEFENSE_MODIFIER"; }

    public void validate(CardEffect effect) {
        EffectParameters.require(effect, Set.of("amount"), Set.of("amount", "minimum"));
        if (EffectParameters.integer(effect, "minimum", 1) < 1) {
            throw new IllegalArgumentException("Starting defense minimum must be positive");
        }
    }

    public void onEnter(CardEffect effect, EffectContext context) {
        int amount = EffectParameters.integer(effect, "amount");
        int minimum = EffectParameters.integer(effect, "minimum", 1);
        int defense = Math.max(minimum, Math.addExact(context.host().getCurrentDef(), amount));
        context.host().setCurrentDef(defense);
        context.record(effect, String.format("starting defense %+d, minimum %d, resulting defense %d", amount, minimum, defense));
    }
}
