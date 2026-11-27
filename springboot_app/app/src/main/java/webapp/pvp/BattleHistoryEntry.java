package webapp.pvp;

import java.time.Instant;
import java.util.List;
import tools.jackson.databind.JsonNode;

/** Public representation of one persisted battle history record. */
public record BattleHistoryEntry(
        int id,
        String mode,
        int playerAId,
        Integer playerBId,
        Integer winnerId,
        String outcome,
        JsonNode deckASnapshot,
        JsonNode deckBSnapshot,
        List<String> battleLog,
        int rankingDelta,
        Instant createdAt) {
    public BattleHistoryEntry {
        battleLog = battleLog == null ? List.of() : List.copyOf(battleLog);
    }
}
