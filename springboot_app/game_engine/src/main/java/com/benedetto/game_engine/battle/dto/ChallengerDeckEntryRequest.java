package com.benedetto.game_engine.battle.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.benedetto.game_engine.model.Faction;
import com.benedetto.game_engine.effect.CardEffect;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChallengerDeckEntryRequest {

    @JsonProperty("cardId")
    private Integer cardId;

    @JsonProperty("artworkId")
    @JsonAlias({"cardArtId", "artwork_id", "card_art_id"})
    private Integer artworkId;

    @JsonProperty("destination")
    private String destination;

    private String name;
    private Faction faction;
    private Integer energyCost;
    private Integer powerPoints;
    private Integer baseAtk;
    private Integer initialDef;
    @Builder.Default
    private List<CardEffect> effects = List.of();
}
