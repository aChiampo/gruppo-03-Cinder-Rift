package com.benedetto.game_engine.battle;

import com.benedetto.game_engine.battle.dto.ChallengerDeckEntryRequest;
import com.benedetto.game_engine.battle.dto.ChallengerDeckRequest;
import com.benedetto.game_engine.battle.dto.ChallengerInfoRequest;
import com.benedetto.game_engine.battle.dto.PveBattleRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "game-engine.cli.enabled=false")
class BattleControllerTest {

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
        List<ChallengerDeckEntryRequest> entries = new ArrayList<>();

        // 5 creatures for each of ROUND1..ROUND5 from mocks/pool_sapiens.json
        for (int round = 1; round <= 5; round++) {
            for (int cardIdx = 1; cardIdx <= 5; cardIdx++) {
                String cardId = "SAP_R" + round + "_" + cardIdx;
                entries.add(new ChallengerDeckEntryRequest(cardId, 1, "ROUND" + round));
            }
        }

        // 5 equipment cards from mocks/pool_equipments.json
        for (int eqIdx = 1; eqIdx <= 5; eqIdx++) {
            entries.add(new ChallengerDeckEntryRequest("EQ_" + eqIdx, 1, "EQUIPMENT"));
        }

        return entries;
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
    void executeBattle_withIncompleteDeckOrNonExistentCard_returnsUnprocessableEntityWithViolations() throws Exception {
        PveBattleRequest request = PveBattleRequest.builder()
                .challenger(ChallengerInfoRequest.builder().userId("user-err").build())
                .deck(ChallengerDeckRequest.builder()
                        .entries(List.of(
                                new ChallengerDeckEntryRequest("NON_EXISTENT_CARD_999", 1, "ROUND1")
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
}
