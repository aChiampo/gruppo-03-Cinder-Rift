package com.benedetto.game_campaign.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CampaignStage {
    private String stageId;
    private int stageNumber;
    private String title;
    private String description;
    private String enemyName;
    private String enemyDeckPath;
    private boolean deterministic;
    private List<String> tutorialHints;
    private String requiredEventCode;
}
