package webapp.pvp;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;

/** Public representation of a direct friend PvP challenge and optional completed battle report. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record PvpChallengeResponse(
        int id,
        int challengerId,
        String challengerNickname,
        int opponentId,
        String opponentNickname,
        int challengerDeckId,
        Integer opponentDeckId,
        String status,
        Integer battleHistoryId,
        Instant createdAt,
        Instant respondedAt,
        BattleHistoryEntry battle) {
}
