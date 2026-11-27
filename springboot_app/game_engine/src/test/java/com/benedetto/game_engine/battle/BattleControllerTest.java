package com.benedetto.game_engine.battle;

import com.benedetto.game_engine.battle.dto.ChallengerDeckEntryRequest;
import com.benedetto.game_engine.SeedDeckFixtures;
import com.benedetto.game_engine.effect.CardEffect;
import com.benedetto.game_engine.battle.dto.ChallengerDeckRequest;
import com.benedetto.game_engine.battle.dto.ChallengerInfoRequest;
import com.benedetto.game_engine.battle.dto.PveBattleRequest;
import com.benedetto.game_engine.battle.dto.PvpBattleRequest;
import com.benedetto.game_engine.battle.dto.PvpBattleResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "game-engine.cli.enabled=false")
class BattleControllerTest {

    @Test
    void executeBattle_rejectsExpiredAndMalformedDeadlinesBeforeDeckMapping() throws Exception {
        mockMvc.perform(post("/api/battle/pve/stage_01").contentType(MediaType.APPLICATION_JSON)
                        .header("X-Battle-Deadline", "2000-01-01T00:00:00Z").content("{}"))
                .andExpect(status().isGatewayTimeout());
        mockMvc.perform(post("/api/battle/pve/stage_01").contentType(MediaType.APPLICATION_JSON)
                        .header("X-Battle-Deadline", "not-a-deadline").content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private BattlePreflightRejectionFilter preflightRejectionFilter;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        this.mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .addFilters(preflightRejectionFilter)
                .build();
    }

    private List<ChallengerDeckEntryRequest> buildValidEntries() {
        return SeedDeckFixtures.entries();
    }

    private PveBattleRequest buildValidRequest() {
        return PveBattleRequest.builder()
                .challenger(ChallengerInfoRequest.builder()
                        .userId("user-test-01")
                        .username("test_challenger")
                        .displayName("Commander Test")
                        .build())
                .deck(ChallengerDeckRequest.builder()
                        .entries(buildValidEntries())
                        .build())
                .build();
    }

    @Test
    void executeBattle_appliesCreatureEffectsFromTheBackendSnapshot() throws Exception {
        PveBattleRequest request = buildValidRequest();
        request.getDeck().getEntries().stream()
                .filter(entry -> "ROUND1".equals(entry.getDestination()))
                .forEach(entry -> entry.setEffects(List.of(
                        new CardEffect(9001, "ATTACK_BONUS", Map.of("amount", 1)))));
        mockMvc.perform(post("/api/battle/pve/1").contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.battleLog", hasItem(containsString("ATTACK_BONUS attack +1"))));
    }

    @Test
    void executeBattle_rejectsUnsupportedCodesAndInvalidEffectParameters() throws Exception {
        for (CardEffect effect : List.of(
                new CardEffect(1, "UNKNOWN", Map.of()),
                new CardEffect(1, "ATTACK_BONUS", Map.of("amount", "2")),
                new CardEffect(1, "ATTACK_BONUS", Map.of("amount", 1.5)))) {
            PveBattleRequest request = buildValidRequest();
            request.getDeck().getEntries().getFirst().setEffects(List.of(effect));
            mockMvc.perform(post("/api/battle/pve/1").contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().is(422))
                    .andExpect(jsonPath("$.violations", not(empty())));
        }
    }

    @Test
    void executeBattle_withValidChallengerAndDeck_returnsOkWithBattleResponse() throws Exception {
        PveBattleRequest request = buildValidRequest();

        mockMvc.perform(post("/api/battle/pve/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stageId").value("stage_01"))
                .andExpect(jsonPath("$.stageNumber").value(1))
                .andExpect(jsonPath("$.outcome").value(anyOf(is("VICTORY"), is("DEFEAT"), is("DRAW"))))
                .andExpect(jsonPath("$.tutorialHints").isArray())
                .andExpect(jsonPath("$.tutorialHints", not(empty())))
                .andExpect(jsonPath("$.battleLog").isArray())
                .andExpect(jsonPath("$.battleLog", not(empty())));
    }

    @Test
    void executeBattle_withoutOriginHeader_succeedsForBackendCaller() throws Exception {
        PveBattleRequest request = buildValidRequest();

        mockMvc.perform(post("/api/battle/pve/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stageId").value("stage_01"))
                .andExpect(jsonPath("$.stageNumber").value(1))
                .andExpect(jsonPath("$.stageTitle").isNotEmpty())
                .andExpect(jsonPath("$.challenger.userId").value("user-test-01"))
                .andExpect(jsonPath("$.challenger.username").value("test_challenger"))
                .andExpect(jsonPath("$.challenger.displayName").value("Commander Test"))
                .andExpect(jsonPath("$.enemyName").isNotEmpty())
                .andExpect(jsonPath("$.outcome").value(anyOf(is("VICTORY"), is("DEFEAT"), is("DRAW"))))
                .andExpect(jsonPath("$.winner").isNotEmpty())
                .andExpect(jsonPath("$.tutorialHints").isArray())
                .andExpect(jsonPath("$.tutorialHints", not(empty())))
                .andExpect(jsonPath("$.battleLog").isArray())
                .andExpect(jsonPath("$.battleLog", not(empty())));
    }

    @Test
    void executeBattle_withIncompleteSnapshot_returnsUnprocessableEntityWithViolations() throws Exception {
        PveBattleRequest request = PveBattleRequest.builder()
                .challenger(ChallengerInfoRequest.builder().userId("user-err").build())
                .deck(ChallengerDeckRequest.builder()
                        .entries(List.of(
                                ChallengerDeckEntryRequest.builder().cardId(999).destination("ROUND1").build()
                        ))
                        .build())
                .build();

        mockMvc.perform(post("/api/battle/pve/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().is(422))
                .andExpect(jsonPath("$.violations").isArray())
                .andExpect(jsonPath("$.violations", not(empty())));
    }

    @Test
    void executeBattle_withNonExistentStage_returnsNotFound() throws Exception {
        PveBattleRequest request = buildValidRequest();

        mockMvc.perform(post("/api/battle/pve/99")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.stageId").value("99"));
    }

    @Test
    void executeBattle_withInactiveStageEvent_returnsConflict() throws Exception {
        PveBattleRequest request = buildValidRequest();

        mockMvc.perform(post("/api/battle/pve/3")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.eventCode").value("BLOOD_MOON"));
    }

    @Test
    void preflightCorsRequest_fromFrontendOrigins_isRejectedWithoutCorsHeaders() throws Exception {
        mockMvc.perform(options("/api/battle/pve/1")
                        .header("Origin", "http://localhost:8082")
                        .header("Access-Control-Request-Method", "POST"))
                .andExpect(status().isForbidden())
                .andExpect(header().doesNotExist("Access-Control-Allow-Origin"));

        mockMvc.perform(options("/api/battle/pve/1")
                        .header("Origin", "http://localhost:5173")
                        .header("Access-Control-Request-Method", "POST"))
                .andExpect(status().isForbidden())
                .andExpect(header().doesNotExist("Access-Control-Allow-Origin"));
    }

    private PvpBattleRequest buildValidPvpRequest() {
        return PvpBattleRequest.builder()
                .playerA(ChallengerInfoRequest.builder()
                        .userId("user-pvp-a")
                        .username("player_alpha")
                        .displayName("Alpha")
                        .build())
                .deckA(ChallengerDeckRequest.builder()
                        .entries(buildValidEntries())
                        .build())
                .playerB(ChallengerInfoRequest.builder()
                        .userId("user-pvp-b")
                        .username("player_beta")
                        .displayName("Beta")
                        .build())
                .deckB(ChallengerDeckRequest.builder()
                        .entries(buildValidEntries())
                        .build())
                .build();
    }

    @Test
    void executePvpBattle_withValidRequest_returnsOkAndConsistentWinnerFields() throws Exception {
        PvpBattleRequest request = buildValidPvpRequest();

        String responseJson = mockMvc.perform(post("/api/battle/pvp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mode").value("RANKED"))
                .andExpect(jsonPath("$.outcome").value(anyOf(is("PLAYER_A_WIN"), is("PLAYER_B_WIN"), is("DRAW"))))
                .andExpect(jsonPath("$.battleLog").isArray())
                .andExpect(jsonPath("$.battleLog", not(empty())))
                .andReturn()
                .getResponse()
                .getContentAsString();

        PvpBattleResponse response = objectMapper.readValue(responseJson, PvpBattleResponse.class);
        if ("PLAYER_A_WIN".equals(response.getOutcome())) {
            org.junit.jupiter.api.Assertions.assertEquals("user-pvp-a", response.getWinnerUserId());
            org.junit.jupiter.api.Assertions.assertEquals("player_alpha", response.getWinnerUsername());
        } else if ("PLAYER_B_WIN".equals(response.getOutcome())) {
            org.junit.jupiter.api.Assertions.assertEquals("user-pvp-b", response.getWinnerUserId());
            org.junit.jupiter.api.Assertions.assertEquals("player_beta", response.getWinnerUsername());
        } else {
            org.junit.jupiter.api.Assertions.assertNull(response.getWinnerUserId());
            org.junit.jupiter.api.Assertions.assertNull(response.getWinnerUsername());
        }
    }

    @Test
    void executePvpBattle_withFriendlyMode_returnsFriendlyMode() throws Exception {
        PvpBattleRequest request = buildValidPvpRequest();
        request.setMode("FRIENDLY");

        mockMvc.perform(post("/api/battle/pvp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mode").value("FRIENDLY"))
                .andExpect(jsonPath("$.outcome").value(anyOf(is("PLAYER_A_WIN"), is("PLAYER_B_WIN"), is("DRAW"))))
                .andExpect(jsonPath("$.battleLog", not(empty())));
    }

    @Test
    void executePvpBattle_withSelfChallenge_returnsBadRequestProblemDetail() throws Exception {
        PvpBattleRequest request = buildValidPvpRequest();
        request.getPlayerB().setUserId("user-pvp-a");

        mockMvc.perform(post("/api/battle/pvp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.title").value("Bad Request"));
    }

    @Test
    void executePvpBattle_withInvalidMode_returnsBadRequestProblemDetail() throws Exception {
        PvpBattleRequest request = buildValidPvpRequest();
        request.setMode("BLITZ");

        mockMvc.perform(post("/api/battle/pvp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.title").value("Bad Request"));
    }

    @Test
    void executePvpBattle_withMissingRequiredFields_returnsBadRequestProblemDetail() throws Exception {
        mockMvc.perform(post("/api/battle/pvp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.title").value("Bad Request"));
    }

    @Test
    void executePvpBattle_withInvalidDeck_returnsUnprocessableEntityWithViolations() throws Exception {
        PvpBattleRequest request = buildValidPvpRequest();
        request.setDeckB(ChallengerDeckRequest.builder()
                .entries(List.of(ChallengerDeckEntryRequest.builder().cardId(999).destination("ROUND1").build()))
                .build());

        mockMvc.perform(post("/api/battle/pvp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().is(422))
                .andExpect(jsonPath("$.violations").isArray())
                .andExpect(jsonPath("$.violations", not(empty())));
    }

    @Test
    void executePvpBattle_withExpiredDeadline_returnsGatewayTimeout() throws Exception {
        PvpBattleRequest request = buildValidPvpRequest();

        mockMvc.perform(post("/api/battle/pvp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Battle-Deadline", "2000-01-01T00:00:00Z")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isGatewayTimeout());
    }

    @Test
    void preflightCorsRequest_pvpFromFrontendOrigins_isRejectedWithoutCorsHeaders() throws Exception {
        mockMvc.perform(options("/api/battle/pvp")
                        .header("Origin", "http://localhost:8082")
                        .header("Access-Control-Request-Method", "POST"))
                .andExpect(status().isForbidden())
                .andExpect(header().doesNotExist("Access-Control-Allow-Origin"));

        mockMvc.perform(options("/api/battle/pvp")
                        .header("Origin", "http://localhost:5173")
                        .header("Access-Control-Request-Method", "POST"))
                .andExpect(status().isForbidden())
                .andExpect(header().doesNotExist("Access-Control-Allow-Origin"));
    }
}
