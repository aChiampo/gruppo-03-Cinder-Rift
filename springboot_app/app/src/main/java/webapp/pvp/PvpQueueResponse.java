package webapp.pvp;

import com.fasterxml.jackson.annotation.JsonInclude;

/** Public ranked queue status payload and optional completed battle report. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record PvpQueueResponse(
        int ticketId,
        String status,
        BattleHistoryEntry battle) {
}
