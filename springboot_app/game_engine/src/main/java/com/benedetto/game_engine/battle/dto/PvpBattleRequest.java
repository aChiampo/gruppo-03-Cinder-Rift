package com.benedetto.game_engine.battle.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PvpBattleRequest {

    @NotNull
    @Valid
    @JsonProperty("playerA")
    private ChallengerInfoRequest playerA;

    @NotNull
    @Valid
    @JsonProperty("deckA")
    private ChallengerDeckRequest deckA;

    @NotNull
    @Valid
    @JsonProperty("playerB")
    private ChallengerInfoRequest playerB;

    @NotNull
    @Valid
    @JsonProperty("deckB")
    private ChallengerDeckRequest deckB;

    @Builder.Default
    @JsonProperty("mode")
    private String mode = "RANKED";
}
