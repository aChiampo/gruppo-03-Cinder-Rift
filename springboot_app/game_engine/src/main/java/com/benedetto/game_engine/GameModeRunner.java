package com.benedetto.game_engine;

import com.benedetto.game_engine.campaign.service.CampaignService;
import com.benedetto.game_engine.model.Player;
import com.benedetto.game_engine.model.PlayerDeck;
import com.benedetto.game_engine.service.CardLoaderService;
import com.benedetto.game_engine.service.MatchEngine;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.util.List;

@Component
@ConditionalOnProperty(name = "game-engine.cli.enabled", havingValue = "true", matchIfMissing = true)
public class GameModeRunner implements CommandLineRunner {

    private static final String CAMPAIGN_STAGE = "campaign/stages/stage_01_fazioni.json";

    private final CardLoaderService cardLoaderService;
    private final CampaignService campaignService;
    private final MatchEngine matchEngine;

    public GameModeRunner(CardLoaderService cardLoaderService,
                          CampaignService campaignService,
                          MatchEngine matchEngine) {
        this.cardLoaderService = cardLoaderService;
        this.campaignService = campaignService;
        this.matchEngine = matchEngine;
    }

    @Override
    public void run(String... args) {
        run(new BufferedReader(new InputStreamReader(System.in)), System.out);
    }

    void run(BufferedReader input, PrintStream output) {
        while (true) {
            output.println("Scegli la modalità di gioco:");
            output.println("1) Campagna - avvia il tutorial");
            output.println("2) Scontro online - simulazione con mazzi mock");
            output.print("> ");
            output.flush();

            String choice;
            try {
                choice = input.readLine();
            } catch (IOException e) {
                throw new IllegalStateException("Impossibile leggere la modalità di gioco.", e);
            }

            if (choice == null) {
                throw new IllegalStateException("Input terminato prima della scelta della modalità di gioco.");
            }

            switch (choice.trim()) {
                case "1" -> runCampaign(output);
                case "2" -> runOnlineMatch(output);
                default -> output.println("Scelta non valida. Inserire 1 oppure 2.");
            }
            if (choice.trim().equals("1") || choice.trim().equals("2")) {
                return;
            }
        }
    }

    private void runCampaign(PrintStream output) {
        PlayerDeck deck = cardLoaderService.loadDeck("mocks/deck_player_a.json");
        Player player = new Player("Comandante Umano", deck);
        List<String> report = campaignService.executeStage(player, CAMPAIGN_STAGE);
        report.forEach(output::println);
    }

    private void runOnlineMatch(PrintStream output) {
        PlayerDeck deckA = cardLoaderService.loadDeck("mocks/deck_player_a.json");
        PlayerDeck deckB = cardLoaderService.loadDeck("mocks/deck_player_b.json");
        Player playerA = new Player("Giocatore A", deckA);
        Player playerB = new Player("Giocatore B", deckB);
        List<String> report = matchEngine.runMatch(playerA, playerB);
        report.forEach(output::println);
    }
}
