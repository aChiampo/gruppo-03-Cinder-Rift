package webapp.economy;

import java.sql.Connection;
import java.time.OffsetDateTime;
import java.util.UUID;
import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;
import org.springframework.transaction.support.TransactionTemplate;
import static org.assertj.core.api.Assertions.*;

/** Boundary and malformed-storage checks use disposable schemas, without adding an F06 migration. */
@SpringBootTest(properties = {"app.security.session-token-cleanup-cron=-", "app.security.password-reset-token-cleanup-cron=-"})
class ReservationReaderDatabaseTests {
    @Autowired DataSource dataSource;
    private Connection connection;
    private String schema;
    private String originalSchema;
    private JdbcClient jdbc;
    private TransactionTemplate transaction;
    private ReservationReader reader;
    private int user;
    private int other;

    @BeforeEach
    void setUp() throws Exception {
        schema = "reservation_test_" + UUID.randomUUID().toString().replace("-", "");
        connection = dataSource.getConnection();
        originalSchema = connection.getSchema();
        try (var statement = connection.createStatement()) { statement.execute("CREATE SCHEMA " + schema); }
        Flyway.configure().dataSource(dataSource).schemas(schema).defaultSchema(schema).load().migrate();
        connection.setSchema(schema);
        var source = new SingleConnectionDataSource(connection, true);
        jdbc = JdbcClient.create(source);
        transaction = new TransactionTemplate(new DataSourceTransactionManager(source));
        reader = new ReservationReader(jdbc);
        user = jdbc.sql("SELECT min(id) FROM users").query(Integer.class).single();
        other = jdbc.sql("""
                INSERT INTO users (nickname, email, hash_password, id_role)
                SELECT 'Other reservation user', 'other@example.invalid', hash_password, id_role FROM users WHERE id = :user RETURNING id
                """).param("user", user).query(Integer.class).single();
        jdbc.sql("INSERT INTO inventory (id_user) VALUES (:other)").param("other", other).update();
        ReservationTestStorage.install(jdbc);
    }

    @AfterEach
    void cleanUp() throws Exception {
        if (connection != null) {
            connection.setSchema(originalSchema);
            try (var statement = connection.createStatement()) { statement.execute("DROP SCHEMA " + schema + " CASCADE"); }
            connection.close();
        }
    }

    @Test
    void expirationEqualityIsExcludedUsingOneFixedDatabaseCutoff() {
        var cutoff = now();
        ReservationTestStorage.hold(jdbc, user, other, 100, cutoff.minusSeconds(1));
        ReservationTestStorage.hold(jdbc, user, other, 200, cutoff);
        ReservationTestStorage.hold(jdbc, user, other, 300, cutoff.plusSeconds(1));
        assertThat(readAt(cutoff)).isEqualTo(300);
    }

    @Test
    void acceptedResponderHoldReservesTheRespondersScaglie() {
        var cutoff = now();
        ReservationTestStorage.hold(jdbc, other, user, 250, cutoff.plusHours(1));
        jdbc.sql("UPDATE current_negotiations SET p_acceptance = false, r_acceptance = true, current_r_token_offer = 250").update();
        jdbc.sql("UPDATE trade_token_reservations SET id_user = :user").param("user", user).update();
        assertThat(readAt(cutoff)).isEqualTo(250);
    }

    @Test
    void summationCannotWrapLongIntoAnUnreservedBalance() {
        var cutoff = now();
        ReservationTestStorage.hold(jdbc, user, other, Long.MAX_VALUE, cutoff.plusHours(1));
        ReservationTestStorage.hold(jdbc, user, other, 1, cutoff.plusHours(1));
        assertThatThrownBy(() -> readAt(cutoff)).isInstanceOf(EconomyFailure.class)
                .extracting(failure -> ((EconomyFailure) failure).code()).isEqualTo("RESERVATION_STATE_INVALID");
    }

    @Test
    void malformedStorageErrorsDoNotBecomeZeroReservations() {
        jdbc.sql("ALTER TABLE trade_token_reservations DROP COLUMN token_amount").update();
        assertThatThrownBy(() -> readAt(now())).isInstanceOf(DataAccessException.class);
    }

    @Test
    void storagePresenceIsNotCachedWhenF06IsInstalledLater() {
        ReservationTestStorage.remove(jdbc);
        assertThat(readAt(now())).isZero();
        ReservationTestStorage.install(jdbc);
        var cutoff = now();
        ReservationTestStorage.hold(jdbc, user, other, 100, cutoff.plusHours(1));
        assertThat(readAt(cutoff)).isEqualTo(100);
    }

    private OffsetDateTime now() { return jdbc.sql("SELECT clock_timestamp()").query(OffsetDateTime.class).single(); }
    private long readAt(OffsetDateTime cutoff) {
        return transaction.execute(status -> {
            jdbc.sql("SELECT id FROM users WHERE id = :user FOR UPDATE").param("user", user).query(Integer.class).single();
            return reader.reservedScaglie(user, cutoff);
        });
    }
}
