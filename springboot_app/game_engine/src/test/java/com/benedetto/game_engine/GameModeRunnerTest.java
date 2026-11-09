package com.benedetto.game_engine;

import com.benedetto.game_engine.campaign.service.CampaignService;
import com.benedetto.game_engine.model.Player;
import com.benedetto.game_engine.model.PlayerDeck;
import com.benedetto.game_engine.service.CardLoaderService;
import com.benedetto.game_engine.service.MatchEngine;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GameModeRunnerTest {

    private final CardLoaderService cardLoaderService = mock(CardLoaderService.class);
    private final CampaignService campaignService = mock(CampaignService.class);
    private final MatchEngine matchEngine = mock(MatchEngine.class);
    private final GameModeRunner runner = new GameModeRunner(cardLoaderService, campaignService, matchEngine);

    @Test
    void choiceOneStartsTheFirstCampaignStage() {
        when(cardLoaderService.loadDeck("mocks/deck_player_a.json")).thenReturn(mock(PlayerDeck.class));
        when(campaignService.executeStage(any(Player.class), any(String.class))).thenReturn(List.of("tutorial"));

        runner.run(reader("1"), output());

        verify(campaignService).executeStage(any(Player.class), org.mockito.ArgumentMatchers.eq(
                "campaign/stages/stage_01_fazioni.json"));
        verify(matchEngine, never()).runMatch(any(Player.class), any(Player.class));
    }

    @Test
    void choiceTwoStartsAnOnlineMatchWithTheTwoMockDecks() {
        when(cardLoaderService.loadDeck("mocks/deck_player_a.json")).thenReturn(mock(PlayerDeck.class));
        when(cardLoaderService.loadDeck("mocks/deck_player_b.json")).thenReturn(mock(PlayerDeck.class));
        when(matchEngine.runMatch(any(Player.class), any(Player.class))).thenReturn(List.of("match"));

        runner.run(reader("2"), output());

        verify(matchEngine).runMatch(any(Player.class), any(Player.class));
        verify(campaignService, never()).executeStage(any(Player.class), any(String.class));
    }

    @Test
    void invalidChoiceIsRetriedAndEndOfInputFailsExplicitly() {
        when(cardLoaderService.loadDeck("mocks/deck_player_a.json")).thenReturn(mock(PlayerDeck.class));
        when(cardLoaderService.loadDeck("mocks/deck_player_b.json")).thenReturn(mock(PlayerDeck.class));
        when(matchEngine.runMatch(any(Player.class), any(Player.class))).thenReturn(List.of("match"));

        runner.run(reader("invalid\n2"), output());
        assertThrows(IllegalStateException.class, () -> runner.run(reader(""), output()));

        verify(matchEngine).runMatch(any(Player.class), any(Player.class));
    }

    private BufferedReader reader(String input) {
        return new BufferedReader(new StringReader(input));
    }

    private PrintStream output() {
        return new PrintStream(new ByteArrayOutputStream(), true, StandardCharsets.UTF_8);
    }
}
