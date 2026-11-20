package com.benedetto.game_engine.effect;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/** Immutable effect snapshot; effectId is metadata, code selects the implementation. */
public record CardEffect(Integer effectId, String code, Map<String, Object> parameters) {
    public CardEffect {
        if (parameters != null) {
            parameters = Collections.unmodifiableMap(new LinkedHashMap<>(parameters));
        }
    }
}
