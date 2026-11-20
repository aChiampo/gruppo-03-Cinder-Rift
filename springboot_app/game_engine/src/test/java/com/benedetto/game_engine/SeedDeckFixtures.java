package com.benedetto.game_engine;

import com.benedetto.game_engine.battle.dto.ChallengerDeckEntryRequest;
import com.benedetto.game_engine.model.PlayerDeck;
import com.benedetto.game_engine.model.Equipment;
import com.benedetto.game_engine.effect.CardEffect;
import java.util.Map;
import com.benedetto.game_engine.service.CardLoaderService;
import org.springframework.core.io.DefaultResourceLoader;

import java.util.ArrayList;
import java.util.List;

public final class SeedDeckFixtures {
    private SeedDeckFixtures() {}

    public static PlayerDeck load(String name) {
        return new CardLoaderService(new DefaultResourceLoader()).loadDeck("mocks/" + name + ".json");
    }

    public static List<ChallengerDeckEntryRequest> entries() {
        PlayerDeck deck = load("mirror");
        List<ChallengerDeckEntryRequest> entries = new ArrayList<>();
        deck.getCreatureRounds().forEach((round, creatures) -> creatures.forEach(card -> entries.add(
                ChallengerDeckEntryRequest.builder().cardId(card.getId()).artworkId(card.getId())
                        .destination("ROUND" + round).name(card.getName()).faction(card.getFaction())
                        .energyCost(card.getEnergyCost()).powerPoints(card.getPowerPoints())
                        .baseAtk(card.getBaseAtk()).initialDef(card.getInitialDef()).effects(card.getEffects()).build())));
        deck.getEquipmentDeck().forEach(card -> entries.add(ChallengerDeckEntryRequest.builder()
                .cardId(card.getId()).artworkId(card.getId()).destination("EQUIPMENT")
                .name(card.getName()).effects(card.getEffects()).build()));
        return entries;
    }

    public static Equipment equipment(Integer id, String name, int attack, int defense) {
        return equipment(id, name, attack, defense, 0);
    }

    public static Equipment equipment(Integer id, String name, int attack, int defense, int opening) {
        List<CardEffect> effects = new ArrayList<>();
        if (attack != 0) effects.add(new CardEffect(1, "ATTACK_BONUS", Map.of("amount", attack)));
        if (defense != 0) effects.add(new CardEffect(2, "STARTING_DEFENSE_MODIFIER", Map.of("amount", defense, "minimum", 1)));
        if (opening != 0) effects.add(new CardEffect(3, "FIRST_EXCHANGE_ATTACK_BONUS", Map.of("amount", opening)));
        return new Equipment(id, name, effects);
    }
}
