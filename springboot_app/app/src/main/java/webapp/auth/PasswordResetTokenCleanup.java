package webapp.auth;

import java.time.Clock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Removes expired or consumed recovery records independently of session cleanup. */
@Component
class PasswordResetTokenCleanup {
    private static final Logger log = LoggerFactory.getLogger(PasswordResetTokenCleanup.class);
    private final AccountRepository accounts;
    private final Clock clock;

    PasswordResetTokenCleanup(AccountRepository accounts, Clock clock) {
        this.accounts = accounts;
        this.clock = clock;
    }

    @Scheduled(cron = "${app.security.password-reset-token-cleanup-cron:0 0 3 * * *}",
            zone = "${app.security.password-reset-token-cleanup-zone:UTC}")
    void cleanUp() {
        int deleted = accounts.deleteExpiredOrUsedPasswordResetTokens(clock.instant());
        log.info("Password reset cleanup deleted {} expired or used tokens", deleted);
    }
}
