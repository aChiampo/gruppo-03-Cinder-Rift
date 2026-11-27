package webapp.pvp;

import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import webapp.battle.BattleBudget;
import webapp.battle.BattleEngineClient;
import webapp.battle.BattleFailure;
import webapp.battle.EnginePvpBattleRequest;
import webapp.battle.EnginePvpBattleResponse;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Database-backed tests for ranked PvP matchmaking queue, polling, cancellation, and history. */
@SpringBootTest(properties = {
        "app.security.session-token-cleanup-cron=-",
        "app.security.password-reset-token-cleanup-cron=-"
})
class PvpQueueDatabaseTests {
    private static final JsonMapper JSON = JsonMapper.builder().build();

    @Autowired private WebApplicationContext context;
    @Autowired private JdbcClient jdbc;
    @MockitoBean private BattleEngineClient engineClient;

    private MockMvc mvc;
    private TestUser playerA;
    private TestUser playerB;
    private int deckAId;
    private int deckBId;

    @BeforeEach
    void prepare() throws Exception {
        reset(engineClient);
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        jdbc.sql("DELETE FROM pvp_matchmaking_queue").update();
        playerA = createUser("PvpA");
        playerB = createUser("PvpB");
        deckAId = createPlayableDeck(playerA.userId());
        deckBId = createPlayableDeck(playerB.userId());
    }

    @Test
    void matchesWaitingOpponentExecutesRankedBattleAndExposesHistoryToBothPlayers() throws Exception {
        when(engineClient.executePvp(any(EnginePvpBattleRequest.class), any(BattleBudget.class)))
                .thenReturn(new EnginePvpBattleResponse(
                        "RANKED",
                        "PLAYER_A_WIN",
                        Integer.toString(playerA.userId()),
                        playerA.nickname(),
                        List.of("Round 1: Player A strikes", "Player A wins")));

        MvcResult joinA = mvc.perform(post("/api/pvp/queue")
                        .header("Authorization", bearer(playerA))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"deckId\":" + deckAId + "}"))
                .andExpect(status().isAccepted())
                .andReturn();
        JsonNode joinABody = JSON.readTree(joinA.getResponse().getContentAsString());
        int ticketA = joinABody.get("ticketId").intValue();
        assertThat(joinABody.get("status").stringValue()).isEqualTo("WAITING");
        assertThat(joinABody.has("battle")).isFalse();
        verifyNoInteractions(engineClient);

        MvcResult pollAWaiting = mvc.perform(get("/api/pvp/queue/" + ticketA)
                        .header("Authorization", bearer(playerA)))
                .andExpect(status().isOk())
                .andReturn();
        assertThat(JSON.readTree(pollAWaiting.getResponse().getContentAsString()).get("status").stringValue())
                .isEqualTo("WAITING");

