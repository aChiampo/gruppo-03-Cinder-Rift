package webapp.card;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {"app.security.session-token-cleanup-cron=-",
        "app.security.password-reset-token-cleanup-cron=-"})
@Transactional
class CardCollectionControllerDatabaseTests {
    @Autowired private WebApplicationContext context;
    @Autowired private JdbcClient jdbc;
    @Autowired private ObjectMapper objectMapper;

    private MockMvc mvc;
    private TestSession owner;
    private TestSession otherUser;
    private DemoCardFixtures.Fixture fixture;

    @BeforeEach
    void setUp() throws Exception {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        owner = createUser();
        otherUser = createUser();
        fixture = DemoCardFixtures.seed(jdbc, owner.userId());
    }

    @Test
    void requiresAuthenticationForCatalogAndCollection() throws Exception {
        mvc.perform(get("/api/cards"))
                .andExpect(status().isUnauthorized());
        mvc.perform(get("/api/users/me/collections"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void returnsCatalogFieldsAndEveryArtworkVariant() throws Exception {
        mvc.perform(get("/api/cards")
                        .header(HttpHeaders.AUTHORIZATION, bearer(owner))
                        .param("name", "Demo Creature R1-01"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.items[0].id").value(fixture.firstCardId()))
                .andExpect(jsonPath("$.items[0].name").value("Demo Creature R1-01"))
                .andExpect(jsonPath("$.items[0].faction.code").value("SAPIENS"))
                .andExpect(jsonPath("$.items[0].rarity.code").value("COMMON"))
                .andExpect(jsonPath("$.items[0].energyCost").value(1))
                .andExpect(jsonPath("$.items[0].attack").value(3))
                .andExpect(jsonPath("$.items[0].defense").value(3))
                .andExpect(jsonPath("$.items[0].power").value(1))
                .andExpect(jsonPath("$.items[0].type.code").value("CREATURE"))
                .andExpect(jsonPath("$.items[0].expansion.code").value("DEMO_FIXTURE"))
                .andExpect(jsonPath("$.items[0].artworks.length()").value(2))
                .andExpect(jsonPath("$.items[0].artworks[0].cardArtId").isNumber())
                .andExpect(jsonPath("$.items[0].artworks[0].imageUrl")
                        .value("https://example.invalid/demo/cards/" + fixture.firstCardId() + "-alternate.svg"));
    }

    @Test
    void appliesCatalogFiltersAndStablePagination() throws Exception {
        mvc.perform(get("/api/cards")
                        .header(HttpHeaders.AUTHORIZATION, bearer(owner))
                        .param("faction", "cyborg")
                        .param("rarity", "common")
                        .param("type", "creature")
                        .param("expansion", "demo_fixture")
                        .param("energyMin", "1")
                        .param("energyMax", "2")
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.items[0].faction.code").value("CYBORG"))
                .andExpect(jsonPath("$.items[0].rarity.code").value("COMMON"));

        mvc.perform(get("/api/cards")
                        .header(HttpHeaders.AUTHORIZATION, bearer(owner))
                        .param("sort", "power")
                        .param("direction", "desc")
                        .param("size", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(fixture.cardCount()))
                .andExpect(jsonPath("$.totalPages").value(fixture.cardCount()))
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].rarity.code").value("LEGENDARY"))
                .andExpect(jsonPath("$.items[0].power").value(5));
    }

    @Test
    void returnsOnlyAuthenticatedUsersOwnCollectionWithCopiesPerArtwork() throws Exception {
        mvc.perform(get("/api/users/me/collections")
                        .header(HttpHeaders.AUTHORIZATION, bearer(owner))
                        .param("name", "Demo Creature R1-01"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.items[0].cardId").value(fixture.firstCardId()))
                .andExpect(jsonPath("$.items[0].cardArtId").value(fixture.firstArtworkId()))
                .andExpect(jsonPath("$.items[0].ownedReplica").value(2))
                .andExpect(jsonPath("$.items[0].tradableReplica").value(1))
                .andExpect(jsonPath("$.items[1].cardArtId").value(fixture.alternateArtworkId()))
                .andExpect(jsonPath("$.items[1].ownedReplica").value(1))
                .andExpect(jsonPath("$.items[1].tradableReplica").value(0));

        mvc.perform(get("/api/cards")
                        .header(HttpHeaders.AUTHORIZATION, bearer(owner))
                        .param("name", "Demo Equipment 05"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.items[0].id").value(fixture.unownedCardId()));

        mvc.perform(get("/api/users/me/collections")
                        .header(HttpHeaders.AUTHORIZATION, bearer(owner))
                        .param("name", "Demo Equipment 05"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0))
                .andExpect(jsonPath("$.items").isEmpty());

        mvc.perform(get("/api/users/me/collections")
                        .header(HttpHeaders.AUTHORIZATION, bearer(otherUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0))
                .andExpect(jsonPath("$.items").isEmpty());

        mvc.perform(get("/api/users/{userId}/collections", owner.userId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(otherUser)))
                .andExpect(status().isNotFound());
    }

    @Test
    void rejectsUnsupportedSortsAndInvertedRanges() throws Exception {
        mvc.perform(get("/api/cards")
                        .header(HttpHeaders.AUTHORIZATION, bearer(owner))
                        .param("sort", "sql"))
                .andExpect(status().isBadRequest());

        mvc.perform(get("/api/cards")
                        .header(HttpHeaders.AUTHORIZATION, bearer(owner))
                        .param("sort", ""))
                .andExpect(status().isBadRequest());

        mvc.perform(get("/api/users/me/collections")
                        .header(HttpHeaders.AUTHORIZATION, bearer(owner))
                        .param("energyMin", "5")
                        .param("energyMax", "1"))
                .andExpect(status().isBadRequest());

        mvc.perform(get("/api/cards")
                        .header(HttpHeaders.AUTHORIZATION, bearer(owner))
                        .param("size", "101"))
                .andExpect(status().isBadRequest());
    }

    private TestSession createUser() throws Exception {
        String suffix = UUID.randomUUID().toString().replace("-", "");
        MvcResult result = mvc.perform(post("/api/auth/users")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nickname":"CardApi-%s","email":"%s@example.invalid","password":"Test password 1"}
                                """.formatted(suffix, suffix)))
                .andExpect(status().isCreated())
                .andReturn();
        JsonNode response = objectMapper.readTree(result.getResponse().getContentAsString());
        return new TestSession(response.path("accessToken").asText(), response.path("user").path("id").asInt());
    }

    private static String bearer(TestSession session) {
        return "Bearer " + session.accessToken();
    }

    private record TestSession(String accessToken, int userId) {
    }
}
