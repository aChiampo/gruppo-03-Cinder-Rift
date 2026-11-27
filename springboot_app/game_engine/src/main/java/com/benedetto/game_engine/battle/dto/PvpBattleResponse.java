package com.benedetto.game_engine.battle.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PvpBattleResponse {

    @JsonProperty("mode")
    private String mode;

    @JsonProperty("outcome")
    private String outcome;

    @JsonProperty("winnerUserId")
    private String winnerUserId;

    @JsonProperty("winnerUsername")
    private String winnerUsername;

    @JsonProperty("battleLog")
    @JsonAlias({"matchLog"})
    private List<String> battleLog;
}
