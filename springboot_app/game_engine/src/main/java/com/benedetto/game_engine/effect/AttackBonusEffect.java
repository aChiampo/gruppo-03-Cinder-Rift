package com.benedetto.game_engine.effect;

import java.util.Set;

public final class AttackBonusEffect implements CardEffectHandler {
    public String code() { return "ATTACK_BONUS"; }

    public void validate(CardEffect effect) {
        EffectParameters.require(effect, Set.of("amount"), Set.of("amount"));
    }

    public void onEnter(CardEffect effect, EffectContext context) {
        int amount = EffectParameters.integer(effect, "amount");
        context.host().setAttackModifier(Math.addExact(context.host().getAttackModifier(), amount));
        context.record(effect, String.format("attack %+d until the creature leaves battle", amount));
    }
}
