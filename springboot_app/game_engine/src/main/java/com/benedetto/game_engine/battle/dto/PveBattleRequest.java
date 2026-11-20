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
public class PveBattleRequest {

    @JsonProperty("stageId")
    private String stageId;

    @JsonProperty("challenger")
    @JsonAlias({"userInfo", "user", "challengerInfo"})
    private ChallengerInfoRequest challenger;

    @JsonProperty("deck")
    @JsonAlias({"challengerDeck"})
    private ChallengerDeckRequest deck;
}
