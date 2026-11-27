package webapp.pack;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import javax.sql.DataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.IllegalTransactionStateException;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.context.WebApplicationContext;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import webapp.card.CardCatalogRepository;
import webapp.economy.DebitService;
import webapp.economy.ReservationReader;
import webapp.economy.ReservationTestStorage;
import static org.assertj.core.api.Assertions.*;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Real API commits, shared resource spending and contention on a dedicated PostgreSQL test database. */
@SpringBootTest(properties = {"app.security.session-token-cleanup-cron=-", "app.security.password-reset-token-cleanup-cron=-"})
class PackPurchaseDatabaseTests {
    @Autowired WebApplicationContext context;
    @Autowired JdbcClient jdbc;
    @Autowired DataSource dataSource;
    @Autowired JsonMapper json;
    @Autowired CardCatalogRepository cards;
    @Autowired PackService packs;
    @Autowired PackOpeningService opening;
    @Autowired DebitService debit;
    @Autowired ReservationReader reservations;
    @Autowired org.springframework.transaction.PlatformTransactionManager transactionManager;
    @MockitoSpyBean PackPurchaseRepository purchases;
    @MockitoSpyBean PackOpeningRepository openings;
    private MockMvc mvc;
    private Session user;
    private Session other;
    private Session admin;
    private int packId;
    private int slotType;
    private int inventoryId;
    private boolean storage;
    private final List<Integer> users = new ArrayList<>();

    @BeforeEach
    void setUp() throws Exception {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        user = register();
        other = register();
        admin = register();
        jdbc.sql("UPDATE users SET id_role = (SELECT id FROM roles WHERE code = 'ADMIN') WHERE id = :id")
                .param("id", admin.id()).update();
        inventoryId = jdbc.sql("SELECT id FROM inventory WHERE id_user = :user").param("user", user.id()).query(Integer.class).single();
        balance(1000);
        var card = cards.findById(jdbc.sql("SELECT min(c.id) FROM cards c JOIN rarities r ON r.id = c.id_rarity WHERE r.code = 'COMMON'")
                .query(Integer.class).single()).orElseThrow();
        slotType = transaction().execute(status -> {
            int type = jdbc.sql("INSERT INTO slot_pull_type (description) VALUES ('Purchase test') RETURNING id").query(Integer.class).single();
            jdbc.sql("INSERT INTO type_pull_rate VALUES (:type, :rarity, 100)").param("type", type).param("rarity", card.rarity().id()).update();
            return type;
        });
        int rates = jdbc.sql("SELECT min(id) FROM artwork_pull_rates").query(Integer.class).single();
        packId = packs.create(new PackWriteRequest(rates, "Purchase test",
                new PackWriteRequest.Description("Test only", 1, 0, "COMMON", "COMMON", Map.of("card1", json.valueToTree(card))),
                300, true, null, List.of(card.faction().code()), List.of(card.type().code()),
                List.of(card.expansion().code()), List.of("COMMON"), null, null, List.of(slotType))).id();
    }

    @AfterEach
    void cleanUp() {
        if (storage) ReservationTestStorage.remove(jdbc);
        jdbc.sql("DELETE FROM user_mercy WHERE id_pack = :pack").param("pack", packId).update();
        jdbc.sql("DELETE FROM inventory_packs WHERE id_pack = :pack").param("pack", packId).update();
        jdbc.sql("DELETE FROM packs WHERE id = :pack").param("pack", packId).update();
        jdbc.sql("DELETE FROM slot_pull_type WHERE id = :type").param("type", slotType).update();
        if (!users.isEmpty()) {
            jdbc.sql("DELETE FROM collections WHERE id_user IN (:ids)").param("ids", users).update();
            jdbc.sql("DELETE FROM refresh_tokens WHERE id_user IN (:ids)").param("ids", users).update();
            jdbc.sql("DELETE FROM inventory WHERE id_user IN (:ids)").param("ids", users).update();
            jdbc.sql("DELETE FROM users WHERE id IN (:ids)").param("ids", users).update();
        }
    }

