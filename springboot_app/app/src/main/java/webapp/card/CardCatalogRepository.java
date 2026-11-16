package webapp.card;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/** Reads cards not logically deleted and all their artwork variants with bounded queries. */
@Repository("cardApiCatalogRepository")
public class CardCatalogRepository {
    private final JdbcClient jdbc;

    public CardCatalogRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    public long count(CardSearchCriteria criteria) {
        CardSearchSql.QueryParts query = CardSearchSql.build(criteria, null);
        var statement = jdbc.sql("SELECT COUNT(*) " + CardSearchSql.FROM_AND_JOINS + query.where());
        query.parameters().forEach(statement::param);
        return statement.query(Long.class).single();
    }

    public List<CardCatalogItem> findPage(CardSearchCriteria criteria) {
        CardSearchSql.QueryParts query = CardSearchSql.build(criteria, null);
        var statement = jdbc.sql("""
                SELECT c.id, c.name, c.energy_cost, c.value_atk, c.value_def, c.power,
                       f.id AS faction_id, f.code AS faction_code, f.name AS faction_name,
                       r.id AS rarity_id, r.code AS rarity_code, r.name AS rarity_name,
                       t.id AS type_id, t.code AS type_code, t.name AS type_name,
                       e.id AS expansion_id, e.code AS expansion_code, e.name AS expansion_name
                """ + CardSearchSql.FROM_AND_JOINS + query.where()
                + " ORDER BY " + query.orderBy() + " LIMIT :limit OFFSET :offset");
        query.parameters().forEach(statement::param);
        List<CardCatalogRow> rows = statement
                .param("limit", criteria.size())
                .param("offset", (long) criteria.page() * criteria.size())
                .query(CardCatalogRepository::mapCard)
                .list();
        if (rows.isEmpty()) {
            return List.of();
        }
        Map<Integer, List<CardArtwork>> artworks = findArtworks(rows.stream().map(CardCatalogRow::id).toList());
        return rows.stream()
                .map(row -> new CardCatalogItem(row.id(), row.name(), row.faction(), row.rarity(),
                        row.energyCost(), row.attack(), row.defense(), row.power(), row.type(),
                        row.expansion(), artworks.getOrDefault(row.id(), List.of())))
                .toList();
    }

    private Map<Integer, List<CardArtwork>> findArtworks(List<Integer> cardIds) {
        Map<Integer, List<CardArtwork>> artworks = new HashMap<>();
        jdbc.sql("""
                SELECT id_card, id_art, art_code, url_card_art
                FROM cards_arts
                WHERE id_card IN (:cardIds)
                ORDER BY id_card, art_code
                """)
                .param("cardIds", cardIds)
                .query((row, rowNumber) -> Map.entry(row.getInt("id_card"),
                        new CardArtwork(row.getInt("id_art"), row.getString("art_code"),
                                row.getString("url_card_art"))))
                .list()
                .forEach(entry -> artworks.computeIfAbsent(entry.getKey(), ignored -> new ArrayList<>())
                        .add(entry.getValue()));
        return artworks;
    }

    private static CardCatalogRow mapCard(ResultSet row, int rowNumber) throws SQLException {
        return new CardCatalogRow(
                row.getInt("id"),
                row.getString("name"),
                reference(row, "faction"),
                reference(row, "rarity"),
                row.getObject("energy_cost", Integer.class),
                row.getObject("value_atk", Integer.class),
                row.getObject("value_def", Integer.class),
                row.getObject("power", Integer.class),
                reference(row, "type"),
                reference(row, "expansion"));
    }

    private static CardReference reference(ResultSet row, String prefix) throws SQLException {
        Integer id = row.getObject(prefix + "_id", Integer.class);
        return id == null ? null
                : new CardReference(id, row.getString(prefix + "_code"), row.getString(prefix + "_name"));
    }

    private record CardCatalogRow(
            int id,
            String name,
            CardReference faction,
            CardReference rarity,
            Integer energyCost,
            Integer attack,
            Integer defense,
            Integer power,
            CardReference type,
            CardReference expansion) {
    }
}
