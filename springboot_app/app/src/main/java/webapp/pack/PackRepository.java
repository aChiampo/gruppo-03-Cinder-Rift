package webapp.pack;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import tools.jackson.databind.json.JsonMapper;

/** Persists package configuration without changing balances, ownership, or mercy progress. */
@Repository
public class PackRepository {
    private static final String SELECT_PACK = """
            SELECT p.*, a.standard, a.extended_art, a.full_art, a.alternate_art
            FROM packs p JOIN artwork_pull_rates a ON a.id = p.id_artwork_pull_rates
            """;
    private final JdbcClient jdbc;
    private final JsonMapper json;

    public PackRepository(JdbcClient jdbc, JsonMapper json) {
        this.jdbc = jdbc;
        this.json = json;
    }

    public long count() {
        return jdbc.sql("SELECT count(*) FROM packs").query(Long.class).single();
    }

    public List<PackView> findPage(int page, int size) {
        return jdbc.sql(SELECT_PACK + " ORDER BY p.id LIMIT :size OFFSET :offset")
                .param("size", size).param("offset", (long) page * size).query(this::map).list();
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
        return parameters(jdbc.sql("""
                INSERT INTO packs (id_artwork_pull_rates, pack_name, pack_description, pack_price, pack_image_url,
                    allowed_factions, allowed_types, allowed_expansions, allowed_rarities,
                    mercy_threshold, slot_pulls)
                VALUES (:artworkRates, :name, CAST(:description AS jsonb), :price, :image,
                    CAST(:factions AS jsonb), CAST(:types AS jsonb), CAST(:expansions AS jsonb),
                    CAST(:rarities AS jsonb), :mercy, CAST(:slots AS jsonb))
                RETURNING id
                """), pack).query(Integer.class).single();
    }

    public void update(int id, PackWriteRequest pack) {
        parameters(jdbc.sql("""
                UPDATE packs SET id_artwork_pull_rates = :artworkRates,
                    pack_name = :name, pack_description = CAST(:description AS jsonb),
                    pack_price = :price, pack_image_url = :image,
                    allowed_factions = CAST(:factions AS jsonb), allowed_types = CAST(:types AS jsonb),
                    allowed_expansions = CAST(:expansions AS jsonb),
                    allowed_rarities = CAST(:rarities AS jsonb), mercy_threshold = :mercy,
                    slot_pulls = CAST(:slots AS jsonb)
                WHERE id = :id
                """), pack).param("id", id).update();
    }

    public boolean isInUse(int id) {
        return jdbc.sql("""
                SELECT EXISTS (SELECT 1 FROM inventory_packs WHERE id_pack = :id)
                    OR EXISTS (SELECT 1 FROM user_mercy WHERE id_pack = :id)
                """).param("id", id).query(Boolean.class).single();
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

    private JdbcClient.StatementSpec parameters(JdbcClient.StatementSpec sql, PackWriteRequest pack) {
        return sql.param("artworkRates", pack.artworkPullRatesId())
                .param("name", pack.name()).param("description", json.writeValueAsString(pack.description()))
                .param("price", pack.price()).param("image", pack.imageUrl())
                .param("factions", json.writeValueAsString(pack.allowedFactions()))
                .param("types", json.writeValueAsString(pack.allowedTypes()))
                .param("expansions", json.writeValueAsString(pack.allowedExpansions()))
                .param("rarities", json.writeValueAsString(pack.allowedRarities()))
                .param("mercy", pack.mercyThreshold()).param("slots", json.writeValueAsString(pack.slotPulls()));
    }

    private PackView map(ResultSet row, int index) throws SQLException {
        int ratesId = row.getInt("id_artwork_pull_rates");
        return new PackView(row.getInt("id"), new PackWriteRequest(ratesId, row.getString("pack_name"),
                json.readValue(row.getString("pack_description"), PackWriteRequest.Description.class),
                row.getInt("pack_price"), row.getString("pack_image_url"),
                strings(row, "allowed_factions"), strings(row, "allowed_types"),
                strings(row, "allowed_expansions"), strings(row, "allowed_rarities"),
                row.getObject("mercy_threshold", Integer.class), json.readTree(row.getString("slot_pulls"))),
                new ArtworkPullRates(ratesId, row.getBigDecimal("standard"), row.getBigDecimal("extended_art"),
                        row.getBigDecimal("full_art"), row.getBigDecimal("alternate_art")));
    }

    private List<String> strings(ResultSet row, String column) throws SQLException {
        return json.readValue(row.getString(column), json.getTypeFactory().constructCollectionType(List.class, String.class));
    }
}