    @Test
    void purchasesNewAndExistingOwnershipAtCurrentPriceWithoutOpeningOrHistory() throws Exception {
        jdbc.sql("UPDATE inventory SET rift_cores = 7, scraps = 9 WHERE id = :id").param("id", inventoryId).update();
        buy(user, body(2)).andExpect(status().isOk()).andExpect(header().doesNotExist("Location"))
                .andExpect(content().json("{\"packId\":" + packId + ",\"quantity\":2,\"unitPrice\":300,\"totalCost\":600,\"ownedQuantity\":2,\"cobaltShards\":400,\"reservedCobaltShards\":0,\"availableCobaltShards\":400}"));
        jdbc.sql("INSERT INTO user_mercy VALUES (:user, :pack, 3)").param("user", user.id()).param("pack", packId).update();
        jdbc.sql("UPDATE packs SET pack_price = 200 WHERE id = :pack").param("pack", packId).update();
        buy(user, body(1)).andExpect(status().isOk()).andExpect(jsonPath("$.ownedQuantity").value(3))
                .andExpect(jsonPath("$.unitPrice").value(200)).andExpect(jsonPath("$.cobaltShards").value(200));
        assertThat(quantity()).isEqualTo(3);
        assertThat(opening.inventory(user.id())).isEqualTo(new InventoryView(200, 7, 9));
        assertThat(jdbc.sql("SELECT count(*) FROM collections WHERE id_user = :user").param("user", user.id()).query(Integer.class).single()).isZero();
        assertThat(jdbc.sql("SELECT mercy_counter FROM user_mercy WHERE id_user = :user AND id_pack = :pack")
                .param("user", user.id()).param("pack", packId).query(Integer.class).single()).isEqualTo(3);
        assertThat(opening.ownedPacks(other.id())).isEmpty();
    }

