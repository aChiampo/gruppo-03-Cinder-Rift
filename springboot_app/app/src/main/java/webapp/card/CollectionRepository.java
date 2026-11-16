package webapp.card;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/** Reads collection rows only for the authenticated owner. */
@Repository
public class CollectionRepository {
    private final JdbcClient jdbc;

    public CollectionRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    public long count(int ownerId, CardSearchCriteria criteria) {
        CardSearchSql.QueryParts query = CardSearchSql.build(criteria, ownerId);
        var statement = jdbc.sql("""
                SELECT COUNT(*)
                FROM collections co
                JOIN cards c ON c.id = co.id_card
                """ + CardSearchSql.REFERENCE_JOINS
                + query.where());
        query.parameters().forEach(statement::param);
        return statement.query(Long.class).single();
    }

    public List<CollectionItem> findPage(int ownerId, CardSearchCriteria criteria) {
        CardSearchSql.QueryParts query = CardSearchSql.build(criteria, ownerId);
        var statement = jdbc.sql("""
                SELECT co.id_card, co.id_card_art, co.owned_replica, co.tradable_replica,
                       ca.art_code, ca.url_card_art,
                       c.name, c.energy_cost, c.value_atk, c.value_def, c.power,
                       f.id AS faction_id, f.code AS faction_code, f.name AS faction_name,
                       r.id AS rarity_id, r.code AS rarity_code, r.name AS rarity_name,
                       t.id AS type_id, t.code AS type_code, t.name AS type_name,
                       e.id AS expansion_id, e.code AS expansion_code, e.name AS expansion_name
                FROM collections co
                JOIN cards c ON c.id = co.id_card
                JOIN cards_arts ca ON ca.id_card = co.id_card AND ca.id_art = co.id_card_art
                """ + CardSearchSql.REFERENCE_JOINS
                + query.where()
                + " ORDER BY " + query.orderBy() + " LIMIT :limit OFFSET :offset");
        query.parameters().forEach(statement::param);
        return statement
                .param("limit", criteria.size())
                .param("offset", (long) criteria.page() * criteria.size())
                .query(CollectionRepository::mapItem)
                .list();
    }

    private static CollectionItem mapItem(ResultSet row, int rowNumber) throws SQLException {
        return new CollectionItem(
                row.getInt("id_card"),
                row.getInt("id_card_art"),
                row.getString("art_code"),
                row.getString("url_card_art"),
                row.getString("name"),
                reference(row, "faction"),
                reference(row, "rarity"),
                row.getObject("energy_cost", Integer.class),
                row.getObject("value_atk", Integer.class),
                row.getObject("value_def", Integer.class),
                row.getObject("power", Integer.class),
                reference(row, "type"),
                reference(row, "expansion"),
                row.getInt("owned_replica"),
                row.getInt("tradable_replica"));
    }

    private static CardReference reference(ResultSet row, String prefix) throws SQLException {
        Integer id = row.getObject(prefix + "_id", Integer.class);
        return id == null ? null
                : new CardReference(id, row.getString(prefix + "_code"), row.getString(prefix + "_name"));
    }
}
