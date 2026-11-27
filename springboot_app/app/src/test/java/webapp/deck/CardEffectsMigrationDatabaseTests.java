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

/** Runs the real migrations in disposable PostgreSQL schemas. */
@SpringBootTest(properties = {"app.security.session-token-cleanup-cron=-",
        "app.security.password-reset-token-cleanup-cron=-"})
class CardEffectsMigrationDatabaseTests {
    @Autowired private DataSource dataSource;
    private String schema;
    private String originalSchema;
    private Connection connection;
    private JdbcClient jdbc;

    @BeforeEach
    void setUp() throws Exception {
        schema = "effects_test_" + UUID.randomUUID().toString().replace("-", "");
        connection = dataSource.getConnection();
        originalSchema = connection.getSchema();
        try (var statement = connection.createStatement()) {
            statement.execute("CREATE SCHEMA " + schema);
        }
        connection.setSchema(schema);
        jdbc = JdbcClient.create(new SingleConnectionDataSource(connection, true));
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

    private Flyway migration(String target) {
        return Flyway.configure().dataSource(dataSource).schemas(schema).defaultSchema(schema).target(target).load();
    }

    @Test
    void freshInstallationSeedsSevenBindingsForFiveEquipmentCards() {
        assertThat(migration("8").migrate().migrationsExecuted).isEqualTo(8);
        assertThat(count("effects")).isEqualTo(3);
        assertThat(count("card_effects")).isEqualTo(7);
        assertThat(jdbc.sql("SELECT count(DISTINCT card_id) FROM card_effects").query(Integer.class).single()).isEqualTo(5);
        assertSeededEffect(cardId("Pulse Blade"), "ATTACK_BONUS", 1, 1);
        assertSeededEffect(cardId("Recovery Armor"), "STARTING_DEFENSE_MODIFIER", 2, 1);
        assertSeededEffect(cardId("Breach Capacitor"), "FIRST_EXCHANGE_ATTACK_BONUS", 2, 1);
        assertSeededEffect(cardId("Overdrive Harness"), "ATTACK_BONUS", 2, 1);
        assertSeededEffect(cardId("Overdrive Harness"), "STARTING_DEFENSE_MODIFIER", -1, 2);
        assertSeededEffect(cardId("Bastion Rig"), "ATTACK_BONUS", 1, 1);
        assertSeededEffect(cardId("Bastion Rig"), "STARTING_DEFENSE_MODIFIER", 2, 2);
        assertThat(migration("8").migrate().migrationsExecuted).isZero();
    }

    @Test
    void usesGeneratedCardAndEffectIdsDespiteExistingRowsAndSequenceGaps() {
        migration("4").migrate();
        jdbc.sql("INSERT INTO cards (name) VALUES ('Discarded identity')").update();
        jdbc.sql("DELETE FROM cards WHERE name = 'Discarded identity'").update();
        int unrelated = jdbc.sql("INSERT INTO cards (name) VALUES ('Pulse Blade') RETURNING id")
                .query(Integer.class).single();
        migration("6").migrate();
        assertThat(cardId("Pulse Blade")).isGreaterThan(unrelated);
        int sentinel = cardId("Sentinel K-17");
        assertThat(migration("7").migrate().migrationsExecuted).isEqualTo(1);
        jdbc.sql("INSERT INTO effects (code, description) VALUES ('CUSTOM_TEST', 'Existing effect')").update();
        assertThat(migration("8").migrate().migrationsExecuted).isEqualTo(1);
        assertThat(count("cards")).isEqualTo(31);
        assertThat(count("collections")).isEqualTo(30);
        assertThat(cardId("Sentinel K-17")).isEqualTo(sentinel);
        assertThat(jdbc.sql("SELECT id FROM effects WHERE code = 'ATTACK_BONUS'").query(Integer.class).single()).isGreaterThan(1);
        assertSeededEffect(cardId("Pulse Blade"), "ATTACK_BONUS", 1, 1);
        assertThat(count("card_effects")).isEqualTo(7);
        assertThat(jdbc.sql("SELECT count(*) FROM card_effects WHERE card_id = :id")
                .param("id", unrelated).query(Integer.class).single()).isZero();
    }

    @Test
    void constraintsProtectReferencesParametersAndExecutionOrder() {
        migration("8").migrate();
        assertThatThrownBy(() -> jdbc.sql("""
                INSERT INTO card_effects (card_id, effect_id, parameters, execution_order)
                VALUES (-1, (SELECT min(id) FROM effects), '{}', 1)
                """).update()).hasMessageContaining("card_effects_card_id_fkey");
        assertThatThrownBy(() -> jdbc.sql("""
                INSERT INTO card_effects (card_id, effect_id, parameters, execution_order)
                VALUES ((SELECT min(id) FROM cards), -1, '{}', 1)
                """).update()).hasMessageContaining("card_effects_effect_id_fkey");
        assertThatThrownBy(() -> jdbc.sql("UPDATE card_effects SET parameters = '[]'").update())
                .hasMessageContaining("card_effects_parameters_check");
        assertThatThrownBy(() -> jdbc.sql("UPDATE card_effects SET execution_order = 0").update())
                .hasMessageContaining("card_effects_execution_order_check");
        assertThatThrownBy(() -> jdbc.sql("""
                INSERT INTO card_effects (card_id, effect_id, parameters, execution_order)
                SELECT card_id, effect_id, parameters, execution_order FROM card_effects LIMIT 1
                """).update()).hasMessageContaining("card_effects_card_order_key");
    }

    @Test
    void creatureCardsCanUseTheSameEffectsAndDeletingACardDeletesItsBindings() {
        migration("8").migrate();
        int cardId = jdbc.sql("""
                INSERT INTO cards (name, id_type) VALUES ('Effect creature',
                    (SELECT id FROM card_types WHERE code = 'CREATURE')) RETURNING id
                """).query(Integer.class).single();
        jdbc.sql("""
                INSERT INTO card_effects (card_id, effect_id, parameters, execution_order)
                VALUES (:cardId, (SELECT id FROM effects WHERE code = 'ATTACK_BONUS'), '{"amount":3}', 1)
                """).param("cardId", cardId).update();
        assertThat(count("card_effects")).isEqualTo(8);
        assertThatThrownBy(() -> jdbc.sql("DELETE FROM effects WHERE code = 'ATTACK_BONUS'").update())
                .hasMessageContaining("card_effects_effect_id_fkey");
        jdbc.sql("DELETE FROM cards WHERE id = :cardId").param("cardId", cardId).update();
        assertThat(count("card_effects")).isEqualTo(7);
    }

    private int cardId(String name) {
        return jdbc.sql("""
                SELECT c.id FROM cards c JOIN expansions e ON e.id = c.id_expansion
                WHERE c.name = :name AND e.code = 'CR01'
                """).param("name", name).query(Integer.class).single();
    }

    private int count(String table) {
        return jdbc.sql("SELECT count(*) FROM " + table).query(Integer.class).single();
    }

    private void assertSeededEffect(int card, String effect, int amount, int order) {
        var rows = jdbc.sql("""
                SELECT (ce.parameters ->> 'amount')::integer AS amount, ce.execution_order
                FROM card_effects ce JOIN effects e ON e.id = ce.effect_id
                WHERE ce.card_id = :card AND e.code = :effect
                """).param("card", card).param("effect", effect)
                .query((row, number) -> java.util.List.of(row.getInt("amount"), row.getInt("execution_order"))).list();
        assertThat(rows).containsExactly(java.util.List.of(amount, order));
    }
}