    @Test
    void repeatedRequestsAreSeparatePurchasesAndExactFundsAreAllowed() throws Exception {
        balance(600);
        buy(user, body(1)).andExpect(status().isOk());
        buy(user, body(1)).andExpect(status().isOk()).andExpect(jsonPath("$.cobaltShards").value(0));
        buy(user, body(1)).andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("INSUFFICIENT_SCAGLIE"));
        assertThat(quantity()).isEqualTo(2);
    }

    @Test
    void permitsMoreThanTenAndFreePacksThroughTheIntegerBoundary() throws Exception {
        jdbc.sql("UPDATE packs SET pack_price = 0 WHERE id = :pack").param("pack", packId).update();
        balance(0);
        buy(user, body(Integer.MAX_VALUE)).andExpect(status().isOk())
                .andExpect(jsonPath("$.ownedQuantity").value(Integer.MAX_VALUE)).andExpect(jsonPath("$.totalCost").value(0));
        buy(user, body(1)).andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("PACK_QUANTITY_LIMIT_EXCEEDED"));
        assertThat(quantity()).isEqualTo(Integer.MAX_VALUE);
    }

    @Test
    void overflowAfterDebitRollsBackBothResources() throws Exception {
        jdbc.sql("INSERT INTO inventory_packs VALUES (:inventory, :pack, 2147483647)").param("inventory", inventoryId).param("pack", packId).update();
        buy(user, body(1)).andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("PACK_QUANTITY_LIMIT_EXCEEDED"));
        assertThat(opening.inventory(user.id()).cobaltShards()).isEqualTo(1000);
        assertThat(quantity()).isEqualTo(Integer.MAX_VALUE);
    }

    @Test
    void costUsesLongArithmeticAndCannotWrapIntoAnAffordableAmount() throws Exception {
        jdbc.sql("UPDATE packs SET pack_price = 2147483647 WHERE id = :pack").param("pack", packId).update();
        buy(user, body(Integer.MAX_VALUE)).andExpect(status().isConflict())
                .andExpect(jsonPath("$.requiredScaglie").value(4611686014132420609L));
        assertThat(quantity()).isZero();
        assertThat(opening.inventory(user.id()).cobaltShards()).isEqualTo(1000);
    }

    @ParameterizedTest
    @ValueSource(strings = {"{}", "null", "[]", "{", "{\"quantity\":1}", "{\"packId\":1}",
            "{\"packId\":1,\"quantity\":0}", "{\"packId\":1,\"quantity\":-1}", "{\"packId\":0,\"quantity\":1}",
            "{\"packId\":1,\"quantity\":2147483648}", "{\"packId\":2147483648,\"quantity\":1}",
            "{\"packId\":1,\"quantity\":1.0}", "{\"packId\":1,\"quantity\":\"1\"}",
            "{\"packId\":1,\"quantity\":null}", "{\"packId\":true,\"quantity\":1}",
            "{\"packId\":1,\"quantity\":1,\"userId\":1}"})
    void rejectsInvalidRequestsWithoutSpending(String body) throws Exception {
        buy(user, body).andExpect(status().isBadRequest()).andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
        assertThat(quantity()).isZero();
        assertThat(opening.inventory(user.id()).cobaltShards()).isEqualTo(1000);
    }

    @Test
    void enforcesAuthenticationRolesMediaAndAvailability() throws Exception {
        mvc.perform(post("/api/users/me/pack-purchases").contentType(MediaType.APPLICATION_JSON).content(body(1)))
                .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
        buy(admin, body(1)).andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("FORBIDDEN"));
        mvc.perform(post("/api/users/me/pack-purchases").header(HttpHeaders.AUTHORIZATION, user.bearer()).contentType(MediaType.TEXT_PLAIN).content(body(1)))
                .andExpect(status().isUnsupportedMediaType()).andExpect(jsonPath("$.code").value("UNSUPPORTED_MEDIA_TYPE"));
        buy(user, "{\"packId\":2147483647,\"quantity\":1}").andExpect(status().isNotFound());
        jdbc.sql("UPDATE packs SET is_acquirable = false WHERE id = :id").param("id", packId).update();
        buy(user, body(1)).andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("PACK_NOT_ACQUIRABLE"));
        assertThat(quantity()).isZero();
        assertThat(opening.inventory(user.id()).cobaltShards()).isEqualTo(1000);
    }

    @Test
    void openApiPublishesThePurchaseContractAndIntegerLimits() throws Exception {
        mvc.perform(get("/v3/api-docs")).andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/api/users/me/pack-purchases'].post.responses['200']").exists())
                .andExpect(jsonPath("$.components.schemas.PurchasePacksRequest.properties.quantity.maximum").value(Integer.MAX_VALUE))
                .andExpect(jsonPath("$.components.schemas.PurchasePacksResponse.properties.totalCost.format").value("int64"));
    }

    @Test
    void missingInventoryIsAnErrorAndNeverInventsABalance() throws Exception {
        jdbc.sql("DELETE FROM inventory WHERE id = :id").param("id", inventoryId).update();
        buy(user, body(1)).andExpect(status().isInternalServerError()).andExpect(jsonPath("$.code").value("INTERNAL_ERROR"));
        assertThat(quantity()).isZero();
    }

    @Test
    void laterCreditFailureRollsBackDebitAndTheAlreadyWrittenOwnership() throws Exception {
        doAnswer(call -> { call.callRealMethod(); throw new IllegalStateException("Injected after credit"); })
                .when(purchases).credit(anyInt(), anyInt(), anyInt());
        buy(user, body(1)).andExpect(status().isInternalServerError());
        assertThat(quantity()).isZero();
        assertThat(opening.inventory(user.id()).cobaltShards()).isEqualTo(1000);
    }

    @Test
    void sharedServicesRequireACallerTransactionAndRollBackWithOtherFeatures() {
        assertThatThrownBy(() -> debit.debitScaglie(user.id(), 100)).isInstanceOf(IllegalTransactionStateException.class);
        assertThatThrownBy(() -> reservations.reservedScaglie(user.id())).isInstanceOf(IllegalTransactionStateException.class);
        transaction().executeWithoutResult(status -> {
            var result = debit.debitScaglie(user.id(), 100);
            assertThat(result.cobaltShards()).isEqualTo(900);
            status.setRollbackOnly();
        });
        assertThat(opening.inventory(user.id()).cobaltShards()).isEqualTo(1000);
        assertThatThrownBy(() -> transaction().executeWithoutResult(status -> debit.debitScaglie(user.id(), -1)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void sumsLiveHoldsIgnoresExpiredAndOtherUsersAndPreservesReservedFunds() throws Exception {
        installStorage();
        var future = databaseTime().plusHours(1);
        ReservationTestStorage.hold(jdbc, user.id(), other.id(), 250, future);
        ReservationTestStorage.hold(jdbc, user.id(), other.id(), 150, future);
        ReservationTestStorage.hold(jdbc, user.id(), other.id(), 9999, databaseTime().minusSeconds(1));
        ReservationTestStorage.hold(jdbc, other.id(), user.id(), 900, future);
        buy(user, body(3)).andExpect(status().isConflict()).andExpect(jsonPath("$.reservedCobaltShards").value(400))
                .andExpect(jsonPath("$.availableCobaltShards").value(600)).andExpect(jsonPath("$.detail").value(containsString("reservations")));
        buy(user, body(2)).andExpect(status().isOk()).andExpect(jsonPath("$.cobaltShards").value(400))
                .andExpect(jsonPath("$.reservedCobaltShards").value(400)).andExpect(jsonPath("$.availableCobaltShards").value(0));
        assertThat(jdbc.sql("SELECT count(*) FROM trade_token_reservations").query(Integer.class).single()).isEqualTo(4);
    }

    @ParameterizedTest
    @ValueSource(strings = {"version", "acceptance", "amount", "expiry", "earlyExpiry", "missing", "status", "coverage", "participant"})
    void invalidLiveReservationsNeverFreeFunds(String invalid) throws Exception {
        installStorage();
        int id = ReservationTestStorage.hold(jdbc, user.id(), other.id(), 200, databaseTime().plusHours(1));
        switch (invalid) {
            case "version" -> jdbc.sql("UPDATE trade_token_reservations SET offer_version = 2").update();
            case "acceptance" -> jdbc.sql("UPDATE current_negotiations SET p_acceptance = false").update();
            case "amount" -> jdbc.sql("UPDATE trade_token_reservations SET token_amount = 0").update();
            case "expiry" -> jdbc.sql("UPDATE trade_token_reservations SET expires_at = expires_at + interval '1 minute'").update();
            case "earlyExpiry" -> jdbc.sql("UPDATE trade_token_reservations SET expires_at = clock_timestamp() - interval '1 minute'").update();
            case "missing" -> jdbc.sql("DELETE FROM trade_token_reservations").update();
            case "status" -> jdbc.sql("UPDATE original_announces SET status = 'COMPLETED'").update();
            case "coverage" -> balance(100);
            case "participant" -> jdbc.sql("UPDATE current_negotiations SET publisher_id = :other WHERE id = :id").param("other", other.id()).param("id", id).update();
        }
        int before = opening.inventory(user.id()).cobaltShards();
        buy(user, body(1)).andExpect(status().isInternalServerError()).andExpect(jsonPath("$.code").value("RESERVATION_STATE_INVALID"));
        assertThat(quantity()).isZero();
        assertThat(opening.inventory(user.id()).cobaltShards()).isEqualTo(before);
    }

    @Test
    void incompleteStorageFailsClosedInsteadOfPretendingThereAreNoReservations() throws Exception {
        installStorage();
        jdbc.sql("DROP TABLE trade_token_reservations").update();
        buy(user, body(1)).andExpect(status().isServiceUnavailable()).andExpect(jsonPath("$.code").value("TEMPORARILY_UNAVAILABLE"));
        assertThat(quantity()).isZero();
        assertThat(opening.inventory(user.id()).cobaltShards()).isEqualTo(1000);
    }

    @ParameterizedTest
    @ValueSource(strings = {"pack", "user", "inventory", "ownership"})
    void conflictingLocksFailPromptlyAndRollBackTheDebit(String locked) throws Exception {
        jdbc.sql("INSERT INTO inventory_packs VALUES (:inventory, :pack, 1)").param("inventory", inventoryId).param("pack", packId).update();
        String sql = switch (locked) {
            case "pack" -> "SELECT id FROM packs WHERE id = " + packId + " FOR UPDATE";
            case "user" -> "SELECT id FROM users WHERE id = " + user.id() + " FOR UPDATE";
            case "inventory" -> "SELECT id FROM inventory WHERE id = " + inventoryId + " FOR UPDATE";
            default -> "SELECT id_pack FROM inventory_packs WHERE id_pack = " + packId + " FOR UPDATE";
        };
        try (var connection = dataSource.getConnection()) {
            connection.setAutoCommit(false);
            try (var statement = connection.createStatement()) { statement.executeQuery(sql).close(); }
            assertTimeoutPreemptively(Duration.ofSeconds(3), () -> buy(user, body(1))
                    .andExpect(status().isServiceUnavailable()).andExpect(jsonPath("$.code").value("TEMPORARILY_UNAVAILABLE")));
            connection.rollback();
        }
        assertThat(quantity()).isEqualTo(1);
        assertThat(opening.inventory(user.id()).cobaltShards()).isEqualTo(1000);
    }

    @Test
    void simultaneousPurchasesCannotSpendTheSameScaglie() throws Exception {
        balance(300);
        var credited = new CountDownLatch(1);
        var release = new CountDownLatch(1);
        doAnswer(call -> {
            Object result = call.callRealMethod();
            credited.countDown();
            if (!release.await(10, TimeUnit.SECONDS)) throw new IllegalStateException("Purchase was not released");
            return result;
        }).when(purchases).credit(anyInt(), anyInt(), anyInt());
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            var first = executor.submit(() -> buy(user, body(1)));
            try {
                assertThat(credited.await(5, TimeUnit.SECONDS)).isTrue();
                buy(user, body(1)).andExpect(status().isServiceUnavailable());
                assertThat(quantity()).isZero();
                assertThat(opening.inventory(user.id()).cobaltShards()).isEqualTo(300);
            } finally { release.countDown(); }
            first.get(10, TimeUnit.SECONDS).andExpect(status().isOk());
        }
        buy(user, body(1)).andExpect(status().isConflict());
        assertThat(quantity()).isEqualTo(1);
        assertThat(opening.inventory(user.id()).cobaltShards()).isZero();
    }

    @Test
    void openingHoldingOwnershipDoesNotDeadlockAgainstPurchaseUserLock() throws Exception {
        jdbc.sql("INSERT INTO inventory_packs VALUES (:inventory, :pack, 2)").param("inventory", inventoryId).param("pack", packId).update();
        var held = new CountDownLatch(1);
        var release = new CountDownLatch(1);
        doAnswer(call -> {
            Object result = call.callRealMethod();
            held.countDown();
            if (!release.await(10, TimeUnit.SECONDS)) throw new IllegalStateException("Opening was not released");
            return result;
        }).when(openings).lockOwned(user.id(), packId);
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            var opened = executor.submit(() -> opening.open(user.id(), new OpenPacksRequest(packId, 1)));
            try {
                assertThat(held.await(5, TimeUnit.SECONDS)).isTrue();
                buy(user, body(1)).andExpect(status().isServiceUnavailable());
            } finally { release.countDown(); }
            assertThat(opened.get(10, TimeUnit.SECONDS).quantityOpened()).isEqualTo(1);
        }
        assertThat(quantity()).isEqualTo(1);
        assertThat(opening.inventory(user.id()).cobaltShards()).isEqualTo(1000);
        buy(user, body(1)).andExpect(status().isOk()).andExpect(jsonPath("$.ownedQuantity").value(2));
    }

    @Test
    void futureReservationWriterUsingTheUserGuardSerializesWithPurchases() throws Exception {
        installStorage();
        try (var connection = dataSource.getConnection()) {
            connection.setAutoCommit(false);
            var writer = JdbcClient.create(new org.springframework.jdbc.datasource.SingleConnectionDataSource(connection, true));
            writer.sql("SELECT id FROM users WHERE id = :user FOR UPDATE").param("user", user.id()).query(Integer.class).single();
            ReservationTestStorage.hold(writer, user.id(), other.id(), 800, databaseTime().plusHours(1));
            buy(user, body(1)).andExpect(status().isServiceUnavailable());
            connection.commit();
        }
        buy(user, body(1)).andExpect(status().isConflict()).andExpect(jsonPath("$.availableCobaltShards").value(200));
        assertThat(quantity()).isZero();
    }

    @Test
    void administrativeDeletionRefundsTheCommittedUnopenedPacks() throws Exception {
        buy(user, body(2)).andExpect(status().isOk());
        mvc.perform(delete("/api/admin/packs/{id}", packId).header(HttpHeaders.AUTHORIZATION, admin.bearer()))
                .andExpect(status().isNoContent());
        assertThat(quantity()).isZero();
        assertThat(opening.inventory(user.id()).cobaltShards()).isEqualTo(1000);
        buy(user, body(1)).andExpect(status().isNotFound());
    }

    @ParameterizedTest
    @ValueSource(strings = {"delete", "replace", "account"})
    void administrativeChangesAndAccountDeletionWaitForPurchaseCommit(String change) throws Exception {
        var definition = json.valueToTree(packs.get(packId).definition());
        ((tools.jackson.databind.node.ObjectNode) definition).put("pack_price", 600);
        var written = new CountDownLatch(1);
        var release = new CountDownLatch(1);
        var changing = new CountDownLatch(1);
        var purchasePid = new AtomicInteger();
        doAnswer(call -> {
            Object result = call.callRealMethod();
            purchasePid.set(jdbc.sql("SELECT pg_backend_pid()").query(Integer.class).single());
            written.countDown();
            if (!release.await(10, TimeUnit.SECONDS)) throw new IllegalStateException("Purchase was not released");
            return result;
        }).when(purchases).credit(anyInt(), anyInt(), anyInt());
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            var purchase = executor.submit(() -> buy(user, body(1)));
            try {
                assertThat(written.await(5, TimeUnit.SECONDS)).isTrue();
                var modification = executor.submit(() -> {
                    changing.countDown();
                    return switch (change) {
                        case "delete" -> mvc.perform(delete("/api/admin/packs/{id}", packId).header(HttpHeaders.AUTHORIZATION, admin.bearer()));
                        case "replace" -> mvc.perform(put("/api/admin/packs/{id}", packId).header(HttpHeaders.AUTHORIZATION, admin.bearer())
                                .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(definition)));
                        default -> mvc.perform(delete("/api/auth/users/{id}", user.id()).header(HttpHeaders.AUTHORIZATION, user.bearer()).with(csrf()));
                    };
                });
                assertThat(changing.await(5, TimeUnit.SECONDS)).isTrue();
                awaitWaiter(purchasePid.get());
                assertThat(quantity()).isZero();
                assertThat(opening.inventory(user.id()).cobaltShards()).isEqualTo(1000);
                release.countDown();
                purchase.get(10, TimeUnit.SECONDS).andExpect(status().isOk());
                modification.get(10, TimeUnit.SECONDS).andExpect(status().is(change.equals("replace") ? 200 : 204));
            } finally { release.countDown(); }
        }
        assertThat(quantity()).isEqualTo(change.equals("delete") ? 0 : 1);
        assertThat(opening.inventory(user.id()).cobaltShards()).isEqualTo(change.equals("delete") ? 1000 : 700);
        if (change.equals("replace")) buy(user, body(1)).andExpect(status().isOk()).andExpect(jsonPath("$.unitPrice").value(600));
        if (change.equals("account")) buy(user, body(1)).andExpect(status().isUnauthorized());
    }

    @Test
    void softDeletedAccountsCannotPurchase() throws Exception {
        mvc.perform(delete("/api/auth/users/{id}", user.id()).header(HttpHeaders.AUTHORIZATION, user.bearer()).with(csrf()))
                .andExpect(status().isNoContent());
        buy(user, body(1)).andExpect(status().isUnauthorized());
        assertThat(quantity()).isZero();
    }

    private void installStorage() { storage = true; ReservationTestStorage.install(jdbc); }
    private void awaitWaiter(int blocker) throws Exception {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
        while (System.nanoTime() < deadline) {
            if (jdbc.sql("SELECT EXISTS (SELECT 1 FROM pg_stat_activity WHERE :blocker = ANY(pg_blocking_pids(pid)))")
                    .param("blocker", blocker).query(Boolean.class).single()) return;
            Thread.sleep(10);
        }
        fail("Expected an administrative operation to wait for purchase backend %s", blocker);
    }
    private OffsetDateTime databaseTime() { return jdbc.sql("SELECT clock_timestamp()").query(OffsetDateTime.class).single(); }
    private TransactionTemplate transaction() { return new TransactionTemplate(transactionManager); }
    private void balance(int amount) { jdbc.sql("UPDATE inventory SET cobalt_shards = :amount WHERE id = :id").param("amount", amount).param("id", inventoryId).update(); }
    private int quantity() { return jdbc.sql("SELECT item_quantity FROM inventory_packs WHERE id_inventory = :inventory AND id_pack = :pack")
            .param("inventory", inventoryId).param("pack", packId).query(Integer.class).optional().orElse(0); }
    private String body(int quantity) { return "{\"packId\":" + packId + ",\"quantity\":" + quantity + "}"; }
    private ResultActions buy(Session caller, String body) throws Exception { return mvc.perform(post("/api/users/me/pack-purchases")
            .header(HttpHeaders.AUTHORIZATION, caller.bearer()).contentType(MediaType.APPLICATION_JSON).content(body)); }
    private Session register() throws Exception {
        String suffix = UUID.randomUUID().toString().replace("-", "");
        String result = mvc.perform(post("/api/auth/signin").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"nickname\":\"Purchase-" + suffix + "\",\"email\":\"" + suffix + "@example.invalid\",\"password\":\"Test password 1\"}"))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        JsonNode value = json.readTree(result);
        int id = value.path("user").path("id").asInt();
        users.add(id);
        return new Session(id, value.path("accessToken").asText());
    }
    private record Session(int id, String token) { String bearer() { return "Bearer " + token; } }
}
