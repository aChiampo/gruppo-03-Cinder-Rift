package webapp.auth;

import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import javax.sql.DataSource;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;
import webapp.battle.*;
import webapp.deck.*;
import org.springframework.transaction.PlatformTransactionManager;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Real F01 JWTs, F05 data, PostgreSQL guards and transactions, and an observable HTTP engine double. */
@SpringBootTest(properties = {"app.security.session-token-cleanup-cron=-",
        "app.security.password-reset-token-cleanup-cron=-"})
class BattleFlowDatabaseTests {
    private static final JsonMapper JSON = JsonMapper.builder().build();
    private static final AtomicInteger CALLS = new AtomicInteger();
    private static final AtomicReference<JsonNode> SNAPSHOT = new AtomicReference<>();
    private static volatile CountDownLatch entered;
    private static volatile CountDownLatch release;
    private static volatile boolean block;
    private static volatile int engineStatus;
    private static volatile String engineBody;
    private static final ExecutorService HTTP_WORKERS = Executors.newVirtualThreadPerTaskExecutor();
    private static final HttpServer ENGINE = startEngine();

    @Autowired private WebApplicationContext context;
    @Autowired private AuthenticationService authentication;
    @Autowired private JdbcClient jdbc;
    @Autowired private DataSource dataSource;
    @Autowired private PostgresBattleGuard guards;
    @Autowired private BattleSnapshotService snapshots;
    @Autowired private DeckRepository decks;
    @Autowired private DeckValidator validator;
    @Autowired private PlatformTransactionManager transactions;
    private MockMvc mvc;
    private AuthenticationService.IssuedSession owner;
    private int deckId;
    private List<Integer> cardIds;

    @DynamicPropertySource
    static void engineUrl(DynamicPropertyRegistry registry) {
        registry.add("app.battle.engine-url", () -> "http://127.0.0.1:" + ENGINE.getAddress().getPort());
    }

