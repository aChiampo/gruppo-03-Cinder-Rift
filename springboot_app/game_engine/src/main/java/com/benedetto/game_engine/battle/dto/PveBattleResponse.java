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
public class PveBattleResponse {

    @JsonProperty("stageId")
    private String stageId;

    @JsonProperty("stageNumber")
    private int stageNumber;

    @JsonProperty("stageTitle")
    private String stageTitle;

    @JsonProperty("challenger")
    private ChallengerInfoRequest challenger;

    @JsonProperty("enemyName")
    private String enemyName;

    @JsonProperty("outcome")
    private String outcome;

    @JsonProperty("winner")
    private String winner;

    @JsonProperty("tutorialHints")
    private List<String> tutorialHints;

    @JsonProperty("battleLog")
    @JsonAlias({"matchLog"})
    private List<String> battleLog;
}
