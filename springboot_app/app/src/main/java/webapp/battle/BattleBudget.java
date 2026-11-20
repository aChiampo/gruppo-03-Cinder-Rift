package webapp.battle;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

/** One request budget, including preparation; remote calls never receive a fresh eight seconds. */
public record BattleBudget(long startedNanos, Instant engineDeadline) {
    public static final String ATTRIBUTE = BattleBudget.class.getName();
    public static BattleBudget start(Clock clock) {
        return new BattleBudget(System.nanoTime(), clock.instant().plusSeconds(8));
    }

    public Duration remaining() {
        long nanos = Duration.ofSeconds(9).toNanos() - (System.nanoTime() - startedNanos);
        if (nanos <= 0 || Thread.currentThread().isInterrupted()) throw BattleFailure.timeout();
        return Duration.ofNanos(nanos);
    }
}
