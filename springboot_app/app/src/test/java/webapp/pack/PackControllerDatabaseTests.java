package webapp.pack;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;
import webapp.card.CardCatalogRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Real security chain, PostgreSQL JSONB and F04 previews; every fixture rolls back. */
@SpringBootTest(properties = {"app.security.session-token-cleanup-cron=-",
        "app.security.password-reset-token-cleanup-cron=-"})
@Transactional
class PackControllerDatabaseTests {
    @Autowired private WebApplicationContext context;
    @Autowired private JdbcClient jdbc;
    @Autowired private CardCatalogRepository cards;
    @Autowired private PackService packs;
    @Autowired private JsonMapper json;
    private MockMvc mvc;
    private Session admin;
    private Session user;
    private ObjectNode payload;

    @BeforeEach
    void setUp() throws Exception {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        admin = register();
        user = register();
        jdbc.sql("UPDATE users SET id_role = (SELECT id FROM roles WHERE code = 'ADMIN') WHERE id = :id")
                .param("id", admin.id()).update();
        int cardId = jdbc.sql("""
                SELECT c.id FROM cards c
                JOIN expansions e ON e.id = c.id_expansion
                WHERE e.code = 'CR01' AND c.name = 'Ash Novice'
                """).query(Integer.class).single();
        JsonNode preview = json.valueToTree(cards.findById(cardId).orElseThrow());
        int ratesId = jdbc.sql("SELECT id FROM artwork_pull_rates ORDER BY id LIMIT 1").query(Integer.class).single();
        int slotTypeId = createSlotType("COMMON");
        payload = json.valueToTree(new PackWriteRequest(ratesId, "Package fixture",
                new PackWriteRequest.Description("Fixture only", 1, 0, "COMMON", "COMMON",
                        Map.of("card1", preview)), 300, false, null,
                List.of("SAP"), List.of("CREATURE"), List.of("CR01"), List.of("COMMON"),
                null, null, List.of(slotTypeId)));
    }

