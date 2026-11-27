package webapp.pvp;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
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
import org.springframework.transaction.support.TransactionSynchronizationManager;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Database-backed tests for direct friendly PvP challenges, polling, acceptance, and decline/cancel. */
@SpringBootTest(properties = {
        "app.security.session-token-cleanup-cron=-",
        "app.security.password-reset-token-cleanup-cron=-"
})
class PvpChallengeDatabaseTests {
    private static final JsonMapper JSON = JsonMapper.builder().build();

    @Autowired private WebApplicationContext context;
    @Autowired private JdbcClient jdbc;
    @MockitoBean private BattleEngineClient engineClient;

    private MockMvc mvc;
    private TestUser alice;
    private TestUser bob;
    private TestUser carol;
    private int aliceDeckId;
    private int bobDeckId;

    @BeforeEach
    void prepare() throws Exception {
        reset(engineClient);
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        jdbc.sql("DELETE FROM pvp_challenges").update();
        alice = createUser("ChallengeA");
        bob = createUser("ChallengeB");
        carol = createUser("ChallengeC");
        aliceDeckId = createPlayableDeck(alice.userId());
        bobDeckId = createPlayableDeck(bob.userId());
    }

    @Test
    void rejectsNonFriendsAndSelfChallengesWithoutCallingEngine() throws Exception {
        // Self-challenge is rejected.
        mvc.perform(post("/api/pvp/challenges")
                        .header("Authorization", bearer(alice))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"opponentUserId\":" + alice.userId() + ",\"deckId\":" + aliceDeckId + "}"))
                .andExpect(status().isBadRequest());

