package webapp.auth;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import webapp.friendship.FriendshipFailure;
import webapp.friendship.FriendshipService;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Uses independent committed transactions against the dedicated verification database. */
@SpringBootTest(properties = {"app.security.session-token-cleanup-cron=-", "app.security.password-reset-token-cleanup-cron=-"})
class FriendshipConcurrencyDatabaseTests {
    @Autowired AuthenticationService authentication;
    @Autowired FriendshipService friendships;
    @Autowired JdbcClient jdbc;
    @Autowired PlatformTransactionManager transactions;

    @Test
    void simultaneousInverseRequestsCreateExactlyOnePendingPair() throws Exception {
        var a = user(); var b = user();
        var start = new CountDownLatch(1);
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            var first = executor.submit(() -> attempt(start, () -> friendships.send(a.id(), b.id())));
            var second = executor.submit(() -> attempt(start, () -> friendships.send(b.id(), a.id())));
            start.countDown();
            assertThat(List.of(first.get(10, TimeUnit.SECONDS), second.get(10, TimeUnit.SECONDS)))
                    .containsExactlyInAnyOrder("OK", "INCOMING_REQUEST_EXISTS");
        }
        assertThat(pairCount(a.id(), b.id())).isEqualTo(1);
        assertThat(friendships.get(a.id(), b.id()).acceptedAt()).isNull();
    }

    @Test
    void simultaneousDuplicateRequestsDoNotChangeOriginalRequest() throws Exception {
        var a = user(); var b = user();
        var start = new CountDownLatch(1);
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            var first = executor.submit(() -> attempt(start, () -> friendships.send(a.id(), b.id())));
            var second = executor.submit(() -> attempt(start, () -> friendships.send(a.id(), b.id())));
            start.countDown();
            assertThat(List.of(first.get(10, TimeUnit.SECONDS), second.get(10, TimeUnit.SECONDS)))
                    .containsExactlyInAnyOrder("OK", "FRIEND_REQUEST_ALREADY_EXISTS");
        }
        assertThat(pairCount(a.id(), b.id())).isEqualTo(1);
    }

    @Test
    void acceptRacingCancellationHasOneWinnerAndAContractError() throws Exception {
        var a = user(); var b = user();
        friendships.send(a.id(), b.id());
        var start = new CountDownLatch(1);
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            var accepting = executor.submit(() -> attempt(start, () -> friendships.accept(b.id(), a.id())));
            var cancelling = executor.submit(() -> attempt(start, () -> friendships.delete(a.id(), b.id(), false)));
            start.countDown();
            var results = List.of(accepting.get(10, TimeUnit.SECONDS), cancelling.get(10, TimeUnit.SECONDS));
            assertThat(results).containsOnlyOnce("OK");
            assertThat(results).allMatch(value -> List.of("OK", "FRIEND_REQUEST_NOT_FOUND", "FRIEND_REQUEST_NOT_PENDING").contains(value));
        }
    }

    @Test
    void sendWaitingBehindDeletionRechecksDeletedTarget() throws Exception {
        var a = user(); var b = user();
        var locked = new CountDownLatch(1);
        var release = new CountDownLatch(1);
        var attempting = new CountDownLatch(1);
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            var deleting = executor.submit(() -> new TransactionTemplate(transactions).executeWithoutResult(tx -> {
                authentication.deleteAccount(b, b.id());
                locked.countDown();
                await(release);
            }));
            try {
                assertThat(locked.await(10, TimeUnit.SECONDS)).isTrue();
                var sending = executor.submit(() -> {
                    attempting.countDown();
                    try { friendships.send(a.id(), b.id()); return "OK"; }
                    catch (FriendshipFailure failure) { return failure.code(); }
                });
                assertThat(attempting.await(10, TimeUnit.SECONDS)).isTrue();
                assertThatThrownBy(() -> sending.get(200, TimeUnit.MILLISECONDS)).isInstanceOf(TimeoutException.class);
                release.countDown();
                deleting.get(10, TimeUnit.SECONDS);
                assertThat(sending.get(10, TimeUnit.SECONDS)).isEqualTo("USER_NOT_FOUND");
                assertThat(pairCount(a.id(), b.id())).isZero();
            } finally { release.countDown(); }
        }
    }

    @Test
    void deletionWaitingBehindSendCleansTheCommittedPair() throws Exception {
        var a = user(); var b = user();
        var locked = new CountDownLatch(1);
        var release = new CountDownLatch(1);
        var attempting = new CountDownLatch(1);
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            var sending = executor.submit(() -> new TransactionTemplate(transactions).executeWithoutResult(tx -> {
                friendships.send(a.id(), b.id());
                locked.countDown();
                await(release);
            }));
            try {
                assertThat(locked.await(10, TimeUnit.SECONDS)).isTrue();
                var deleting = executor.submit(() -> {
                    attempting.countDown();
                    authentication.deleteAccount(b, b.id());
                });
                assertThat(attempting.await(10, TimeUnit.SECONDS)).isTrue();
                assertThatThrownBy(() -> deleting.get(200, TimeUnit.MILLISECONDS)).isInstanceOf(TimeoutException.class);
                release.countDown();
                sending.get(10, TimeUnit.SECONDS);
                deleting.get(10, TimeUnit.SECONDS);
                assertThat(pairCount(a.id(), b.id())).isZero();
            } finally { release.countDown(); }
        }
    }

    private String attempt(CountDownLatch start, Runnable action) {
        await(start);
        try { action.run(); return "OK"; }
        catch (FriendshipFailure failure) { return failure.code(); }
    }

    private static void await(CountDownLatch latch) {
        try { assertThat(latch.await(10, TimeUnit.SECONDS)).isTrue(); }
        catch (InterruptedException exception) { Thread.currentThread().interrupt(); throw new AssertionError(exception); }
    }

    private int pairCount(int a, int b) {
        return jdbc.sql("SELECT count(*) FROM friendships WHERE user_a_id = :a AND user_b_id = :b")
                .param("a", Math.min(a, b)).param("b", Math.max(a, b)).query(Integer.class).single();
    }

    private UserProfile user() {
        String suffix = UUID.randomUUID().toString();
        return authentication.register("F13-" + suffix, suffix + "@example.invalid", "test password").user();
    }
}
