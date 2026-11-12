package webapp.auth;

import java.time.Clock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Reclaims session records whose credentials can no longer authenticate requests. */
@Component
class SessionTokenCleanup {
    private static final Logger log = LoggerFactory.getLogger(SessionTokenCleanup.class);

    private final AccountRepository accounts;
    private final Clock clock;

    SessionTokenCleanup(AccountRepository accounts, Clock clock) {
        this.accounts = accounts;
        this.clock = clock;
    }

    /** Deletes expired or revoked sessions using a single UTC cutoff for each run. */
    @Scheduled(cron = "${app.security.session-token-cleanup-cron:0 0 3 * * *}",
            zone = "${app.security.session-token-cleanup-zone:UTC}")
    void cleanUp() {
        int deleted = accounts.deleteExpiredOrRevokedRefreshTokens(clock.instant());
        log.info("Session token cleanup deleted {} expired or revoked sessions", deleted);
    }
}
