package webapp.pvp;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import tools.jackson.databind.json.JsonMapper;
import webapp.battle.EngineBattleRequest;

/** Persists and queries direct friendly PvP challenges in {@code pvp_challenges}. */
@Repository
public class PvpChallengeRepository {
    private static final String SELECT_COLUMNS = """
            SELECT c.id, c.challenger_id, cu.nickname AS challenger_nickname,
                   c.opponent_id, ou.nickname AS opponent_nickname,
                   c.challenger_deck_id, c.opponent_deck_id, c.status,
                   c.battle_history_id, c.created_at, c.responded_at
            FROM pvp_challenges c
            JOIN users cu ON cu.id = c.challenger_id
            JOIN users ou ON ou.id = c.opponent_id
            """;

    private final JdbcClient jdbc;
    private final JsonMapper json;

    public PvpChallengeRepository(JdbcClient jdbc, JsonMapper json) {
        this.jdbc = jdbc;
        this.json = json;
    }

    /** Inserts a new PENDING friendly challenge with the challenger's snapshotted deck. */
    public int insertPending(int challengerId, int opponentId, int challengerDeckId, EngineBattleRequest.Deck deck) {
        String snapshotJson = json.writeValueAsString(deck);
        return jdbc.sql("""
                INSERT INTO pvp_challenges (
                    challenger_id, opponent_id, challenger_deck_id, challenger_deck_snapshot, status
                )
                VALUES (:challengerId, :opponentId, :challengerDeckId, CAST(:deckSnapshot AS jsonb), 'PENDING')
                RETURNING id
                """)
                .param("challengerId", challengerId)
                .param("opponentId", opponentId)
                .param("challengerDeckId", challengerDeckId)
                .param("deckSnapshot", snapshotJson)
                .query(Integer.class)
                .single();
    }

    /** Loads one challenge by id without battle payload. */
    public Optional<PvpChallengeResponse> findById(int challengeId) {
        return jdbc.sql(SELECT_COLUMNS + " WHERE c.id = :id")
                .param("id", challengeId)
                .query((rs, rowNum) -> mapChallenge(rs))
                .optional();
    }

    /** Lists PENDING incoming challenges addressed to the given user. */
    public List<PvpChallengeResponse> findIncomingPending(int opponentId) {
        return jdbc.sql(SELECT_COLUMNS + """
                WHERE c.opponent_id = :userId
                  AND c.status = 'PENDING'
                  AND NOT cu.is_deleted
                  AND NOT ou.is_deleted
                ORDER BY c.created_at DESC, c.id DESC
                """)
                .param("userId", opponentId)
                .query((rs, rowNum) -> mapChallenge(rs))
                .list();
    }

    /** Lists PENDING outgoing challenges created by the given user. */
    public List<PvpChallengeResponse> findOutgoingPending(int challengerId) {
        return jdbc.sql(SELECT_COLUMNS + """
                WHERE c.challenger_id = :userId
                  AND c.status = 'PENDING'
                  AND NOT cu.is_deleted
                  AND NOT ou.is_deleted
                ORDER BY c.created_at DESC, c.id DESC
                """)
                .param("userId", challengerId)
                .query((rs, rowNum) -> mapChallenge(rs))
                .list();
    }

    /** Loads a challenge along with the challenger's snapshotted deck for battle execution. */
    public Optional<ChallengeExecutionData> findForExecution(int challengeId) {
        return jdbc.sql("""
                SELECT c.id, c.challenger_id, cu.nickname AS challenger_nickname, cu.is_deleted AS challenger_deleted,
                       c.opponent_id, c.status, c.challenger_deck_snapshot::text AS challenger_deck_snapshot
                FROM pvp_challenges c
                JOIN users cu ON cu.id = c.challenger_id
                WHERE c.id = :id
                """)
                .param("id", challengeId)
                .query((rs, rowNum) -> new ChallengeExecutionData(
                        rs.getInt("id"),
                        rs.getInt("challenger_id"),
                        rs.getString("challenger_nickname"),
                        rs.getBoolean("challenger_deleted"),
                        rs.getInt("opponent_id"),
                        rs.getString("status"),
                        json.readValue(rs.getString("challenger_deck_snapshot"), EngineBattleRequest.Deck.class)))
                .optional();
    }

    /** Locks a challenge row during completion and returns its current status. */
    public Optional<String> lockStatus(int challengeId) {
        return jdbc.sql("SELECT status FROM pvp_challenges WHERE id = :id FOR UPDATE")
                .param("id", challengeId)
                .query(String.class)
                .optional();
    }

    /** Transitions a PENDING challenge to COMPLETED and links the persisted battle history record. */
    public boolean markCompleted(int challengeId, int opponentId, int opponentDeckId, int battleHistoryId) {
        return jdbc.sql("""
                UPDATE pvp_challenges
                SET status = 'COMPLETED',
                    opponent_deck_id = :opponentDeckId,
                    battle_history_id = :battleHistoryId,
                    responded_at = now()
                WHERE id = :id
                  AND opponent_id = :opponentId
                  AND status = 'PENDING'
                """)
                .param("id", challengeId)
                .param("opponentId", opponentId)
                .param("opponentDeckId", opponentDeckId)
                .param("battleHistoryId", battleHistoryId)
                .update() == 1;
    }

    /** Atomically transitions a PENDING challenge to DECLINED (opponent) or CANCELLED (challenger). */
    public boolean markDeclinedOrCancelled(int challengeId, int actorUserId, String newStatus) {
        return jdbc.sql("""
                UPDATE pvp_challenges
                SET status = :newStatus,
                    responded_at = now()
                WHERE id = :id
                  AND status = 'PENDING'
                  AND (
                      (:newStatus = 'DECLINED' AND opponent_id = :actorUserId)
                      OR (:newStatus = 'CANCELLED' AND challenger_id = :actorUserId)
                  )
                """)
                .param("id", challengeId)
                .param("actorUserId", actorUserId)
                .param("newStatus", newStatus)
                .update() == 1;
    }

    private static PvpChallengeResponse mapChallenge(ResultSet rs) throws SQLException {
        return new PvpChallengeResponse(
                rs.getInt("id"),
                rs.getInt("challenger_id"),
                rs.getString("challenger_nickname"),
                rs.getInt("opponent_id"),
                rs.getString("opponent_nickname"),
                rs.getInt("challenger_deck_id"),
                rs.getObject("opponent_deck_id", Integer.class),
                rs.getString("status"),
                rs.getObject("battle_history_id", Integer.class),
                instant(rs, "created_at"),
                instant(rs, "responded_at"),
                null);
    }

    private static Instant instant(ResultSet rs, String column) throws SQLException {
        OffsetDateTime value = rs.getObject(column, OffsetDateTime.class);
        return value == null ? null : value.toInstant();
    }

    public record ChallengeExecutionData(
            int id,
            int challengerId,
            String challengerNickname,
            boolean challengerDeleted,
            int opponentId,
            String status,
            EngineBattleRequest.Deck challengerDeck) {
    }
}
