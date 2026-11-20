package com.benedetto.game_engine.effect;

import java.util.Set;

public final class FirstExchangeAttackBonusEffect implements CardEffectHandler {
    public String code() { return "FIRST_EXCHANGE_ATTACK_BONUS"; }

    public void validate(CardEffect effect) {
        EffectParameters.require(effect, Set.of("amount"), Set.of("amount"));
    }

    public void onEnter(CardEffect effect, EffectContext context) {
        int amount = EffectParameters.integer(effect, "amount");
        context.host().setAttackModifier(Math.addExact(context.host().getAttackModifier(), amount));
        context.record(effect, String.format("attack %+d for the first exchange", amount));
    }

    public void afterExchange(CardEffect effect, EffectContext context) {
        if (context.host().getExchangesCompleted() == 1) {
            context.host().setAttackModifier(Math.subtractExact(context.host().getAttackModifier(), EffectParameters.integer(effect, "amount")));
            context.record(effect, "expired after the first exchange");
        }
    }
}
