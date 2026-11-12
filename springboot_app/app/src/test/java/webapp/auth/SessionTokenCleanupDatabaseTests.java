package webapp.auth;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import javax.sql.DataSource;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = "app.security.session-token-cleanup-cron=-")
class SessionTokenCleanupDatabaseTests {
    @Autowired
    private DataSource dataSource;

    @Test
    void cleanupDeletesOnlyUnusableSessionsAndCanBeRepeated() throws Exception {
        Instant now = Instant.parse("2026-11-12T03:00:00Z");
        // A connection-local PostgreSQL table shadows the real table without touching existing sessions.
        try (var connection = dataSource.getConnection()) {
            JdbcClient jdbc = JdbcClient.create(new SingleConnectionDataSource(connection, true));
            try {
                jdbc.sql("""
                        CREATE TEMPORARY TABLE refresh_tokens (
                            id int PRIMARY KEY,
                            expires_at timestamptz NOT NULL,
                            revoked_at timestamptz
                        )
                        """).update();
                jdbc.sql("""
                        INSERT INTO refresh_tokens (id, expires_at, revoked_at) VALUES
                            (1, :past, NULL),
                            (2, :now, NULL),
                            (3, :future, :past),
                            (4, :past, :past),
                            (5, :future, NULL),
                            (6, :justAfterNow, NULL)
                        """)
                        .param("past", now.minusSeconds(1).atOffset(ZoneOffset.ofHours(13)))
                        .param("now", now.atOffset(ZoneOffset.ofHours(-5)))
                        .param("future", now.plusSeconds(3600).atOffset(ZoneOffset.UTC))
                        .param("justAfterNow", now.plusNanos(1000).atOffset(ZoneOffset.UTC))
                        .update();

                var accounts = new AccountRepository(jdbc);
                var cleanup = new SessionTokenCleanup(accounts, Clock.fixed(now, ZoneOffset.UTC));
                cleanup.cleanUp();
                assertThat(jdbc.sql("SELECT id FROM pg_temp.refresh_tokens ORDER BY id").query(Integer.class).list())
                        .containsExactly(5, 6);
                assertThat(accounts.deleteExpiredOrRevokedRefreshTokens(now)).isZero();
                assertThat(jdbc.sql("SELECT id FROM pg_temp.refresh_tokens ORDER BY id").query(Integer.class).list())
                        .containsExactly(5, 6);
            } finally {
                jdbc.sql("DROP TABLE IF EXISTS pg_temp.refresh_tokens").update();
            }
        }
    }
}
