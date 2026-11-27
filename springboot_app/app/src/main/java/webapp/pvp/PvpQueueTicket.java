package webapp.pvp;

import java.time.Instant;

/** Internal representation of a row in {@code pvp_matchmaking_queue}. */
public record PvpQueueTicket(
        int id,
        int userId,
        int deckId,
        String status,
        Integer battleHistoryId,
        Instant createdAt,
        Instant updatedAt) {
}
