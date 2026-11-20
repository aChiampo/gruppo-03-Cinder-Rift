package webapp.battle;

import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.async.DeferredResult;
import webapp.auth.UserProfile;

/** Bounds preparation and the remote call together, and cleans up the HTTP-owned guard. */
@Service
public class BattleService {
    private static final Logger LOG = LoggerFactory.getLogger(BattleService.class);
    private final PostgresBattleGuard guards;
    private final BattleSnapshotService snapshots;
    private final BattleEngineClient engine;
    private final ExecutorService workers = Executors.newVirtualThreadPerTaskExecutor();
    private final ScheduledExecutorService timer = Executors.newSingleThreadScheduledExecutor(
            Thread.ofPlatform().daemon().name("battle-deadline").factory());

    public BattleService(PostgresBattleGuard guards, BattleSnapshotService snapshots, BattleEngineClient engine) {
        this.guards = guards;
        this.snapshots = snapshots;
        this.engine = engine;
    }

    public DeferredResult<Object> execute(UserProfile user, int deckId, String stageId, String path, BattleBudget budget) {
        var result = new DeferredResult<Object>(Math.max(1, budget.remaining().toMillis()));
        var execution = new Execution();
        Runnable timeout = () -> {
            result.setResult(BattleFailure.timeout().response(path));
            execution.close();
        };
        result.onTimeout(timeout);
        result.onError(ignored -> execution.close());
        result.onCompletion(execution::close);
        execution.alarm = timer.schedule(timeout, budget.remaining().toNanos(), TimeUnit.NANOSECONDS);
        execution.worker = workers.submit(() -> {
            try {
                PostgresBattleGuard.Handle guard = guards.acquire(user.id(), budget);
                if (!execution.attach(guard)) return;
                EngineBattleRequest payload = snapshots.prepare(user, deckId, budget);
                budget.remaining();
                BattleResponse response = engine.execute(stageId, payload, budget);
                budget.remaining();
                result.setResult(response);
            } catch (BattleFailure expected) {
                result.setResult(expected.response(path));
            } catch (Exception unexpected) {
                // Do not expose SQL errors, engine bodies, or profile data to the caller.
                LOG.error("Battle request failed ({})", unexpected.getClass().getSimpleName());
                BattleFailure failure;
                try { budget.remaining(); failure = new BattleFailure(HttpStatus.INTERNAL_SERVER_ERROR,
                        "Internal Server Error", "The battle request could not be completed."); }
                catch (BattleFailure expired) { failure = expired; }
                result.setResult(failure.response(path));
            }
        });
        return result;
    }

    @PreDestroy
    public void stop() {
        timer.shutdownNow();
        workers.shutdownNow();
    }

    private static final class Execution {
        private boolean closed;
        private PostgresBattleGuard.Handle guard;
        private volatile Future<?> worker;
        private volatile ScheduledFuture<?> alarm;

        synchronized boolean attach(PostgresBattleGuard.Handle acquired) {
            if (closed) { acquired.close(); return false; }
            guard = acquired;
            return true;
        }

        synchronized void close() {
            if (closed) return;
            closed = true;
            if (alarm != null) alarm.cancel(false);
            if (worker != null) worker.cancel(true);
            if (guard != null) guard.close();
        }
    }
}
