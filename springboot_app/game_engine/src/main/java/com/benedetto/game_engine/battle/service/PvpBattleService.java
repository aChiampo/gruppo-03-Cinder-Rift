package com.benedetto.game_engine.battle.service;

import com.benedetto.game_engine.battle.BattleDeadline;
import com.benedetto.game_engine.battle.ChallengerDeckMapperService;
import com.benedetto.game_engine.battle.dto.ChallengerInfoRequest;
import com.benedetto.game_engine.battle.dto.PvpBattleRequest;
import com.benedetto.game_engine.battle.dto.PvpBattleResponse;
import com.benedetto.game_engine.model.Player;
import com.benedetto.game_engine.model.PlayerDeck;
import com.benedetto.game_engine.service.MatchEngine;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

@Service
public class PvpBattleService {

    private static final String MODE_RANKED = "RANKED";
    private static final String MODE_FRIENDLY = "FRIENDLY";

    private final ChallengerDeckMapperService challengerDeckMapperService;
    private final MatchEngine matchEngine;

    public PvpBattleService(ChallengerDeckMapperService challengerDeckMapperService,
                            MatchEngine matchEngine) {
        this.challengerDeckMapperService = challengerDeckMapperService;
        this.matchEngine = matchEngine;
    }

    public PvpBattleResponse executeBattle(PvpBattleRequest request) {
        return executeBattle(request, BattleDeadline.fromHeader(null));
    }

    public PvpBattleResponse executeBattle(PvpBattleRequest request, Runnable checkpoint) {
        Runnable effectiveCheckpoint = checkpoint != null ? checkpoint : BattleDeadline.fromHeader(null);
        effectiveCheckpoint.run();

        if (request == null) {
            throw new IllegalArgumentException("PvP battle request must not be null");
        }
        if (request.getPlayerA() == null || request.getDeckA() == null
                || request.getPlayerB() == null || request.getDeckB() == null) {
            throw new IllegalArgumentException("playerA, deckA, playerB and deckB must not be null");
        }

        String mode = validateAndResolveMode(request.getMode());
        validateDistinctPlayers(request.getPlayerA(), request.getPlayerB());

        PlayerDeck deckA = challengerDeckMapperService.mapToPlayerDeck(request.getPlayerA(), request.getDeckA());
        PlayerDeck deckB = challengerDeckMapperService.mapToPlayerDeck(request.getPlayerB(), request.getDeckB());

        Player playerA = new Player(resolvePlayerName(request.getPlayerA(), "Player A"), deckA);
        Player playerB = new Player(resolvePlayerName(request.getPlayerB(), "Player B"), deckB);

        effectiveCheckpoint.run();
        List<String> matchLog = matchEngine.runMatch(playerA, playerB, effectiveCheckpoint);
        effectiveCheckpoint.run();

        List<String> battleLog = matchLog != null ? new ArrayList<>(matchLog) : Collections.emptyList();

        String outcome;
        String winnerUserId;
        String winnerUsername;
        if (playerA.getRoundsWon() > playerB.getRoundsWon()) {
            outcome = "PLAYER_A_WIN";
            winnerUserId = trimToNull(request.getPlayerA().getUserId());
            winnerUsername = trimToNull(request.getPlayerA().getUsername());
        } else if (playerB.getRoundsWon() > playerA.getRoundsWon()) {
            outcome = "PLAYER_B_WIN";
            winnerUserId = trimToNull(request.getPlayerB().getUserId());
            winnerUsername = trimToNull(request.getPlayerB().getUsername());
        } else {
            outcome = "DRAW";
            winnerUserId = null;
            winnerUsername = null;
        }

        return PvpBattleResponse.builder()
                .mode(mode)
                .outcome(outcome)
                .winnerUserId(winnerUserId)
                .winnerUsername(winnerUsername)
                .battleLog(battleLog)
                .build();
    }

    private String validateAndResolveMode(String rawMode) {
        if (rawMode == null) {
            return MODE_RANKED;
        }
        String normalized = rawMode.trim().toUpperCase(Locale.ROOT);
        if (!MODE_RANKED.equals(normalized) && !MODE_FRIENDLY.equals(normalized)) {
            throw new IllegalArgumentException(
                    "Invalid PvP battle mode '" + rawMode + "'. Allowed values are RANKED or FRIENDLY");
        }
        return normalized;
    }

    private void validateDistinctPlayers(ChallengerInfoRequest playerA, ChallengerInfoRequest playerB) {
        String userIdA = trimToNull(playerA.getUserId());
        String userIdB = trimToNull(playerB.getUserId());
        if (userIdA != null && userIdA.equals(userIdB)) {
            throw new IllegalArgumentException("Self-challenge is not allowed: playerA and playerB share userId '"
                    + userIdA + "'");
        }
    }

    private String resolvePlayerName(ChallengerInfoRequest player, String fallback) {
        if (player != null) {
            String displayName = trimToNull(player.getDisplayName());
            if (displayName != null) {
                return displayName;
            }
            String username = trimToNull(player.getUsername());
            if (username != null) {
                return username;
            }
            String userId = trimToNull(player.getUserId());
            if (userId != null) {
                return userId;
            }
        }
        return fallback;
    }

    private String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
