package webapp.economy;

import java.time.OffsetDateTime;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** Reads F06 Scaglie holds; callers must already hold the owner's exclusive user lock. */
@Service
@Transactional(propagation = Propagation.MANDATORY)
public class ReservationReader {
    private final JdbcClient jdbc;

    public ReservationReader(JdbcClient jdbc) { this.jdbc = jdbc; }

    /** Expiration uses one database cutoff taken after the caller acquired the user lock. */
    public long reservedScaglie(int userId) {
        var cutoff = jdbc.sql("SELECT clock_timestamp()").query(OffsetDateTime.class).single();
        return reservedScaglie(userId, cutoff);
    }

    long reservedScaglie(int userId, OffsetDateTime cutoff) {
        var schema = jdbc.sql("""
                SELECT to_regclass('trade_token_reservations') IS NOT NULL AS holds,
                    to_regclass('current_negotiations') IS NOT NULL AS negotiations,
                    to_regclass('original_announces') IS NOT NULL AS announcements
                """).query((r, n) -> new Storage(r.getBoolean("holds"), r.getBoolean("negotiations"),
                        r.getBoolean("announcements"))).single();
        // No F06 migration is installed yet. Never cache this across schemas or deployments.
        if (!schema.holds() && !schema.negotiations() && !schema.announcements()) return 0;
        if (!schema.holds() || !schema.negotiations() || !schema.announcements()) {
            throw new EconomyFailure(503, "TEMPORARILY_UNAVAILABLE", "Marketplace reservation storage is incomplete.");
        }
        var holds = jdbc.sql("""
                SELECT h.token_amount,
                    COALESCE(h.token_amount > 0 AND h.offer_version > 0 AND h.offer_version = n.offer_version
                        AND h.expires_at = a.expires_at AND a.status = 'PENDING'
                        AND a.publisher_id = n.publisher_id AND n.publisher_id <> n.responder_id
                        AND a.is_private = n.is_private AND a.is_negotiable = n.is_negotiable
                        AND ((h.id_user = n.publisher_id AND n.p_acceptance
                                AND h.token_amount = n.current_p_token_offer)
                            OR (h.id_user = n.responder_id AND n.r_acceptance
                                AND h.token_amount = n.current_r_token_offer)), false) AS valid
                FROM trade_token_reservations h
                LEFT JOIN current_negotiations n ON n.id = h.negotiation_id
                LEFT JOIN original_announces a ON a.id = n.announce_id
                WHERE h.id_user = :user AND (h.expires_at > :cutoff OR a.expires_at > :cutoff)
                UNION ALL
                SELECT 0, false FROM current_negotiations n
                JOIN original_announces a ON a.id = n.announce_id
                WHERE a.expires_at > :cutoff
                    AND ((n.publisher_id = :user AND n.p_acceptance AND n.current_p_token_offer <> 0)
                        OR (n.responder_id = :user AND n.r_acceptance AND n.current_r_token_offer <> 0))
                    AND NOT EXISTS (SELECT 1 FROM trade_token_reservations h
                        WHERE h.negotiation_id = n.id AND h.id_user = :user)
                """).param("user", userId).param("cutoff", cutoff)
                .query((r, n) -> new Hold(r.getLong("token_amount"), r.getBoolean("valid"))).list();
        long total = 0;
        for (var hold : holds) {
            if (!hold.valid()) throw invalid();
            try { total = Math.addExact(total, hold.amount()); }
            catch (ArithmeticException failure) { throw invalid(); }
        }
        return total;
    }

    private static EconomyFailure invalid() {
        return new EconomyFailure(500, "RESERVATION_STATE_INVALID", "Marketplace reservations are inconsistent.");
    }

    private record Storage(boolean holds, boolean negotiations, boolean announcements) { }
    private record Hold(long amount, boolean valid) { }
}
