package com.benedetto.game_engine.battle.dto;

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
public class ChallengerDeckRequest {

    @JsonProperty("entries")
    private List<ChallengerDeckEntryRequest> entries;
}
