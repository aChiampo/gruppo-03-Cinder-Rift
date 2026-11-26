package webapp.pack;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import tools.jackson.databind.json.JsonMapper;

/** Persists package configuration and reconciles dependent player resources. */
@Repository
public class PackRepository {
    private static final String SELECT_PACK = """
            SELECT p.*, a.standard, a.extended_art, a.full_art, a.alternate_art,
                COALESCE((SELECT jsonb_agg(s.id_slot_pull_type ORDER BY s.slot_position)
                    FROM pack_slots s WHERE s.id_pack = p.id), '[]'::jsonb) AS slot_type_ids,
                COALESCE((SELECT jsonb_agg(jsonb_build_object(
                    'id', t.id, 'description', t.description, 'pull_rates',
                    (SELECT jsonb_agg(jsonb_build_object('id_rarity', r.id_rarity,
                        'rarity_code', c.code, 'probability', r.probability) ORDER BY r.id_rarity)
                     FROM type_pull_rate r JOIN rarities c ON c.id = r.id_rarity
                     WHERE r.id_slot_pull_type = t.id)) ORDER BY t.id)
                    FROM slot_pull_type t WHERE t.id IN
                        (SELECT s.id_slot_pull_type FROM pack_slots s WHERE s.id_pack = p.id)),
                    '[]'::jsonb) AS slot_pull_types
            FROM packs p JOIN artwork_pull_rates a ON a.id = p.id_artwork_pull_rates
            """;
    private final JdbcClient jdbc;
    private final JsonMapper json;

    public PackRepository(JdbcClient jdbc, JsonMapper json) {
        this.jdbc = jdbc;
        this.json = json;
    }

    public List<PackView> findAll(boolean includeUnavailable) {
        return jdbc.sql(SELECT_PACK + " WHERE (:includeUnavailable OR p.is_acquirable) ORDER BY p.id")
                .param("includeUnavailable", includeUnavailable).query(this::map).list();
    }

    public Optional<PackView> findById(int id) {
        return jdbc.sql(SELECT_PACK + " WHERE p.id = :id")
                .param("id", id).query(this::map).optional();
    }

    public boolean lock(int id) {
        return jdbc.sql("SELECT id FROM packs WHERE id = :id FOR UPDATE")
                .param("id", id).query(Integer.class).optional().isPresent();
    }

    public int create(PackWriteRequest pack) {
        int id = parameters(jdbc.sql("""
                INSERT INTO packs (id_artwork_pull_rates, pack_name, pack_description, pack_price, is_acquirable, pack_image_url,
                    allowed_factions, allowed_types, allowed_expansions, allowed_rarities,
                    mercy_threshold)
                VALUES (:artworkRates, :name, CAST(:description AS jsonb), :price, :acquirable, :image,
                    CAST(:factions AS jsonb), CAST(:types AS jsonb), CAST(:expansions AS jsonb),
                    CAST(:rarities AS jsonb), :mercy)
                RETURNING id
                """), pack).query(Integer.class).single();
        insertSlots(id, pack.packSlots());
        return id;
    }

    public void update(int id, PackWriteRequest pack) {
        parameters(jdbc.sql("""
                UPDATE packs SET id_artwork_pull_rates = :artworkRates,
                    pack_name = :name, pack_description = CAST(:description AS jsonb),
                    pack_price = :price, is_acquirable = :acquirable, pack_image_url = :image,
                    allowed_factions = CAST(:factions AS jsonb), allowed_types = CAST(:types AS jsonb),
                    allowed_expansions = CAST(:expansions AS jsonb),
                    allowed_rarities = CAST(:rarities AS jsonb), mercy_threshold = :mercy
                WHERE id = :id
                """), pack).param("id", id).update();
        jdbc.sql("DELETE FROM pack_slots WHERE id_pack = :id").param("id", id).update();
        insertSlots(id, pack.packSlots());
    }

    private void insertSlots(int id, List<Integer> typeIds) {
        for (int index = 0; index < typeIds.size(); index++) {
            jdbc.sql("INSERT INTO pack_slots (id_pack, slot_position, id_slot_pull_type) VALUES (:id, :position, :type)")
                    .param("id", id).param("position", index + 1).param("type", typeIds.get(index)).update();
        }
    }