        // Same user cannot queue twice while already WAITING.
        mvc.perform(post("/api/pvp/queue")
                        .header("Authorization", bearer(playerA))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"deckId\":" + deckAId + "}"))
                .andExpect(status().isConflict());

        MvcResult joinB = mvc.perform(post("/api/pvp/queue")
                        .header("Authorization", bearer(playerB))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"deckId\":" + deckBId + "}"))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode joinBBody = JSON.readTree(joinB.getResponse().getContentAsString());
        int ticketB = joinBBody.get("ticketId").intValue();
        assertThat(ticketB).isNotEqualTo(ticketA);
        assertThat(joinBBody.get("status").stringValue()).isEqualTo("MATCHED");
        JsonNode battleB = joinBBody.get("battle");
        int battleId = battleB.get("id").intValue();
        assertThat(battleB.get("mode").stringValue()).isEqualTo("RANKED");
        assertThat(battleB.get("playerAId").intValue()).isEqualTo(playerA.userId());
        assertThat(battleB.get("playerBId").intValue()).isEqualTo(playerB.userId());
        assertThat(battleB.get("winnerId").intValue()).isEqualTo(playerA.userId());
        assertThat(battleB.get("outcome").stringValue()).isEqualTo("PLAYER_A_WIN");
        assertThat(battleB.get("rankingDelta").intValue()).isZero();
        assertThat(battleB.get("battleLog").size()).isEqualTo(2);

        ArgumentCaptor<EnginePvpBattleRequest> requestCaptor = ArgumentCaptor.forClass(EnginePvpBattleRequest.class);
        verify(engineClient, times(1)).executePvp(requestCaptor.capture(), any(BattleBudget.class));
        EnginePvpBattleRequest engineRequest = requestCaptor.getValue();
        assertThat(engineRequest.mode()).isEqualTo("RANKED");
        assertThat(engineRequest.playerA().userId()).isEqualTo(Integer.toString(playerA.userId()));
        assertThat(engineRequest.playerB().userId()).isEqualTo(Integer.toString(playerB.userId()));
        assertThat(engineRequest.deckA().entries()).hasSize(30);
        assertThat(engineRequest.deckB().entries()).hasSize(30);

        MvcResult pollAMatched = mvc.perform(get("/api/pvp/queue/" + ticketA)
                        .header("Authorization", bearer(playerA)))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode pollABody = JSON.readTree(pollAMatched.getResponse().getContentAsString());
        assertThat(pollABody.get("ticketId").intValue()).isEqualTo(ticketA);
        assertThat(pollABody.get("status").stringValue()).isEqualTo("MATCHED");
        assertThat(pollABody.get("battle").get("id").intValue()).isEqualTo(battleId);
        assertThat(pollABody.get("battle").get("rankingDelta").intValue()).isZero();

        for (TestUser player : List.of(playerA, playerB)) {
            MvcResult historyResult = mvc.perform(get("/api/battles/history?mode=RANKED")
                            .header("Authorization", bearer(player)))
                    .andExpect(status().isOk())
                    .andReturn();
            JsonNode history = JSON.readTree(historyResult.getResponse().getContentAsString());
            assertThat(history.isArray()).isTrue();
            assertThat(history.size()).isEqualTo(1);
            assertThat(history.get(0).get("id").intValue()).isEqualTo(battleId);
            assertThat(history.get(0).get("mode").stringValue()).isEqualTo("RANKED");
            assertThat(history.get(0).get("rankingDelta").intValue()).isZero();
        }
    }

    @Test
    void cancelsWaitingTicketAndAllowsSubsequentRequeue() throws Exception {
        MvcResult joinA = mvc.perform(post("/api/pvp/queue")
                        .header("Authorization", bearer(playerA))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"deckId\":" + deckAId + "}"))
                .andExpect(status().isAccepted())
                .andReturn();
        int ticketA = JSON.readTree(joinA.getResponse().getContentAsString()).get("ticketId").intValue();

        MvcResult deleted = mvc.perform(delete("/api/pvp/queue/" + ticketA)
                        .header("Authorization", bearer(playerA)))
                .andExpect(status().isOk())
                .andReturn();
        assertThat(JSON.readTree(deleted.getResponse().getContentAsString()).get("status").stringValue())
                .isEqualTo("CANCELLED");

        MvcResult polled = mvc.perform(get("/api/pvp/queue/" + ticketA)
                        .header("Authorization", bearer(playerA)))
                .andExpect(status().isOk())
                .andReturn();
        assertThat(JSON.readTree(polled.getResponse().getContentAsString()).get("status").stringValue())
                .isEqualTo("CANCELLED");

        // Deleting a CANCELLED ticket again is rejected.
        mvc.perform(delete("/api/pvp/queue/" + ticketA)
                        .header("Authorization", bearer(playerA)))
                .andExpect(status().isConflict());

        // Player B does not match a cancelled ticket and enters as WAITING.
        mvc.perform(post("/api/pvp/queue")
                        .header("Authorization", bearer(playerB))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"deckId\":" + deckBId + "}"))
                .andExpect(status().isAccepted());
        verifyNoInteractions(engineClient);
    }

    @Test
    void deniesAccessToForeignTicketAndRejectsMissingTicket() throws Exception {
        MvcResult joinA = mvc.perform(post("/api/pvp/queue")
                        .header("Authorization", bearer(playerA))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"deckId\":" + deckAId + "}"))
                .andExpect(status().isAccepted())
                .andReturn();
        int ticketA = JSON.readTree(joinA.getResponse().getContentAsString()).get("ticketId").intValue();

        mvc.perform(get("/api/pvp/queue/" + ticketA)
                        .header("Authorization", bearer(playerB)))
                .andExpect(status().isForbidden());
        mvc.perform(delete("/api/pvp/queue/" + ticketA)
                        .header("Authorization", bearer(playerB)))
                .andExpect(status().isForbidden());

        // Ticket remains WAITING for player A.
        MvcResult pollA = mvc.perform(get("/api/pvp/queue/" + ticketA)
                        .header("Authorization", bearer(playerA)))
                .andExpect(status().isOk())
                .andReturn();
        assertThat(JSON.readTree(pollA.getResponse().getContentAsString()).get("status").stringValue())
                .isEqualTo("WAITING");

        mvc.perform(get("/api/pvp/queue/99999999")
                        .header("Authorization", bearer(playerA)))
                .andExpect(status().isNotFound());
        mvc.perform(delete("/api/pvp/queue/99999999")
                        .header("Authorization", bearer(playerA)))
                .andExpect(status().isNotFound());
    }

    @Test
    void restoresOpponentToWaitingWhenEngineFails() throws Exception {
        MvcResult joinA = mvc.perform(post("/api/pvp/queue")
                        .header("Authorization", bearer(playerA))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"deckId\":" + deckAId + "}"))
                .andExpect(status().isAccepted())
                .andReturn();
        int ticketA = JSON.readTree(joinA.getResponse().getContentAsString()).get("ticketId").intValue();

        when(engineClient.executePvp(any(EnginePvpBattleRequest.class), any(BattleBudget.class)))
                .thenThrow(BattleFailure.unavailable());

        mvc.perform(post("/api/pvp/queue")
                        .header("Authorization", bearer(playerB))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"deckId\":" + deckBId + "}"))
                .andExpect(status().isServiceUnavailable());

        MvcResult pollA = mvc.perform(get("/api/pvp/queue/" + ticketA)
                        .header("Authorization", bearer(playerA)))
                .andExpect(status().isOk())
                .andReturn();
        assertThat(JSON.readTree(pollA.getResponse().getContentAsString()).get("status").stringValue())
                .isEqualTo("WAITING");

        when(engineClient.executePvp(any(EnginePvpBattleRequest.class), any(BattleBudget.class)))
                .thenReturn(new EnginePvpBattleResponse(
                        "RANKED",
                        "DRAW",
                        null,
                        null,
                        List.of("Draw")));

        MvcResult retryB = mvc.perform(post("/api/pvp/queue")
                        .header("Authorization", bearer(playerB))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"deckId\":" + deckBId + "}"))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode retryBody = JSON.readTree(retryB.getResponse().getContentAsString());
        assertThat(retryBody.get("status").stringValue()).isEqualTo("MATCHED");
        assertThat(retryBody.get("battle").get("outcome").stringValue()).isEqualTo("DRAW");
        assertThat(retryBody.get("battle").get("winnerId").isNull()).isTrue();
    }

    @Test
    void exposesMatchingTicketAsWaitingAndValidatesPayloads() throws Exception {
        MvcResult joinA = mvc.perform(post("/api/pvp/queue")
                        .header("Authorization", bearer(playerA))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"deckId\":" + deckAId + "}"))
                .andExpect(status().isAccepted())
                .andReturn();
        int ticketA = JSON.readTree(joinA.getResponse().getContentAsString()).get("ticketId").intValue();

        jdbc.sql("UPDATE pvp_matchmaking_queue SET status = 'MATCHING' WHERE id = :id")
                .param("id", ticketA)
                .update();

        MvcResult pollMatching = mvc.perform(get("/api/pvp/queue/" + ticketA)
                        .header("Authorization", bearer(playerA)))
                .andExpect(status().isOk())
                .andReturn();
        assertThat(JSON.readTree(pollMatching.getResponse().getContentAsString()).get("status").stringValue())
                .isEqualTo("WAITING");

        mvc.perform(post("/api/pvp/queue")
                        .header("Authorization", bearer(playerB))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"deckId\":0}"))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/pvp/queue")
                        .header("Authorization", bearer(playerB))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"deckId\":" + deckAId + "}"))
                .andExpect(status().isNotFound());
    }

    private TestUser createUser(String prefix) throws Exception {
        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        String nickname = prefix + "-" + suffix;
        MvcResult result = mvc.perform(post("/api/auth/signin")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nickname":"%s","email":"%s@example.invalid","password":"Test password 1"}
                                """.formatted(nickname, suffix)))
                .andExpect(status().isCreated())
                .andReturn();
        JsonNode body = JSON.readTree(result.getResponse().getContentAsString());
        return new TestUser(
                body.path("accessToken").stringValue(),
                body.path("user").path("id").intValue(),
                nickname);
    }

    private int createPlayableDeck(int userId) {
        int deckId = jdbc.sql("INSERT INTO decks(id_user) VALUES (:user) RETURNING id")
                .param("user", userId)
                .query(Integer.class)
                .single();
        List<Integer> cardIds = jdbc.sql("""
                INSERT INTO cards(name, id_type, id_faction, id_rarity, energy_cost, power, value_atk, value_def)
                SELECT 'PvP card ' || n,
                       (SELECT id FROM card_types WHERE code = CASE WHEN n <= 25 THEN 'CREATURE' ELSE 'EQUIPMENT' END),
                       (SELECT id FROM factions WHERE code = 'CYB'),
                       (SELECT id FROM rarities WHERE code = 'COMMON'), 1, 1, 3, 5
                FROM generate_series(1, 30) n RETURNING id
                """).query(Integer.class).list();
        for (int index = 0; index < cardIds.size(); index++) {
            int cardId = cardIds.get(index);
            int artId = jdbc.sql("INSERT INTO cards_arts(id_card, art_code) VALUES (:card, 'default') RETURNING id_art")
                    .param("card", cardId)
                    .query(Integer.class)
                    .single();
            jdbc.sql("""
                    INSERT INTO collections(id_user, id_card, id_card_art, owned_replica, tradable_replica)
                    VALUES (:user, :card, :art, 1, 1)
                    """)
                    .param("user", userId)
                    .param("card", cardId)
                    .param("art", artId)
                    .update();
            jdbc.sql("INSERT INTO decks_cards(id_deck, id_card, id_card_art, destination) VALUES (:deck, :card, :art, :dest)")
                    .param("deck", deckId)
                    .param("card", cardId)
                    .param("art", artId)
                    .param("dest", index < 25 ? "ROUND" + (index / 5 + 1) : "EQUIPMENT")
                    .update();
        }
        return deckId;
    }

    private static String bearer(TestUser user) {
        return "Bearer " + user.accessToken();
    }

    private record TestUser(String accessToken, int userId, String nickname) {
    }
}