    @Test
    void requiresAuthenticationForEveryOperation() throws Exception {
        mvc.perform(get("/api/packs")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/packs/1")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/admin/packs").contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(payload))).andExpect(status().isUnauthorized());
        mvc.perform(put("/api/admin/packs/1")).andExpect(status().isUnauthorized());
        mvc.perform(delete("/api/admin/packs/1")).andExpect(status().isUnauthorized());
    }

    @Test
    void userCanReadButCannotCreateUpdateOrDelete() throws Exception {
        payload.put("is_acquirable", true);
        int id = createPack();
        mvc.perform(get("/api/packs").header(HttpHeaders.AUTHORIZATION, user.bearer()))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].id").value(id));
        mvc.perform(get("/api/packs/{id}", id).header(HttpHeaders.AUTHORIZATION, user.bearer()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.pack_name").value("Package fixture"));
        mvc.perform(post("/api/admin/packs").header(HttpHeaders.AUTHORIZATION, user.bearer())
                .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(payload)))
                .andExpect(status().isForbidden());
        mvc.perform(put("/api/admin/packs/{id}", id).header(HttpHeaders.AUTHORIZATION, user.bearer())
                .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(payload)))
                .andExpect(status().isForbidden());
        mvc.perform(delete("/api/admin/packs/{id}", id).header(HttpHeaders.AUTHORIZATION, user.bearer()))
                .andExpect(status().isForbidden());
    }

    @Test
    void createsFlatCatalogRepresentationAndNoPlayerResources() throws Exception {
        payload.put("mercy_threshold", 10);
        int id = createPack();
        mvc.perform(get("/api/packs/{id}", id).header(HttpHeaders.AUTHORIZATION, admin.bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.pack_description.pack_description").value("Fixture only"))
                .andExpect(jsonPath("$.pack_description.example_pull.card1.artworks").isArray())
                .andExpect(jsonPath("$.pack_price").value(300))
                .andExpect(jsonPath("$.allowed_types[0]").value("CREATURE"))
                .andExpect(jsonPath("$.mercy_threshold").value(10))
                .andExpect(jsonPath("$.pack_slots[0]").value(payload.path("pack_slots").get(0).asInt()))
                .andExpect(jsonPath("$.is_acquirable").value(false))
                .andExpect(jsonPath("$.slot_pull_types[0].pull_rates[0].rarity_code").value("COMMON"))
                .andExpect(jsonPath("$.slot_pull_types[0].pull_rates[0].probability").value(100.0))
                .andExpect(jsonPath("$.slot_pulls").doesNotExist())
                .andExpect(jsonPath("$.id_artwork_pull_rates").value(payload.path("id_artwork_pull_rates").asInt()))
                .andExpect(jsonPath("$.artwork_pull_rates.standard").value(81.0))
                .andExpect(jsonPath("$.artwork_pull_rates.extended_art").value(10.0))
                .andExpect(jsonPath("$.artwork_pull_rates.full_art").value(7.5))
                .andExpect(jsonPath("$.artwork_pull_rates.alternate_art").value(1.5))
                .andExpect(jsonPath("$.definition").doesNotExist());
        assertThat(jdbc.sql("SELECT count(*) FROM inventory_packs").query(Integer.class).single()).isZero();
        assertThat(jdbc.sql("SELECT count(*) FROM user_mercy").query(Integer.class).single()).isZero();
        assertThat(jdbc.sql("SELECT cobalt_shards + rift_cores + scraps FROM inventory WHERE id_user = :id")
                .param("id", user.id()).query(Integer.class).single()).isZero();
        assertThat(packs.get(id).definition().packSlots()).containsExactly(payload.path("pack_slots").get(0).asInt());
    }

    @Test
    void roundTripsNullablePackMercyRarityAndClearsProgressOnChange() throws Exception {
        int epic = jdbc.sql("SELECT id FROM rarities WHERE code = 'COMMON'").query(Integer.class).single();
        payload.put("mercy_threshold", 10);
        payload.put("mercy_rarity", epic);
        int id = createPack();
        mvc.perform(get("/api/packs/{id}", id).header(HttpHeaders.AUTHORIZATION, admin.bearer()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.mercy_rarity").value(epic));
        assertThat(packs.get(id).definition().mercyRarity()).isEqualTo(epic);
        mercy(user, id);
        payload.putNull("mercy_rarity");
        mvc.perform(put("/api/admin/packs/{id}", id).header(HttpHeaders.AUTHORIZATION, admin.bearer())
                .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(payload)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.mercy_rarity").value((Object) null));
        assertThat(packs.get(id).definition().mercyRarity()).isNull();
        assertThat(mercyRows(id)).isZero();
        payload.remove("mercy_rarity");
        int omitted = createPack();
        assertThat(packs.get(omitted).definition().mercyRarity()).isNull();
    }

    @Test
    void rejectsUnknownDeletedAndInvalidMercyRaritiesWithoutChangingProgress() throws Exception {
        int id = createPack();
        mercy(user, id);
        for (int rarity : new int[]{0, -1, Integer.MAX_VALUE}) {
            payload.put("mercy_rarity", rarity);
            submit(payload).andExpect(status().isBadRequest());
            mvc.perform(put("/api/admin/packs/{id}", id).header(HttpHeaders.AUTHORIZATION, admin.bearer())
                    .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(payload)))
                    .andExpect(status().isBadRequest());
        }
        int deleted = jdbc.sql("SELECT id FROM rarities WHERE code = 'EPIC'").query(Integer.class).single();
        jdbc.sql("UPDATE rarities SET is_deleted = true WHERE id = :id").param("id", deleted).update();
        payload.put("mercy_rarity", deleted);
        submit(payload).andExpect(status().isBadRequest());
        mvc.perform(put("/api/admin/packs/{id}", id).header(HttpHeaders.AUTHORIZATION, admin.bearer())
                .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(payload)))
                .andExpect(status().isBadRequest());
        assertThat(mercyRows(id)).isEqualTo(1);
        assertThat(packs.get(id).definition().mercyRarity()).isNull();
    }

    @Test
    void listsTheEntireCatalogInIdOrderAndIgnoresObsoletePaginationParameters() throws Exception {
        mvc.perform(get("/api/packs").header(HttpHeaders.AUTHORIZATION, admin.bearer()))
                .andExpect(status().isOk()).andExpect(content().json("[]"));
        payload.put("is_acquirable", true);
        var ids = new java.util.ArrayList<Integer>();
        for (int index = 0; index < 23; index++) ids.add(createPack());
        for (Session caller : List.of(user, admin)) {
            String response = mvc.perform(get("/api/packs").param("size", "1").param("page", "99")
                    .header(HttpHeaders.AUTHORIZATION, caller.bearer()))
                    .andExpect(status().isOk()).andExpect(jsonPath("$").isArray())
                    .andReturn().getResponse().getContentAsString();
            JsonNode result = json.readTree(response);
            assertThat(result.size()).isEqualTo(ids.size());
            for (int index = 0; index < ids.size(); index++) {
                assertThat(result.get(index).path("id").asInt()).isEqualTo(ids.get(index));
            }
        }
    }

    @Test
    void replacesDefinitionAndDeletesUnusedPackage() throws Exception {
        int id = createPack();
        payload.put("pack_name", "Replaced");
        payload.put("pack_price", 0);
        payload.put("mercy_threshold", 2);
        ensureEligibleCard("RARE");
        payload.set("allowed_rarities", json.createArrayNode().add("COMMON").add("RARE"));
        int replacementType = createSlotType("RARE");
        payload.set("pack_slots", json.createArrayNode().add(replacementType));
        payload.put("is_acquirable", true);
        mvc.perform(put("/api/admin/packs/{id}", id).header(HttpHeaders.AUTHORIZATION, admin.bearer())
                .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(payload)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.pack_name").value("Replaced"))
                .andExpect(jsonPath("$.pack_price").value(0))
                .andExpect(jsonPath("$.pack_slots[0]").value(replacementType))
                .andExpect(jsonPath("$.is_acquirable").value(true));
        assertThat(packs.get(id).definition().mercyThreshold()).isEqualTo(2);
        mvc.perform(delete("/api/admin/packs/{id}", id).header(HttpHeaders.AUTHORIZATION, admin.bearer()))
                .andExpect(status().isNoContent());
        mvc.perform(get("/api/packs/{id}", id).header(HttpHeaders.AUTHORIZATION, user.bearer()))
                .andExpect(status().isNotFound());
        assertThat(jdbc.sql("SELECT count(*) FROM pack_slots WHERE id_pack = :id")
                .param("id", id).query(Integer.class).single()).isZero();
    }

    @Test
    void returnsNotFoundForMissingPackage() throws Exception {
        int missing = Integer.MAX_VALUE;
        mvc.perform(get("/api/packs/{id}", missing).header(HttpHeaders.AUTHORIZATION, user.bearer()))
                .andExpect(status().isNotFound());
        mvc.perform(put("/api/admin/packs/{id}", missing).header(HttpHeaders.AUTHORIZATION, admin.bearer())
                .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(payload)))
                .andExpect(status().isNotFound());
        mvc.perform(delete("/api/admin/packs/{id}", missing).header(HttpHeaders.AUTHORIZATION, admin.bearer()))
                .andExpect(status().isNotFound());
    }

    @Test
    void rejectsInvalidIdentifiers() throws Exception {
        mvc.perform(get("/api/packs/0").header(HttpHeaders.AUTHORIZATION, user.bearer()))
                .andExpect(status().isBadRequest());
        mvc.perform(put("/api/admin/packs/0").header(HttpHeaders.AUTHORIZATION, admin.bearer())
                .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(payload)))
                .andExpect(status().isBadRequest());
        mvc.perform(delete("/api/admin/packs/0").header(HttpHeaders.AUTHORIZATION, admin.bearer()))
                .andExpect(status().isBadRequest());
    }

    @Test
    void publishesCatalogAndAdministrativeOperationsInOpenApi() throws Exception {
        String response = mvc.perform(get("/v3/api-docs")).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        JsonNode paths = json.readTree(response).path("paths");
        assertThat(paths.path("/api/packs").has("get")).isTrue();
        assertThat(paths.path("/api/packs").path("get").path("parameters").isMissingNode()).isTrue();
        JsonNode catalogContent = paths.path("/api/packs").path("get").path("responses").path("200").path("content");
        assertThat(catalogContent.size()).isPositive();
        for (JsonNode media : catalogContent) {
            assertThat(media.path("schema").path("type").asText()).isEqualTo("array");
        }
        assertThat(paths.path("/api/packs/{packId}").has("get")).isTrue();
        assertThat(paths.path("/api/admin/packs").has("post")).isTrue();
        assertThat(paths.path("/api/admin/packs/{packId}").has("put")).isTrue();
        assertThat(paths.path("/api/admin/packs/{packId}").has("delete")).isTrue();
    }

    @Test
    void rejectsMissingRequiredFieldsAndInvalidNumbersWithoutInserts() throws Exception {
        for (String field : List.of("pack_name", "pack_description", "pack_price", "allowed_factions",
                "allowed_types", "allowed_expansions", "allowed_rarities", "pack_slots", "is_acquirable", "id_artwork_pull_rates")) {
            ObjectNode invalid = payload.deepCopy();
            invalid.remove(field);
            submit(invalid).andExpect(status().isBadRequest());
        }
        for (String field : List.of("pack_price", "mercy_threshold", "mercy_rarity", "id_artwork_pull_rates")) {
            ObjectNode invalid = payload.deepCopy();
            invalid.put(field, -1);
            submit(invalid).andExpect(status().isBadRequest());
        }
        payload.put("pack_price", 2147483648L);
        submit(payload).andExpect(status().isBadRequest());
        assertThat(jdbc.sql("SELECT count(*) FROM packs").query(Integer.class).single()).isZero();
    }

    @Test
    void rejectsMissingRatesAndCanReassignAnUnusedPackWithoutChangingSharedRates() throws Exception {
        int first = createPack();
        int second = createPack();
        int original = payload.path("id_artwork_pull_rates").asInt();
        payload.put("id_artwork_pull_rates", Integer.MAX_VALUE);
        submit(payload).andExpect(status().isBadRequest());
        mvc.perform(put("/api/admin/packs/{id}", first).header(HttpHeaders.AUTHORIZATION, admin.bearer())
                .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(payload)))
                .andExpect(status().isBadRequest());
        assertThat(packs.get(first).definition().artworkPullRatesId()).isEqualTo(original);
        int replacement = jdbc.sql("""
                INSERT INTO artwork_pull_rates (standard, extended_art, full_art, alternate_art)
                VALUES (100, 0, 0, 0) RETURNING id
                """).query(Integer.class).single();
        payload.put("id_artwork_pull_rates", replacement);
        mvc.perform(put("/api/admin/packs/{id}", first).header(HttpHeaders.AUTHORIZATION, admin.bearer())
                .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(payload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id_artwork_pull_rates").value(replacement))
                .andExpect(jsonPath("$.artwork_pull_rates.standard").value(100.0));
        assertThat(packs.get(second).definition().artworkPullRatesId()).isEqualTo(original);
        assertThat(packs.get(second).artworkPullRates().standard()).isEqualByComparingTo("81");
    }

    @Test
    void rejectsUnknownDeletedAndDuplicateFilterCodes() throws Exception {
        for (String field : List.of("allowed_factions", "allowed_types", "allowed_expansions", "allowed_rarities")) {
            ObjectNode invalid = payload.deepCopy();
            invalid.set(field, json.createArrayNode().add("UNKNOWN"));
            submit(invalid).andExpect(status().isBadRequest());
        }
        payload.set("allowed_factions", json.createArrayNode().add("SAP").add("SAP"));
        submit(payload).andExpect(status().isBadRequest());
        payload.set("allowed_factions", json.createArrayNode().add("SAP"));
        jdbc.sql("UPDATE factions SET is_deleted = true WHERE code = 'SAP'").update();
        submit(payload).andExpect(status().isBadRequest());
    }

    @Test
    void rejectsInvalidDescriptionsAndScalarSlotConfiguration() throws Exception {
        for (String field : List.of("pack_description", "card_number", "granted_number", "granted_rarity",
                "best_pull_rarity", "example_pull")) {
            ObjectNode invalid = payload.deepCopy();
            ((ObjectNode) invalid.get("pack_description")).remove(field);
            submit(invalid).andExpect(status().isBadRequest());
        }
        ObjectNode invalid = payload.deepCopy();
        ((ObjectNode) invalid.get("pack_description")).put("granted_number", 2);
        submit(invalid).andExpect(status().isBadRequest());
        invalid = payload.deepCopy();
        ((ObjectNode) invalid.get("pack_description")).put("granted_rarity", "UNKNOWN");
        submit(invalid).andExpect(status().isBadRequest());
        payload.put("pack_slots", "not a configuration");
        submit(payload).andExpect(status().isBadRequest());
    }

    @Test
    void rejectsIncompleteFabricatedUnknownAndMisnumberedPreviewCards() throws Exception {
        ObjectNode invalid = payload.deepCopy();
        preview(invalid).remove("attack");
        submit(invalid).andExpect(status().isBadRequest());
        invalid = payload.deepCopy();
        preview(invalid).put("unrecognizedField", true);
        submit(invalid).andExpect(status().isBadRequest());
        invalid = payload.deepCopy();
        preview(invalid).put("name", "Fabricated");
        submit(invalid).andExpect(status().isBadRequest());
        invalid = payload.deepCopy();
        preview(invalid).put("id", Integer.MAX_VALUE);
        submit(invalid).andExpect(status().isBadRequest());
        invalid = payload.deepCopy();
        ObjectNode examples = (ObjectNode) invalid.path("pack_description").path("example_pull");
        JsonNode card = examples.remove("card1");
        examples.set("card2", card);
        submit(invalid).andExpect(status().isBadRequest());
    }

    @Test
    void preservesDefinitionAfterRejectedUpdate() throws Exception {
        int id = createPack();
        payload.put("pack_name", "Must not persist");
        preview(payload).put("id", Integer.MAX_VALUE);
        mvc.perform(put("/api/admin/packs/{id}", id).header(HttpHeaders.AUTHORIZATION, admin.bearer())
                .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(payload)))
                .andExpect(status().isBadRequest());
        assertThat(packs.get(id).definition().name()).isEqualTo("Package fixture");
        assertThat(packs.get(id).definition().packSlots()).containsExactly(payload.path("pack_slots").get(0).asInt());
    }

    @Test
    void preservesSlotOrderAndRepeatedTypesWithAnAdditionalRarity() throws Exception {
        jdbc.sql("INSERT INTO rarities (code, name, order_index) VALUES ('MYTHIC', 'Mythic', 5)").update();
        int first = payload.path("pack_slots").get(0).asInt();
        ensureEligibleCard("MYTHIC");
        payload.set("allowed_rarities", json.createArrayNode().add("COMMON").add("MYTHIC"));
        int second = createSlotType("MYTHIC");
        payload.set("pack_slots", json.createArrayNode().add(second).add(first).add(second));
        ObjectNode description = (ObjectNode) payload.path("pack_description");
        description.put("card_number", 3);
        ObjectNode examples = (ObjectNode) description.path("example_pull");
        examples.set("card2", examples.get("card1").deepCopy());
        examples.set("card3", examples.get("card1").deepCopy());
        int id = createPack();
        assertThat(packs.get(id).definition().packSlots()).containsExactly(second, first, second);
        assertThat(packs.get(id).slotPullTypes()).hasSize(2);
        assertThat(jdbc.sql("SELECT id_slot_pull_type FROM pack_slots WHERE id_pack = :id ORDER BY slot_position")
                .param("id", id).query(Integer.class).list()).containsExactly(second, first, second);
        mvc.perform(get("/api/packs").header(HttpHeaders.AUTHORIZATION, admin.bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].pack_slots[0]").value(second))
                .andExpect(jsonPath("$[0].slot_pull_types[1].pull_rates[0].rarity_code").value("MYTHIC"));
    }

    @Test
    void rejectsInvalidSlotTypesCountsAndDeletedRaritiesWithoutChangingStoredSlots() throws Exception {
        int id = createPack();
        int original = payload.path("pack_slots").get(0).asInt();
        for (JsonNode invalid : List.of(json.createArrayNode(), json.createArrayNode().add(0),
                json.createArrayNode().add(-1), json.createArrayNode().addNull(),
                json.createArrayNode().add(Integer.MAX_VALUE), json.createArrayNode().add(original).add(original))) {
            ObjectNode request = payload.deepCopy();
            request.set("pack_slots", invalid);
            submit(request).andExpect(status().isBadRequest());
            mvc.perform(put("/api/admin/packs/{id}", id).header(HttpHeaders.AUTHORIZATION, admin.bearer())
                    .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }
        jdbc.sql("UPDATE rarities SET is_deleted = true WHERE code = 'COMMON'").update();
        submit(payload).andExpect(status().isBadRequest());
        assertThat(packs.get(id).definition().packSlots()).containsExactly(original);
    }

    private void ensureEligibleCard(String rarity) {
        int id = jdbc.sql("""
                INSERT INTO cards (name, id_type, id_faction, id_expansion, id_rarity)
                SELECT 'Eligible fixture', c.id_type, (SELECT id FROM factions WHERE code = 'SAP'), c.id_expansion, r.id
                FROM cards c CROSS JOIN rarities r WHERE c.name = 'Ash Novice' AND r.code = :rarity RETURNING id
                """).param("rarity", rarity).query(Integer.class).single();
        jdbc.sql("INSERT INTO cards_arts (id_card, art_code) VALUES (:id, 'STANDARD')").param("id", id).update();
    }

    private int createSlotType(String rarity) {
        int id = jdbc.sql("INSERT INTO slot_pull_type (description) VALUES ('Test distribution') RETURNING id")
                .query(Integer.class).single();
        jdbc.sql("INSERT INTO type_pull_rate (id_slot_pull_type, id_rarity, probability) "
                + "SELECT :id, id, 100.0000 FROM rarities WHERE code = :rarity")
                .param("id", id).param("rarity", rarity).update();
        return id;
    }

    @Test
    void hidesUnavailablePacksFromUsersButPreservesOwnership() throws Exception {
        int hidden = createPack();
        payload.put("is_acquirable", true);
        int visible = createPack();
        own(user, hidden, 2);
        mvc.perform(get("/api/packs").header(HttpHeaders.AUTHORIZATION, user.bearer()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(visible));
        mvc.perform(get("/api/packs/{id}", hidden).header(HttpHeaders.AUTHORIZATION, user.bearer()))
                .andExpect(status().isNotFound());
        mvc.perform(get("/api/packs").header(HttpHeaders.AUTHORIZATION, admin.bearer()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(hidden)).andExpect(jsonPath("$[1].id").value(visible));
        mvc.perform(get("/api/packs/{id}", hidden).header(HttpHeaders.AUTHORIZATION, admin.bearer()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.is_acquirable").value(false));
        assertThat(ownedQuantity(user, hidden)).isEqualTo(2);
    }

    @Test
    void replacesOwnedPackWithoutRevokingOwnershipOrChangingBalancesAndClearsMercy() throws Exception {
        payload.put("is_acquirable", true);
        int id = createPack();
        own(user, id, 2);
        mercy(user, id);
        payload.put("pack_name", "Updated owned pack");
        payload.put("pack_price", 450);
        payload.put("is_acquirable", false);
        payload.put("mercy_threshold", 5);
        mvc.perform(put("/api/admin/packs/{id}", id).header(HttpHeaders.AUTHORIZATION, admin.bearer())
                .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(payload)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.pack_name").value("Updated owned pack"));
        assertThat(ownedQuantity(user, id)).isEqualTo(2);
        assertThat(balance(user)).isZero();
        assertThat(mercyRows(id)).isZero();
        mvc.perform(get("/api/packs/{id}", id).header(HttpHeaders.AUTHORIZATION, user.bearer()))
                .andExpect(status().isNotFound());
    }

    @Test
    void refundsAllOwnersAtCurrentPriceAndDeletesOnlyRelatedResourcesOnce() throws Exception {
        int id = createPack();
        int other = createPack();
        own(user, id, 3);
        own(admin, id, 2);
        own(user, other, 4);
        mercy(user, id);
        mercy(admin, id);
        mercy(user, other);
        jdbc.sql("UPDATE inventory SET cobalt_shards = 70, rift_cores = 11, scraps = 13 WHERE id_user = :id")
                .param("id", user.id()).update();
        payload.put("pack_price", 450);
        mvc.perform(put("/api/admin/packs/{id}", id).header(HttpHeaders.AUTHORIZATION, admin.bearer())
                .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(payload)))
                .andExpect(status().isOk());
        mercy(user, id);
        mercy(admin, id);
        mvc.perform(delete("/api/admin/packs/{id}", id).header(HttpHeaders.AUTHORIZATION, admin.bearer()))
                .andExpect(status().isNoContent());
        assertThat(balance(user)).isEqualTo(1420);
        assertThat(balance(admin)).isEqualTo(900);
        assertThat(jdbc.sql("SELECT rift_cores + scraps FROM inventory WHERE id_user = :id")
                .param("id", user.id()).query(Integer.class).single()).isEqualTo(24);
        assertThat(jdbc.sql("SELECT count(*) FROM inventory_packs WHERE id_pack = :id")
                .param("id", id).query(Integer.class).single()).isZero();
        assertThat(mercyRows(id)).isZero();
        assertThat(ownedQuantity(user, other)).isEqualTo(4);
        assertThat(mercyRows(other)).isEqualTo(1);
        assertThat(packs.get(other).id()).isEqualTo(other);
        assertThat(jdbc.sql("SELECT count(*) FROM pack_slots WHERE id_pack = :id")
                .param("id", id).query(Integer.class).single()).isZero();
        mvc.perform(delete("/api/admin/packs/{id}", id).header(HttpHeaders.AUTHORIZATION, admin.bearer()))
                .andExpect(status().isNotFound());
        assertThat(balance(user)).isEqualTo(1420);
        assertThat(balance(admin)).isEqualTo(900);
    }

    @Test
    void replacesAndDeletesMercyOnlyPack() throws Exception {
        int id = createPack();
        mercy(user, id);
        mvc.perform(put("/api/admin/packs/{id}", id).header(HttpHeaders.AUTHORIZATION, admin.bearer())
                .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(payload)))
                .andExpect(status().isOk());
        assertThat(mercyRows(id)).isZero();
        mercy(user, id);
        mvc.perform(delete("/api/admin/packs/{id}", id).header(HttpHeaders.AUTHORIZATION, admin.bearer()))
                .andExpect(status().isNoContent());
        assertThat(mercyRows(id)).isZero();
        assertThat(balance(user)).isZero();
    }

    @Test
    void invalidReplacementPreservesOwnedPacksMercyAndDefinition() throws Exception {
        int id = createPack();
        own(user, id, 2);
        mercy(user, id);
        payload.set("pack_slots", json.createArrayNode().add(Integer.MAX_VALUE));
        mvc.perform(put("/api/admin/packs/{id}", id).header(HttpHeaders.AUTHORIZATION, admin.bearer())
                .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(payload)))
                .andExpect(status().isBadRequest());
        assertThat(packs.get(id).definition().name()).isEqualTo("Package fixture");
        assertThat(ownedQuantity(user, id)).isEqualTo(2);
        assertThat(mercyRows(id)).isEqualTo(1);
        assertThat(balance(user)).isZero();
    }

    @Test
    void deletesFreeOwnedPackWithoutChangingBalance() throws Exception {
        payload.put("pack_price", 0);
        int id = createPack();
        own(user, id, 3);
        mvc.perform(delete("/api/admin/packs/{id}", id).header(HttpHeaders.AUTHORIZATION, admin.bearer()))
                .andExpect(status().isNoContent());
        assertThat(balance(user)).isZero();
    }

    private void own(Session owner, int packId, int quantity) {
        jdbc.sql("""
                INSERT INTO inventory_packs (id_inventory, id_pack, item_quantity)
                SELECT id, :packId, :quantity FROM inventory WHERE id_user = :userId
                """).param("packId", packId).param("userId", owner.id()).param("quantity", quantity).update();
    }

    private void mercy(Session owner, int packId) {
        jdbc.sql("""
                INSERT INTO user_mercy (id_user, id_pack, mercy_counter)
                VALUES (:userId, :packId, 3)
                """).param("packId", packId).param("userId", owner.id()).update();
    }

    private int ownedQuantity(Session owner, int packId) {
        return jdbc.sql("""
                SELECT ip.item_quantity FROM inventory_packs ip JOIN inventory i ON i.id = ip.id_inventory
                WHERE i.id_user = :userId AND ip.id_pack = :packId
                """).param("packId", packId).param("userId", owner.id()).query(Integer.class).single();
    }

    private int balance(Session owner) {
        return jdbc.sql("SELECT cobalt_shards FROM inventory WHERE id_user = :id")
                .param("id", owner.id()).query(Integer.class).single();
    }

    private int mercyRows(int packId) {
        return jdbc.sql("SELECT count(*) FROM user_mercy WHERE id_pack = :id")
                .param("id", packId).query(Integer.class).single();
    }

    private static ObjectNode preview(ObjectNode body) {
        return (ObjectNode) body.path("pack_description").path("example_pull").path("card1");
    }

    private ResultActions submit(JsonNode body) throws Exception {
        return mvc.perform(post("/api/admin/packs").header(HttpHeaders.AUTHORIZATION, admin.bearer())
                .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body)));
    }

    private int createPack() throws Exception {
        var response = submit(payload).andExpect(status().isCreated()).andReturn().getResponse();
        int id = json.readTree(response.getContentAsString()).path("id").asInt();
        assertThat(response.getHeader(HttpHeaders.LOCATION)).isEqualTo("/api/packs/" + id);
        return id;
    }

    private Session register() throws Exception {
        String suffix = UUID.randomUUID().toString().replace("-", "");
        var response = mvc.perform(post("/api/auth/signin").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"nickname":"Pack-%s","email":"%s@example.invalid","password":"Test password 1"}
                        """.formatted(suffix, suffix)))
                .andExpect(status().isCreated()).andReturn().getResponse();
        JsonNode result = json.readTree(response.getContentAsString());
        return new Session(result.path("user").path("id").asInt(), result.path("accessToken").asText());
    }

    private record Session(int id, String token) {
        String bearer() { return "Bearer " + token; }
    }
}
