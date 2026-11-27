package webapp.pvp;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.Optional;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import tools.jackson.databind.json.JsonMapper;
import webapp.battle.EngineBattleRequest;

/** Persists and locks ranked matchmaking tickets in {@code pvp_matchmaking_queue}. */
@Repository
public class PvpQueueRepository {
    private final JdbcClient jdbc;
    private final JsonMapper json;

    public PvpQueueRepository(JdbcClient jdbc, JsonMapper json) {
        this.jdbc = jdbc;
        this.json = json;
    }

    /** Checks whether the user currently holds an active WAITING or MATCHING ticket. */
    public boolean hasActiveTicket(int userId) {
        return Boolean.TRUE.equals(jdbc.sql("""
                SELECT EXISTS(
                    SELECT 1 FROM pvp_matchmaking_queue
                    WHERE user_id = :userId AND status IN ('WAITING', 'MATCHING')
                )
                """)
                .param("userId", userId)
                .query(Boolean.class)
                .single());
    }

    /** Finds and locks the oldest WAITING ticket belonging to another active user. */
    public Optional<LockedOpponent> lockOldestWaitingOpponent(int excludeUserId) {
        return jdbc.sql("""
                SELECT q.id, q.user_id, u.nickname, q.deck_snapshot::text AS deck_snapshot
                FROM pvp_matchmaking_queue q
                JOIN users u ON u.id = q.user_id
                WHERE q.status = 'WAITING'
                  AND q.user_id <> :excludeUserId
                  AND u.is_deleted = false
                ORDER BY q.created_at ASC, q.id ASC
                FOR UPDATE OF q SKIP LOCKED
                LIMIT 1
                """)
                .param("excludeUserId", excludeUserId)
                .query((rs, rowNum) -> {
                    int opponentUserId = rs.getInt("user_id");
                    String nickname = rs.getString("nickname");
                    EngineBattleRequest.Deck deck = json.readValue(
                            rs.getString("deck_snapshot"), EngineBattleRequest.Deck.class);
                    var challenger = new EngineBattleRequest.Challenger(
                            Integer.toString(opponentUserId), nickname, nickname);
                    return new LockedOpponent(rs.getInt("id"), opponentUserId, challenger, deck);
                })
                .optional();
    }

    /** Inserts a new queue ticket with the given initial status and returns its id. */
    public int insertTicket(int userId, int deckId, EngineBattleRequest.Deck deckSnapshot, String status) {
        String snapshotJson = json.writeValueAsString(deckSnapshot);
        return jdbc.sql("""
                INSERT INTO pvp_matchmaking_queue (user_id, deck_id, deck_snapshot, status)
                VALUES (:userId, :deckId, CAST(:deckSnapshot AS jsonb), :status)
                RETURNING id
                """)
                .param("userId", userId)
                .param("deckId", deckId)
                .param("deckSnapshot", snapshotJson)
                .param("status", status)
                .query(Integer.class)
                .single();
    }

    /** Transitions a WAITING ticket to MATCHING. */
    public boolean markMatching(int ticketId) {
        return jdbc.sql("""
                UPDATE pvp_matchmaking_queue
                SET status = 'MATCHING', updated_at = now()
                WHERE id = :id AND status = 'WAITING'
                """)
                .param("id", ticketId)
                .update() == 1;
    }

    /** Transitions both MATCHING tickets to MATCHED and links the persisted battle history row. */
    public int markMatched(int ticketIdA, int ticketIdB, int battleHistoryId) {
        return jdbc.sql("""
                UPDATE pvp_matchmaking_queue
                SET status = 'MATCHED', battle_history_id = :battleHistoryId, updated_at = now()
                WHERE id IN (:idA, :idB) AND status = 'MATCHING'
                """)
                .param("battleHistoryId", battleHistoryId)
                .param("idA", ticketIdA)
                .param("idB", ticketIdB)
                .update();
    }

    /** Restores the opponent's MATCHING ticket to WAITING and cancels the caller's ticket. */
    public void revertFailedMatch(int opponentTicketId, int selfTicketId) {
        jdbc.sql("""
                UPDATE pvp_matchmaking_queue
                SET status = 'WAITING', updated_at = now()
                WHERE id = :id AND status = 'MATCHING'
                """)
                .param("id", opponentTicketId)
                .update();
        jdbc.sql("""
                UPDATE pvp_matchmaking_queue
                SET status = 'CANCELLED', updated_at = now()
                WHERE id = :id AND status = 'MATCHING'
                """)
                .param("id", selfTicketId)
                .update();
    }

    /** Loads one queue ticket by id. */
    public Optional<PvpQueueTicket> findById(int ticketId) {
        return jdbc.sql("""
                SELECT id, user_id, deck_id, status, battle_history_id, created_at, updated_at
                FROM pvp_matchmaking_queue
                WHERE id = :id
                """)
                .param("id", ticketId)
                .query((rs, rowNum) -> mapTicket(rs))
                .optional();
    }

    /** Cancels an owned WAITING ticket atomically. */
    public boolean cancelWaitingTicket(int ticketId, int userId) {
        return jdbc.sql("""
                UPDATE pvp_matchmaking_queue
                SET status = 'CANCELLED', updated_at = now()
                WHERE id = :id AND user_id = :userId AND status = 'WAITING'
                """)
                .param("id", ticketId)
                .param("userId", userId)
                .update() == 1;
    }

    private static PvpQueueTicket mapTicket(ResultSet rs) throws SQLException {
        return new PvpQueueTicket(
                rs.getInt("id"),
                rs.getInt("user_id"),
                rs.getInt("deck_id"),
                rs.getString("status"),
                rs.getObject("battle_history_id", Integer.class),
                rs.getObject("created_at", OffsetDateTime.class).toInstant(),
                rs.getObject("updated_at", OffsetDateTime.class).toInstant());
    }

    public record LockedOpponent(
            int ticketId,
            int userId,
            EngineBattleRequest.Challenger challenger,
            EngineBattleRequest.Deck deck) {
    }
}
