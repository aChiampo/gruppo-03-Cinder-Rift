package webapp.battle;

import java.util.List;

/** Internal PvP engine response payload parsed from the game engine. */
public record EnginePvpBattleResponse(
        String mode,
        String outcome,
        String winnerUserId,
        String winnerUsername,
        List<String> battleLog) {
    public EnginePvpBattleResponse {
        battleLog = battleLog == null ? List.of() : List.copyOf(battleLog);
    }
}
