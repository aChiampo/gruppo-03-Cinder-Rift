package com.benedetto.game_engine.effect;

/** Stateless behavior; mutable effect state belongs to the individual battle host. */
public interface CardEffectHandler {
    String code();
    void validate(CardEffect effect);
    void onEnter(CardEffect effect, EffectContext context);
    default void afterExchange(CardEffect effect, EffectContext context) {}
}
