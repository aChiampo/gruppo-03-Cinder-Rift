package com.benedetto.game_engine.campaign;

import com.benedetto.game_engine.campaign.service.CampaignService;
import com.benedetto.game_engine.campaign.service.CampaignStageLoaderService;
import com.benedetto.game_engine.model.Player;
import com.benedetto.game_engine.service.CardLoaderService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(properties = "game-engine.cli.enabled=false")
class CampaignApplicationTests {

	@Autowired
	private CampaignService campaignService;

	@Autowired
	private CampaignStageLoaderService campaignStageLoaderService;

	@Autowired
	private CardLoaderService cardLoaderService;

	@Test
	void contextLoads() {
	}

	@Test
	void loadsCampaignStageAndItsEnemyDeck() {
		assertEquals("mocks/mirror.json",
				campaignStageLoaderService.loadStage("stage_01_mirror.json").getEnemyDeckPath());
	}

	@Test
	void executesTutorialStageAndPrependsHintsToMatchLog() {
		Player player = new Player("Comandante Umano", cardLoaderService.loadDeck("mocks/mirror.json"));

		List<String> report = campaignService.executeStage(player, "stage_01_mirror.json");

		assertTrue(report.get(0).startsWith("[TUTORIAL]"));
		assertTrue(report.stream().anyMatch(line -> line.contains("INIZIO PARTITA AUTOMATICA TCG")));
	}

	@Test
	void skipsStageWhenRequiredSpecialEventIsInactive() {
		Player player = new Player("Comandante Umano", cardLoaderService.loadDeck("mocks/mirror.json"));

		List<String> report = campaignService.executeStage(player, "stage_03_demo_event.json");

		assertTrue(report.get(0).contains("BLOOD_MOON"));
		assertTrue(report.get(0).contains("non è attivo o non esiste"));
	}
}
