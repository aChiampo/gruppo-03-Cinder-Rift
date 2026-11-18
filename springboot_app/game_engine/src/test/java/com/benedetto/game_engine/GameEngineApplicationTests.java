package com.benedetto.game_engine;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.ApplicationContext;

@SpringBootTest(properties = "game-engine.cli.enabled=false")
class GameEngineApplicationTests {

	@Autowired
	private ApplicationContext applicationContext;

	@Test
	void contextLoads() {
	}

	@Test
	void commandLineSelectionCanBeDisabled() {
		Assertions.assertTrue(applicationContext.getBeansOfType(CommandLineRunner.class).isEmpty());
	}

}
