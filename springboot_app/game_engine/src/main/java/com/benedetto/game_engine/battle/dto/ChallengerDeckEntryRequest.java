package com.benedetto.game_engine.battle.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
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
    private String cardId;

    @JsonProperty("artworkId")
    @JsonAlias({"cardArtId", "artwork_id", "card_art_id"})
    private Integer artworkId;

    @JsonProperty("destination")
    private String destination;

    public ChallengerDeckEntryRequest(Integer cardId, Integer artworkId, String destination) {
        this.cardId = cardId != null ? String.valueOf(cardId) : null;
        this.artworkId = artworkId;
        this.destination = destination;
    }
}
