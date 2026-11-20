package webapp.battle;

import java.util.List;

/** Public response deliberately omits the internal tutorial field. */
public record BattleResponse(String stageId, int stageNumber, String stageTitle,
        EngineBattleRequest.Challenger challenger, String enemyName, String outcome,
        String winner, List<String> battleLog) {
    public BattleResponse { battleLog = List.copyOf(battleLog); }
}
