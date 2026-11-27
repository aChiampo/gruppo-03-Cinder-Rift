package webapp.pack;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/** Definition guards and additive unopened-pack credits participate in the caller's transaction. */
@Repository
public class PackPurchaseRepository {
    private final JdbcClient jdbc;

    public PackPurchaseRepository(JdbcClient jdbc) { this.jdbc = jdbc; }

    Offer lockOffer(int packId) {
        return jdbc.sql("SELECT pack_price, is_acquirable FROM packs WHERE id = :pack FOR SHARE NOWAIT")
                .param("pack", packId).query((r, n) -> new Offer(r.getInt("pack_price"), r.getBoolean("is_acquirable")))
                .optional().orElseThrow(() -> new PackOpeningFailure(404, "PACK_NOT_FOUND", "Pack not found."));
    }

    void lockBuyer(int userId) {
        var buyer = jdbc.sql("""
                SELECT u.id FROM users u JOIN roles r ON r.id = u.id_role
                WHERE u.id = :user AND NOT u.is_deleted AND r.code = 'USER' FOR UPDATE OF u NOWAIT
                """).param("user", userId).query(Integer.class).optional();
        if (buyer.isEmpty()) throw new PackOpeningFailure(403, "FORBIDDEN", "The account cannot purchase packs.");
    }

    int credit(int inventoryId, int packId, int quantity) {
        // Openings lock this row before the definition/user: abort instead of waiting in reverse order.
        jdbc.sql("SELECT item_quantity FROM inventory_packs WHERE id_inventory = :inventory AND id_pack = :pack FOR UPDATE NOWAIT")
                .param("inventory", inventoryId).param("pack", packId).query(Integer.class).optional();
        return jdbc.sql("""
                INSERT INTO inventory_packs (id_inventory, id_pack, item_quantity) VALUES (:inventory, :pack, :quantity)
                ON CONFLICT (id_inventory, id_pack) DO UPDATE
                SET item_quantity = inventory_packs.item_quantity + EXCLUDED.item_quantity
                WHERE inventory_packs.item_quantity <= 2147483647 - EXCLUDED.item_quantity
                RETURNING item_quantity
                """).param("inventory", inventoryId).param("pack", packId).param("quantity", quantity)
                .query(Integer.class).optional().orElseThrow(() -> new PackOpeningFailure(409,
                        "PACK_QUANTITY_LIMIT_EXCEEDED", "The resulting owned pack quantity exceeds its supported range."));
    }

    record Offer(int price, boolean acquirable) { }
}
