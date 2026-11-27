package webapp.pvp;

import java.util.List;
import tools.jackson.databind.JsonNode;

/** Input command used to persist a completed battle in {@code battle_history}. */
public record NewBattleHistoryEntry(
        String mode,
        int playerAId,
        Integer playerBId,
        Integer winnerId,
        String outcome,
        JsonNode deckASnapshot,
        JsonNode deckBSnapshot,
        List<String> battleLog,
        int rankingDelta) {
    public NewBattleHistoryEntry {
        battleLog = battleLog == null ? List.of() : List.copyOf(battleLog);
    }
}
