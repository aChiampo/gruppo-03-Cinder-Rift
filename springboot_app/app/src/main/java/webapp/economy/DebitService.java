package webapp.economy;

import java.util.Map;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** Shared ordinary spending: debits and the feature's credits must commit or roll back together. */
@Service
public class DebitService {
    private final JdbcClient jdbc;
    private final ReservationReader reservations;

    public DebitService(JdbcClient jdbc, ReservationReader reservations) {
        this.jdbc = jdbc;
        this.reservations = reservations;
    }

    /**
     * Spends only unreserved Scaglie, including a zero-cost operation.
     * Callers authorize the feature; multi-user operations must first lock all users in ID order.
     * F06 settlement must consume its own holds in the same transaction before using ordinary spending.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public DebitResult debitScaglie(int userId, long amount) {
        if (amount < 0) throw new IllegalArgumentException("A debit cannot be negative");
        var active = jdbc.sql("SELECT id FROM users WHERE id = :user AND NOT is_deleted FOR UPDATE NOWAIT")
                .param("user", userId).query(Integer.class).optional();
        if (active.isEmpty()) throw new EconomyFailure(403, "FORBIDDEN", "The account cannot spend resources.");
        var balance = jdbc.sql("SELECT id, cobalt_shards FROM inventory WHERE id_user = :user FOR UPDATE NOWAIT")
                .param("user", userId).query((r, n) -> new Balance(r.getInt("id"), r.getInt("cobalt_shards")))
                .optional().orElseThrow(() -> new EconomyFailure(500, "INTERNAL_ERROR", "The user's inventory is unavailable."));
        long reserved = reservations.reservedScaglie(userId);
        if (balance.scaglie() < 0 || reserved > balance.scaglie()) {
            throw new EconomyFailure(500, "RESERVATION_STATE_INVALID", "Resource balances do not cover their reservations.");
        }
        long available = balance.scaglie() - reserved;
        if (amount > available) throw new EconomyFailure(409, "INSUFFICIENT_SCAGLIE",
                reserved > 0 ? "Not enough available Scaglie. Marketplace reservations cannot be spent."
                        : "Not enough Scaglie.",
                Map.of("requiredScaglie", amount, "cobaltShards", balance.scaglie(),
                        "reservedCobaltShards", reserved, "availableCobaltShards", available));
        int remaining = Math.toIntExact(balance.scaglie() - amount);
        int changed = jdbc.sql("UPDATE inventory SET cobalt_shards = :remaining WHERE id = :inventory")
                .param("remaining", remaining).param("inventory", balance.id()).update();
        if (changed != 1) throw new IllegalStateException("Locked inventory disappeared");
        return new DebitResult(balance.id(), remaining, reserved, remaining - reserved);
    }

    public record DebitResult(int inventoryId, int cobaltShards, long reservedCobaltShards, long availableCobaltShards) { }
    private record Balance(int id, int scaglie) { }
}
