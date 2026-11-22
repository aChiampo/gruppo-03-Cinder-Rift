package com.benedetto.game_engine.campaign;

import com.benedetto.game_engine.SeedDeckFixtures;
import com.benedetto.game_engine.battle.ChallengerDeckMapperService;
import com.benedetto.game_engine.battle.dto.ChallengerDeckRequest;
import com.benedetto.game_engine.battle.dto.PveBattleRequest;
import com.benedetto.game_engine.battle.service.PveBattleService;
import com.benedetto.game_engine.campaign.repository.SpecialEventRepository;
import com.benedetto.game_engine.campaign.service.CampaignService;
import com.benedetto.game_engine.campaign.service.CampaignStageLoaderService;
import com.benedetto.game_engine.effect.EffectEngine;
import com.benedetto.game_engine.model.Player;
import com.benedetto.game_engine.service.CardLoaderService;
import com.benedetto.game_engine.service.MatchEngine;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.core.io.DefaultResourceLoader;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TutorialShufflingTest {
    private final MatchEngine matchEngine = mock(MatchEngine.class);
    private final CardLoaderService cards = new CardLoaderService(new DefaultResourceLoader());
    private final CampaignStageLoaderService stages = new CampaignStageLoaderService(new DefaultResourceLoader());
    private final SpecialEventRepository events = mock(SpecialEventRepository.class);

    @Test
    void pveTutorialKeepsShufflingEnabledForBothPlayers() {
        when(matchEngine.runMatch(any(), any(), any())).thenReturn(List.of());
        var service = new PveBattleService(new ChallengerDeckMapperService(new EffectEngine()),
                stages, events, cards, matchEngine);
        var request = PveBattleRequest.builder().stageId("stage_01")
                .deck(ChallengerDeckRequest.builder().entries(SeedDeckFixtures.entries()).build()).build();

        service.executeBattle(request);

        var challenger = ArgumentCaptor.forClass(Player.class);
        var enemy = ArgumentCaptor.forClass(Player.class);
        verify(matchEngine).runMatch(challenger.capture(), enemy.capture(), any());
        assertFalse(challenger.getValue().getDeck().isDeterministic());
        assertFalse(enemy.getValue().getDeck().isDeterministic());
    }

    @Test
    void cliTutorialKeepsShufflingEnabledForBothPlayers() {
        when(matchEngine.runMatch(any(), any())).thenReturn(List.of());
        var service = new CampaignService(matchEngine, cards, stages, events);
        var player = new Player("Commander", cards.loadDeck("mocks/mirror.json"));

        service.executeStage(player, "stage_01_mirror.json");

        var challenger = ArgumentCaptor.forClass(Player.class);
        var enemy = ArgumentCaptor.forClass(Player.class);
        verify(matchEngine).runMatch(challenger.capture(), enemy.capture());
        assertFalse(challenger.getValue().getDeck().isDeterministic());
        assertFalse(enemy.getValue().getDeck().isDeterministic());
    }
}
