package webapp.friendship;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/** Persists ordered pairs; stable user locks coordinate writes with F01 deletion. */
@Repository
class FriendshipRepository {
    private final JdbcClient jdbc;

    FriendshipRepository(JdbcClient jdbc) { this.jdbc = jdbc; }

    List<Person> lockUsers(int actor, int other) {
        return jdbc.sql("""
                SELECT id, nickname, url_user_icon, is_deleted FROM users
                WHERE id IN (:actor, :other) ORDER BY id FOR UPDATE
                """).param("actor", actor).param("other", other)
                .query((rs, n) -> person(rs)).list();
    }

    Optional<Person> findUser(int id) {
        return jdbc.sql("SELECT id, nickname, url_user_icon, is_deleted FROM users WHERE id = :id")
                .param("id", id).query((rs, n) -> person(rs)).optional();
    }

    Optional<Pair> findPair(int actor, int other) {
        return jdbc.sql("""
                SELECT requested_by, status, created_at, accepted_at FROM friendships
                WHERE user_a_id = :a AND user_b_id = :b
                """).param("a", Math.min(actor, other)).param("b", Math.max(actor, other))
                .query((rs, n) -> new Pair(rs.getInt("requested_by"), rs.getString("status"),
                        instant(rs, "created_at"), instant(rs, "accepted_at"))).optional();
    }

    int insert(int actor, int other) {
        return jdbc.sql("""
                INSERT INTO friendships (user_a_id, user_b_id, requested_by, status)
                VALUES (:a, :b, :actor, 'pending') ON CONFLICT (user_a_id, user_b_id) DO NOTHING
                """).param("a", Math.min(actor, other)).param("b", Math.max(actor, other))
                .param("actor", actor).update();
    }

    int accept(int actor, int other) {
        return jdbc.sql("""
                UPDATE friendships SET status = 'accepted', accepted_at = now()
                WHERE user_a_id = :a AND user_b_id = :b AND status = 'pending' AND requested_by = :other
                """).param("a", Math.min(actor, other)).param("b", Math.max(actor, other))
                .param("other", other).update();
    }

    int delete(int actor, int other, String status) {
        return jdbc.sql("""
                DELETE FROM friendships WHERE user_a_id = :a AND user_b_id = :b AND status = :status
                """).param("a", Math.min(actor, other)).param("b", Math.max(actor, other))
                .param("status", status).update();
    }

    List<FriendshipView> list(int actor, String direction) {
        boolean friends = direction.equals("friends");
        String predicate = friends ? "f.status = 'accepted'"
                : "f.status = 'pending' AND f.requested_by " + (direction.equals("outgoing") ? "=" : "<>") + " :actor";
        String timestamp = friends ? "f.accepted_at" : "f.created_at";
        // Only fixed server-side fragments are composed; client values remain bound parameters.
        return jdbc.sql("""
                SELECT u.id, u.nickname, u.url_user_icon, f.created_at, f.accepted_at
                FROM friendships f JOIN users u
                  ON u.id = CASE WHEN f.user_a_id = :actor THEN f.user_b_id ELSE f.user_a_id END
                WHERE (f.user_a_id = :actor OR f.user_b_id = :actor) AND NOT u.is_deleted AND
                """ + predicate + " ORDER BY " + timestamp + " DESC, u.id ASC")
                .param("actor", actor).query((rs, n) -> new FriendshipView(rs.getInt("id"),
                        rs.getString("nickname"), rs.getString("url_user_icon"), direction,
                        instant(rs, "created_at"), instant(rs, "accepted_at"))).list();
    }

    private static Person person(ResultSet rs) throws SQLException {
        return new Person(rs.getInt("id"), rs.getString("nickname"), rs.getString("url_user_icon"),
                rs.getBoolean("is_deleted"));
    }

    private static Instant instant(ResultSet rs, String column) throws SQLException {
        OffsetDateTime value = rs.getObject(column, OffsetDateTime.class);
        return value == null ? null : value.toInstant();
    }

    record Person(int id, String nickname, String iconUrl, boolean deleted) { }
    record Pair(int requester, String status, Instant createdAt, Instant acceptedAt) { }
}
