package webapp.battle;

import java.sql.Connection;
import java.util.UUID;
import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;
import static org.assertj.core.api.Assertions.*;

/** V9 works on empty databases and preserves customizations when upgrading existing catalogs. */
@SpringBootTest(properties = {"app.security.session-token-cleanup-cron=-",
        "app.security.password-reset-token-cleanup-cron=-"})
class BattleSeedMigrationDatabaseTests {
    @Autowired private DataSource dataSource;
    private Connection connection;
    private String schema;
    private String originalSchema;
    private JdbcClient jdbc;

    @BeforeEach
    void createSchema() throws Exception {
        schema = "battle_seed_" + UUID.randomUUID().toString().replace("-", "");
        connection = dataSource.getConnection();
        originalSchema = connection.getSchema();
        try (var statement = connection.createStatement()) { statement.execute("CREATE SCHEMA " + schema); }
        connection.setSchema(schema);
        jdbc = JdbcClient.create(new SingleConnectionDataSource(connection, true));
    }

    @AfterEach
    void removeSchema() throws Exception {
        try {
            connection.setSchema(originalSchema);
            try (var statement = connection.createStatement()) { statement.execute("DROP SCHEMA " + schema + " CASCADE"); }
        } finally { connection.close(); }
    }

    private Flyway target(String target) {
        return Flyway.configure().dataSource(dataSource).schemas(schema).defaultSchema(schema).target(target).load();
    }

    @Test
    void freshDatabaseHasUsableFactionSnapshotsAndEquipmentEnergy() {
        assertThat(target("9").migrate().migrationsExecuted).isEqualTo(9);
        assertThat(jdbc.sql("""
                SELECT count(*) FROM cards c JOIN card_types t ON t.id=c.id_type
                WHERE t.code='CREATURE' AND c.id_faction IS NOT NULL
                """).query(Integer.class).single()).isEqualTo(25);
        assertThat(jdbc.sql("""
                SELECT count(*) FROM cards c JOIN card_types t ON t.id=c.id_type
                WHERE t.code='EQUIPMENT' AND c.energy_cost=0
                """).query(Integer.class).single()).isEqualTo(5);
        assertThat(jdbc.sql("SELECT f.code FROM cards c JOIN factions f ON f.id=c.id_faction WHERE c.code='CR01-003'")
                .query(String.class).single()).isEqualTo("RAD");
        assertThat(target("9").migrate().migrationsExecuted).isZero();
    }

    @Test
    void upgradePreservesNonNullValuesUnrelatedCardsIdsAndOwnership() {
        target("8").migrate();
        int sentinel = jdbc.sql("SELECT id FROM cards WHERE code='CR01-001'").query(Integer.class).single();
        jdbc.sql("UPDATE cards SET id_faction=(SELECT id FROM factions WHERE code='SAP') WHERE code='CR01-001'").update();
        jdbc.sql("UPDATE cards SET energy_cost=4 WHERE code='CR01-006'").update();
        int unrelated = jdbc.sql("INSERT INTO cards(code,name) VALUES ('CUSTOM-F07','Unrelated') RETURNING id")
                .query(Integer.class).single();
        assertThat(target("9").migrate().migrationsExecuted).isEqualTo(1);
        assertThat(jdbc.sql("SELECT id FROM cards WHERE code='CR01-001'").query(Integer.class).single()).isEqualTo(sentinel);
        assertThat(jdbc.sql("SELECT f.code FROM cards c JOIN factions f ON f.id=c.id_faction WHERE c.id=:id")
                .param("id", sentinel).query(String.class).single()).isEqualTo("SAP");
        assertThat(jdbc.sql("SELECT energy_cost FROM cards WHERE code='CR01-006'").query(Integer.class).single()).isEqualTo(4);
        assertThat(jdbc.sql("SELECT count(*) FROM cards WHERE id=:id AND id_faction IS NULL AND energy_cost IS NULL")
                .param("id", unrelated).query(Integer.class).single()).isEqualTo(1);
        assertThat(jdbc.sql("SELECT count(*) FROM collections").query(Integer.class).single()).isEqualTo(30);
        assertThat(jdbc.sql("SELECT count(*) FROM card_effects").query(Integer.class).single()).isEqualTo(7);
    }
}
