package com.benedetto.game_engine.effect;

import java.util.Set;

final class EffectParameters {
    private EffectParameters() {}

    static void require(CardEffect effect, Set<String> required, Set<String> allowed) {
        var parameters = effect.parameters();
        if (parameters == null || !parameters.keySet().containsAll(required)
                || !allowed.containsAll(parameters.keySet()) || parameters.values().stream().anyMatch(v -> !(v instanceof Integer))) {
            throw new IllegalArgumentException("Invalid parameters for effect " + effect.code());
        }
    }
    static int integer(CardEffect effect, String key) {
        return (Integer) effect.parameters().get(key);
    }

    static int integer(CardEffect effect, String key, int defaultValue) {
        return (Integer) effect.parameters().getOrDefault(key, defaultValue);
    }
}