    public void refundAndRemoveOwnership(int id) {
        // Lock balances in ID order, including when different packs share an owner.
        jdbc.sql("""
                SELECT i.id FROM inventory i
                JOIN inventory_packs ip ON ip.id_inventory = i.id
                WHERE ip.id_pack = :id ORDER BY i.id FOR UPDATE OF i
                """).param("id", id).query(Integer.class).list();
        jdbc.sql("""
                UPDATE inventory i
                SET cobalt_shards = i.cobalt_shards::bigint + ip.item_quantity::bigint * p.pack_price
                FROM inventory_packs ip JOIN packs p ON p.id = ip.id_pack
                WHERE ip.id_inventory = i.id AND ip.id_pack = :id
                """).param("id", id).update();
        jdbc.sql("DELETE FROM inventory_packs WHERE id_pack = :id").param("id", id).update();
    }

    public void clearMercy(int id) {
        jdbc.sql("DELETE FROM user_mercy WHERE id_pack = :id").param("id", id).update();
    }

    public void delete(int id) {
        jdbc.sql("DELETE FROM packs WHERE id = :id").param("id", id).update();
    }

    public boolean referenceExists(String table, String code) {
        String sql = switch (table) {
            case "factions", "card_types", "expansions", "rarities" ->
                "SELECT EXISTS (SELECT 1 FROM " + table + " WHERE code = :code AND NOT is_deleted)";
            default -> throw new IllegalArgumentException("Unsupported catalog reference");
        };
        return jdbc.sql(sql).param("code", code).query(Boolean.class).single();
    }

    public boolean artworkPullRatesExist(int id) {
        return jdbc.sql("SELECT EXISTS (SELECT 1 FROM artwork_pull_rates WHERE id = :id)")
                .param("id", id).query(Boolean.class).single();
    }

    public boolean lockValidSlotPullType(int id) {
        if (jdbc.sql("SELECT id FROM slot_pull_type WHERE id = :id FOR SHARE")
                .param("id", id).query(Integer.class).optional().isEmpty()) return false;
        return jdbc.sql("""
                SELECT COALESCE(sum(r.probability), 0) = 100.0000
                    AND COALESCE(bool_and(NOT c.is_deleted), false)
                FROM type_pull_rate r JOIN rarities c ON c.id = r.id_rarity
                WHERE r.id_slot_pull_type = :id
                """).param("id", id).query(Boolean.class).single();
    }

    private JdbcClient.StatementSpec parameters(JdbcClient.StatementSpec sql, PackWriteRequest pack) {
        return sql.param("artworkRates", pack.artworkPullRatesId())
                .param("name", pack.name()).param("description", json.writeValueAsString(pack.description()))
                .param("price", pack.price()).param("acquirable", pack.acquirable()).param("image", pack.imageUrl())
                .param("factions", json.writeValueAsString(pack.allowedFactions()))
                .param("types", json.writeValueAsString(pack.allowedTypes()))
                .param("expansions", json.writeValueAsString(pack.allowedExpansions()))
                .param("rarities", json.writeValueAsString(pack.allowedRarities()))
                .param("mercy", pack.mercyThreshold());
    }

    private PackView map(ResultSet row, int index) throws SQLException {
        int ratesId = row.getInt("id_artwork_pull_rates");
        return new PackView(row.getInt("id"), new PackWriteRequest(ratesId, row.getString("pack_name"),
                json.readValue(row.getString("pack_description"), PackWriteRequest.Description.class),
                row.getInt("pack_price"), row.getBoolean("is_acquirable"), row.getString("pack_image_url"),
                strings(row, "allowed_factions"), strings(row, "allowed_types"),
                strings(row, "allowed_expansions"), strings(row, "allowed_rarities"),
                row.getObject("mercy_threshold", Integer.class),
                json.readValue(row.getString("slot_type_ids"),
                        json.getTypeFactory().constructCollectionType(List.class, Integer.class))),
                new ArtworkPullRates(ratesId, row.getBigDecimal("standard"), row.getBigDecimal("extended_art"),
                        row.getBigDecimal("full_art"), row.getBigDecimal("alternate_art")),
                json.readValue(row.getString("slot_pull_types"),
                        json.getTypeFactory().constructCollectionType(List.class, SlotPullType.class)));
    }

    private List<String> strings(ResultSet row, String column) throws SQLException {
        return json.readValue(row.getString(column), json.getTypeFactory().constructCollectionType(List.class, String.class));
    }
}
