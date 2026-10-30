package com.benedetto.game_engine;

import com.benedetto.game_engine.model.*;
import com.benedetto.game_engine.service.CardLoaderService;
import com.benedetto.game_engine.service.MatchEngine;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.List;

@SpringBootApplication
public class GameEngineApplication implements CommandLineRunner {

	private final CardLoaderService cardLoaderService;
	private final MatchEngine matchEngine;

	public GameEngineApplication(CardLoaderService cardLoaderService, MatchEngine matchEngine) {
		this.cardLoaderService = cardLoaderService;
		this.matchEngine = matchEngine;
	}

	public static void main(String[] args) {
		SpringApplication.run(GameEngineApplication.class, args);
	}

	@Override
	public void run(String... args) {
		PlayerDeck deckA = cardLoaderService.loadDeck("mocks/deck_player_a.json");
		PlayerDeck deckB = cardLoaderService.loadDeck("mocks/deck_player_b.json");

		Player p1 = new Player("Giocatore A", deckA);
		Player p2 = new Player("Giocatore B", deckB);

		List<String> report = matchEngine.runMatch(p1, p2);
		report.forEach(System.out::println);
	}
}