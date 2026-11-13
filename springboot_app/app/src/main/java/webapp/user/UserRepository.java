package webapp.user;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.Optional;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import webapp.auth.UserProfile;

/** Reads and updates user profile data using the existing users schema. */
@Repository
class UserRepository {
    private static final String ACTIVE_PROFILE = """
            SELECT u.id, u.nickname, u.email, r.code AS role, u.ranking_points, u.url_user_icon
            FROM users u
            JOIN roles r ON r.id = u.id_role
            WHERE u.id = :userId AND NOT u.is_deleted AND r.code IN ('USER', 'ADMIN')
            """;

    private final JdbcClient jdbc;

    UserRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    Optional<PublicUserProfile> findPublicProfile(int userId) {
        return jdbc.sql("""
                SELECT id, nickname, ranking_points, url_user_icon, registration_date
                FROM users
                WHERE id = :userId AND NOT is_deleted
                """)
                .param("userId", userId)
                .query(UserRepository::mapPublicProfile)
                .optional();
    }

    Optional<UserProfile> lockActiveProfile(int userId) {
        Optional<Integer> lockedUser = jdbc.sql("""
                SELECT id
                FROM users
                WHERE id = :userId AND NOT is_deleted
                FOR UPDATE
                """)
                .param("userId", userId)
                .query(Integer.class)
                .optional();
        if (lockedUser.isEmpty()) {
            return Optional.empty();
        }
        return jdbc.sql(ACTIVE_PROFILE + " AND u.id = :userId")
                .param("userId", userId)
                .query((row, rowNumber) -> mapUserProfile(row))
                .optional();
    }

    void updateProfile(int userId, String nickname, String email, String iconUrl) {
        int updated = jdbc.sql("""
                UPDATE users
                SET nickname = :nickname, email = :email, url_user_icon = :iconUrl
                WHERE id = :userId AND NOT is_deleted
                """)
                .param("userId", userId)
                .param("nickname", nickname)
                .param("email", email)
                .param("iconUrl", iconUrl)
                .update();
        if (updated != 1) {
            throw new IllegalStateException("The locked active user is missing");
        }
    }

    private static PublicUserProfile mapPublicProfile(ResultSet row, int rowNumber) throws SQLException {
        OffsetDateTime registrationDate = row.getObject("registration_date", OffsetDateTime.class);
        return new PublicUserProfile(row.getInt("id"), row.getString("nickname"),
                row.getInt("ranking_points"), row.getString("url_user_icon"), registrationDate.toInstant());
    }

    private static UserProfile mapUserProfile(ResultSet row) throws SQLException {
        return new UserProfile(row.getInt("id"), row.getString("nickname"), row.getString("email"),
                row.getString("role"), row.getInt("ranking_points"), row.getString("url_user_icon"));
    }
}
