package com.benedetto.game_engine.campaign.service;

import com.benedetto.game_engine.campaign.entity.SpecialEventEntity;
import com.benedetto.game_engine.campaign.model.CampaignStage;
import com.benedetto.game_engine.campaign.repository.SpecialEventRepository;
import com.benedetto.game_engine.model.Player;
import com.benedetto.game_engine.model.PlayerDeck;
import com.benedetto.game_engine.service.CardLoaderService;
import com.benedetto.game_engine.service.MatchEngine;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@Transactional(readOnly = true)
public class CampaignService {

    private final MatchEngine matchEngine;
    private final CardLoaderService cardLoaderService;
    private final CampaignStageLoaderService campaignStageLoaderService;
    private final SpecialEventRepository specialEventRepository;

    public CampaignService(MatchEngine matchEngine,
                           CardLoaderService cardLoaderService,
                           CampaignStageLoaderService campaignStageLoaderService,
                           SpecialEventRepository specialEventRepository) {
        this.matchEngine = matchEngine;
        this.cardLoaderService = cardLoaderService;
        this.campaignStageLoaderService = campaignStageLoaderService;
        this.specialEventRepository = specialEventRepository;
    }

    public List<String> executeStage(Player humanPlayer, String stageJsonPath) {
        CampaignStage stage = campaignStageLoaderService.loadStage(stageJsonPath);

        if (stage.getRequiredEventCode() != null && !stage.getRequiredEventCode().isBlank()) {
            Optional<SpecialEventEntity> specialEvent = specialEventRepository.findByCode(stage.getRequiredEventCode());
            if (specialEvent.isEmpty() || !specialEvent.get().isActive()) {
                List<String> errorLog = new ArrayList<>();
                errorLog.add(String.format("ERRORE: L'evento speciale richiesto [%s] non è attivo o non esiste!", stage.getRequiredEventCode()));
                return errorLog;
            }
        }

        PlayerDeck enemyDeck = cardLoaderService.loadDeck(stage.getEnemyDeckPath());

        if (stage.isDeterministic()) {
            if (humanPlayer != null && humanPlayer.getDeck() != null) {
                humanPlayer.getDeck().setDeterministic(true);
            }
            if (enemyDeck != null) {
                enemyDeck.setDeterministic(true);
            }
        }

        Player enemyBot = new Player(stage.getEnemyName(), enemyDeck);

        List<String> matchLog = matchEngine.runMatch(humanPlayer, enemyBot);

        List<String> finalLog = new ArrayList<>();
        if (stage.getTutorialHints() != null && !stage.getTutorialHints().isEmpty()) {
            finalLog.addAll(stage.getTutorialHints());
        }
        finalLog.addAll(matchLog);

        return finalLog;
    }
}
