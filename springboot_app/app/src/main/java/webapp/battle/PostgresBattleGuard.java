package webapp.battle;

import java.sql.Connection;
import java.sql.SQLException;
import javax.sql.DataSource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import java.util.Map;

/** L2 session locks reserve one connection, separate from the short snapshot transaction. */
@Component
public class PostgresBattleGuard {
    // Two-key locks have a separate namespace from PostgreSQL's single-bigint locks.
    static final int DOMAIN = 0x463037;
    private final DataSource dataSource;

    public PostgresBattleGuard(DataSource dataSource) { this.dataSource = dataSource; }

    public Handle acquire(int userId, BattleBudget budget) {
        Connection connection = null;
        try {
            budget.remaining();
            connection = dataSource.getConnection();
            connection.setAutoCommit(true);
            boolean acquired;
            try (var statement = connection.prepareStatement("SELECT pg_try_advisory_lock(?, ?)")) {
                statement.setQueryTimeout((int) Math.max(1, budget.remaining().toSeconds()));
                statement.setInt(1, DOMAIN);
                statement.setInt(2, userId);
                try (var result = statement.executeQuery()) {
                    result.next();
                    acquired = result.getBoolean(1);
                }
            }
            if (!acquired) {
                connection.close();
                connection = null;
                throw new BattleFailure(HttpStatus.CONFLICT, "Conflict",
                        "A battle request is already in progress for this user.",
                        Map.of("code", "BATTLE_IN_PROGRESS"));
            }
            return new Handle(connection, userId);
        } catch (SQLException | RuntimeException failure) {
            // An ambiguous acquisition must never return a potentially locked session to the pool.
            discard(connection);
            if (failure instanceof BattleFailure expected) throw expected;
            throw new IllegalStateException("Could not acquire the battle guard", failure);
        }
    }

    private static void discard(Connection connection) {
        if (connection == null) return;
        try { connection.abort(Runnable::run); } catch (SQLException ignored) { }
        try { connection.close(); } catch (SQLException ignored) { }
    }

    public static final class Handle implements AutoCloseable {
        private Connection connection;
        private final int userId;

        private Handle(Connection connection, int userId) { this.connection = connection; this.userId = userId; }

        @Override
        public synchronized void close() {
            if (connection == null) return;
            Connection owned = connection;
            connection = null;
            try {
                try (var statement = owned.prepareStatement("SELECT pg_advisory_unlock(?, ?)")) {
                    statement.setQueryTimeout(1);
                    statement.setInt(1, DOMAIN);
                    statement.setInt(2, userId);
                    try (var result = statement.executeQuery()) {
                        if (!result.next() || !result.getBoolean(1)) throw new SQLException("Battle guard ownership lost");
                    }
                }
                owned.close();
            } catch (SQLException failure) {
                discard(owned);
            }
        }
    }
}
