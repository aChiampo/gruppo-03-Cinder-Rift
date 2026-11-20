package webapp.deck;

import java.sql.Connection;
import java.util.UUID;

import javax.sql.DataSource;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Verifies fresh installation in a disposable PostgreSQL schema. */
@SpringBootTest(properties = {"app.security.session-token-cleanup-cron=-",
        "app.security.password-reset-token-cleanup-cron=-"})
class RoundConfigurationMigrationDatabaseTests {
    @Autowired private DataSource dataSource;
    private String schema;
    private String originalSchema;
    private Connection connection;
    private JdbcClient jdbc;
    private Flyway flyway;

    @BeforeEach
    void setUp() throws Exception {
        schema = "round_limits_test_" + UUID.randomUUID().toString().replace("-", "");
        connection = dataSource.getConnection();
        originalSchema = connection.getSchema();
        try (var statement = connection.createStatement()) {
            statement.execute("CREATE SCHEMA " + schema);
        }
        connection.setSchema(schema);
        jdbc = JdbcClient.create(new SingleConnectionDataSource(connection, true));
        flyway = Flyway.configure().dataSource(dataSource).schemas(schema).defaultSchema(schema).load();
    }

    @AfterEach
    void tearDown() throws Exception {
        if (connection != null) {
            try {
                connection.setSchema(originalSchema);
                try (var statement = connection.createStatement()) {
                    statement.execute("DROP SCHEMA " + schema + " CASCADE");
                }
            } finally {
                connection.close();
            }
        }
    }

    @Test
    void freshInstallationLoadsApprovedLimitsAndEnforcesConstraints() {
        assertThat(flyway.migrate().migrationsExecuted).isEqualTo(9);
        assertApprovedLimits();
        assertThatThrownBy(() -> jdbc.sql(
                "UPDATE round_configurations SET max_card_energy = 0 WHERE round_name = 'ROUND1'").update())
                .hasMessageContaining("round_configurations_max_card_energy_check");
        assertThatThrownBy(() -> jdbc.sql(
                "UPDATE round_configurations SET max_round_power = 0 WHERE round_name = 'ROUND1'").update())
                .hasMessageContaining("round_configurations_max_round_power_check");
        assertThat(flyway.migrate().migrationsExecuted).isZero();
    }

    private void assertApprovedLimits() {
        var rounds = new CardCatalogRepository(jdbc).findRoundConfigurations();
        assertThat(rounds).hasSize(6);
        for (int round = 1; round <= 5; round++) {
            String name = "ROUND" + round;
            assertThat(rounds.get(name)).isEqualTo(new RoundConfiguration(name, round, round, 14, 5));
        }
        assertThat(rounds.get("EQUIPMENT"))
                .isEqualTo(new RoundConfiguration("EQUIPMENT", 6, 6, 10, 5));
    }
}
