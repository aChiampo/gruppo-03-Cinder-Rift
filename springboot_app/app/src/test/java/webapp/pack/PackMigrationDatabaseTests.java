package webapp.pack;

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

import static org.assertj.core.api.Assertions.*;

/** Exercises installation, V9 upgrade and ownership/mercy constraints in disposable schemas. */
@SpringBootTest(properties = {"app.security.session-token-cleanup-cron=-",
        "app.security.password-reset-token-cleanup-cron=-"})
class PackMigrationDatabaseTests {
    @Autowired private DataSource dataSource;
    private Connection connection;
    private String schema;
    private String originalSchema;
    private JdbcClient jdbc;

    @BeforeEach
    void createSchema() throws Exception {
        schema = "packs_test_" + UUID.randomUUID().toString().replace("-", "");
        connection = dataSource.getConnection();
        originalSchema = connection.getSchema();
        try (var statement = connection.createStatement()) { statement.execute("CREATE SCHEMA " + schema); }
        connection.setSchema(schema);
        jdbc = JdbcClient.create(new SingleConnectionDataSource(connection, true));
    }

    @AfterEach
    void removeSchema() throws Exception {
        if (connection != null) {
            try {
                connection.setSchema(originalSchema);
                try (var statement = connection.createStatement()) {
                    statement.execute("DROP SCHEMA " + schema + " CASCADE");
                }
            } finally { connection.close(); }
        }
    }

    private Flyway target(String target) {
        return Flyway.configure().dataSource(dataSource).schemas(schema).defaultSchema(schema).target(target).load();
    }

    @Test
    void installsOnEmptyDatabaseWithoutPackageSeedAndDoesNotRerun() {
        assertThat(target("10").migrate().migrationsExecuted).isEqualTo(10);
        assertEmptyPackageTables();
        assertThat(target("10").migrate().migrationsExecuted).isZero();
        assertThat(jdbc.sql("SELECT data_type FROM information_schema.columns WHERE table_schema = :schema "
                + "AND table_name = 'packs' AND column_name = 'pack_description'")
                .param("schema", schema).query(String.class).single()).isEqualTo("jsonb");
        assertThat(jdbc.sql("SELECT column_default FROM information_schema.columns WHERE table_schema = :schema "
                + "AND table_name = 'packs' AND column_name = 'pack_price'")
                .param("schema", schema).query((row, index) -> row.getString(1)).list()).containsExactly((String) null);
    }

    @Test
    void upgradesV9WithoutChangingPlayersBalancesCardsOrCollection() {
        target("9").migrate();
        jdbc.sql("UPDATE inventory SET cobalt_shards = 123, rift_cores = 45, scraps = 67").update();
        String before = snapshot();
        assertThat(target("10").migrate().migrationsExecuted).isEqualTo(1);
        assertThat(snapshot()).isEqualTo(before);
        assertEmptyPackageTables();
    }

    @Test
    void enforcesArtworkRatesAndDirectPackReference() {
        target("10").migrate();
        for (String values : new String[]{"-1, 101, 0, 0", "101, 0, 0, -1"}) {
            assertThatThrownBy(() -> jdbc.sql("INSERT INTO artwork_pull_rates "
                    + "(standard, extended_art, full_art, alternate_art) VALUES (" + values + ")").update())
                    .hasMessageContaining("artwork_pull_rates_range_check");
        }
        assertThatThrownBy(() -> jdbc.sql("INSERT INTO artwork_pull_rates "
                + "(standard, extended_art, full_art, alternate_art) VALUES (99, 0, 0, 0)").update())
                .hasMessageContaining("artwork_pull_rates_total_check");
        assertThatThrownBy(() -> jdbc.sql("INSERT INTO artwork_pull_rates (standard) VALUES (NULL)").update())
                .hasMessageContaining("null");
        int packId = createPack();
        assertThatThrownBy(() -> jdbc.sql("UPDATE packs SET id_artwork_pull_rates = 2147483647 WHERE id = :id")
                .param("id", packId).update()).hasMessageContaining("packs_id_artwork_pull_rates_fk");
        assertThatThrownBy(() -> jdbc.sql("UPDATE packs SET id_artwork_pull_rates = NULL WHERE id = :id")
                .param("id", packId).update()).hasMessageContaining("null");
        assertThatThrownBy(() -> jdbc.sql("DELETE FROM artwork_pull_rates").update())
                .hasMessageContaining("packs_id_artwork_pull_rates_fk");
    }

    @Test
    void normalizesLegacyArtCodesPreservingArtworkIdentitiesAndEnforcesFourTypes() {
        target("9").migrate();
        jdbc.sql("INSERT INTO cards_arts (id_card, art_code) SELECT min(id), 'ALTERNATE' FROM cards").update();
        String before = snapshot();
        target("10").migrate();
        assertThat(snapshot()).isEqualTo(before);
        assertThat(jdbc.sql("SELECT DISTINCT art_code FROM cards_arts ORDER BY art_code").query(String.class).list())
                .containsExactly("ALTERNATE_ART", "STANDARD");
        int artId = jdbc.sql("SELECT id_art FROM cards_arts WHERE art_code = 'ALTERNATE_ART'")
                .query(Integer.class).single();
        for (String code : new String[]{"EXTENDED_ART", "FULL_ART", "ALTERNATE_ART"}) {
            jdbc.sql("UPDATE cards_arts SET art_code = :code WHERE id_art = :id")
                    .param("code", code).param("id", artId).update();
        }
        for (String code : new String[]{"SHOWCASE", "default", "LV2", "ALTERNATE", "standard"}) {
            assertThatThrownBy(() -> jdbc.sql("UPDATE cards_arts SET art_code = :code WHERE id_art = :id")
                    .param("code", code).param("id", artId).update()).hasMessageContaining("cards_arts_art_code_check");
        }
    }

