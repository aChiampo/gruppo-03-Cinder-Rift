package webapp.auth;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/** Uses v1.1 database defaults and constraints for account, inventory, and session persistence. */
@Repository
class AccountRepository {
    private static final String PROFILE_COLUMNS = "u.id, u.nickname, u.email, r.code AS role, u.ranking_points, u.url_user_icon";
    // Share the role allowlist across password login, refresh rotation, and bearer authentication.
    private static final String ACTIVE_ACCOUNTS = """
            FROM users u JOIN roles r ON r.id = u.id_role
            WHERE NOT u.is_deleted AND r.code IN ('USER', 'ADMIN')
            """;
    private static final String SELECT_PROFILE = "SELECT " + PROFILE_COLUMNS + " " + ACTIVE_ACCOUNTS;

    private final JdbcClient jdbc;

    AccountRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    /**
     * Looks up an active account for password verification.
     *
     * @param email normalized email address
     * @return the account and its password hash, or empty if the account cannot sign in
     */
    Optional<Account> findByEmail(String email) {
        return jdbc.sql("SELECT " + PROFILE_COLUMNS + ", u.hash_password " + ACTIVE_ACCOUNTS + " AND u.email = :email")
                .param("email", email)
                .query(AccountRepository::mapAccount)
                .optional();
    }

    /**
     * Takes a write lock on an active user's row for the caller's refresh transaction.
     *
     * @param userId the user that owns the refresh token
     * @return the current profile, or empty if the account is inactive or missing
     */
    Optional<UserProfile> lockActiveProfile(int userId) {
        return jdbc.sql(SELECT_PROFILE + " AND u.id = :userId FOR UPDATE OF u")
                .param("userId", userId)
                .query((row, rowNumber) -> mapProfile(row))
                .optional();
    }

    /**
     * Checks that the session belongs to an active account and can still authenticate requests.
     *
     * @param userId account ID from the validated JWT subject
     * @param sessionId refresh-token row ID from the validated JWT
     * @param now exclusive expiration cutoff
     * @return the current profile and role, or empty if the account or session is unusable
     */
    Optional<UserProfile> findActiveSessionProfile(int userId, int sessionId, Instant now) {
        return jdbc.sql(SELECT_PROFILE + """
                 AND u.id = :userId AND EXISTS (
                    SELECT 1 FROM refresh_tokens t
                    WHERE t.id = :sessionId AND t.id_user = u.id
                      AND t.revoked_at IS NULL AND t.expires_at > :now
                 )
                """)
                .param("userId", userId)
                .param("sessionId", sessionId)
                .param("now", utc(now))
                .query((row, rowNumber) -> mapProfile(row))
                .optional();
    }

    /**
     * Inserts a player with the role designated by {@code ROLES.code = 'USER'}.
     *
     * @param nickname trimmed display name
     * @param email normalized email address
     * @param passwordHash encoded password for persistence
     * @return the new profile, or empty if the insert conflicts with an existing row
     * @throws IllegalStateException if the database has no {@code USER} role
     */
    Optional<UserProfile> insertPlayer(String nickname, String email, String passwordHash) {
        int roleId = jdbc.sql("SELECT id FROM roles WHERE code = 'USER'")
                .query(Integer.class)
                .optional()
                .orElseThrow(() -> new IllegalStateException("The USER role is missing"));

        // Let the unique constraints arbitrate concurrent registrations in the insert itself.
        return jdbc.sql("""
                INSERT INTO users (nickname, email, hash_password, id_role)
                VALUES (:nickname, :email, :passwordHash, :roleId)
                ON CONFLICT DO NOTHING
                RETURNING id, nickname, email, 'USER' AS role, ranking_points, url_user_icon
                """)
                .param("nickname", nickname)
                .param("email", email)
                .param("passwordHash", passwordHash)
                .param("roleId", roleId)
                .query((row, rowNumber) -> mapProfile(row))
                .optional();
    }

    /**
     * Creates the player's inventory using the balance defaults defined by v1.1.
     *
     * @param userId the account being created in the same transaction
     */
    void createInventory(int userId) {
        jdbc.sql("INSERT INTO inventory (id_user) VALUES (:userId)")
                .param("userId", userId)
                .update();
    }

    /**
     * Stores the digest of a newly issued refresh token and returns the session reference for the JWT.
     *
     * @param userId the owning account
     * @param tokenHash the SHA-256 digest of the refresh token in hexadecimal
     * @param now the issuance time
     * @param expiresAt the exclusive expiration time
     * @return the generated refresh-token row ID
     */
    int insertRefreshToken(int userId, String tokenHash, Instant now, Instant expiresAt) {
        return jdbc.sql("""
                INSERT INTO refresh_tokens (id_user, token_hash, created_at, expires_at)
                VALUES (:userId, :tokenHash, :now, :expiresAt)
                RETURNING id
                """)
                .param("userId", userId)
                .param("tokenHash", tokenHash)
                .param("now", utc(now))
                .param("expiresAt", utc(expiresAt))
                .query(Integer.class)
                .single();
    }

    /**
     * Locates the account to lock before checking whether the token can be consumed.
     *
     * @param tokenHash the SHA-256 digest of the presented token in hexadecimal
     * @return the owner ID if the digest is known, regardless of expiration or revocation
     */
    Optional<Integer> findRefreshTokenOwner(String tokenHash) {
        return jdbc.sql("SELECT id_user FROM refresh_tokens WHERE token_hash = :tokenHash")
                .param("tokenHash", tokenHash)
                .query(Integer.class)
                .optional();
    }

    /**
     * Marks an unexpired, unrevoked token as revoked in a single conditional update.
     * Only one concurrent update of the same token can succeed.
     *
     * @param tokenHash the SHA-256 digest of the presented token in hexadecimal
     * @param now the revocation time and exclusive expiration cutoff
     * @return whether the update revoked a usable token
     */
    boolean revokeRefreshToken(String tokenHash, Instant now) {
        return jdbc.sql("""
                UPDATE refresh_tokens SET revoked_at = :now
                WHERE token_hash = :tokenHash AND revoked_at IS NULL AND expires_at > :now
                """)
                .param("tokenHash", tokenHash)
                .param("now", utc(now))
                .update() == 1;
    }

    /**
     * Deletes unusable sessions without retaining expired or revoked token records.
     *
     * @param now inclusive expiration cutoff, shared by the entire cleanup run
     * @return the number of deleted sessions
     */
    int deleteExpiredOrRevokedRefreshTokens(Instant now) {
        return jdbc.sql("""
                DELETE FROM refresh_tokens
                WHERE expires_at <= :now OR revoked_at IS NOT NULL
                """)
                .param("now", utc(now))
                .update();
    }

    private static Account mapAccount(ResultSet row, int rowNumber) throws SQLException {
        return new Account(mapProfile(row), row.getString("hash_password"));
    }

    private static UserProfile mapProfile(ResultSet row) throws SQLException {
        return new UserProfile(row.getInt("id"), row.getString("nickname"), row.getString("email"),
                row.getString("role"), row.getInt("ranking_points"), row.getString("url_user_icon"));
    }

    private static OffsetDateTime utc(Instant instant) {
        return instant.atOffset(ZoneOffset.UTC);
    }

    record Account(UserProfile profile, String passwordHash) {
        @Override
        public String toString() {
            return "Account[id=" + profile.id() + "]";
        }
    }
}
