package webapp.card;

import java.sql.Connection;
import java.util.UUID;
import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;
import static org.assertj.core.api.Assertions.assertThat;

/** Verifies image assignment and legacy code conversion without changing ownership. */
@SpringBootTest(properties = {"app.security.session-token-cleanup-cron=-",
        "app.security.password-reset-token-cleanup-cron=-"})
class CardArtworkMigrationDatabaseTests {
    @Autowired DataSource dataSource;
    Connection connection;
    String schema;
    String originalSchema;
    JdbcClient jdbc;

    @BeforeEach
    void createSchema() throws Exception {
        schema = "artwork_test_" + UUID.randomUUID().toString().replace("-", "");
        connection = dataSource.getConnection();
        originalSchema = connection.getSchema();
        try (var statement = connection.createStatement()) { statement.execute("CREATE SCHEMA " + schema); }
        connection.setSchema(schema);
        jdbc = JdbcClient.create(new SingleConnectionDataSource(connection, true));
    }

    @AfterEach
    void removeSchema() throws Exception {
        if (connection == null) return;
        try {
            connection.setSchema(originalSchema);
            try (var statement = connection.createStatement()) { statement.execute("DROP SCHEMA " + schema + " CASCADE"); }
        } finally { connection.close(); }
    }

    @Test
    void freshInstallationAssignsAllImagesAndDoesNotRerun() {
        assertThat(target("10").migrate().migrationsExecuted).isEqualTo(10);
        assertArtwork();
        assertThat(target("10").migrate().migrationsExecuted).isZero();
    }

    @Test
    void upgradeFromV9PreservesArtworkIdentitiesPlayersAndOwnership() {
        target("9").migrate();
        String before = snapshot();
        assertThat(target("10").migrate().migrationsExecuted).isEqualTo(1);
        assertThat(snapshot()).isEqualTo(before);
        assertArtwork();
    }

    private void assertArtwork() {
        assertThat(jdbc.sql("SELECT DISTINCT art_code FROM cards_arts").query(String.class).list())
                .containsExactly("STANDARD");
        assertThat(jdbc.sql("""
                SELECT count(*) FROM cards_arts ca JOIN cards c ON c.id = ca.id_card
                WHERE c.code LIKE 'CR01-%' AND ca.art_code = 'STANDARD'
                  AND ca.url_card_art LIKE '/img/cards/%.webp'
                """).query(Integer.class).single()).isEqualTo(30);
        assertThat(jdbc.sql("SELECT url_card_art FROM cards_arts ORDER BY url_card_art")
                .query(String.class).list()).doesNotHaveDuplicates();
    }

    private String snapshot() {
        return jdbc.sql("""
                SELECT jsonb_build_object(
                    'users', (SELECT jsonb_agg(to_jsonb(u) ORDER BY u.id) FROM users u),
                    'inventory', (SELECT jsonb_agg(to_jsonb(i) ORDER BY i.id) FROM inventory i),
                    'cards', (SELECT jsonb_agg(to_jsonb(c) ORDER BY c.id) FROM cards c),
                    'artworks', (SELECT jsonb_agg(to_jsonb(a) - 'art_code' - 'url_card_art'
                        ORDER BY a.id_art) FROM cards_arts a),
                    'collection', (SELECT jsonb_agg(to_jsonb(c) ORDER BY c.id_user, c.id_card, c.id_card_art)
                        FROM collections c))::text
                """).query(String.class).single();
    }

    private Flyway target(String version) {
        return Flyway.configure().dataSource(dataSource).schemas(schema).defaultSchema(schema).target(version).load();
    }
}
