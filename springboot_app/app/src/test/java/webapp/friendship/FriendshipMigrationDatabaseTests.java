package webapp.friendship;

import java.sql.Connection;
import java.util.UUID;
import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;
import static org.assertj.core.api.Assertions.*;

/** Verifies V10 installation, upgrade, constraints and physical-deletion cascades. */
@SpringBootTest(properties = {"app.security.session-token-cleanup-cron=-", "app.security.password-reset-token-cleanup-cron=-"})
class FriendshipMigrationDatabaseTests {
    @Autowired DataSource dataSource;
    String schema;
    String originalSchema;
    Connection connection;
    JdbcClient jdbc;

    @BeforeEach
    void createSchema() throws Exception {
        schema = "friendship_test_" + UUID.randomUUID().toString().replace("-", "");
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
    void freshInstallationAndRepeatStartup() {
        assertThat(target("10").migrate().migrationsExecuted).isEqualTo(10);
        assertThat(target("10").migrate().migrationsExecuted).isZero();
    }

    @Test
    void upgradeFromV9PreservesExistingProfilesAndCards() {
        target("9").migrate();
        int people = count("users"), cards = count("cards");
        assertThat(target("10").migrate().migrationsExecuted).isEqualTo(1);
        assertThat(count("users")).isEqualTo(people);
        assertThat(count("cards")).isEqualTo(cards);
        assertThat(count("friendships")).isZero();
    }

    @ParameterizedTest
    @ValueSource(strings = {"user_a_id = user_b_id", "requested_by = 2147483647", "status = 'removed'",
            "accepted_at = now()", "status = 'accepted'"})
    void rejectsInvalidRows(String assignment) {
        target("10").migrate();
        int a = user(), b = user();
        insert(a, b, a);
        assertThatThrownBy(() -> jdbc.sql("UPDATE friendships SET " + assignment).update())
                .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
    }

    @Test
    void enforcesUniquenessForeignKeysDefaultsAndCascadesOnEitherSide() {
        target("10").migrate();
        int a = user(), b = user(), c = user();
        insert(a, b, b);
        insert(b, c, c);
        assertThatThrownBy(() -> insert(a, b, a)).isInstanceOf(org.springframework.dao.DuplicateKeyException.class);
        assertThatThrownBy(() -> insert(a, Integer.MAX_VALUE, a))
                .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
        assertThat(jdbc.sql("SELECT count(*) FROM friendships WHERE created_at IS NOT NULL AND accepted_at IS NULL")
                .query(Integer.class).single()).isEqualTo(2);
        jdbc.sql("DELETE FROM users WHERE id = :id").param("id", b).update();
        assertThat(count("friendships")).isZero();
    }

    private void insert(int a, int b, int requester) {
        jdbc.sql("INSERT INTO friendships (user_a_id, user_b_id, requested_by, status) VALUES (:a, :b, :r, 'pending')")
                .param("a", a).param("b", b).param("r", requester).update();
    }

    private int user() {
        String name = UUID.randomUUID().toString();
        return jdbc.sql("""
                INSERT INTO users (nickname, email, hash_password, id_role)
                VALUES (:name, :email, 'unused', (SELECT id FROM roles WHERE code = 'USER')) RETURNING id
                """).param("name", name).param("email", name + "@example.invalid").query(Integer.class).single();
    }

    private int count(String table) { return jdbc.sql("SELECT count(*) FROM " + table).query(Integer.class).single(); }
    private Flyway target(String version) {
        return Flyway.configure().dataSource(dataSource).schemas(schema).defaultSchema(schema).target(version).load();
    }
}
