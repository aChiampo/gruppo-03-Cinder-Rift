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

/** V3 defines the complete card schema and V5 seeds usable battle data immediately. */
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
    void v3DefinesTheCompleteCardAndArtworkSchema() {
        target("3").migrate();
        assertThat(jdbc.sql("""
                SELECT column_name FROM information_schema.columns
                WHERE table_schema = :schema AND table_name = 'cards'
                """).param("schema", schema).query(String.class).list()).containsExactlyInAnyOrder(
                        "id", "name", "description", "flavor_text", "id_type", "energy_cost", "power",
                        "id_rarity", "value_atk", "value_def", "id_faction", "id_expansion", "is_deleted");
        assertThat(jdbc.sql("""
                SELECT is_identity FROM information_schema.columns
                WHERE table_schema = :schema AND table_name = 'cards' AND column_name = 'id'
                """).param("schema", schema).query(String.class).single()).isEqualTo("YES");
        assertThat(jdbc.sql("""
                SELECT count(*) FROM information_schema.columns
                WHERE table_schema = :schema AND table_name = 'cards_arts' AND column_name = 'source_image'
                """).param("schema", schema).query(Integer.class).single()).isEqualTo(1);
    }

    @Test
    void v5HasUsableFactionSnapshotsAndEquipmentEnergy() {
        assertThat(target("5").migrate().migrationsExecuted).isEqualTo(5);
        assertThat(jdbc.sql("""
                SELECT f.code FROM cards c JOIN card_types t ON t.id = c.id_type
                JOIN factions f ON f.id = c.id_faction WHERE t.code = 'CREATURE'
                """).query(String.class).list()).hasSize(25)
                .containsOnly("CYB", "SAP", "RAD", "ALN", "CLT");
        assertThat(jdbc.sql("""
                SELECT count(*) FROM cards c JOIN card_types t ON t.id = c.id_type
                JOIN factions f ON f.id = c.id_faction
                WHERE t.code = 'EQUIPMENT' AND c.energy_cost = 0 AND f.code = 'NEUTRAL'
                """).query(Integer.class).single()).isEqualTo(5);
        assertThat(target("5").migrate().migrationsExecuted).isZero();
    }

    @Test
    void laterMigrationsPreserveCustomizationsGeneratedIdsAndOwnership() {
        target("5").migrate();
        int sentinel = jdbc.sql("SELECT id FROM cards WHERE name = 'Sentinel K-17'")
                .query(Integer.class).single();
        jdbc.sql("UPDATE cards SET id_faction = (SELECT id FROM factions WHERE code = 'SAP') WHERE id = :id")
                .param("id", sentinel).update();
        int equipment = jdbc.sql("SELECT id FROM cards WHERE name = 'Pulse Blade'").query(Integer.class).single();
        jdbc.sql("UPDATE cards SET energy_cost = 4 WHERE id = :id").param("id", equipment).update();
        int unrelated = jdbc.sql("INSERT INTO cards (name) VALUES ('Unrelated') RETURNING id")
                .query(Integer.class).single();
        assertThat(unrelated).isGreaterThan(jdbc.sql("SELECT max(id) FROM cards WHERE id <> :id")
                .param("id", unrelated).query(Integer.class).single());
        assertThat(target("13").migrate().migrationsExecuted).isEqualTo(6);
        assertThat(jdbc.sql("SELECT f.code FROM cards c JOIN factions f ON f.id = c.id_faction WHERE c.id = :id")
                .param("id", sentinel).query(String.class).single()).isEqualTo("SAP");
        assertThat(jdbc.sql("SELECT energy_cost FROM cards WHERE id = :id")
                .param("id", equipment).query(Integer.class).single()).isEqualTo(4);
        assertThat(jdbc.sql("SELECT count(*) FROM cards WHERE id = :id AND id_faction IS NULL AND energy_cost IS NULL")
                .param("id", unrelated).query(Integer.class).single()).isEqualTo(1);
        assertThat(jdbc.sql("SELECT count(*) FROM collections").query(Integer.class).single()).isEqualTo(30);
        assertThat(jdbc.sql("SELECT count(*) FROM card_effects").query(Integer.class).single()).isEqualTo(7);
    }
}
