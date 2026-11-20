package com.benedetto.game_engine.battle;

import java.time.Instant;
import java.time.Duration;
import java.time.format.DateTimeParseException;

/** Per-execution cooperative checkpoint; never stored in shared singleton engine state. */
public final class BattleDeadline implements Runnable {
    private final Instant deadline;
    private final long endNanos;

    private BattleDeadline(Instant deadline) {
        this.deadline = deadline;
        long remaining = Math.min(Duration.ofSeconds(8).toNanos(),
                Math.max(0, Duration.between(Instant.now(), deadline).toNanos()));
        this.endNanos = System.nanoTime() + remaining;
    }

    public static BattleDeadline fromHeader(String header) {
        Instant now = Instant.now();
        Instant deadline;
        try { deadline = header == null ? now.plusSeconds(8) : Instant.parse(header); }
        catch (DateTimeParseException invalid) { throw new IllegalArgumentException("Invalid X-Battle-Deadline header"); }
        if (!deadline.isAfter(now)) throw new Exceeded();
        // A direct internal caller cannot extend the engine's eight-second execution budget.
        if (deadline.isAfter(now.plusSeconds(8))) deadline = now.plusSeconds(8);
        BattleDeadline checkpoint = new BattleDeadline(deadline);
        checkpoint.run();
        return checkpoint;
    }

    @Override
    public void run() {
        if (Thread.currentThread().isInterrupted() || !Instant.now().isBefore(deadline)
                || System.nanoTime() - endNanos >= 0) throw new Exceeded();
    }

    public static final class Exceeded extends RuntimeException {
        public Exceeded() { super("The battle execution deadline was exceeded."); }
    }
}
