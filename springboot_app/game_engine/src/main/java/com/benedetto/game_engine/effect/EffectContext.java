package com.benedetto.game_engine.effect;

import com.benedetto.game_engine.model.Creature;
import com.benedetto.game_engine.model.Player;

import java.util.List;

/** Exposes battle participants so each handler chooses its target and lifetime. */
public record EffectContext(Player owner, Player opponent, Creature host, String source, List<String> log) {
    public void record(CardEffect effect, String action) {
        log.add(String.format("[EFFECT] %s: %s -> %s: %s %s", owner.getName(), source, host.getName(), effect.code(), action));
    }
}
