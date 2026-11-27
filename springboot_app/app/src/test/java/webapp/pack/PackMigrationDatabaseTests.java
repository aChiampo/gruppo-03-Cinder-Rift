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

/** Exercises installation, V11 upgrade and ownership/mercy constraints in disposable schemas. */
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
        assertThat(target("13").migrate().migrationsExecuted).isEqualTo(11);
        assertEmptyPackageTables();
        assertThat(target("13").migrate().migrationsExecuted).isZero();
        assertThat(jdbc.sql("SELECT data_type FROM information_schema.columns WHERE table_schema = :schema "
                + "AND table_name = 'packs' AND column_name = 'pack_description'")
                .param("schema", schema).query(String.class).single()).isEqualTo("jsonb");
        assertThat(jdbc.sql("SELECT column_default FROM information_schema.columns WHERE table_schema = :schema "
                + "AND table_name = 'packs' AND column_name = 'pack_price'")
                .param("schema", schema).query((row, index) -> row.getString(1)).list()).containsExactly((String) null);
    }

    @Test
    void upgradesDevV11ThroughV13WithoutChangingPlayersCardsOwnershipOrFriendships() {
        target("11").migrate();
        jdbc.sql("UPDATE inventory SET cobalt_shards = 123, rift_cores = 45, scraps = 67").update();
        int friendId = jdbc.sql("""
                INSERT INTO users (nickname, hash_password, email, id_role)
                SELECT 'Migration friend', hash_password, 'migration-friend@example.invalid', id_role
                FROM users ORDER BY id LIMIT 1 RETURNING id
                """).query(Integer.class).single();
        jdbc.sql("""
                INSERT INTO friendships (user_a_id, user_b_id, requested_by, status, accepted_at)
                SELECT id, :friendId, id, 'accepted', now() FROM users WHERE id <> :friendId
                """).param("friendId", friendId).update();
        String before = snapshot();
        assertThat(target("12").migrate().migrationsExecuted).isEqualTo(1);
        assertThat(snapshot()).isEqualTo(before);
        assertEmptyPackageTables();
        assertThat(target("13").migrate().migrationsExecuted).isEqualTo(1);
        assertThat(snapshot()).isEqualTo(before);
        assertEmptyPackageTables();
        assertThat(target("13").migrate().migrationsExecuted).isZero();
    }

    @Test
    void enforcesArtworkRatesAndDirectPackReference() {
        target("12").migrate();
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
    void preservesSeededArtworkIdentitiesAndEnforcesFourTypes() {
        target("11").migrate();
        jdbc.sql("INSERT INTO cards_arts (id_card, art_code) SELECT min(id), 'ALTERNATE_ART' FROM cards").update();
        String before = snapshot();
        target("12").migrate();
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
        target("12").migrate();
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
        target("12").migrate();
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

    @Test
    void upgradesV12PreservingPacksAndArtworkRatesWithFourDecimalPlaces() {
        target("12").migrate();
        int id = createPack();
        String before = snapshot();
        assertThat(target("13").migrate().migrationsExecuted).isEqualTo(1);
        assertThat(snapshot()).isEqualTo(before);
        assertThat(target("13").migrate().migrationsExecuted).isZero();
        assertThat(jdbc.sql("SELECT is_acquirable FROM packs WHERE id = :id")
                .param("id", id).query(Boolean.class).single()).isFalse();
        assertThat(jdbc.sql("SELECT pack_price FROM packs WHERE id = :id")
                .param("id", id).query(Integer.class).single()).isEqualTo(37);
        assertThat(jdbc.sql("SELECT standard FROM artwork_pull_rates")
                .query(java.math.BigDecimal.class).single()).isEqualByComparingTo("81.0000");
        assertThat(jdbc.sql("SELECT count(*) FROM information_schema.columns WHERE table_schema = :schema "
                + "AND table_name = 'packs' AND column_name = 'slot_pulls'")
                .param("schema", schema).query(Integer.class).single()).isZero();
        assertThat(jdbc.sql("SELECT numeric_precision || ',' || numeric_scale FROM information_schema.columns "
                + "WHERE table_schema = :schema AND ((table_name = 'artwork_pull_rates' "
                + "AND column_name IN ('standard', 'extended_art', 'full_art', 'alternate_art')) "
                + "OR (table_name = 'type_pull_rate' AND column_name = 'probability'))")
                .param("schema", schema).query(String.class).list()).containsExactly("7,4", "7,4", "7,4", "7,4", "7,4");
        for (String table : new String[]{"slot_pull_type", "type_pull_rate", "pack_slots"}) {
            assertThat(jdbc.sql("SELECT count(*) FROM " + table).query(Integer.class).single()).isZero();
        }
        jdbc.sql("UPDATE artwork_pull_rates SET standard = 80.9999, alternate_art = 1.5001").update();
        assertThat(jdbc.sql("SELECT alternate_art FROM artwork_pull_rates")
                .query(java.math.BigDecimal.class).single()).isEqualByComparingTo("1.5001");
    }

    @Test
    void refusesToDiscardNonEmptyLegacySlotsAndRollsBackTheUpgrade() {
        target("12").migrate();
        int id = createPack();
        jdbc.sql("UPDATE packs SET slot_pulls = '{\"legacy\":true}'::jsonb WHERE id = :id")
                .param("id", id).update();
        assertThatThrownBy(() -> target("13").migrate()).hasStackTraceContaining("requires an explicit mapping");
        assertThat(jdbc.sql("SELECT slot_pulls::text FROM packs WHERE id = :id")
                .param("id", id).query(String.class).single()).isEqualTo("{\"legacy\": true}");
        assertThat(jdbc.sql("SELECT count(*) FROM information_schema.tables WHERE table_schema = :schema "
                + "AND table_name = 'slot_pull_type'").param("schema", schema).query(Integer.class).single()).isZero();
    }

    @Test
    void validatesCrossRowTotalsAtCommitIncludingEmptyAndReassignedProfiles() throws Exception {
        target("13").migrate();
        assertThatThrownBy(() -> transaction(() -> jdbc.sql(
                "INSERT INTO slot_pull_type (description) VALUES ('Empty') RETURNING id")
                .query(Integer.class).single())).hasMessageContaining("must total 100.0000");
        assertThatThrownBy(() -> createType("98.9999", "1.0000"))
                .hasMessageContaining("must total 100.0000");
        assertThatThrownBy(() -> createType("0", "0")).hasMessageContaining("must total 100.0000");
        int first = createType("98.9999", "1.0001");
        int second = createType("100", "0");
        assertThatThrownBy(() -> transaction(() -> jdbc.sql(
                "DELETE FROM type_pull_rate WHERE id_slot_pull_type = :id")
                .param("id", first).update())).hasMessageContaining("must total 100.0000");
        assertThatThrownBy(() -> transaction(() -> jdbc.sql("UPDATE type_pull_rate SET probability = 0 "
                + "WHERE id_slot_pull_type = :id").param("id", first).update()))
                .hasMessageContaining("must total 100.0000");
        assertThatThrownBy(() -> transaction(() -> {
            jdbc.sql("DELETE FROM type_pull_rate WHERE id_slot_pull_type = :id").param("id", second).update();
            return jdbc.sql("UPDATE type_pull_rate SET id_slot_pull_type = :second WHERE id_slot_pull_type = :first")
                    .param("first", first).param("second", second).update();
        })).hasMessageContaining("must total 100.0000");
        assertThat(jdbc.sql("SELECT sum(probability) FROM type_pull_rate WHERE id_slot_pull_type = :id")
                .param("id", first).query(java.math.BigDecimal.class).single()).isEqualByComparingTo("100.0000");
        jdbc.sql("DELETE FROM slot_pull_type WHERE id = :id").param("id", second).update();
    }

    @Test
    void enforcesRarityReferencesUniquenessAndProbabilityRanges() throws Exception {
        target("13").migrate();
        int id = createType("98.9999", "1.0001");
        assertThat(jdbc.sql("SELECT probability FROM type_pull_rate t JOIN rarities r ON r.id = t.id_rarity "
                + "WHERE t.id_slot_pull_type = :id AND r.code = 'RARE'")
                .param("id", id).query(java.math.BigDecimal.class).single()).isEqualByComparingTo("1.0001");
        for (String probability : new String[]{"-0.0001", "100.0001"}) {
            assertThatThrownBy(() -> jdbc.sql("UPDATE type_pull_rate SET probability = :value WHERE id_slot_pull_type = :id")
                    .param("value", new java.math.BigDecimal(probability)).param("id", id).update())
                    .hasMessageContaining("type_pull_rate_probability_check");
        }
        assertThatThrownBy(() -> jdbc.sql("INSERT INTO type_pull_rate VALUES (:id, 2147483647, 0)")
                .param("id", id).update()).hasMessageContaining("type_pull_rate_rarity_fk");
        assertThatThrownBy(() -> jdbc.sql("INSERT INTO type_pull_rate SELECT :id, id, 0 FROM rarities WHERE code = 'RARE'")
                .param("id", id).update()).hasMessageContaining("type_pull_rate_pkey");
        assertThatThrownBy(() -> jdbc.sql("UPDATE type_pull_rate SET probability = NULL WHERE id_slot_pull_type = :id")
                .param("id", id).update()).hasMessageContaining("null");
        jdbc.sql("INSERT INTO rarities (code, name, order_index) VALUES ('MYTHIC', 'Mythic', 5)").update();
        transaction(() -> {
            int dynamic = jdbc.sql("INSERT INTO slot_pull_type (description) VALUES ('Dynamic') RETURNING id")
                    .query(Integer.class).single();
            return jdbc.sql("INSERT INTO type_pull_rate SELECT :id, id, 100 FROM rarities WHERE code = 'MYTHIC'")
                    .param("id", dynamic).update();
        });
    }

    @Test
    void preservesRepeatedSlotsAndFreezesReferencedDistributions() throws Exception {
        target("12").migrate();
        int packId = createPack();
        target("13").migrate();
        int type = createType("98.9999", "1.0001");
        jdbc.sql("INSERT INTO pack_slots VALUES (:pack, 1, :type), (:pack, 2, :type)")
                .param("pack", packId).param("type", type).update();
        assertThatThrownBy(() -> jdbc.sql("INSERT INTO pack_slots VALUES (:pack, 1, :type)")
                .param("pack", packId).param("type", type).update()).hasMessageContaining("pack_slots_pkey");
        assertThatThrownBy(() -> jdbc.sql("INSERT INTO pack_slots VALUES (:pack, 0, :type)")
                .param("pack", packId).param("type", type).update()).hasMessageContaining("pack_slots_position_check");
        assertThatThrownBy(() -> jdbc.sql("INSERT INTO pack_slots VALUES (:pack, 3, 2147483647)")
                .param("pack", packId).update()).hasMessageContaining("pack_slots_type_fk");
        assertThatThrownBy(() -> jdbc.sql("INSERT INTO pack_slots VALUES (2147483647, 1, :type)")
                .param("type", type).update()).hasMessageContaining("pack_slots_pack_fk");
        assertThatThrownBy(() -> jdbc.sql("UPDATE type_pull_rate SET probability = probability WHERE id_slot_pull_type = :id")
                .param("id", type).update()).hasMessageContaining("Referenced slot pull types are immutable");
        assertThatThrownBy(() -> jdbc.sql("DELETE FROM type_pull_rate WHERE id_slot_pull_type = :id")
                .param("id", type).update()).hasMessageContaining("Referenced slot pull types are immutable");
        assertThatThrownBy(() -> jdbc.sql("INSERT INTO type_pull_rate SELECT :id, id, 0 FROM rarities WHERE code = 'EPIC'")
                .param("id", type).update()).hasMessageContaining("Referenced slot pull types are immutable");
        assertThatThrownBy(() -> jdbc.sql("DELETE FROM slot_pull_type WHERE id = :id").param("id", type).update())
                .hasMessageContaining("Referenced slot pull types are immutable");
        assertThatThrownBy(() -> jdbc.sql("UPDATE packs SET is_acquirable = NULL WHERE id = :id")
                .param("id", packId).update()).hasMessageContaining("null");
        jdbc.sql("DELETE FROM packs WHERE id = :id").param("id", packId).update();
        assertThat(jdbc.sql("SELECT count(*) FROM pack_slots").query(Integer.class).single()).isZero();
        assertThat(jdbc.sql("SELECT count(*) FROM slot_pull_type WHERE id = :id")
                .param("id", type).query(Integer.class).single()).isEqualTo(1);
    }

    private int createType(String common, String rare) throws Exception {
        return transaction(() -> {
            int id = jdbc.sql("INSERT INTO slot_pull_type (description) VALUES ('Test') RETURNING id")
                    .query(Integer.class).single();
            jdbc.sql("INSERT INTO type_pull_rate SELECT :id, id, CASE code WHEN 'COMMON' THEN :common ELSE :rare END "
                    + "FROM rarities WHERE code IN ('COMMON', 'RARE')")
                    .param("id", id).param("common", new java.math.BigDecimal(common))
                    .param("rare", new java.math.BigDecimal(rare)).update();
            return id;
        });
    }

    private <T> T transaction(java.util.function.Supplier<T> action) throws Exception {
        connection.setAutoCommit(false);
        try {
            T result = action.get();
            connection.commit();
            return result;
        } catch (Exception error) {
            connection.rollback();
            throw error;
        } finally {
            connection.setAutoCommit(true);
        }
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
                    'artworks', (SELECT jsonb_agg(to_jsonb(a) ORDER BY a.id_art) FROM cards_arts a),
                    'friendships', (SELECT jsonb_agg(to_jsonb(f) ORDER BY f.user_a_id, f.user_b_id)
                        FROM friendships f),
                    'effects', (SELECT jsonb_agg(to_jsonb(e) ORDER BY e.card_id, e.execution_order)
                        FROM card_effects e),
                    'decks', (SELECT jsonb_agg(to_jsonb(d) ORDER BY d.id) FROM decks d),
                    'deckCards', (SELECT jsonb_agg(to_jsonb(d) ORDER BY d.id) FROM decks_cards d),
                    'collection', (SELECT jsonb_agg(to_jsonb(c) ORDER BY c.id_user, c.id_card, c.id_card_art)
                        FROM collections c))::text
                """).query(String.class).single();
    }
}
