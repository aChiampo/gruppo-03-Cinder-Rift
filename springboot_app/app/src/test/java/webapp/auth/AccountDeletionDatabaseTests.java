package webapp.auth;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import javax.sql.DataSource;

import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpHeaders;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DelegatingDataSource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.mock.web.MockServletContext;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import webapp.AppApplication;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.spy;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Exercises the HTTP security chain and real commits, rollbacks and locks in a disposable schema. */
@SpringBootTest(classes = AppApplication.class, properties = {"app.security.session-token-cleanup-cron=-",
        "app.security.password-reset-token-cleanup-cron=-"})
class AccountDeletionDatabaseTests {
    @Autowired private DataSource dataSource;
    @Autowired private ConfigurableApplicationContext application;
    private String schema;
    private JdbcClient jdbc;
    private AccountRepository accounts;
    private AuthenticationService authentication;
    private AnnotationConfigWebApplicationContext context;
    private MockMvc mvc;
    private AuthenticationService.IssuedSession player;
    private AuthenticationService.IssuedSession admin;

    @BeforeEach
    void setUp() throws Exception {
        schema = "account_deletion_test_" + UUID.randomUUID().toString().replace("-", "");
        try (var connection = dataSource.getConnection(); var statement = connection.createStatement()) {
            statement.execute("CREATE SCHEMA " + schema);
        }
        DataSource isolated = new DelegatingDataSource(dataSource) {
            @Override
            public java.sql.Connection getConnection() throws java.sql.SQLException {
                var connection = super.getConnection();
                String originalSchema = connection.getSchema();
                connection.setSchema(schema);
                // Restore the pooled connection before the disposable schema is dropped.
                return (java.sql.Connection) Proxy.newProxyInstance(
                        connection.getClass().getClassLoader(),
                        new Class<?>[] {java.sql.Connection.class},
                        (proxy, method, args) -> {
                            if (method.getName().equals("close")) {
                                try {
                                    connection.setSchema(originalSchema);
                                } finally {
                                    connection.close();
                                }
                                return null;
                            }
                            try {
                                return method.invoke(connection, args);
                            } catch (InvocationTargetException exception) {
                                throw exception.getCause();
                            }
                        });
            }
        };
        new ResourceDatabasePopulator(new ClassPathResource("db/migration/V1__create_f01_schema.sql"),
                new ClassPathResource("db/migration/V2__seed_f01_roles.sql"),
                new ClassPathResource("db/migration/V11__create_friendships.sql")).execute(isolated);
        jdbc = JdbcClient.create(isolated);
        accounts = spy(new AccountRepository(jdbc));
        context = new AnnotationConfigWebApplicationContext();
        context.setServletContext(new MockServletContext());
        context.getEnvironment().merge(application.getEnvironment());
        context.register(DeletionTestConfiguration.class);
        context.addBeanFactoryPostProcessor(factory -> {
            factory.registerSingleton("accounts", accounts);
            factory.registerSingleton("transactionManager", new DataSourceTransactionManager(isolated));
        });
        context.refresh();
        authentication = context.getBean(AuthenticationService.class);
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        player = authentication.register("Player", "player@example.invalid", "test password");
        var registeredAdmin = authentication.register("Administrator", "admin@example.invalid", "test password");
        jdbc.sql("UPDATE users SET id_role = (SELECT id FROM roles WHERE code = 'ADMIN') WHERE id = :id")
                .param("id", registeredAdmin.user().id()).update();
        admin = authentication.login("admin@example.invalid", "test password");
    }

    @AfterEach
    void tearDown() throws Exception {
        if (context != null) {
            context.close();
        }
        if (schema != null && schema.matches("account_deletion_test_[a-f0-9]{32}")) {
            try (var connection = dataSource.getConnection(); var statement = connection.createStatement()) {
                statement.execute("DROP SCHEMA " + schema + " CASCADE");
            }
        }
    }

