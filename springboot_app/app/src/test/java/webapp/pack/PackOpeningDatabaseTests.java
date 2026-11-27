package webapp.pack;

import java.time.Duration;
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
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;
import webapp.card.CardCatalogRepository;
import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Spring MVC requests, real commits, rollback and lock contention on an isolated test database. */
@SpringBootTest(properties = {"app.security.session-token-cleanup-cron=-", "app.security.password-reset-token-cleanup-cron=-"})
class PackOpeningDatabaseTests {
    @Autowired WebApplicationContext context;
    @Autowired JdbcClient jdbc;
    @Autowired DataSource dataSource;
    @Autowired JsonMapper json;
    @Autowired CardCatalogRepository cards;
    @Autowired PackService packs;
    @Autowired PackOpeningService opening;
    @MockitoBean PackDrawSource random;
    @MockitoSpyBean PackOpeningRepository ownership;
    @MockitoSpyBean PackRepository definitions;
    private MockMvc mvc;
    private Session user;
    private Session admin;
    private int expansion;
    private String expansionCode;
    private int common;
    private int epic;
    private int slotType;
    private int packId;
    private final List<Integer> users = new ArrayList<>();
    private final List<Integer> products = new ArrayList<>();
    private final List<Integer> types = new ArrayList<>();

    @BeforeEach
    void setUp() throws Exception {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        when(random.nextInt(anyInt())).thenReturn(0);
        user = register();
        admin = register();
        jdbc.sql("UPDATE users SET id_role = (SELECT id FROM roles WHERE code = 'ADMIN') WHERE id = :id")
                .param("id", admin.id()).update();
        expansionCode = "OPEN_" + UUID.randomUUID().toString().replace("-", "");
        expansion = jdbc.sql("INSERT INTO expansions (code) VALUES (:code) RETURNING id")
                .param("code", expansionCode).query(Integer.class).single();
        common = card("COMMON");
        epic = card("EPIC");
        slotType = type("COMMON");
        packId = product();
        own(user, packId, 5);
    }

    @AfterEach
    void cleanUp() {
        if (!products.isEmpty()) {
            jdbc.sql("DELETE FROM user_mercy WHERE id_pack IN (:ids)").param("ids", products).update();
            jdbc.sql("DELETE FROM inventory_packs WHERE id_pack IN (:ids)").param("ids", products).update();
            jdbc.sql("DELETE FROM packs WHERE id IN (:ids)").param("ids", products).update();
        }
        if (!types.isEmpty()) jdbc.sql("DELETE FROM slot_pull_type WHERE id IN (:ids)").param("ids", types).update();
        if (expansion != 0) {
            jdbc.sql("DELETE FROM collections WHERE id_card IN (SELECT id FROM cards WHERE id_expansion = :id)")
                    .param("id", expansion).update();
            jdbc.sql("DELETE FROM cards_arts WHERE id_card IN (SELECT id FROM cards WHERE id_expansion = :id)")
                    .param("id", expansion).update();
            jdbc.sql("DELETE FROM cards WHERE id_expansion = :id").param("id", expansion).update();
            jdbc.sql("DELETE FROM expansions WHERE id = :id").param("id", expansion).update();
        }
        if (!users.isEmpty()) {
            jdbc.sql("DELETE FROM refresh_tokens WHERE id_user IN (:ids)").param("ids", users).update();
            jdbc.sql("DELETE FROM inventory WHERE id_user IN (:ids)").param("ids", users).update();
            jdbc.sql("DELETE FROM users WHERE id IN (:ids)").param("ids", users).update();
        }
    }

    @Test
    void readsOwnBalancesAndAllOwnedPacksIncludingDisabledProducts() throws Exception {
        jdbc.sql("UPDATE inventory SET cobalt_shards = 23, rift_cores = 7, scraps = 9 WHERE id_user = :id")
                .param("id", user.id()).update();
        mvc.perform(get("/api/users/me/inventory").header(HttpHeaders.AUTHORIZATION, user.bearer()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.cobaltShards").value(23))
                .andExpect(jsonPath("$.riftCores").value(7)).andExpect(jsonPath("$.scraps").value(9));
        mvc.perform(get("/api/users/me/inventory/packs").header(HttpHeaders.AUTHORIZATION, user.bearer()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].packId").value(packId)).andExpect(jsonPath("$[0].quantity").value(5))
                .andExpect(jsonPath("$[0].pack.is_acquirable").value(false));
        var other = register();
        mvc.perform(get("/api/users/me/inventory/packs").header(HttpHeaders.AUTHORIZATION, other.bearer()))
                .andExpect(status().isOk()).andExpect(content().json("[]"));
        openRequest(other, request(1)).andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("INSUFFICIENT_PACK_QUANTITY"));
        assertThat(quantity(packId)).isEqualTo(5);
    }

    @Test
    void enforcesRealAuthenticationAndUserRoleForAllThreeRoutes() throws Exception {
        for (String route : List.of("/api/users/me/inventory", "/api/users/me/inventory/packs")) {
            mvc.perform(get(route)).andExpect(status().isUnauthorized()).andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
            mvc.perform(get(route).header(HttpHeaders.AUTHORIZATION, admin.bearer()))
                    .andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("FORBIDDEN"));
        }
        mvc.perform(post("/api/users/me/pack-openings").contentType(MediaType.APPLICATION_JSON).content(request(1)))
                .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
        openRequest(admin, request(1)).andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("FORBIDDEN"));
        assertThat(quantity(packId)).isEqualTo(5);
        verifyNoInteractions(random);
    }

