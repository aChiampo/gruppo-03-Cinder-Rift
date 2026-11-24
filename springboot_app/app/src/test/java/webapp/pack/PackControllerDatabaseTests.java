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
        int cardId = jdbc.sql("SELECT id FROM cards WHERE code = 'CR01-001'").query(Integer.class).single();
        JsonNode preview = json.valueToTree(cards.findById(cardId).orElseThrow());
        int ratesId = jdbc.sql("SELECT id FROM artwork_pull_rates ORDER BY id LIMIT 1").query(Integer.class).single();
        payload = json.valueToTree(new PackWriteRequest(ratesId, "Package fixture",
                new PackWriteRequest.Description("Fixture only", 1, 0, "COMMON", "COMMON",
                        Map.of("card1", preview)), 300, null,
                List.of("SAP"), List.of("CREATURE"), List.of("CR01"), List.of("COMMON"),
                null, json.createObjectNode()));
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
        int id = createPack();
        mvc.perform(get("/api/packs").header(HttpHeaders.AUTHORIZATION, user.bearer()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.items[0].id").value(id));
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
                .andExpect(jsonPath("$.slot_pulls").isMap())
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
        assertThat(packs.get(id).definition().slotPulls()).isEqualTo(payload.path("slot_pulls"));
    }

    @Test
    void paginatesInStableIdOrderAndReturnsEmptyPagePastTheEnd() throws Exception {
        int first = createPack();
        int second = createPack();
        mvc.perform(get("/api/packs").param("size", "1").header(HttpHeaders.AUTHORIZATION, user.bearer()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.totalPages").value(2)).andExpect(jsonPath("$.items[0].id").value(first));
        mvc.perform(get("/api/packs").param("size", "1").param("page", "1")
                .header(HttpHeaders.AUTHORIZATION, user.bearer()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.items[0].id").value(second));
        mvc.perform(get("/api/packs").param("page", "2").header(HttpHeaders.AUTHORIZATION, user.bearer()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.items").isEmpty());
    }

    @Test
    void replacesDefinitionAndDeletesUnusedPackage() throws Exception {
        int id = createPack();
        payload.put("pack_name", "Replaced");
        payload.put("pack_price", 0);
        payload.put("mercy_threshold", 2);
        payload.set("slot_pulls", json.createArrayNode());
        mvc.perform(put("/api/admin/packs/{id}", id).header(HttpHeaders.AUTHORIZATION, admin.bearer())
                .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(payload)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.pack_name").value("Replaced"))
                .andExpect(jsonPath("$.pack_price").value(0)).andExpect(jsonPath("$.slot_pulls").isArray());
        assertThat(packs.get(id).definition().mercyThreshold()).isEqualTo(2);
        mvc.perform(delete("/api/admin/packs/{id}", id).header(HttpHeaders.AUTHORIZATION, admin.bearer()))
                .andExpect(status().isNoContent());
        mvc.perform(get("/api/packs/{id}", id).header(HttpHeaders.AUTHORIZATION, user.bearer()))
                .andExpect(status().isNotFound());
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
    void rejectsInvalidPaginationAndIdentifiers() throws Exception {
        for (String size : List.of("0", "101", "invalid")) {
            mvc.perform(get("/api/packs").param("size", size).header(HttpHeaders.AUTHORIZATION, user.bearer()))
                    .andExpect(status().isBadRequest());
        }
        mvc.perform(get("/api/packs").param("page", "-1").header(HttpHeaders.AUTHORIZATION, user.bearer()))
                .andExpect(status().isBadRequest());
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
        assertThat(paths.path("/api/packs/{packId}").has("get")).isTrue();
        assertThat(paths.path("/api/admin/packs").has("post")).isTrue();
        assertThat(paths.path("/api/admin/packs/{packId}").has("put")).isTrue();
        assertThat(paths.path("/api/admin/packs/{packId}").has("delete")).isTrue();
    }

    @Test
    void rejectsMissingRequiredFieldsAndInvalidNumbersWithoutInserts() throws Exception {
        for (String field : List.of("pack_name", "pack_description", "pack_price", "allowed_factions",
                "allowed_types", "allowed_expansions", "allowed_rarities", "slot_pulls", "id_artwork_pull_rates")) {
            ObjectNode invalid = payload.deepCopy();
            invalid.remove(field);
            submit(invalid).andExpect(status().isBadRequest());
        }
        for (String field : List.of("pack_price", "mercy_threshold", "id_artwork_pull_rates")) {
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
        payload.put("slot_pulls", "not a configuration");
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
    }

    @Test
    void protectsOwnedPackagesFromChangesAndDeletion() throws Exception {
        int id = createPack();
        jdbc.sql("""
                INSERT INTO inventory_packs (id_inventory, id_pack, item_quantity)
                SELECT id, :packId, 1 FROM inventory WHERE id_user = :userId
                """).param("packId", id).param("userId", user.id()).update();
        assertProtected(id);
        assertThat(jdbc.sql("SELECT item_quantity FROM inventory_packs WHERE id_pack = :id")
                .param("id", id).query(Integer.class).single()).isEqualTo(1);
    }

    @Test
    void protectsMercyProgressEvenWithoutOwnedPackages() throws Exception {
        int id = createPack();
        jdbc.sql("""
                INSERT INTO user_mercy (id_user, id_pack, mercy_counter, mercy_rarity)
                VALUES (:userId, :packId, 0, (SELECT id FROM rarities WHERE code = 'EPIC'))
                """).param("packId", id).param("userId", user.id()).update();
        assertProtected(id);
        assertThat(jdbc.sql("SELECT mercy_counter FROM user_mercy WHERE id_pack = :id")
                .param("id", id).query(Integer.class).single()).isZero();
    }

    private void assertProtected(int id) throws Exception {
        payload.put("pack_name", "Forbidden update");
        mvc.perform(put("/api/admin/packs/{id}", id).header(HttpHeaders.AUTHORIZATION, admin.bearer())
                .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(payload)))
                .andExpect(status().isConflict());
        mvc.perform(delete("/api/admin/packs/{id}", id).header(HttpHeaders.AUTHORIZATION, admin.bearer()))
                .andExpect(status().isConflict());
        assertThat(packs.get(id).definition().name()).isEqualTo("Package fixture");
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
