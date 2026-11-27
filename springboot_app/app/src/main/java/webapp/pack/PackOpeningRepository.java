package webapp.pack;

import java.sql.SQLException;
import java.util.Optional;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/** Ownership, collection increments and mercy counters participate in the batch transaction. */
@Repository
public class PackOpeningRepository {
    private final JdbcClient jdbc;

    public PackOpeningRepository(JdbcClient jdbc) { this.jdbc = jdbc; }

    public InventoryView inventory(int userId) {
        return jdbc.sql("SELECT cobalt_shards, rift_cores, scraps FROM inventory WHERE id_user = :user")
                .param("user", userId).query((r, n) -> new InventoryView(r.getInt("cobalt_shards"),
                        r.getInt("rift_cores"), r.getInt("scraps"))).optional()
                .orElseThrow(() -> new PackOpeningFailure(500, "INTERNAL_ERROR", "The user's inventory is unavailable."));
    }

    Optional<OwnedQuantity> lockOwned(int userId, int packId) {
        try {
            return jdbc.sql("""
                    SELECT ip.id_inventory, ip.item_quantity FROM inventory_packs ip
                    JOIN inventory i ON i.id = ip.id_inventory
                    WHERE i.id_user = :user AND ip.id_pack = :pack FOR UPDATE OF ip NOWAIT
                    """).param("user", userId).param("pack", packId)
                    .query((r, n) -> new OwnedQuantity(r.getInt("id_inventory"), r.getInt("item_quantity"))).optional();
        } catch (DataAccessException failure) {
            if (lockUnavailable(failure)) throw new PackOpeningFailure(409, "OPENING_IN_PROGRESS",
                    "An opening of this pack is already in progress.");
            throw failure;
        }
    }

    boolean lockPack(int packId) {
        try {
            // Admin PUT/DELETE lock the definition first: never wait in the reverse order.
            return jdbc.sql("SELECT id FROM packs WHERE id = :id FOR SHARE NOWAIT")
                    .param("id", packId).query(Integer.class).optional().isPresent();
        } catch (DataAccessException failure) {
            if (lockUnavailable(failure)) throw new PackOpeningFailure(503, "TEMPORARILY_UNAVAILABLE",
                    "The pack configuration is being updated. Try again later.");
            throw failure;
        }
    }

    void lockActiveUser(int userId) {
        var active = jdbc.sql("""
                SELECT u.id FROM users u JOIN roles r ON r.id = u.id_role
                WHERE u.id = :id AND NOT u.is_deleted AND r.code = 'USER' FOR SHARE OF u
                """).param("id", userId).query(Integer.class).optional();
        if (active.isEmpty()) throw new PackOpeningFailure(403, "FORBIDDEN", "The account cannot open packs.");
    }

    boolean packExists(int packId) {
        return jdbc.sql("SELECT EXISTS (SELECT 1 FROM packs WHERE id = :id)")
                .param("id", packId).query(Boolean.class).single();
    }

    Optional<Integer> mercyCounter(int userId, int packId) {
        return jdbc.sql("SELECT mercy_counter FROM user_mercy WHERE id_user = :user AND id_pack = :pack")
                .param("user", userId).param("pack", packId).query(Integer.class).optional();
    }

    void saveMercy(int userId, int packId, int counter) {
        jdbc.sql("""
                INSERT INTO user_mercy (id_user, id_pack, mercy_counter) VALUES (:user, :pack, :counter)
                ON CONFLICT (id_user, id_pack) DO UPDATE SET mercy_counter = EXCLUDED.mercy_counter
                """).param("user", userId).param("pack", packId).param("counter", counter).update();
    }

    void grant(int userId, int cardId, int artId, int quantity) {
        int changed = jdbc.sql("""
                INSERT INTO collections (id_user, id_card, id_card_art, owned_replica, tradable_replica)
                VALUES (:user, :card, :art, :quantity, 0)
                ON CONFLICT (id_user, id_card, id_card_art) DO UPDATE
                SET owned_replica = collections.owned_replica + EXCLUDED.owned_replica
                WHERE collections.owned_replica <= 2147483647 - EXCLUDED.owned_replica
                """).param("user", userId).param("card", cardId).param("art", artId).param("quantity", quantity).update();
        if (changed != 1) throw new PackOpeningFailure(500, "INTERNAL_ERROR", "The collection quantity exceeds its supported range.");
    }

    void consume(int inventoryId, int packId, int remaining) {
        int changed = remaining == 0
                ? jdbc.sql("DELETE FROM inventory_packs WHERE id_inventory = :inventory AND id_pack = :pack")
                        .param("inventory", inventoryId).param("pack", packId).update()
                : jdbc.sql("UPDATE inventory_packs SET item_quantity = :remaining WHERE id_inventory = :inventory AND id_pack = :pack")
                        .param("remaining", remaining).param("inventory", inventoryId).param("pack", packId).update();
        if (changed != 1) throw new IllegalStateException("Locked pack ownership disappeared");
    }

    private static boolean lockUnavailable(Throwable error) {
        for (Throwable cause = error; cause != null; cause = cause.getCause()) {
            if (cause instanceof SQLException sql && "55P03".equals(sql.getSQLState())) return true;
        }
        return false;
    }

    record OwnedQuantity(int inventoryId, int quantity) { }
}
