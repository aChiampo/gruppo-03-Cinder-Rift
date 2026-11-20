package com.benedetto.game_engine.battle;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicInteger;
import com.benedetto.game_engine.SeedDeckFixtures;
import com.benedetto.game_engine.effect.EffectEngine;
import com.benedetto.game_engine.model.Player;
import com.benedetto.game_engine.service.CombatEngine;
import com.benedetto.game_engine.service.MatchEngine;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class BattleDeadlineTest {
    @Test
    void refusesAnExpiredOrMalformedDeadlineBeforeAnyWork() {
        assertThrows(BattleDeadline.Exceeded.class,
                () -> BattleDeadline.fromHeader(Instant.now().minusSeconds(1).toString()));
        assertThrows(IllegalArgumentException.class, () -> BattleDeadline.fromHeader("invalid"));
        assertDoesNotThrow(() -> BattleDeadline.fromHeader(null).run());
    }

    @Test
    void honoursWorkerInterruption() {
        BattleDeadline deadline = BattleDeadline.fromHeader(null);
        Thread.currentThread().interrupt();
        try { assertThrows(BattleDeadline.Exceeded.class, deadline::run); }
        finally { Thread.interrupted(); }
    }

    @Test
    void stopsInsideAnExchangeRatherThanReturningAPartialResult() {
        MatchEngine match = new MatchEngine(new CombatEngine(new EffectEngine()));
        Player one = new Player("one", SeedDeckFixtures.load("mirror"));
        Player two = new Player("two", SeedDeckFixtures.load("mirror"));
        AtomicInteger checkpoints = new AtomicInteger();
        assertThrows(BattleDeadline.Exceeded.class, () -> match.runMatch(one, two, () -> {
            if (checkpoints.incrementAndGet() == 5) throw new BattleDeadline.Exceeded();
        }));
        assertEquals(5, checkpoints.get());
        assertEquals(0, one.getRoundsWon());
        assertEquals(0, two.getRoundsWon());
    }
}