    @ParameterizedTest
    @ValueSource(strings = {"null", "[]", "{}", "{\"packId\":1}", "{\"packId\":1,\"quantity\":null}",
            "{\"packId\":1,\"quantity\":0}", "{\"packId\":1,\"quantity\":-1}", "{\"packId\":1,\"quantity\":11}",
            "{\"packId\":1,\"quantity\":\"1\"}", "{\"packId\":1,\"quantity\":1.0}",
            "{\"packId\":1,\"quantity\":2147483648}", "{\"packId\":0,\"quantity\":1}",
            "{\"packId\":1.0,\"quantity\":1}", "{\"packId\":\"1\",\"quantity\":1}",
            "{\"quantity\":1}", "{\"packId\":null,\"quantity\":1}", "{\"packId\":-1,\"quantity\":1}",
            "{\"packId\":true,\"quantity\":1}", "{\"packId\":1,\"quantity\":true}",
            "{\"packId\":2147483648,\"quantity\":1}", "{\"packId\":1,\"quantity\":1,\"userId\":2}", "{"})
    void rejectsMalformedOrCoercibleRequestsWithoutDrawing(String body) throws Exception {
        openRequest(user, body).andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
        assertThat(quantity(packId)).isEqualTo(5);
        verifyNoInteractions(random);
    }

