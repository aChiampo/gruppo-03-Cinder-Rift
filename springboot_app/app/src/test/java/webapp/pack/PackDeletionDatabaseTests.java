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
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import tools.jackson.databind.json.JsonMapper;
import webapp.AppApplication;
import webapp.card.CardCatalogRepository;

import static org.assertj.core.api.Assertions.*;

/** Verifies real administrative deletion commits and rollbacks in disposable PostgreSQL schemas. */
@SpringBootTest(classes = AppApplication.class, properties = {"app.security.session-token-cleanup-cron=-",
        "app.security.password-reset-token-cleanup-cron=-"})
class PackDeletionDatabaseTests {
    @Autowired private DataSource dataSource;
    @Autowired private JsonMapper json;
    private Connection connection;
    private String originalSchema;
    private String schema;
    private JdbcClient jdbc;
    private AnnotationConfigApplicationContext transactions;
    private PackService packs;
    private int packId;

    @BeforeEach
    void setUp() throws Exception {
        schema = "pack_deletion_test_" + UUID.randomUUID().toString().replace("-", "");
        connection = dataSource.getConnection();
        originalSchema = connection.getSchema();
        try (var statement = connection.createStatement()) { statement.execute("CREATE SCHEMA " + schema); }
        Flyway.configure().dataSource(dataSource).schemas(schema).defaultSchema(schema).load().migrate();
        connection.setSchema(schema);
        var isolated = new SingleConnectionDataSource(connection, true);
        jdbc = JdbcClient.create(isolated);
        packId = jdbc.sql("""
                INSERT INTO packs (id_artwork_pull_rates, pack_name, pack_description, pack_price,
                    allowed_factions, allowed_types, allowed_expansions, allowed_rarities)
                SELECT id, 'Deletion fixture', '{}', 300, '[]', '[]', '[]', '[]'
                FROM artwork_pull_rates RETURNING id
                """).query(Integer.class).single();
        jdbc.sql("UPDATE inventory SET cobalt_shards = 70").update();
        jdbc.sql("INSERT INTO inventory_packs SELECT id, :id, 3 FROM inventory")
                .param("id", packId).update();
        jdbc.sql("""
                INSERT INTO user_mercy (id_user, id_pack, mercy_counter) SELECT id, :id, 3 FROM users
                """).param("id", packId).update();
        transactions = new AnnotationConfigApplicationContext();
        transactions.register(TransactionConfig.class);
        transactions.registerBean("transactionManager", DataSourceTransactionManager.class,
                () -> new DataSourceTransactionManager(isolated));
        transactions.registerBean(PackService.class, () -> new PackService(
                new PackRepository(jdbc, json), new CardCatalogRepository(jdbc), json,
                new PackConfigurationValidationService(new PackDrawRepository(jdbc))));
        transactions.refresh();
        packs = transactions.getBean(PackService.class);
    }

    @AfterEach
    void tearDown() throws Exception {
        if (transactions != null) transactions.close();
        if (connection != null) {
            try {
                connection.setSchema(originalSchema);
                try (var statement = connection.createStatement()) {
                    statement.execute("DROP SCHEMA " + schema + " CASCADE");
                }
            } finally { connection.close(); }
        }
    }

    @Test
    void failureAfterRefundAndCleanupRollsBackEverythingAndRetryRefundsOnce() {
        jdbc.sql("""
                CREATE FUNCTION reject_pack_delete() RETURNS trigger LANGUAGE plpgsql AS $$
                BEGIN RAISE EXCEPTION 'Injected deletion failure'; END $$
                """).update();
        jdbc.sql("CREATE TRIGGER reject_pack_delete BEFORE DELETE ON packs "
                + "FOR EACH ROW EXECUTE FUNCTION reject_pack_delete()").update();
        assertThatThrownBy(() -> packs.delete(packId)).isInstanceOf(DataAccessException.class)
                .hasMessageContaining("Injected deletion failure");
        assertUnchanged();
        jdbc.sql("DROP TRIGGER reject_pack_delete ON packs").update();
        packs.delete(packId);
        assertThat(jdbc.sql("SELECT cobalt_shards FROM inventory").query(Integer.class).single()).isEqualTo(970);
        for (String table : new String[]{"packs", "inventory_packs", "user_mercy"}) {
            assertThat(jdbc.sql("SELECT count(*) FROM " + table).query(Integer.class).single()).isZero();
        }
    }

    @Test
    void unrepresentableRefundFailsWithoutWrappingBalanceOrLosingResources() {
        jdbc.sql("UPDATE packs SET pack_price = 1431655766 WHERE id = :id").param("id", packId).update();
        assertThatThrownBy(() -> packs.delete(packId)).isInstanceOf(DataAccessException.class);
        assertUnchanged();
    }

    private void assertUnchanged() {
        assertThat(jdbc.sql("SELECT cobalt_shards FROM inventory").query(Integer.class).single()).isEqualTo(70);
        assertThat(jdbc.sql("SELECT item_quantity FROM inventory_packs").query(Integer.class).single()).isEqualTo(3);
        assertThat(jdbc.sql("SELECT mercy_counter FROM user_mercy").query(Integer.class).single()).isEqualTo(3);
        assertThat(jdbc.sql("SELECT id FROM packs").query(Integer.class).single()).isEqualTo(packId);
    }

    @TestConfiguration(proxyBeanMethods = false)
    @EnableTransactionManagement
    static class TransactionConfig {}
}