        // Strangers cannot challenge each other.
        mvc.perform(post("/api/pvp/challenges")
                        .header("Authorization", bearer(alice))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"opponentUserId\":" + bob.userId() + ",\"deckId\":" + aliceDeckId + "}"))
                .andExpect(status().isForbidden());

        // Pending friendship is not sufficient; must be ACCEPTED.
        insertFriendship(alice.userId(), bob.userId(), alice.userId(), "pending");
        mvc.perform(post("/api/pvp/challenges")
                        .header("Authorization", bearer(alice))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"opponentUserId\":" + bob.userId() + ",\"deckId\":" + aliceDeckId + "}"))
                .andExpect(status().isForbidden());

        // Invalid payload is rejected.
        mvc.perform(post("/api/pvp/challenges")
                        .header("Authorization", bearer(alice))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"opponentUserId\":" + bob.userId() + ",\"deckId\":0}"))
                .andExpect(status().isBadRequest());

        mvc.perform(post("/api/pvp/challenges")
                        .header("Authorization", bearer(alice))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"opponentUserId\":" + bob.userId() + ",\"deckId\":" + aliceDeckId
                                + ",\"unexpected\":true}"))
                .andExpect(status().isBadRequest());

        acceptFriendship(alice.userId(), carol.userId());
        int challengeId = createChallenge(alice, carol.userId(), aliceDeckId);
        mvc.perform(post("/api/pvp/challenges/" + challengeId + "/accept")
                        .header("Authorization", bearer(carol))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"deckId\":\"" + aliceDeckId + "\"}"))
                .andExpect(status().isBadRequest());
        int emptyDeckId = jdbc.sql("INSERT INTO decks(id_user) VALUES (:user) RETURNING id")
                .param("user", carol.userId())
                .query(Integer.class)
                .single();
        mvc.perform(post("/api/pvp/challenges/" + challengeId + "/accept")
                        .header("Authorization", bearer(carol))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"deckId\":" + emptyDeckId + "}"))
                .andExpect(status().isUnprocessableEntity());

        JsonNode pending = JSON.readTree(mvc.perform(get("/api/pvp/challenges/" + challengeId)
                        .header("Authorization", bearer(alice)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString());
        assertThat(pending.get("status").stringValue()).isEqualTo("PENDING");
        verifyNoInteractions(engineClient);
    }

    @Test
    void createsPendingListsIncomingOutgoingAndCompletesFriendlyBattleOutsideTransaction() throws Exception {
        acceptFriendship(alice.userId(), bob.userId());

        MvcResult created = mvc.perform(post("/api/pvp/challenges")
                        .header("Authorization", bearer(alice))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"opponentUserId\":" + bob.userId() + ",\"deckId\":" + aliceDeckId + "}"))
                .andExpect(status().isCreated())
                .andReturn();
        JsonNode createdBody = JSON.readTree(created.getResponse().getContentAsString());
        int challengeId = createdBody.get("id").intValue();
        assertThat(createdBody.get("challengerId").intValue()).isEqualTo(alice.userId());
        assertThat(createdBody.get("opponentId").intValue()).isEqualTo(bob.userId());
        assertThat(createdBody.get("challengerDeckId").intValue()).isEqualTo(aliceDeckId);
        assertThat(createdBody.get("status").stringValue()).isEqualTo("PENDING");
        assertThat(createdBody.has("battle")).isFalse();
        verifyNoInteractions(engineClient);

        // Challenger sees it in outgoing; opponent sees it in incoming.
        JsonNode aliceList = JSON.readTree(mvc.perform(get("/api/pvp/challenges")
                        .header("Authorization", bearer(alice)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString());
        assertThat(aliceList.get("incoming").size()).isZero();
        assertThat(aliceList.get("outgoing").size()).isEqualTo(1);
        assertThat(aliceList.get("outgoing").get(0).get("id").intValue()).isEqualTo(challengeId);
        assertThat(aliceList.get("outgoing").get(0).get("status").stringValue()).isEqualTo("PENDING");

        JsonNode bobList = JSON.readTree(mvc.perform(get("/api/pvp/challenges")
                        .header("Authorization", bearer(bob)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString());
        assertThat(bobList.get("outgoing").size()).isZero();
        assertThat(bobList.get("incoming").size()).isEqualTo(1);
        assertThat(bobList.get("incoming").get(0).get("id").intValue()).isEqualTo(challengeId);
        assertThat(bobList.get("incoming").get(0).get("status").stringValue()).isEqualTo("PENDING");

        for (TestUser participant : List.of(alice, bob)) {
            JsonNode pending = JSON.readTree(mvc.perform(get("/api/pvp/challenges/" + challengeId)
                            .header("Authorization", bearer(participant)))
                    .andExpect(status().isOk())
                    .andReturn().getResponse().getContentAsString());
            assertThat(pending.get("status").stringValue()).isEqualTo("PENDING");
        }
        mvc.perform(get("/api/pvp/challenges/" + challengeId)
                        .header("Authorization", bearer(carol)))
                .andExpect(status().isForbidden());

        // Only the challenged opponent may accept.
        mvc.perform(post("/api/pvp/challenges/" + challengeId + "/accept")
                        .header("Authorization", bearer(alice))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"deckId\":" + aliceDeckId + "}"))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/pvp/challenges/" + challengeId + "/accept")
                        .header("Authorization", bearer(carol))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"deckId\":" + aliceDeckId + "}"))
                .andExpect(status().isForbidden());

        CountDownLatch engineStarted = new CountDownLatch(1);
        CountDownLatch finishEngine = new CountDownLatch(1);
        when(engineClient.executePvp(any(EnginePvpBattleRequest.class), any(BattleBudget.class)))
                .thenAnswer(invocation -> {
                    assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isFalse();
                    engineStarted.countDown();
                    if (!finishEngine.await(5, TimeUnit.SECONDS)) {
                        throw new AssertionError("Timed out waiting to finish the test battle.");
                    }
                    return new EnginePvpBattleResponse(
                            "FRIENDLY",
                            "PLAYER_B_WIN",
                            Integer.toString(bob.userId()),
                            bob.nickname(),
                            List.of("Round 1: Friendly clash", "Bob wins"));
                });

        var executor = Executors.newSingleThreadExecutor();
        MvcResult accepted;
        try {
            var firstAccept = executor.submit(() -> mvc.perform(post("/api/pvp/challenges/" + challengeId + "/accept")
                            .header("Authorization", bearer(bob))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"deckId\":" + bobDeckId + "}"))
                    .andReturn());
            assertThat(engineStarted.await(5, TimeUnit.SECONDS)).isTrue();
            mvc.perform(post("/api/pvp/challenges/" + challengeId + "/accept")
                            .header("Authorization", bearer(bob))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"deckId\":" + bobDeckId + "}"))
                    .andExpect(status().isConflict());
            finishEngine.countDown();
            accepted = firstAccept.get(5, TimeUnit.SECONDS);
            assertThat(accepted.getResponse().getStatus()).isEqualTo(200);
        } finally {
            finishEngine.countDown();
            executor.shutdownNow();
        }
        JsonNode acceptedBody = JSON.readTree(accepted.getResponse().getContentAsString());
        assertThat(acceptedBody.get("id").intValue()).isEqualTo(challengeId);
        assertThat(acceptedBody.get("status").stringValue()).isEqualTo("COMPLETED");
        assertThat(acceptedBody.get("opponentDeckId").intValue()).isEqualTo(bobDeckId);

        JsonNode battle = acceptedBody.get("battle");
        int battleId = battle.get("id").intValue();
        assertThat(battle.get("mode").stringValue()).isEqualTo("FRIENDLY");
        assertThat(battle.get("playerAId").intValue()).isEqualTo(alice.userId());
        assertThat(battle.get("playerBId").intValue()).isEqualTo(bob.userId());
        assertThat(battle.get("winnerId").intValue()).isEqualTo(bob.userId());
        assertThat(battle.get("outcome").stringValue()).isEqualTo("PLAYER_B_WIN");
        assertThat(battle.get("rankingDelta").intValue()).isZero();
        assertThat(battle.get("battleLog").size()).isEqualTo(2);

        ArgumentCaptor<EnginePvpBattleRequest> requestCaptor = ArgumentCaptor.forClass(EnginePvpBattleRequest.class);
        verify(engineClient, times(1)).executePvp(requestCaptor.capture(), any(BattleBudget.class));
        EnginePvpBattleRequest engineRequest = requestCaptor.getValue();
        assertThat(engineRequest.mode()).isEqualTo("FRIENDLY");
        assertThat(engineRequest.playerA().userId()).isEqualTo(Integer.toString(alice.userId()));
        assertThat(engineRequest.playerB().userId()).isEqualTo(Integer.toString(bob.userId()));
        assertThat(engineRequest.deckA().entries()).hasSize(30);
        assertThat(engineRequest.deckB().entries()).hasSize(30);

        // No longer pending in incoming/outgoing, and battle appears in history for both players.
        JsonNode aliceAfter = JSON.readTree(mvc.perform(get("/api/pvp/challenges")
                        .header("Authorization", bearer(alice)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString());
        assertThat(aliceAfter.get("outgoing").size()).isZero();

        JsonNode alicePoll = JSON.readTree(mvc.perform(get("/api/pvp/challenges/" + challengeId)
                        .header("Authorization", bearer(alice)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString());
        assertThat(alicePoll.get("status").stringValue()).isEqualTo("COMPLETED");
        assertThat(alicePoll.get("battle").get("id").intValue()).isEqualTo(battleId);
        assertThat(alicePoll.get("battle").get("mode").stringValue()).isEqualTo("FRIENDLY");
        assertThat(alicePoll.get("battle").get("rankingDelta").intValue()).isZero();

        for (TestUser player : List.of(alice, bob)) {
            JsonNode history = JSON.readTree(mvc.perform(get("/api/battles/history?mode=FRIENDLY")
                            .header("Authorization", bearer(player)))
                    .andExpect(status().isOk())
                    .andReturn().getResponse().getContentAsString());
            assertThat(history.size()).isEqualTo(1);
            assertThat(history.get(0).get("id").intValue()).isEqualTo(battleId);
            assertThat(history.get(0).get("mode").stringValue()).isEqualTo("FRIENDLY");
            assertThat(history.get(0).get("rankingDelta").intValue()).isZero();
        }

        // Accepting or declining a COMPLETED challenge is rejected.
        mvc.perform(post("/api/pvp/challenges/" + challengeId + "/accept")
                        .header("Authorization", bearer(bob))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"deckId\":" + bobDeckId + "}"))
                .andExpect(status().isConflict());
        mvc.perform(post("/api/pvp/challenges/" + challengeId + "/decline")
                        .header("Authorization", bearer(bob)))
                .andExpect(status().isConflict());
    }

    @Test
    void opponentDeclinesAndChallengerCancelsPendingChallenges() throws Exception {
        acceptFriendship(alice.userId(), bob.userId());

        // Case 1: opponent declines -> DECLINED
        int firstId = createChallenge(alice, bob.userId(), aliceDeckId);
        JsonNode declined = JSON.readTree(mvc.perform(post("/api/pvp/challenges/" + firstId + "/decline")
                        .header("Authorization", bearer(bob)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString());
        assertThat(declined.get("status").stringValue()).isEqualTo("DECLINED");
        assertThat(declined.get("respondedAt").isString()).isTrue();

        mvc.perform(post("/api/pvp/challenges/" + firstId + "/decline")
                        .header("Authorization", bearer(bob)))
                .andExpect(status().isConflict());
        mvc.perform(post("/api/pvp/challenges/" + firstId + "/accept")
                        .header("Authorization", bearer(bob))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"deckId\":" + bobDeckId + "}"))
                .andExpect(status().isConflict());

        // Case 2: challenger cancels via decline -> CANCELLED
        int secondId = createChallenge(alice, bob.userId(), aliceDeckId);
        mvc.perform(post("/api/pvp/challenges/" + secondId + "/decline")
                        .header("Authorization", bearer(carol)))
                .andExpect(status().isForbidden());

        JsonNode cancelled = JSON.readTree(mvc.perform(post("/api/pvp/challenges/" + secondId + "/decline")
                        .header("Authorization", bearer(alice)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString());
        assertThat(cancelled.get("status").stringValue()).isEqualTo("CANCELLED");

        mvc.perform(post("/api/pvp/challenges/99999999/decline")
                        .header("Authorization", bearer(alice)))
                .andExpect(status().isNotFound());
        verifyNoInteractions(engineClient);
    }

    @Test
    void keepsChallengePendingWhenEngineFails() throws Exception {
        acceptFriendship(alice.userId(), bob.userId());
        int challengeId = createChallenge(alice, bob.userId(), aliceDeckId);

        when(engineClient.executePvp(any(EnginePvpBattleRequest.class), any(BattleBudget.class)))
                .thenThrow(BattleFailure.unavailable());

        mvc.perform(post("/api/pvp/challenges/" + challengeId + "/accept")
                        .header("Authorization", bearer(bob))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"deckId\":" + bobDeckId + "}"))
                .andExpect(status().isServiceUnavailable());

        JsonNode bobList = JSON.readTree(mvc.perform(get("/api/pvp/challenges")
                        .header("Authorization", bearer(bob)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString());
        assertThat(bobList.get("incoming").size()).isEqualTo(1);
        assertThat(bobList.get("incoming").get(0).get("status").stringValue()).isEqualTo("PENDING");
    }

    private int createChallenge(TestUser challenger, int opponentUserId, int deckId) throws Exception {
        MvcResult result = mvc.perform(post("/api/pvp/challenges")
                        .header("Authorization", bearer(challenger))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"opponentUserId\":" + opponentUserId + ",\"deckId\":" + deckId + "}"))
                .andExpect(status().isCreated())
                .andReturn();
        return JSON.readTree(result.getResponse().getContentAsString()).get("id").intValue();
    }

    private void acceptFriendship(int userA, int userB) {
        insertFriendship(userA, userB, userA, "accepted");
    }

    private void insertFriendship(int user1, int user2, int requester, String status) {
        jdbc.sql("""
                INSERT INTO friendships (user_a_id, user_b_id, requested_by, status, accepted_at)
                VALUES (:a, :b, :requester, :status, CASE WHEN :status = 'accepted' THEN now() ELSE NULL END)
                """)
                .param("a", Math.min(user1, user2))
                .param("b", Math.max(user1, user2))
                .param("requester", requester)
                .param("status", status)
                .update();
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
                SELECT 'PvP challenge card ' || n,
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