    private static HttpServer startEngine() {
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            server.setExecutor(HTTP_WORKERS);
            server.createContext("/api/battle/pve/", exchange -> {
                try {
                    JsonNode request = JSON.readTree(exchange.getRequestBody().readAllBytes());
                    SNAPSHOT.set(request);
                    CALLS.incrementAndGet();
                    entered.countDown();
                    if (block) release.await(15, TimeUnit.SECONDS);
                    ObjectNode result = JSON.createObjectNode();
                    result.put("stageId", "stage_01").put("stageNumber", 1).put("stageTitle", "Tutorial: title")
                            .put("enemyName", "Bot").put("outcome", "VICTORY")
                            .put("winner", request.get("challenger").get("displayName").stringValue());
                    result.set("challenger", request.get("challenger"));
                    result.putArray("tutorialHints").add("hint");
                    result.putArray("battleLog").add("hint").add("  combat\nline  ").add("[EFFECT] event").add("hint");
                    byte[] body = (engineBody == null ? JSON.writeValueAsString(result) : engineBody)
                            .getBytes(StandardCharsets.UTF_8);
                    exchange.getResponseHeaders().set("Content-Type", engineStatus == 200
                            ? "application/json" : "application/problem+json");
                    exchange.sendResponseHeaders(engineStatus, body.length);
                    exchange.getResponseBody().write(body);
                } catch (InterruptedException interrupted) { Thread.currentThread().interrupt(); }
                finally { exchange.close(); }
            });
            server.start();
            return server;
        } catch (Exception failure) { throw new IllegalStateException(failure); }
    }

    @BeforeEach
    void prepare() {
        entered = new CountDownLatch(1);
        release = new CountDownLatch(1);
        block = false;
        engineStatus = 200;
        engineBody = null;
        CALLS.set(0);
        SNAPSHOT.set(null);
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity())
                .addFilters(context.getBean(BattleBudgetFilter.class)).build();
        String suffix = UUID.randomUUID().toString();
        owner = authentication.register("Battle-" + suffix, suffix + "@example.invalid", "Test password 1");
        deckId = jdbc.sql("INSERT INTO decks(id_user) VALUES (:user) RETURNING id")
                .param("user", owner.user().id()).query(Integer.class).single();
        // Isolated fixtures remain valid regardless of pending editorial seed repairs.
        cardIds = jdbc.sql("""
                INSERT INTO cards(name, id_type, id_faction, id_rarity, energy_cost, power, value_atk, value_def)
                SELECT 'F07 card ' || n,
                       (SELECT id FROM card_types WHERE code = CASE WHEN n <= 25 THEN 'CREATURE' ELSE 'EQUIPMENT' END),
                       (SELECT id FROM factions WHERE code = 'CYB'),
                       (SELECT id FROM rarities WHERE code = 'COMMON'), 1, 1, 3, 5
                FROM generate_series(1, 30) n RETURNING id
                """).query(Integer.class).list();
        for (int index = 0; index < cardIds.size(); index++) {
            int card = cardIds.get(index);
            int art = jdbc.sql("INSERT INTO cards_arts(id_card, art_code) VALUES (:card, 'default') RETURNING id_art")
                    .param("card", card).query(Integer.class).single();
            jdbc.sql("""
                    INSERT INTO collections(id_user, id_card, id_card_art, owned_replica, tradable_replica)
                    VALUES (:user, :card, :art, 1, 1)
                    """).param("user", owner.user().id()).param("card", card).param("art", art).update();
            jdbc.sql("INSERT INTO decks_cards(id_deck,id_card,id_card_art,destination) VALUES (:deck,:card,:art,:dest)")
                    .param("deck", deckId).param("card", card).param("art", art)
                    .param("dest", index < 25 ? "ROUND" + (index / 5 + 1) : "EQUIPMENT").update();
        }
        jdbc.sql("""
                INSERT INTO card_effects(card_id, effect_id, parameters, execution_order)
                VALUES (:card, (SELECT id FROM effects WHERE code='ATTACK_BONUS'), '{"amount":2}', 2),
                       (:card, (SELECT id FROM effects WHERE code='STARTING_DEFENSE_MODIFIER'), '{"amount":1}', 1)
                """).param("card", cardIds.getFirst()).update();
    }

    @AfterEach void unblockEngine() { release.countDown(); }
    @AfterAll static void stopEngine() { ENGINE.stop(0); HTTP_WORKERS.shutdownNow(); }

    private MvcResult send(String body) throws Exception {
        return mvc.perform(post("/api/battle/pve/stage_01").header("Authorization", "Bearer " + owner.accessToken())
                .contentType(MediaType.APPLICATION_JSON).content(body)).andReturn();
    }

    private MvcResult send() throws Exception { return send("{\"deckId\":" + deckId + "}"); }

    private MvcResult finish(MvcResult pending) throws Exception {
        pending.getAsyncResult(11000);
        return mvc.perform(asyncDispatch(pending)).andReturn();
    }

    @ParameterizedTest
    @ValueSource(strings = {"USER", "ADMIN"})
    void completesBattleWithTrustedProfileOrderedEffectsAndCleanLog(String role) throws Exception {
        jdbc.sql("UPDATE users SET id_role=(SELECT id FROM roles WHERE code=:role) WHERE id=:user")
                .param("role", role).param("user", owner.user().id()).update();
        MvcResult response = finish(send());
        assertThat(response.getResponse().getStatus()).isEqualTo(200);
        JsonNode result = JSON.readTree(response.getResponse().getContentAsString());
        assertThat(result.has("tutorialHints")).isFalse();
        assertThat(result.get("battleLog").get(0).stringValue()).isEqualTo("  combat\nline  ");
        assertThat(result.get("battleLog").get(2).stringValue()).isEqualTo("hint");
        assertThat(CALLS.get()).isEqualTo(1);
        JsonNode payload = SNAPSHOT.get();
        assertThat(payload.get("challenger").get("userId").stringValue()).isEqualTo(Integer.toString(owner.user().id()));
        JsonNode entries = payload.get("deck").get("entries");
        assertThat(entries.size()).isEqualTo(30);
        assertThat(entries.get(0).get("cardId").isIntegralNumber()).isTrue();
        assertThat(entries.get(0).get("faction").stringValue()).isEqualTo("CYBORG");
        assertThat(entries.get(0).get("effects").get(0).get("code").stringValue()).isEqualTo("STARTING_DEFENSE_MODIFIER");
        try (var next = guards.acquire(owner.user().id(), BattleBudget.start(Clock.systemUTC()))) { assertThat(next).isNotNull(); }
    }

    @ParameterizedTest
    @ValueSource(strings = {"null", "{}", "[]", "{\"deckId\":null}", "{\"deckId\":\"1\"}",
            "{\"deckId\":1.0}", "{\"deckId\":2147483648}", "{\"deckId\":0}", "{\"deckId\":-1}",
            "{\"deckId\":1,\"userId\":1}", "{\"deckId\":true}", "{"})
    void rejectsInvalidPublicBodiesBeforeCallingTheEngine(String body) throws Exception {
        assertThat(send(body).getResponse().getStatus()).isEqualTo(400);
        assertThat(CALLS.get()).isZero();
    }

    @Test
    void rejectsMissingBodiesUnsupportedTypesAndInvalidOrRevokedTokens() throws Exception {
        mvc.perform(post("/api/battle/pve/stage_01").header("Authorization", "Bearer " + owner.accessToken())
                .contentType(MediaType.APPLICATION_JSON)).andExpect(status().isBadRequest());
        mvc.perform(post("/api/battle/pve/stage_01").header("Authorization", "Bearer " + owner.accessToken())
                .contentType(MediaType.TEXT_PLAIN).content("{}")).andExpect(status().isUnsupportedMediaType());
        mvc.perform(post("/api/battle/pve/stage_01").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/api/battle/pve/stage_01").header("Authorization", "Bearer invalid")
                .contentType(MediaType.APPLICATION_JSON).content("{}")).andExpect(status().isUnauthorized());
        jdbc.sql("UPDATE refresh_tokens SET revoked_at=now() WHERE id_user=:user").param("user", owner.user().id()).update();
        assertThat(send().getResponse().getStatus()).isEqualTo(401);
        assertThat(CALLS.get()).isZero();
    }

    @Test
    void makesMissingAndForeignDecksIndistinguishableAndReleasesGuardAfterValidation() throws Exception {
        int ownDeck = deckId;
        String suffix = UUID.randomUUID().toString();
        int other = authentication.register("Other-" + suffix, suffix + "@example.invalid", "Test password 1").user().id();
        int foreign = jdbc.sql("INSERT INTO decks(id_user) VALUES (:user) RETURNING id")
                .param("user", other).query(Integer.class).single();
        deckId = foreign;
        String foreignResponse = finish(send()).getResponse().getContentAsString();
        deckId = Integer.MAX_VALUE;
        assertThat(finish(send()).getResponse().getContentAsString()).isEqualTo(foreignResponse);
        assertThat(JSON.readTree(foreignResponse).get("status").intValue()).isEqualTo(404);
        deckId = ownDeck;
        jdbc.sql("DELETE FROM decks_cards WHERE id_deck=:deck AND id_card=:card")
                .param("deck", deckId).param("card", cardIds.getFirst()).update();
        MvcResult invalid = finish(send());
        assertThat(invalid.getResponse().getStatus()).isEqualTo(422);
        assertThat(JSON.readTree(invalid.getResponse().getContentAsString()).get("violations").isArray()).isTrue();
        assertThat(CALLS.get()).isZero();
        try (var next = guards.acquire(owner.user().id(), BattleBudget.start(Clock.systemUTC()))) { assertThat(next).isNotNull(); }
    }

    @Test
    void rejectsSameUserAcrossGuardInstancesAndDoesNotHoldDataLocksDuringRemoteWork() throws Exception {
        block = true;
        MvcResult first = send();
        assertThat(entered.await(4, TimeUnit.SECONDS)).isTrue();
        MvcResult second = finish(send());
        assertThat(second.getResponse().getStatus()).isEqualTo(409);
        assertThat(JSON.readTree(second.getResponse().getContentAsString()).get("code").stringValue())
                .isEqualTo("BATTLE_IN_PROGRESS");
        PostgresBattleGuard anotherInstance = new PostgresBattleGuard(dataSource);
        assertThatThrownBy(() -> anotherInstance.acquire(owner.user().id(), BattleBudget.start(Clock.systemUTC())))
                .isInstanceOf(BattleFailure.class);
        try (var independent = anotherInstance.acquire(Integer.MAX_VALUE, BattleBudget.start(Clock.systemUTC()))) {
            assertThat(independent).isNotNull();
        }
        try (var connection = dataSource.getConnection()) {
            connection.setAutoCommit(false);
            try (var statement = connection.createStatement()) {
                statement.executeQuery("SELECT id FROM users WHERE id=" + owner.user().id() + " FOR UPDATE NOWAIT").close();
                statement.executeQuery("SELECT id FROM decks WHERE id=" + deckId + " FOR UPDATE NOWAIT").close();
            }
            connection.rollback();
        }
        release.countDown();
        assertThat(finish(first).getResponse().getStatus()).isEqualTo(200);
        assertThat(CALLS.get()).isEqualTo(1);
        var old = guards.acquire(owner.user().id(), BattleBudget.start(Clock.systemUTC()));
        old.close();
        try (var fresh = anotherInstance.acquire(owner.user().id(), BattleBudget.start(Clock.systemUTC()))) {
            old.close();
            assertThatThrownBy(() -> guards.acquire(owner.user().id(), BattleBudget.start(Clock.systemUTC())))
                    .isInstanceOf(BattleFailure.class);
        }
    }

    @Test
    void releasesGuardAtHttpTimeoutEvenWhileRemoteWorkRemainsUncertain() throws Exception {
        block = true;
        long started = System.nanoTime();
        MvcResult pending = send();
        assertThat(entered.await(4, TimeUnit.SECONDS)).isTrue();
        MvcResult result = finish(pending);
        assertThat(result.getResponse().getStatus()).isEqualTo(504);
        assertThat(TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - started)).isLessThan(10000);
        try (var next = guards.acquire(owner.user().id(), BattleBudget.start(Clock.systemUTC()))) { assertThat(next).isNotNull(); }
        assertThat(CALLS.get()).isEqualTo(1);
    }

    @Test
    void snapshotsAreImmutableAfterPreparationAndIgnoreChallengeability() {
        var payload = snapshots.prepare(owner.user(), deckId, BattleBudget.start(Clock.systemUTC()));
        jdbc.sql("UPDATE cards SET value_atk=99 WHERE id=:card").param("card", cardIds.getFirst()).update();
        assertThat(payload.deck().entries().getFirst().baseAtk()).isEqualTo(3);
        assertThat(jdbc.sql("SELECT is_challengeable FROM decks WHERE id=:deck")
                .param("deck", deckId).query(Boolean.class).single()).isFalse();
    }

    @Test
    void translatesEngineErrorsAndReleasesGuardWithoutExposingInternalDetails() throws Exception {
        engineStatus = 422;
        engineBody = "{\"status\":422,\"violations\":[\"private snapshot error\"]}";
        MvcResult failure = finish(send());
        assertThat(failure.getResponse().getStatus()).isEqualTo(502);
        assertThat(failure.getResponse().getContentAsString()).doesNotContain("private");
        engineStatus = 404;
        engineBody = "{\"status\":404,\"stageId\":\"missing\"}";
        MvcResult missing = finish(send());
        assertThat(missing.getResponse().getStatus()).isEqualTo(404);
        assertThat(JSON.readTree(missing.getResponse().getContentAsString()).get("stageId").stringValue()).isEqualTo("missing");
        engineStatus = 409;
        engineBody = "{\"status\":409,\"eventCode\":\"BLOOD_MOON\"}";
        MvcResult inactive = finish(send());
        assertThat(inactive.getResponse().getStatus()).isEqualTo(409);
        assertThat(JSON.readTree(inactive.getResponse().getContentAsString()).get("eventCode").stringValue())
                .isEqualTo("BLOOD_MOON");
    }

    @Test
    void validationAndStatsShareOneDatabaseViewDespiteAConcurrentCatalogCommit() {
        CardCatalogRepository concurrentCatalog = new CardCatalogRepository(jdbc) {
            @Override
            public java.util.Map<Integer, CardDefinition> findCards(java.util.Set<Integer> ids) {
                var metadata = super.findCards(ids);
                assertThat(jdbc.sql("SHOW transaction_isolation").query(String.class).single()).isEqualTo("repeatable read");
                java.util.concurrent.CompletableFuture.runAsync(() ->
                        jdbc.sql("UPDATE cards SET power=9, value_atk=99 WHERE id=:card")
                                .param("card", cardIds.getFirst()).update()).join();
                return metadata;
            }
        };
        var service = new BattleSnapshotService(transactions, decks, concurrentCatalog, validator, jdbc, JSON);
        var payload = service.prepare(owner.user(), deckId, BattleBudget.start(Clock.systemUTC()));
        assertThat(payload.deck().entries().getFirst().powerPoints()).isEqualTo(1);
        assertThat(payload.deck().entries().getFirst().baseAtk()).isEqualTo(3);
        assertThat(jdbc.sql("SELECT value_atk FROM cards WHERE id=:card")
                .param("card", cardIds.getFirst()).query(Integer.class).single()).isEqualTo(99);
    }

    @Test
    void migratedInitialCollectionCanProduceACompletePlayableBattleSnapshot() throws Exception {
        var rows = jdbc.sql("""
                SELECT c.id, a.id_art, c.code, t.code AS type
                FROM cards c JOIN cards_arts a ON a.id_card=c.id
                JOIN card_types t ON t.id=c.id_type
                WHERE c.code LIKE 'CR01-%' AND a.art_code='default' ORDER BY c.code
                """).query((row, index) -> new Object[]{row.getInt("id"), row.getInt("id_art"),
                        row.getString("code"), row.getString("type")}).list();
        jdbc.sql("DELETE FROM decks_cards WHERE id_deck=:deck").param("deck", deckId).update();
        int creature = 0;
        for (Object[] row : rows) {
            String destination = "EQUIPMENT".equals(row[3]) ? "EQUIPMENT" : "ROUND" + (creature++ / 5 + 1);
            jdbc.sql("""
                    INSERT INTO collections(id_user,id_card,id_card_art,owned_replica,tradable_replica)
                    VALUES (:user,:card,:art,1,1)
                    """).param("user", owner.user().id()).param("card", row[0]).param("art", row[1]).update();
            jdbc.sql("INSERT INTO decks_cards(id_deck,id_card,id_card_art,destination) VALUES (:deck,:card,:art,:dest)")
                    .param("deck", deckId).param("card", row[0]).param("art", row[1]).param("dest", destination).update();
        }
        MvcResult result = finish(send());
        assertThat(result.getResponse().getStatus()).as(result.getResponse().getContentAsString()).isEqualTo(200);
        assertThat(SNAPSHOT.get().get("deck").get("entries").size()).isEqualTo(30);
    }
}