    @Test
    void selfDeletionRevokesEverySessionAndRecoveryTokenWhilePreservingOtherAccounts() throws Exception {
        var secondSession = authentication.login(player.user().email(), "test password");
        String recoveryToken = seedRecoveryToken(player.user().id());
        seedRecoveryToken(admin.user().id());
        authentication.requestPasswordReset(player.user().email());
        var response = mvc.perform(delete("/api/auth/users/{id}", player.user().id())
                        .header(HttpHeaders.AUTHORIZATION, bearer(player)).with(csrf()))
                .andExpect(status().isNoContent()).andReturn().getResponse();
        var cookie = response.getCookie("__Host-cinder-refresh");
        assertThat(cookie).isNotNull();
        assertThat(cookie.getValue()).isEmpty();
        assertThat(cookie.getMaxAge()).isZero();
        assertThat(cookie.getPath()).isEqualTo("/");
        assertThat(cookie.getSecure()).isTrue();
        assertThat(cookie.isHttpOnly()).isTrue();
        assertThat(cookie.getAttribute("SameSite")).isEqualTo("Strict");
        assertThat(isDeleted(player.user().id())).isTrue();
        assertThat(jdbc.sql("SELECT count(*) FROM refresh_tokens WHERE id_user = :id AND revoked_at IS NULL")
                .param("id", player.user().id()).query(Integer.class).single()).isZero();
        assertThat(jdbc.sql("SELECT count(*) FROM password_reset_tokens WHERE id_user = :id")
                .param("id", player.user().id()).query(Integer.class).single()).isZero();
        assertThat(jdbc.sql("SELECT count(*) FROM inventory WHERE id_user = :id")
                .param("id", player.user().id()).query(Integer.class).single()).isEqualTo(1);
        for (var session : List.of(player, secondSession)) {
            mvc.perform(get("/api/auth/user/me").header(HttpHeaders.AUTHORIZATION, bearer(session)))
                    .andExpect(status().isUnauthorized());
            mvc.perform(post("/api/auth/session-renewals").with(csrf())
                            .cookie(new Cookie("__Host-cinder-refresh", session.refreshToken())))
                    .andExpect(status().isUnauthorized());
        }
        mvc.perform(post("/api/auth/login").with(csrf()).contentType("application/json")
                        .content("{\"email\":\"player@example.invalid\",\"password\":\"test password\"}"))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/api/auth/password-resets").with(csrf()).contentType("application/json")
                        .content("{\"token\":\"" + recoveryToken + "\",\"newPassword\":\"new password\"}"))
                .andExpect(status().isBadRequest());
        authentication.requestPasswordReset(player.user().email());
        assertThat(jdbc.sql("SELECT count(*) FROM password_reset_tokens WHERE id_user = :id")
                .param("id", player.user().id()).query(Integer.class).single()).isZero();
        mvc.perform(get("/api/auth/user/me").header(HttpHeaders.AUTHORIZATION, bearer(admin)))
                .andExpect(status().isOk());
        assertThat(jdbc.sql("SELECT count(*) FROM password_reset_tokens WHERE id_user = :id")
                .param("id", admin.user().id()).query(Integer.class).single()).isEqualTo(1);
        mvc.perform(delete("/api/auth/users/{id}", player.user().id())
                        .header(HttpHeaders.AUTHORIZATION, bearer(player)).with(csrf()))
                .andExpect(status().isUnauthorized());
    }

    @ParameterizedTest
    @ValueSource(strings = {"USER", "ADMIN", "OTHER"})
    void adminCanDeleteAnyTargetRoleAndRepeatDeletion(String role) throws Exception {
        jdbc.sql("INSERT INTO roles (code) VALUES ('OTHER')").update();
        jdbc.sql("UPDATE users SET id_role = (SELECT id FROM roles WHERE code = :role) WHERE id = :id")
                .param("role", role).param("id", player.user().id()).update();
        for (int attempt = 0; attempt < 2; attempt++) {
            mvc.perform(delete("/api/auth/users/{id}", player.user().id())
                            .header(HttpHeaders.AUTHORIZATION, bearer(admin)).with(csrf()))
                    .andExpect(status().isNoContent())
                    .andExpect(header().doesNotExist(HttpHeaders.SET_COOKIE));
        }
        assertThat(isDeleted(player.user().id())).isTrue();
        mvc.perform(get("/api/auth/user/me").header(HttpHeaders.AUTHORIZATION, bearer(admin)))
                .andExpect(status().isOk());
    }

    @Test
    void userCannotDeleteAnotherAccountOrProbeAnUnknownTarget() throws Exception {
        for (int target : List.of(admin.user().id(), Integer.MAX_VALUE)) {
            mvc.perform(delete("/api/auth/users/{id}", target)
                            .header(HttpHeaders.AUTHORIZATION, bearer(player)).with(csrf()))
                    .andExpect(status().isForbidden());
        }
        assertThat(isDeleted(admin.user().id())).isFalse();
        mvc.perform(delete("/api/auth/users/{id}", Integer.MAX_VALUE)
                        .header(HttpHeaders.AUTHORIZATION, bearer(admin)).with(csrf()))
                .andExpect(status().isNotFound());
    }

    @Test
    void deletionRequiresBothBearerAuthenticationAndValidCsrf() throws Exception {
        String path = "/api/auth/users/" + player.user().id();
        var bootstrap = mvc.perform(get("/api/auth/csrf-tokens"))
                .andExpect(status().isOk()).andReturn().getResponse();
        Cookie cookie = bootstrap.getCookie("__Host-cinder-csrf");
        assertThat(cookie).isNotNull();
        String token = com.jayway.jsonpath.JsonPath.read(bootstrap.getContentAsString(), "$.token");
        mvc.perform(delete(path).header(HttpHeaders.AUTHORIZATION, bearer(player)))
                .andExpect(status().isForbidden());
        mvc.perform(delete(path).cookie(cookie).header("X-XSRF-TOKEN", "invalid")
                        .header(HttpHeaders.AUTHORIZATION, bearer(player)))
                .andExpect(status().isForbidden());
        mvc.perform(delete(path).cookie(cookie).header("X-XSRF-TOKEN", token))
                .andExpect(status().isUnauthorized());
        mvc.perform(delete(path).cookie(cookie).header("X-XSRF-TOKEN", token)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer invalid-token"))
                .andExpect(status().isUnauthorized());
        assertThat(isDeleted(player.user().id())).isFalse();
        mvc.perform(delete(path).cookie(cookie).header("X-XSRF-TOKEN", token)
                        .header(HttpHeaders.AUTHORIZATION, bearer(player)))
                .andExpect(status().isNoContent());
        assertThat(isDeleted(player.user().id())).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "-1", "invalid", "2147483648"})
    void invalidTargetIdsAreRejected(String id) throws Exception {
        mvc.perform(delete("/api/auth/users/{id}", id).header(HttpHeaders.AUTHORIZATION, bearer(admin)).with(csrf()))
                .andExpect(status().isBadRequest());
    }

    @ParameterizedTest
    @ValueSource(strings = {"USER", "OTHER", "DELETED"})
    void serviceRechecksCallerStatusAndRoleAfterAuthentication(String state) {
        jdbc.sql("INSERT INTO roles (code) VALUES ('OTHER')").update();
        if ("DELETED".equals(state)) {
            jdbc.sql("UPDATE users SET is_deleted = true WHERE id = :id").param("id", admin.user().id()).update();
        } else {
            jdbc.sql("UPDATE users SET id_role = (SELECT id FROM roles WHERE code = :role) WHERE id = :id")
                    .param("role", state).param("id", admin.user().id()).update();
        }
        int expectedStatus = "USER".equals(state) ? 403 : 401;
        assertThatThrownBy(() -> authentication.deleteAccount(admin.user(), player.user().id()))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        exception -> assertThat(exception.getStatusCode().value()).isEqualTo(expectedStatus));
        assertThat(isDeleted(player.user().id())).isFalse();
    }

    @Test
    void failureAfterRevocationsAndRecoveryCleanupRollsBackAllChanges() throws Exception {
        seedRecoveryToken(player.user().id());
        jdbc.sql("""
                INSERT INTO friendships (user_a_id, user_b_id, requested_by, status)
                VALUES (:a, :b, :a, 'pending')
                """).param("a", player.user().id()).param("b", admin.user().id()).update();
        jdbc.sql("ALTER TABLE users ADD CONSTRAINT reject_account_deletion CHECK (NOT is_deleted)").update();
        assertThatThrownBy(() -> authentication.deleteAccount(player.user(), player.user().id()))
                .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
        assertThat(isDeleted(player.user().id())).isFalse();
        assertThat(jdbc.sql("SELECT count(*) FROM friendships").query(Integer.class).single()).isEqualTo(1);
        assertThat(jdbc.sql("SELECT count(*) FROM password_reset_tokens WHERE id_user = :id")
                .param("id", player.user().id()).query(Integer.class).single()).isEqualTo(1);
        mvc.perform(get("/api/auth/user/me").header(HttpHeaders.AUTHORIZATION, bearer(player)))
                .andExpect(status().isOk());
    }

    @Test
    void loginRenewalAndRecoveryWaitingBehindDeletionCannotRestoreTheAccount() throws Exception {
        String recoveryToken = seedRecoveryToken(player.user().id());
        var deleting = new CountDownLatch(1);
        var finishDeletion = new CountDownLatch(1);
        var attempts = new CountDownLatch(4);
        doAnswer(invocation -> {
            deleting.countDown();
            assertThat(finishDeletion.await(10, TimeUnit.SECONDS)).isTrue();
            return invocation.callRealMethod();
        }).when(accounts).markDeleted(player.user().id());
        doAnswer(invocation -> {
            attempts.countDown();
            return invocation.callRealMethod();
        }).when(accounts).lockByEmail(player.user().email());
        doAnswer(invocation -> {
            attempts.countDown();
            return invocation.callRealMethod();
        }).when(accounts).lockActiveProfile(player.user().id());
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            var deletion = executor.submit(() -> authentication.deleteAccount(admin.user(), player.user().id()));
            try {
                assertThat(deleting.await(10, TimeUnit.SECONDS)).isTrue();
                var login = executor.submit(() -> assertThatThrownBy(
                        () -> authentication.login(player.user().email(), "test password"))
                        .isInstanceOf(ResponseStatusException.class));
                var renewal = executor.submit(() -> assertThatThrownBy(() -> authentication.renew(player.refreshToken()))
                        .isInstanceOf(ResponseStatusException.class));
                var request = executor.submit(() -> authentication.requestPasswordReset(player.user().email()));
                var reset = executor.submit(() -> assertThatThrownBy(
                        () -> authentication.resetPassword(recoveryToken, "new password"))
                        .isInstanceOf(ResponseStatusException.class));
                assertThat(attempts.await(10, TimeUnit.SECONDS)).isTrue();
                finishDeletion.countDown();
                deletion.get(10, TimeUnit.SECONDS);
                login.get(10, TimeUnit.SECONDS);
                renewal.get(10, TimeUnit.SECONDS);
                request.get(10, TimeUnit.SECONDS);
                reset.get(10, TimeUnit.SECONDS);
            } finally {
                finishDeletion.countDown();
            }
        }
        assertThat(isDeleted(player.user().id())).isTrue();
        assertThat(jdbc.sql("SELECT count(*) FROM password_reset_tokens WHERE id_user = :id")
                .param("id", player.user().id()).query(Integer.class).single()).isZero();
    }

    @Test
    void deletionWaitsForEarlierLoginAndRevokesItsNewSession() throws Exception {
        var issuing = new CountDownLatch(1);
        var finishLogin = new CountDownLatch(1);
        var deleting = new CountDownLatch(1);
        doAnswer(invocation -> {
            issuing.countDown();
            assertThat(finishLogin.await(10, TimeUnit.SECONDS)).isTrue();
            return invocation.callRealMethod();
        }).when(accounts).insertRefreshToken(anyInt(), anyString(), any(Instant.class), any(Instant.class));
        doAnswer(invocation -> {
            deleting.countDown();
            return invocation.callRealMethod();
        }).when(accounts).lockAccountsForDeletion(admin.user().id(), player.user().id());
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            var login = executor.submit(() -> authentication.login(player.user().email(), "test password"));
            try {
                assertThat(issuing.await(10, TimeUnit.SECONDS)).isTrue();
                var deletion = executor.submit(() -> authentication.deleteAccount(admin.user(), player.user().id()));
                assertThat(deleting.await(10, TimeUnit.SECONDS)).isTrue();
                finishLogin.countDown();
                var session = login.get(10, TimeUnit.SECONDS);
                deletion.get(10, TimeUnit.SECONDS);
                mvc.perform(get("/api/auth/user/me").header(HttpHeaders.AUTHORIZATION, bearer(session)))
                        .andExpect(status().isUnauthorized());
            } finally {
                finishLogin.countDown();
            }
        }
    }

    @Test
    void adminsDeletingEachOtherUseTheSameLockOrder() throws Exception {
        jdbc.sql("UPDATE users SET id_role = (SELECT id FROM roles WHERE code = 'ADMIN') WHERE id = :id")
                .param("id", player.user().id()).update();
        var otherAdmin = authentication.login(player.user().email(), "test password");
        var ready = new CountDownLatch(2);
        var start = new CountDownLatch(1);
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            try {
                var first = executor.submit(() -> deleteConcurrently(admin.user(), otherAdmin.user().id(), ready, start));
                var second = executor.submit(() -> deleteConcurrently(otherAdmin.user(), admin.user().id(), ready, start));
                assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();
                start.countDown();
                assertThat(List.of(first.get(10, TimeUnit.SECONDS), second.get(10, TimeUnit.SECONDS)))
                        .containsExactlyInAnyOrder(true, false);
            } finally {
                start.countDown();
            }
        }
    }

    private boolean deleteConcurrently(UserProfile caller, int target, CountDownLatch ready, CountDownLatch start)
            throws Exception {
        ready.countDown();
        assertThat(start.await(10, TimeUnit.SECONDS)).isTrue();
        try {
            authentication.deleteAccount(caller, target);
            return true;
        } catch (ResponseStatusException exception) {
            assertThat(exception.getStatusCode().value()).isEqualTo(401);
            return false;
        }
    }

    private boolean isDeleted(int userId) {
        return jdbc.sql("SELECT is_deleted FROM users WHERE id = :id").param("id", userId)
                .query(Boolean.class).single();
    }

    private String seedRecoveryToken(int userId) throws Exception {
        String token = UUID.randomUUID().toString().replace("-", "") + "A".repeat(11);
        String hash = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest(token.getBytes(StandardCharsets.US_ASCII)));
        Instant now = Instant.now();
        accounts.insertPasswordResetToken(userId, hash, now, now.plusSeconds(600));
        return token;
    }

    private static String bearer(AuthenticationService.IssuedSession session) {
        return "Bearer " + session.accessToken();
    }

    @TestConfiguration(proxyBeanMethods = false)
    @EnableWebMvc
    @EnableTransactionManagement
    @Import({SecurityConfiguration.class, AuthController.class})
    static class DeletionTestConfiguration {
        @Bean
        AuthenticationService authentication(AccountRepository accounts, PasswordEncoder passwords,
                JwtEncoder jwtEncoder, Clock clock) {
            return new AuthenticationService(accounts, passwords, jwtEncoder, clock,
                    Duration.ofMinutes(15), Duration.ofDays(14));
        }
    }
}
