package com.benedetto.game_campaign;

import com.benedetto.game_campaign.service.CampaignService;
import com.benedetto.game_engine.model.Player;
import com.benedetto.game_engine.model.PlayerDeck;
import com.benedetto.game_engine.service.CardLoaderService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

import java.util.List;

@SpringBootApplication
@ComponentScan(basePackages = "com.benedetto")
public class GameCampaignApplication implements CommandLineRunner {

	private final CardLoaderService cardLoaderService;
	private final CampaignService campaignService;

	public GameCampaignApplication(CardLoaderService cardLoaderService, CampaignService campaignService) {
		this.cardLoaderService = cardLoaderService;
		this.campaignService = campaignService;
	}

	public static void main(String[] args) {
		SpringApplication.run(GameCampaignApplication.class, args);
	}

	@Override
	public void run(String... args) {
		try {
			PlayerDeck humanDeck = cardLoaderService.loadDeck("mocks/deck_player_a.json");
			Player humanPlayer = new Player("Comandante Umano", humanDeck);

			List<String> report = campaignService.executeStage(humanPlayer, "campaign/stages/stage_01_fazioni.json");
			report.forEach(System.out::println);
		} catch (Exception e) {
			System.err.println("Impossibile avviare lo stage della campagna: " + e.getMessage());
		}
	}
}