    @Test
    void rejectsUnsupportedMediaAndMissingOrInsufficientPacks() throws Exception {
        mvc.perform(post("/api/users/me/pack-openings").header(HttpHeaders.AUTHORIZATION, user.bearer())
                .contentType(MediaType.TEXT_PLAIN).content(request(1)))
                .andExpect(status().isUnsupportedMediaType()).andExpect(jsonPath("$.code").value("UNSUPPORTED_MEDIA_TYPE"));
        openRequest(user, "{\"packId\":2147483647,\"quantity\":1}").andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("PACK_NOT_FOUND"));
        openRequest(user, request(6)).andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value("about:blank"))
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.code").value("INSUFFICIENT_PACK_QUANTITY"))
                .andExpect(jsonPath("$.instance").value("/api/users/me/pack-openings"))
                .andExpect(jsonPath("$.detail").isNotEmpty())
                .andExpect(jsonPath("$.packId").value(packId))
                .andExpect(jsonPath("$.availableQuantity").value(5)).andExpect(jsonPath("$.requestedQuantity").value(6));
        openRequest(user, "").andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
        verifyNoInteractions(random);
    }

    @Test
    void invalidBearerTokensUseTheDocumentedErrorOnAllThreeRoutes() throws Exception {
        for (String route : List.of("/api/users/me/inventory", "/api/users/me/inventory/packs", "/api/users/me/pack-openings")) {
            mvc.perform((route.endsWith("pack-openings") ? post(route).contentType(MediaType.APPLICATION_JSON).content(request(1)) : get(route))
                    .header(HttpHeaders.AUTHORIZATION, "Bearer invalid-token"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(header().string(HttpHeaders.WWW_AUTHENTICATE, "Bearer"))
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                    .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"))
                    .andExpect(jsonPath("$.instance").value(route));
        }
        assertThat(quantity(packId)).isEqualTo(5);
        verifyNoInteractions(random);
    }

    @Test
    void commitsFullBatchReplacesLastSlotAndKeepsTradableCopiesUnchanged() throws Exception {
        int art = standard(common);
        jdbc.sql("INSERT INTO collections VALUES (:user, :card, :art, 4, 1)")
                .param("user", user.id()).param("card", common).param("art", art).update();
        openRequest(user, request(3)).andExpect(status().isOk()).andExpect(header().doesNotExist("Location"))
                .andExpect(jsonPath("$.quantityOpened").value(3)).andExpect(jsonPath("$.remainingPackQuantity").value(2))
                .andExpect(jsonPath("$.packs[2].cards[0].cardId").value(common))
                .andExpect(jsonPath("$.packs[2].cards[1].cardId").value(epic))
                .andExpect(jsonPath("$.mercy.counter").value(3));
        assertThat(quantity(packId)).isEqualTo(2);
        assertThat(owned(common)).isEqualTo(9);
        assertThat(owned(epic)).isEqualTo(1);
        assertThat(jdbc.sql("SELECT tradable_replica FROM collections WHERE id_user = :user AND id_card = :card")
                .param("user", user.id()).param("card", common).query(Integer.class).single()).isEqualTo(1);
        assertThat(jdbc.sql("SELECT tradable_replica FROM collections WHERE id_user = :user AND id_card = :card")
                .param("user", user.id()).param("card", epic).query(Integer.class).single()).isZero();
        assertThat(counter()).isEqualTo(3);
    }

    @Test
    void acceptsTenAndPreservesMercyAfterConsumingTheLastOwnedPack() throws Exception {
        jdbc.sql("UPDATE inventory_packs SET item_quantity = 10 WHERE id_pack = :pack").param("pack", packId).update();
        String body = openRequest(user, request(10)).andExpect(status().isOk()).andExpect(jsonPath("$.quantityOpened").value(10))
                .andExpect(jsonPath("$.remainingPackQuantity").value(0)).andExpect(jsonPath("$.packs.length()").value(10))
                .andReturn().getResponse().getContentAsString();
        JsonNode batch = json.readTree(body).path("packs");
        for (int index = 0; index < 10; index++) {
            assertThat(batch.get(index).path("sequence").asInt()).isEqualTo(index + 1);
            assertThat(batch.get(index).path("cards").get(0).path("cardId").asInt()).isEqualTo(common);
            assertThat(batch.get(index).path("cards").get(1).path("cardId").asInt())
                    .isEqualTo((index + 1) % 3 == 0 ? epic : common);
        }
        assertThat(quantity(packId)).isZero();
        assertThat(counter()).isEqualTo(2);
        assertThat(owned(common) + owned(epic)).isEqualTo(20);
    }

    @Test
    void singlePackResponseMatchesTheContractAndDeletesLastOwnershipWithoutLosingMercy() throws Exception {
        jdbc.sql("UPDATE inventory_packs SET item_quantity = 1 WHERE id_pack = :pack").param("pack", packId).update();
        String body = openRequest(user, request(1)).andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.packId").value(packId))
                .andExpect(jsonPath("$.quantityOpened").value(1))
                .andExpect(jsonPath("$.remainingPackQuantity").value(0))
                .andExpect(jsonPath("$.packs[0].sequence").value(1))
                .andExpect(jsonPath("$.packs[0].cards.length()").value(2))
                .andExpect(jsonPath("$.mercy.threshold").value(3))
                .andExpect(jsonPath("$.mercy.counter").value(2))
                .andExpect(jsonPath("$.mercy.rarity.id").value(rarity("EPIC")))
                .andExpect(jsonPath("$.mercy.rarity.code").value("EPIC"))
                .andReturn().getResponse().getContentAsString();
        JsonNode result = json.readTree(body);
        assertThat(result.propertyNames()).containsExactlyInAnyOrder("packId", "quantityOpened", "remainingPackQuantity", "packs", "mercy");
        for (int index = 0; index < 2; index++) {
            JsonNode card = result.path("packs").get(0).path("cards").get(index);
            assertThat(card.propertyNames()).containsExactlyInAnyOrder("slot", "cardId", "cardArtId", "artCode", "name", "rarity", "imageUrl");
            assertThat(card.path("slot").asInt()).isEqualTo(index + 1);
            assertThat(card.path("cardId").asInt()).isEqualTo(common);
            assertThat(card.path("cardArtId").asInt()).isEqualTo(standard(common));
            assertThat(card.path("artCode").asText()).isEqualTo("STANDARD");
            assertThat(card.path("name").asText()).isEqualTo("Opening COMMON");
            assertThat(card.path("rarity").path("code").asText()).isEqualTo("COMMON");
            assertThat(card.has("imageUrl") && card.path("imageUrl").isNull()).isTrue();
        }
        assertThat(jdbc.sql("SELECT count(*) FROM inventory_packs WHERE id_pack = :id")
                .param("id", packId).query(Integer.class).single()).isZero();
        assertThat(counter()).isEqualTo(2);
        assertThat(owned(common)).isEqualTo(2);
    }

    @Test
    void disabledMercyDoesNotInitializeAdvanceOrDeleteExistingProgress() throws Exception {
        jdbc.sql("UPDATE packs SET mercy_rarity = NULL WHERE id = :id").param("id", packId).update();
        openRequest(user, request(1)).andExpect(status().isOk()).andExpect(jsonPath("$.mercy").value((Object) null));
        assertThat(counter()).isNull();
        jdbc.sql("INSERT INTO user_mercy VALUES (:user, :pack, 2)").param("user", user.id()).param("pack", packId).update();
        openRequest(user, request(1)).andExpect(status().isOk());
        assertThat(counter()).isEqualTo(2);
    }

    @Test
    void failsWithoutConsumingWhenCatalogChangesInvalidatePreviouslyValidConfiguration() throws Exception {
        jdbc.sql("UPDATE cards SET is_deleted = true WHERE id = :id").param("id", common).update();
        openRequest(user, request(1)).andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("PACK_CONFIGURATION_INVALID"));
        assertThat(quantity(packId)).isEqualTo(5);
        assertThat(counter()).isNull();
        verifyNoInteractions(random);
    }

    @Test
    void rollsBackCardsConsumptionAndNewMercyWhenFailureOccursAfterAllWrites() throws Exception {
        doAnswer(i -> { i.callRealMethod(); throw new IllegalStateException("Injected after mercy write"); })
                .when(ownership).saveMercy(anyInt(), anyInt(), anyInt());
        openRequest(user, request(3)).andExpect(status().isInternalServerError()).andExpect(jsonPath("$.code").value("INTERNAL_ERROR"));
        assertThat(quantity(packId)).isEqualTo(5);
        assertThat(owned(common)).isZero();
        assertThat(owned(epic)).isZero();
        assertThat(counter()).isNull();
    }

    @Test
    void failureAfterDeletingLastOwnershipRestoresTheWholeBatchAndExistingMercy() throws Exception {
        jdbc.sql("INSERT INTO user_mercy VALUES (:user, :pack, 1)").param("user", user.id()).param("pack", packId).update();
        doAnswer(i -> { i.callRealMethod(); throw new IllegalStateException("Injected after ownership deletion"); })
                .when(ownership).consume(anyInt(), anyInt(), anyInt());
        openRequest(user, request(5)).andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("INTERNAL_ERROR"));
        assertThat(quantity(packId)).isEqualTo(5);
        assertThat(counter()).isEqualTo(1);
        assertThat(owned(common)).isZero();
        assertThat(owned(epic)).isZero();
    }

    @Test
    void collectionOverflowRollsBackPreviouslyCreditedVariantsAndPreservesExistingProgress() throws Exception {
        jdbc.sql("INSERT INTO user_mercy VALUES (:user, :pack, 1)").param("user", user.id()).param("pack", packId).update();
        jdbc.sql("INSERT INTO collections VALUES (:user, :card, :art, 2147483647, 0)")
                .param("user", user.id()).param("card", epic).param("art", standard(epic)).update();
        openRequest(user, request(1)).andExpect(status().isInternalServerError());
        assertThat(owned(common)).isZero();
        assertThat(owned(epic)).isEqualTo(Integer.MAX_VALUE);
        assertThat(quantity(packId)).isEqualTo(5);
        assertThat(counter()).isEqualTo(1);
    }

    @Test
    void refusesContendedOwnershipImmediatelyAndAllowsOpeningAfterRelease() throws Exception {
        try (var connection = dataSource.getConnection()) {
            connection.setAutoCommit(false);
            try (var statement = connection.prepareStatement("SELECT * FROM inventory_packs WHERE id_pack = ? FOR UPDATE")) {
                statement.setInt(1, packId);
                statement.executeQuery().close();
            }
            assertTimeoutPreemptively(Duration.ofSeconds(3), () -> openRequest(user, request(1))
                    .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("OPENING_IN_PROGRESS")));
            verifyNoInteractions(random);
            assertThat(quantity(packId)).isEqualTo(5);
            assertThat(owned(common)).isZero();
            assertThat(counter()).isNull();
            connection.rollback();
        }
        openRequest(user, request(1)).andExpect(status().isOk());
        assertThat(quantity(packId)).isEqualTo(4);
    }

    @Test
    void refusesConcurrentAdminDefinitionLockWithoutDeadlocking() throws Exception {
        try (var connection = dataSource.getConnection()) {
            connection.setAutoCommit(false);
            try (var statement = connection.prepareStatement("SELECT * FROM packs WHERE id = ? FOR UPDATE")) {
                statement.setInt(1, packId);
                statement.executeQuery().close();
            }
            assertTimeoutPreemptively(Duration.ofSeconds(3), () -> openRequest(user, request(1))
                    .andExpect(status().isServiceUnavailable()).andExpect(jsonPath("$.code").value("TEMPORARILY_UNAVAILABLE")));
            connection.rollback();
        }
        assertThat(quantity(packId)).isEqualTo(5);
        verifyNoInteractions(random);
    }

    @Test
    void concurrentDifferentPackTypesAddToTheSameVariantWithoutLosingCopies() throws Exception {
        int other = product();
        own(user, other, 2);
        var start = new CountDownLatch(1);
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            var first = executor.submit(() -> { start.await(); return opening.open(user.id(), new OpenPacksRequest(packId, 1)); });
            var second = executor.submit(() -> { start.await(); return opening.open(user.id(), new OpenPacksRequest(other, 1)); });
            start.countDown();
            assertThat(first.get(10, TimeUnit.SECONDS).quantityOpened()).isEqualTo(1);
            assertThat(second.get(10, TimeUnit.SECONDS).quantityOpened()).isEqualTo(1);
        }
        assertThat(owned(common)).isEqualTo(4);
        assertThat(quantity(packId)).isEqualTo(4);
        assertThat(quantity(other)).isEqualTo(1);
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void activeAdministrativePutOrDeleteRejectsOpeningBeforeDrawing(boolean delete) throws Exception {
        JsonNode definition = json.valueToTree(packs.get(packId).definition());
        var locked = new CountDownLatch(1);
        var release = new CountDownLatch(1);
        doAnswer(i -> {
            Object result = i.callRealMethod();
            locked.countDown();
            if (!release.await(10, TimeUnit.SECONDS)) throw new IllegalStateException("Admin lock was not released");
            return result;
        }).when(definitions).lock(packId);
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            var adminRequest = executor.submit(() -> administrativeChange(definition, delete));
            try {
                assertThat(locked.await(5, TimeUnit.SECONDS)).isTrue();
                openRequest(user, request(1)).andExpect(status().isServiceUnavailable())
                        .andExpect(jsonPath("$.code").value("TEMPORARILY_UNAVAILABLE"));
                verifyNoInteractions(random);
                assertThat(quantity(packId)).isEqualTo(5);
                assertThat(owned(common)).isZero();
            } finally {
                release.countDown();
            }
            adminRequest.get(10, TimeUnit.SECONDS).andExpect(status().is(delete ? 204 : 200));
        }
        if (delete) {
            assertThat(quantity(packId)).isZero();
            assertThat(opening.inventory(user.id()).cobaltShards()).isEqualTo(1500);
            openRequest(user, request(1)).andExpect(status().isNotFound());
        } else {
            openRequest(user, request(1)).andExpect(status().isOk());
            assertThat(quantity(packId)).isEqualTo(4);
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void administrativePutOrDeleteWaitsForOpeningCommitAndUsesItsRemainingOwnership(boolean delete) throws Exception {
        JsonNode definition = json.valueToTree(packs.get(packId).definition());
        var written = new CountDownLatch(1);
        var release = new CountDownLatch(1);
        var adminStarted = new CountDownLatch(1);
        var openingPid = new AtomicInteger();
        var adminPid = new AtomicInteger();
        doAnswer(i -> {
            i.callRealMethod();
            openingPid.set(jdbc.sql("SELECT pg_backend_pid()").query(Integer.class).single());
            written.countDown();
            if (!release.await(10, TimeUnit.SECONDS)) throw new IllegalStateException("Opening was not released");
            return null;
        }).when(ownership).saveMercy(anyInt(), anyInt(), anyInt());
        doAnswer(i -> {
            adminPid.set(jdbc.sql("SELECT pg_backend_pid()").query(Integer.class).single());
            adminStarted.countDown();
            return i.callRealMethod();
        }).when(definitions).lock(packId);
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            var opened = executor.submit(() -> openRequest(user, request(1)));
            try {
                assertThat(written.await(5, TimeUnit.SECONDS)).isTrue();
                var adminRequest = executor.submit(() -> administrativeChange(definition, delete));
                assertThat(adminStarted.await(5, TimeUnit.SECONDS)).isTrue();
                awaitBlockedBy(adminPid.get(), openingPid.get());
                // Uncommitted consumption and cards are invisible to other connections.
                assertThat(quantity(packId)).isEqualTo(5);
                assertThat(owned(common)).isZero();
                release.countDown();
                opened.get(10, TimeUnit.SECONDS).andExpect(status().isOk());
                adminRequest.get(10, TimeUnit.SECONDS).andExpect(status().is(delete ? 204 : 200));
            } finally {
                release.countDown();
            }
        }
        assertThat(owned(common)).isEqualTo(2);
        assertThat(counter()).isNull();
        assertThat(quantity(packId)).isEqualTo(delete ? 0 : 4);
        assertThat(opening.inventory(user.id()).cobaltShards()).isEqualTo(delete ? 1200 : 0);
    }

    @Test
    void openingAddsToAConcurrentCollectionUpdateAndPreservesItsTradableQuantity() throws Exception {
        int art = standard(common);
        jdbc.sql("INSERT INTO collections VALUES (:user, :card, :art, 4, 1)")
                .param("user", user.id()).param("card", common).param("art", art).update();
        var granting = new CountDownLatch(1);
        var openingPid = new AtomicInteger();
        doAnswer(i -> {
            openingPid.set(jdbc.sql("SELECT pg_backend_pid()").query(Integer.class).single());
            granting.countDown();
            return i.callRealMethod();
        }).when(ownership).grant(anyInt(), anyInt(), anyInt(), anyInt());
        try (var connection = dataSource.getConnection(); var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            connection.setAutoCommit(false);
            try {
                int writerPid;
                try (var statement = connection.createStatement(); var rows = statement.executeQuery("SELECT pg_backend_pid()")) {
                    rows.next();
                    writerPid = rows.getInt(1);
                }
                try (var statement = connection.prepareStatement("UPDATE collections SET owned_replica = owned_replica + 3, tradable_replica = 2 WHERE id_user = ? AND id_card = ?")) {
                    statement.setInt(1, user.id());
                    statement.setInt(2, common);
                    assertThat(statement.executeUpdate()).isEqualTo(1);
                }
                var opened = executor.submit(() -> openRequest(user, request(1)));
                assertThat(granting.await(5, TimeUnit.SECONDS)).isTrue();
                awaitBlockedBy(openingPid.get(), writerPid);
                assertThat(quantity(packId)).isEqualTo(5);
                connection.commit();
                opened.get(10, TimeUnit.SECONDS).andExpect(status().isOk());
            } finally {
                connection.rollback();
            }
        }
        assertThat(owned(common)).isEqualTo(9);
        assertThat(jdbc.sql("SELECT tradable_replica FROM collections WHERE id_user = :user AND id_card = :card")
                .param("user", user.id()).param("card", common).query(Integer.class).single()).isEqualTo(2);
        assertThat(quantity(packId)).isEqualTo(4);
    }

    @Test
    void adminWritesRejectEmptyOrIncompatibleFiltersAndMissingFallbackWithoutChangingDefinition() throws Exception {
        ObjectNode valid = json.valueToTree(packs.get(packId).definition());
        for (String field : List.of("allowed_factions", "allowed_types", "allowed_expansions", "allowed_rarities")) {
            ObjectNode invalid = valid.deepCopy();
            invalid.set(field, json.createArrayNode());
            adminWrite(invalid, false).andExpect(status().isBadRequest());
            adminWrite(invalid, true).andExpect(status().isBadRequest());
        }
        for (var mismatch : Map.of("allowed_factions", "CYB", "allowed_types", "EQUIPMENT",
                "allowed_expansions", "CR01", "allowed_rarities", "RARE").entrySet()) {
            ObjectNode invalid = valid.deepCopy();
            invalid.set(mismatch.getKey(), json.createArrayNode().add(mismatch.getValue()));
            adminWrite(invalid, false).andExpect(status().isBadRequest());
            adminWrite(invalid, true).andExpect(status().isBadRequest());
        }
        jdbc.sql("DELETE FROM cards_arts WHERE id_card = :card").param("card", common).update();
        adminWrite(valid, true).andExpect(status().isBadRequest());
        assertThat(packs.get(packId).definition().name()).isEqualTo("Opening fixture");
        assertThat(quantity(packId)).isEqualTo(5);
    }

    @Test
    void zeroProbabilityDoesNotRequireCardsAndUnreachableRaritiesNeedNoFallback() throws Exception {
        int zero = type("COMMON");
        jdbc.sql("INSERT INTO type_pull_rate SELECT :id, id, 0 FROM rarities WHERE code = 'RARE'")
                .param("id", zero).update();
        jdbc.sql("DELETE FROM cards_arts WHERE id_card = :card").param("card", epic).update();
        ObjectNode definition = json.valueToTree(packs.get(packId).definition());
        definition.putNull("mercy_rarity");
        definition.putNull("mercy_threshold");
        definition.set("pack_slots", json.createArrayNode().add(zero).add(zero));
        adminWrite(definition, true).andExpect(status().isOk());
        openRequest(user, request(1)).andExpect(status().isOk()).andExpect(jsonPath("$.mercy").value((Object) null));
        assertThat(owned(common)).isEqualTo(2);
        assertThat(owned(epic)).isZero();
    }

    @Test
    void rejectsMercyCounterAboveThresholdWithoutChangingProgressOrOwnership() throws Exception {
        jdbc.sql("INSERT INTO user_mercy VALUES (:user, :pack, 4)").param("user", user.id()).param("pack", packId).update();
        openRequest(user, request(1)).andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("PACK_CONFIGURATION_INVALID"));
        assertThat(quantity(packId)).isEqualTo(5);
        assertThat(counter()).isEqualTo(4);
        assertThat(owned(common)).isZero();
    }

    @Test
    void sharedValidationRejectsMissingExactMercyPoolForBothAdministrativeWritesAndOpening() throws Exception {
        ObjectNode definition = json.valueToTree(packs.get(packId).definition());
        // Common slots remain drawable, but the configured Epic replacement is unavailable.
        definition.set("allowed_rarities", json.createArrayNode().add("COMMON"));
        adminWrite(definition, false).andExpect(status().isBadRequest());
        adminWrite(definition, true).andExpect(status().isBadRequest());
        jdbc.sql("UPDATE cards SET is_deleted = true WHERE id = :id").param("id", epic).update();
        openRequest(user, request(1)).andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("PACK_CONFIGURATION_INVALID"));
        assertThat(quantity(packId)).isEqualTo(5);
        assertThat(counter()).isNull();
        verifyNoInteractions(random);
    }

    @Test
    void missingInventoryIsAnErrorAndOpenApiPublishesTheQuantityLimit() throws Exception {
        jdbc.sql("DELETE FROM inventory WHERE id_user = :id").param("id", admin.id()).update();
        jdbc.sql("UPDATE users SET id_role = (SELECT id FROM roles WHERE code = 'USER') WHERE id = :id")
                .param("id", admin.id()).update();
        mvc.perform(get("/api/users/me/inventory").header(HttpHeaders.AUTHORIZATION, admin.bearer()))
                .andExpect(status().isInternalServerError()).andExpect(jsonPath("$.code").value("INTERNAL_ERROR"));
        String content = mvc.perform(get("/v3/api-docs")).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        JsonNode doc = json.readTree(content);
        assertThat(doc.path("paths").has("/api/users/me/pack-openings")).isTrue();
        JsonNode quantity = doc.path("components").path("schemas").path("OpenPacksRequest").path("properties").path("quantity");
        assertThat(quantity.path("minimum").asInt()).isEqualTo(1);
        assertThat(quantity.path("maximum").asInt()).isEqualTo(10);
        JsonNode responses = doc.path("paths").path("/api/users/me/pack-openings").path("post").path("responses");
        assertThat(responses.propertyNames()).contains("200", "400", "401", "403", "404", "409", "415", "500", "503");
        assertThat(responses.path("409").path("description").asText())
                .contains("OPENING_IN_PROGRESS", "INSUFFICIENT_PACK_QUANTITY", "PACK_CONFIGURATION_INVALID");
        assertThat(responses.path("200").path("content").path("application/json").path("schema").path("$ref").asText())
                .endsWith("/OpenPacksResponse");
        for (String route : List.of("/api/users/me/inventory", "/api/users/me/inventory/packs")) {
            JsonNode read = doc.path("paths").path(route).path("get");
            assertThat(read.path("responses").propertyNames()).contains("200", "401", "403", "500", "503");
            assertThat(read.path("security").get(0).has("bearerAuth")).isTrue();
        }
    }

    private int card(String rarity) {
        int id = jdbc.sql("""
                INSERT INTO cards (name, id_type, id_faction, id_expansion, id_rarity)
                SELECT :name, t.id, f.id, :expansion, r.id FROM card_types t, factions f, rarities r
                WHERE t.code = 'CREATURE' AND f.code = 'SAP' AND r.code = :rarity RETURNING id
                """).param("name", "Opening " + rarity).param("expansion", expansion).param("rarity", rarity)
                .query(Integer.class).single();
        jdbc.sql("INSERT INTO cards_arts (id_card, art_code) VALUES (:id, 'STANDARD')").param("id", id).update();
        return id;
    }

    private int type(String rarity) {
        // Deferred probability triggers require type and rates in the same transaction.
        var template = new org.springframework.transaction.support.TransactionTemplate(
                new org.springframework.jdbc.datasource.DataSourceTransactionManager(dataSource));
        int id = template.execute(status -> {
            int created = jdbc.sql("INSERT INTO slot_pull_type (description) VALUES ('Opening fixture') RETURNING id")
                    .query(Integer.class).single();
            jdbc.sql("INSERT INTO type_pull_rate SELECT :id, id, 100 FROM rarities WHERE code = :code")
                    .param("id", created).param("code", rarity).update();
            return created;
        });
        types.add(id);
        return id;
    }

    private int product() {
        int rates = jdbc.sql("SELECT min(id) FROM artwork_pull_rates").query(Integer.class).single();
        JsonNode preview = json.valueToTree(cards.findById(common).orElseThrow());
        var request = new PackWriteRequest(rates, "Opening fixture",
                new PackWriteRequest.Description("Metadata only", 2, 0, "COMMON", "COMMON", Map.of("card1", preview, "card2", preview)),
                300, false, null, List.of("SAP"), List.of("CREATURE"), List.of(expansionCode),
                List.of("COMMON", "EPIC"), 3, rarity("EPIC"), List.of(slotType, slotType));
        int id = packs.create(request).id();
        products.add(id);
        return id;
    }

    private int rarity(String code) { return jdbc.sql("SELECT id FROM rarities WHERE code = :code").param("code", code).query(Integer.class).single(); }
    private int standard(int card) { return jdbc.sql("SELECT id_art FROM cards_arts WHERE id_card = :id AND art_code = 'STANDARD'").param("id", card).query(Integer.class).single(); }
    private Integer counter() { return jdbc.sql("SELECT mercy_counter FROM user_mercy WHERE id_user = :user AND id_pack = :pack")
            .param("user", user.id()).param("pack", packId).query(Integer.class).optional().orElse(null); }
    private int quantity(int pack) { return jdbc.sql("SELECT item_quantity FROM inventory_packs WHERE id_pack = :pack AND id_inventory = (SELECT id FROM inventory WHERE id_user = :user)")
            .param("pack", pack).param("user", user.id()).query(Integer.class).optional().orElse(0); }
    private int owned(int card) { return jdbc.sql("SELECT COALESCE(sum(owned_replica), 0) FROM collections WHERE id_user = :user AND id_card = :card")
            .param("user", user.id()).param("card", card).query(Integer.class).single(); }
    private void own(Session owner, int pack, int quantity) { jdbc.sql("INSERT INTO inventory_packs SELECT id, :pack, :quantity FROM inventory WHERE id_user = :user")
            .param("pack", pack).param("quantity", quantity).param("user", owner.id()).update(); }
    private String request(int quantity) { return "{\"packId\":" + packId + ",\"quantity\":" + quantity + "}"; }
    private ResultActions openRequest(Session caller, String body) throws Exception { return mvc.perform(post("/api/users/me/pack-openings")
            .header(HttpHeaders.AUTHORIZATION, caller.bearer()).contentType(MediaType.APPLICATION_JSON).content(body)); }
    private ResultActions adminWrite(JsonNode body, boolean replace) throws Exception {
        return mvc.perform((replace ? put("/api/admin/packs/{id}", packId) : post("/api/admin/packs"))
                .header(HttpHeaders.AUTHORIZATION, admin.bearer()).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body)));
    }
    private Session register() throws Exception {
        String suffix = UUID.randomUUID().toString().replace("-", "");
        String result = mvc.perform(post("/api/auth/signin").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"nickname\":\"Opening-" + suffix + "\",\"email\":\"" + suffix + "@example.invalid\",\"password\":\"Test password 1\"}"))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        JsonNode value = json.readTree(result);
        int id = value.path("user").path("id").asInt();
        users.add(id);
        return new Session(id, value.path("accessToken").asText());
    }
    private ResultActions administrativeChange(JsonNode definition, boolean delete) throws Exception {
        return delete ? mvc.perform(delete("/api/admin/packs/{id}", packId).header(HttpHeaders.AUTHORIZATION, admin.bearer()))
                : adminWrite(definition, true);
    }

    private void awaitBlockedBy(int waitingPid, int blockerPid) throws Exception {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
        while (System.nanoTime() < deadline) {
            if (jdbc.sql("SELECT :blocker = ANY(pg_blocking_pids(:waiting))")
                    .param("blocker", blockerPid).param("waiting", waitingPid).query(Boolean.class).single()) return;
            Thread.sleep(10);
        }
        fail("Expected PostgreSQL backend %s to block on %s", waitingPid, blockerPid);
    }

    private record Session(int id, String token) { String bearer() { return "Bearer " + token; } }
}
