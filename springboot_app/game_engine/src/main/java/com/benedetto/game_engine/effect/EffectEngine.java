package com.benedetto.game_engine.effect;

import com.benedetto.game_engine.model.Creature;
import com.benedetto.game_engine.model.Player;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/** Explicit behavior registry, independent of card IDs and card catalogs. */
@Service
public class EffectEngine {
    private final Map<String, CardEffectHandler> handlers = Map.of(
            "ATTACK_BONUS", new AttackBonusEffect(),
            "STARTING_DEFENSE_MODIFIER", new StartingDefenseModifierEffect(),
            "FIRST_EXCHANGE_ATTACK_BONUS", new FirstExchangeAttackBonusEffect());

    public void validate(List<CardEffect> effects) {
        if (effects == null) throw new IllegalArgumentException("Effects must be an array, possibly empty");
        for (CardEffect effect : effects) {
            if (effect == null || effect.effectId() == null || effect.effectId() <= 0 || effect.code() == null
                    || !handlers.containsKey(effect.code())) {
                throw new IllegalArgumentException("Effect must have a positive effectId and a supported code");
            }
            handlers.get(effect.code()).validate(effect);
        }
    }

    public void activateEntries(Player first, Player second, List<String> log) {
        // Both hosts are visible before activation. Equipment for both players
        // resolves before either creature's own effects.
        activateEquipment(first, second, log);
        activateEquipment(second, first, log);
        activateCreature(first, second, log);
        activateCreature(second, first, log);
    }

    private void activateEquipment(Player owner, Player opponent, List<String> log) {
        Creature host = owner.getActiveCreature();
        if (host == null || host.isEffectsActivated()) return;
        if (host.getEquipment() != null) {
            dispatch(host.getEquipment().getEffects(), new EffectContext(owner, opponent, host,
                    host.getEquipment().getName(), log), CardEffectHandler::onEnter);
        }
    }

    private void activateCreature(Player owner, Player opponent, List<String> log) {
        Creature host = owner.getActiveCreature();
        if (host == null || host.isEffectsActivated()) return;
        dispatch(host.getEffects(), new EffectContext(owner, opponent, host, host.getName(), log), CardEffectHandler::onEnter);
        host.setEffectsActivated(true);
    }

    public void afterExchange(Player owner, Player opponent, List<String> log) {
        Creature host = owner.getActiveCreature();
        host.setExchangesCompleted(host.getExchangesCompleted() + 1);
        if (host.getEquipment() != null) {
            dispatch(host.getEquipment().getEffects(), new EffectContext(owner, opponent, host,
                    host.getEquipment().getName(), log), CardEffectHandler::afterExchange);
        }
        dispatch(host.getEffects(), new EffectContext(owner, opponent, host, host.getName(), log), CardEffectHandler::afterExchange);
    }

    private void dispatch(List<CardEffect> effects, EffectContext context,
                          EffectAction action) {
        validate(effects);
        for (CardEffect effect : effects) {
            action.apply(handlers.get(effect.code()), effect, context);
        }
    }

    @FunctionalInterface
    private interface EffectAction {
        void apply(CardEffectHandler handler, CardEffect effect, EffectContext context);
    }
}
