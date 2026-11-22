package com.benedetto.game_engine.battle.service;

import com.benedetto.game_engine.battle.ChallengerDeckMapperService;
import com.benedetto.game_engine.battle.BattleDeadline;
import com.benedetto.game_engine.battle.dto.ChallengerInfoRequest;
import com.benedetto.game_engine.battle.dto.PveBattleRequest;
import com.benedetto.game_engine.battle.dto.PveBattleResponse;
import com.benedetto.game_engine.battle.exception.InactiveStageEventException;
import com.benedetto.game_engine.battle.exception.StageNotFoundException;
import com.benedetto.game_engine.campaign.entity.SpecialEventEntity;
import com.benedetto.game_engine.campaign.model.CampaignStage;
import com.benedetto.game_engine.campaign.repository.SpecialEventRepository;
import com.benedetto.game_engine.campaign.service.CampaignStageLoaderService;
import com.benedetto.game_engine.model.Player;
import com.benedetto.game_engine.model.PlayerDeck;
import com.benedetto.game_engine.service.CardLoaderService;
import com.benedetto.game_engine.service.MatchEngine;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Service
@Transactional(readOnly = true)
public class PveBattleService {

    private final ChallengerDeckMapperService challengerDeckMapperService;
    private final CampaignStageLoaderService campaignStageLoaderService;
    private final SpecialEventRepository specialEventRepository;
    private final CardLoaderService cardLoaderService;
    private final MatchEngine matchEngine;

    public PveBattleService(ChallengerDeckMapperService challengerDeckMapperService,
                            CampaignStageLoaderService campaignStageLoaderService,
                            SpecialEventRepository specialEventRepository,
                            CardLoaderService cardLoaderService,
                            MatchEngine matchEngine) {
        this.challengerDeckMapperService = challengerDeckMapperService;
        this.campaignStageLoaderService = campaignStageLoaderService;
        this.specialEventRepository = specialEventRepository;
        this.cardLoaderService = cardLoaderService;
        this.matchEngine = matchEngine;
    }

    public PveBattleResponse executeBattle(PveBattleRequest request) {
        return executeBattle(request, BattleDeadline.fromHeader(null));
    }

    public PveBattleResponse executeBattle(PveBattleRequest request, Runnable checkpoint) {
        checkpoint.run();
        if (request == null) {
            throw new IllegalArgumentException("Battle request must not be null");
        }

        PlayerDeck challengerDeck = challengerDeckMapperService.mapToPlayerDeck(
                request.getChallenger(),
                request.getDeck()
        );

        String stageIdentifier = request.getStageId();
        if (stageIdentifier == null || stageIdentifier.isBlank()) {
            throw new StageNotFoundException(null, "Stage identifier in battle request cannot be null or empty");
        }

        CampaignStage stage = campaignStageLoaderService.loadStageByIdentifier(stageIdentifier);

        if (stage.getRequiredEventCode() != null && !stage.getRequiredEventCode().isBlank()) {
            String eventCode = stage.getRequiredEventCode().trim();
            Optional<SpecialEventEntity> specialEvent = specialEventRepository.findByCode(eventCode);
            if (specialEvent.isEmpty() || !specialEvent.get().isActive()) {
                throw new InactiveStageEventException(
                        eventCode,
                        String.format("Special event [%s] required for stage [%s] is inactive or does not exist",
                                eventCode, stage.getStageId())
                );
            }
        }

        PlayerDeck enemyDeck = cardLoaderService.loadDeck(stage.getEnemyDeckPath());

        String challengerName = resolveChallengerName(request.getChallenger());
        Player challengerPlayer = new Player(challengerName, challengerDeck);

        String enemyName = stage.getEnemyName() != null && !stage.getEnemyName().isBlank()
                ? stage.getEnemyName().trim()
                : "Enemy Bot";
        Player enemyPlayer = new Player(enemyName, enemyDeck);

        checkpoint.run();
        List<String> matchLog = matchEngine.runMatch(challengerPlayer, enemyPlayer, checkpoint);
        checkpoint.run();

        List<String> completeLog = new ArrayList<>();
        if (stage.getTutorialHints() != null && !stage.getTutorialHints().isEmpty()) {
            completeLog.addAll(stage.getTutorialHints());
        }
        if (matchLog != null) {
            completeLog.addAll(matchLog);
        }

        String outcome;
        String winner;
        if (challengerPlayer.getRoundsWon() > enemyPlayer.getRoundsWon()) {
            outcome = "VICTORY";
            winner = challengerPlayer.getName();
        } else if (enemyPlayer.getRoundsWon() > challengerPlayer.getRoundsWon()) {
            outcome = "DEFEAT";
            winner = enemyPlayer.getName();
        } else {
            outcome = "DRAW";
            winner = "NONE";
        }

        return PveBattleResponse.builder()
                .stageId(stage.getStageId())
                .stageNumber(stage.getStageNumber())
                .stageTitle(stage.getTitle())
                .challenger(request.getChallenger())
                .enemyName(enemyPlayer.getName())
                .outcome(outcome)
                .winner(winner)
                .tutorialHints(stage.getTutorialHints() != null ? stage.getTutorialHints() : Collections.emptyList())
                .battleLog(completeLog)
                .build();
    }

    private String resolveChallengerName(ChallengerInfoRequest challenger) {
        if (challenger != null) {
            if (challenger.getDisplayName() != null && !challenger.getDisplayName().isBlank()) {
                return challenger.getDisplayName().trim();
            }
            if (challenger.getUsername() != null && !challenger.getUsername().isBlank()) {
                return challenger.getUsername().trim();
            }
            if (challenger.getUserId() != null && !challenger.getUserId().isBlank()) {
                return challenger.getUserId().trim();
            }
        }
        return "Challenger";
    }
}