    @Test
    void enforcesPositiveOwnedQuantitiesAndKeepsMercyAfterLastPackage() {
        target("10").migrate();
        int packId = createPack();
        assertThatThrownBy(() -> jdbc.sql("UPDATE packs SET mercy_threshold = 0 WHERE id = :id")
                .param("id", packId).update()).hasMessageContaining("packs_mercy_threshold_check");
        assertThatThrownBy(() -> jdbc.sql("""
                INSERT INTO inventory_packs (id_inventory, id_pack, item_quantity)
                SELECT id, :id, 0 FROM inventory
                """).param("id", packId).update()).hasMessageContaining("inventory_packs_item_quantity_check");
        jdbc.sql("""
                INSERT INTO inventory_packs (id_inventory, id_pack, item_quantity)
                SELECT id, :id, 1 FROM inventory
                """).param("id", packId).update();
        jdbc.sql("""
                INSERT INTO user_mercy (id_user, id_pack, mercy_counter, mercy_rarity)
                SELECT u.id, :id, 0, r.id FROM users u CROSS JOIN rarities r WHERE r.code = 'EPIC'
                """).param("id", packId).update();
        assertThatThrownBy(() -> jdbc.sql("UPDATE user_mercy SET mercy_counter = -1").update())
                .hasMessageContaining("user_mercy_counter_check");
        assertThatThrownBy(() -> jdbc.sql("DELETE FROM packs WHERE id = :id").param("id", packId).update())
                .hasMessageContaining("inventory_packs_id_pack_fk");
        jdbc.sql("DELETE FROM inventory_packs WHERE id_pack = :id").param("id", packId).update();
        assertThat(jdbc.sql("SELECT mercy_counter FROM user_mercy WHERE id_pack = :id")
                .param("id", packId).query(Integer.class).single()).isZero();
        assertThatThrownBy(() -> jdbc.sql("DELETE FROM packs WHERE id = :id").param("id", packId).update())
                .hasMessageContaining("user_mercy_id_pack_fk");
    }

    @Test
    void enforcesOwnershipAndMercyForeignKeysAndUniquePairs() {
        target("10").migrate();
        int packId = createPack();
        assertThatThrownBy(() -> jdbc.sql("INSERT INTO inventory_packs VALUES (2147483647, :id, 1)")
                .param("id", packId).update()).hasMessageContaining("inventory_packs_id_inventory_fk");
        jdbc.sql("INSERT INTO inventory_packs SELECT id, :id, 1 FROM inventory")
                .param("id", packId).update();
        assertThatThrownBy(() -> jdbc.sql("INSERT INTO inventory_packs SELECT id, :id, 1 FROM inventory")
                .param("id", packId).update()).hasMessageContaining("inventory_packs_pkey");
        assertThatThrownBy(() -> jdbc.sql("""
                INSERT INTO user_mercy SELECT id, :id, 1, 2147483647 FROM users
                """).param("id", packId).update()).hasMessageContaining("user_mercy_rarity_fk");
        jdbc.sql("""
                INSERT INTO user_mercy SELECT u.id, :id, 1, r.id
                FROM users u CROSS JOIN rarities r WHERE r.code = 'RARE'
                """).param("id", packId).update();
        assertThatThrownBy(() -> jdbc.sql("""
                INSERT INTO user_mercy SELECT u.id, :id, 1, r.id
                FROM users u CROSS JOIN rarities r WHERE r.code = 'RARE'
                """).param("id", packId).update()).hasMessageContaining("user_mercy_pkey");
    }

    private int createPack() {
        return jdbc.sql("""
                INSERT INTO packs (id_artwork_pull_rates, pack_name, pack_description, pack_price, allowed_factions, allowed_types,
                    allowed_expansions, allowed_rarities, mercy_threshold, slot_pulls)
                VALUES ((SELECT min(id) FROM artwork_pull_rates), 'Migration fixture', '{}'::jsonb, 37, '[]'::jsonb, '[]'::jsonb, '[]'::jsonb,
                    '[]'::jsonb, 10, '{}'::jsonb) RETURNING id
                """).query(Integer.class).single();
    }

    private void assertEmptyPackageTables() {
        assertThat(jdbc.sql("SELECT count(*) FROM artwork_pull_rates").query(Integer.class).single()).isEqualTo(1);
        assertThat(jdbc.sql("SELECT standard + extended_art + full_art + alternate_art FROM artwork_pull_rates")
                .query(java.math.BigDecimal.class).single()).isEqualByComparingTo("100");
        assertThat(jdbc.sql("SELECT standard FROM artwork_pull_rates")
                .query(java.math.BigDecimal.class).single()).isEqualByComparingTo("81");
        for (String table : new String[]{"packs", "inventory_packs", "user_mercy"}) {
            assertThat(jdbc.sql("SELECT count(*) FROM " + table).query(Integer.class).single()).isZero();
        }
    }

    private String snapshot() {
        return jdbc.sql("""
                SELECT jsonb_build_object(
                    'users', (SELECT jsonb_agg(to_jsonb(u) ORDER BY u.id) FROM users u),
                    'inventory', (SELECT jsonb_agg(to_jsonb(i) ORDER BY i.id) FROM inventory i),
                    'cards', (SELECT jsonb_agg(to_jsonb(c) ORDER BY c.id) FROM cards c),
                    'artworks', (SELECT jsonb_agg(to_jsonb(a) - 'art_code' ORDER BY a.id_art) FROM cards_arts a),
                    'collection', (SELECT jsonb_agg(to_jsonb(c) ORDER BY c.id_user, c.id_card, c.id_card_art)
                        FROM collections c))::text
                """).query(String.class).single();
    }
}
