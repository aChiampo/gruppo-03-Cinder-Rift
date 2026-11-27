package webapp.pvp;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/** Persists and queries {@code battle_history} rows using server-owned JSONB snapshots. */
@Repository
public class BattleHistoryRepository {
    private static final String SELECT_COLUMNS = """
            id, mode, player_a_id, player_b_id, winner_id, outcome,
            deck_a_snapshot::text AS deck_a_snapshot,
            deck_b_snapshot::text AS deck_b_snapshot,
            battle_log::text AS battle_log,
            ranking_delta, created_at
            """;

    private final JdbcTemplate jdbc;
    private final JsonMapper json;

    public BattleHistoryRepository(JdbcTemplate jdbc, JsonMapper json) {
        this.jdbc = jdbc;
        this.json = json;
    }

    /** Inserts one battle history record and returns the persisted row with generated fields. */
    public BattleHistoryEntry save(NewBattleHistoryEntry entry) {
        String deckAJson = json.writeValueAsString(entry.deckASnapshot());
        String deckBJson = entry.deckBSnapshot() == null ? null : json.writeValueAsString(entry.deckBSnapshot());
        String battleLogJson = json.writeValueAsString(entry.battleLog());

        String sql = """
                INSERT INTO battle_history (
                    mode, player_a_id, player_b_id, winner_id, outcome,
                    deck_a_snapshot, deck_b_snapshot, battle_log, ranking_delta
                )
                VALUES (?, ?, ?, ?, ?, CAST(? AS jsonb), CAST(? AS jsonb), CAST(? AS jsonb), ?)
                RETURNING """ + " " + SELECT_COLUMNS;

        return jdbc.queryForObject(
                sql,
                (rs, rowNum) -> mapRow(rs),
                entry.mode(),
                entry.playerAId(),
                entry.playerBId(),
                entry.winnerId(),
                entry.outcome(),
                deckAJson,
                deckBJson,
                battleLogJson,
                entry.rankingDelta());
    }

    /** Returns one persisted battle history record by id, if present. */
    public Optional<BattleHistoryEntry> findById(int id) {
        String sql = "SELECT " + SELECT_COLUMNS + " FROM battle_history WHERE id = ?";
        return jdbc.query(sql, (rs, rowNum) -> mapRow(rs), id).stream().findFirst();
    }

    /** Returns all battles where the user participated as player A or player B, newest first. */
    public List<BattleHistoryEntry> findByUserId(int userId, String mode) {
        if (mode == null) {
            String sql = "SELECT " + SELECT_COLUMNS
                    + " FROM battle_history WHERE player_a_id = ? OR player_b_id = ?"
                    + " ORDER BY created_at DESC, id DESC";
            return jdbc.query(sql, (rs, rowNum) -> mapRow(rs), userId, userId);
        }
        String sql = "SELECT " + SELECT_COLUMNS
                + " FROM battle_history WHERE (player_a_id = ? OR player_b_id = ?) AND UPPER(mode) = UPPER(?)"
                + " ORDER BY created_at DESC, id DESC";
        return jdbc.query(
                sql,
                new Object[] { userId, userId, mode },
                new int[] { Types.INTEGER, Types.INTEGER, Types.VARCHAR },
                (rs, rowNum) -> mapRow(rs));
    }

    private BattleHistoryEntry mapRow(ResultSet rs) throws SQLException {
        int id = rs.getInt("id");
        String mode = rs.getString("mode");
        int playerAId = rs.getInt("player_a_id");
        Integer playerBId = rs.getObject("player_b_id", Integer.class);
        Integer winnerId = rs.getObject("winner_id", Integer.class);
        String outcome = rs.getString("outcome");
        JsonNode deckASnapshot = parseJson(rs.getString("deck_a_snapshot"));
        JsonNode deckBSnapshot = parseJson(rs.getString("deck_b_snapshot"));
        List<String> battleLog = parseLog(rs.getString("battle_log"));
        int rankingDelta = rs.getInt("ranking_delta");
        OffsetDateTime createdAt = rs.getObject("created_at", OffsetDateTime.class);
        return new BattleHistoryEntry(
                id,
                mode,
                playerAId,
                playerBId,
                winnerId,
                outcome,
                deckASnapshot,
                deckBSnapshot,
                battleLog,
                rankingDelta,
                createdAt.toInstant());
    }

    private JsonNode parseJson(String raw) {
        if (raw == null) return null;
        return json.readTree(raw);
    }

    private List<String> parseLog(String raw) {
        if (raw == null) return List.of();
        JsonNode node = json.readTree(raw);
        if (!node.isArray()) return List.of();
        List<String> lines = new ArrayList<>(node.size());
        for (JsonNode entry : node) {
            lines.add(entry.asString());
        }
        return lines;
    }
}
