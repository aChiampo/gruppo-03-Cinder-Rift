package webapp.auth;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {"app.security.session-token-cleanup-cron=-",
        "app.security.password-reset-token-cleanup-cron=-"})
@Transactional
class DeckControllerDatabaseTests {
    @Autowired private WebApplicationContext context;
    @Autowired private AuthenticationService authentication;
    @Autowired private JdbcClient jdbc;

    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    @Test
    void createsListsAndReadsDecksWithLiveValidation() throws Exception {
        AuthenticationService.IssuedSession owner = createUser();
        int[] card = createOwnedCard(owner.user().id());
        String authorization = bearer(owner);

        String created = mvc.perform(post("/api/users/me/decks")
                        .header(HttpHeaders.AUTHORIZATION, authorization)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"entries":[{"cardId":%d,"cardArtId":%d,"destination":"ROUND1"}]}
                                """.formatted(card[0], card[1])))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.ownerId").value(owner.user().id()))
                .andExpect(jsonPath("$.isChallengeable").value(false))
                .andExpect(jsonPath("$.entries[0].entryId").isNumber())
                .andExpect(jsonPath("$.entries[0].cardId").value(card[0]))
                .andExpect(jsonPath("$.validation.valid").value(true))
                .andReturn()
                .getResponse()
                .getContentAsString();
        int deckId = new com.fasterxml.jackson.databind.ObjectMapper().readTree(created).get("id").asInt();

        mvc.perform(get("/api/users/me/decks").header(HttpHeaders.AUTHORIZATION, authorization))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(deckId))
                .andExpect(jsonPath("$[0].cardCount").value(1))
                .andExpect(jsonPath("$[0].validation.summary.totalCards").value(1))
                .andExpect(jsonPath("$[0].validation.warnings[0].severity").value("WARNING"));

        mvc.perform(get("/api/decks/{deckId}", deckId).header(HttpHeaders.AUTHORIZATION, authorization))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(deckId))
                .andExpect(jsonPath("$.ownerId").value(owner.user().id()))
                .andExpect(jsonPath("$.entries[0].cardArtId").value(card[1]))
                .andExpect(jsonPath("$.validation.summary.creatureCount").value(1));
    }

    @Test
    void createsAnEmptyDraftWhenPostHasNoBody() throws Exception {
        AuthenticationService.IssuedSession owner = createUser();

        mvc.perform(post("/api/users/me/decks").header(HttpHeaders.AUTHORIZATION, bearer(owner)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.ownerId").value(owner.user().id()))
                .andExpect(jsonPath("$.entries").isEmpty())
                .andExpect(jsonPath("$.isChallengeable").value(false));
    }

    @Test
    void replacesDeckAtomicallyAndRejectsAnInvalidChallengeableDeckAsProblemDetail() throws Exception {
        AuthenticationService.IssuedSession owner = createUser();
        int[] card = createOwnedCard(owner.user().id());
        String authorization = bearer(owner);
        int deckId = createEmptyDeck(authorization);

        mvc.perform(put("/api/decks/{deckId}", deckId)
                        .header(HttpHeaders.AUTHORIZATION, authorization)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"entries":[{"cardId":%d,"cardArtId":%d,"destination":"ROUND1"}]}
                                """.formatted(card[0], card[1])))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isChallengeable").value(false))
                .andExpect(jsonPath("$.entries").isArray())
                .andExpect(jsonPath("$.entries.length()").value(1))
                .andExpect(jsonPath("$.validation.warnings").isNotEmpty());

        mvc.perform(put("/api/decks/{deckId}", deckId)
                        .header(HttpHeaders.AUTHORIZATION, authorization)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"entries":[],"isChallengeable":true}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.errors[?(@.code == 'DECK_COUNT_INCOMPLETE')]").isNotEmpty());

        mvc.perform(get("/api/decks/{deckId}", deckId).header(HttpHeaders.AUTHORIZATION, authorization))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isChallengeable").value(false))
                .andExpect(jsonPath("$.entries.length()").value(1))
                .andExpect(jsonPath("$.entries[0].cardId").value(card[0]));
    }

    @Test
    void requiresAuthenticationAndHidesDecksOwnedByOtherUsers() throws Exception {
        AuthenticationService.IssuedSession owner = createUser();
        AuthenticationService.IssuedSession other = createUser();
        String ownerAuthorization = bearer(owner);
        String otherAuthorization = bearer(other);
        int deckId = createEmptyDeck(ownerAuthorization);

        mvc.perform(get("/api/users/me/decks")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/decks/{deckId}", deckId).header(HttpHeaders.AUTHORIZATION, otherAuthorization))
                .andExpect(status().isNotFound());
        mvc.perform(put("/api/decks/{deckId}", deckId)
                        .header(HttpHeaders.AUTHORIZATION, otherAuthorization)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"entries":[]}
                                """))
                .andExpect(status().isNotFound());
        mvc.perform(delete("/api/decks/{deckId}", deckId).header(HttpHeaders.AUTHORIZATION, otherAuthorization))
                .andExpect(status().isNotFound());
        mvc.perform(get("/api/decks/{deckId}", Integer.MAX_VALUE)
                        .header(HttpHeaders.AUTHORIZATION, ownerAuthorization))
                .andExpect(status().isNotFound());
        mvc.perform(get("/api/users/me/decks").header(HttpHeaders.AUTHORIZATION, otherAuthorization))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());

        mvc.perform(delete("/api/decks/{deckId}", deckId).header(HttpHeaders.AUTHORIZATION, ownerAuthorization))
                .andExpect(status().isNoContent());
        mvc.perform(get("/api/decks/{deckId}", deckId).header(HttpHeaders.AUTHORIZATION, ownerAuthorization))
                .andExpect(status().isNotFound());
    }

    private AuthenticationService.IssuedSession createUser() {
        String suffix = UUID.randomUUID().toString().replace("-", "");
        return authentication.register("DeckApi-" + suffix, suffix + "@example.invalid", "Test password 1");
    }

    private int createEmptyDeck(String authorization) throws Exception {
        String response = mvc.perform(post("/api/users/me/decks")
                        .header(HttpHeaders.AUTHORIZATION, authorization)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return new com.fasterxml.jackson.databind.ObjectMapper().readTree(response).get("id").asInt();
    }

    private int[] createOwnedCard(int userId) {
        int cardId = jdbc.sql("""
                INSERT INTO cards (id_type, energy_cost, power, id_rarity)
                VALUES ((SELECT id FROM card_types WHERE code = 'CREATURE'), 1, 1,
                        (SELECT id FROM rarities WHERE code = 'COMMON'))
                RETURNING id
                """)
                .query(Integer.class)
                .single();
        int cardArtId = jdbc.sql("""
                INSERT INTO cards_arts (id_card, art_code)
                VALUES (:cardId, 'default')
                RETURNING id_art
                """)
                .param("cardId", cardId)
                .query(Integer.class)
                .single();
        jdbc.sql("""
                INSERT INTO collections (id_user, id_card, id_card_art, owned_replica, tradable_replica)
                VALUES (:userId, :cardId, :cardArtId, 1, 1)
                """)
                .param("userId", userId)
                .param("cardId", cardId)
                .param("cardArtId", cardArtId)
                .update();
        return new int[] {cardId, cardArtId};
    }

    private static String bearer(AuthenticationService.IssuedSession session) {
        return "Bearer " + session.accessToken();
    }
}
