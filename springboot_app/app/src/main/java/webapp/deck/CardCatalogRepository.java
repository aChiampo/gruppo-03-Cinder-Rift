package webapp.deck;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/** Loads card catalog, artwork, round limits, and ownership in bounded batch queries. */
@Repository
public class CardCatalogRepository {
    private final JdbcClient jdbc;

    public CardCatalogRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    public Map<Integer, CardDefinition> findCards(Set<Integer> cardIds) {
        if (cardIds.isEmpty()) {
            return Map.of();
        }
        Map<Integer, CardDefinition> cards = new HashMap<>();
        jdbc.sql("""
                SELECT c.id, t.code AS type_code, c.energy_cost, c.power,
                       r.code AS rarity_code, c.is_deleted
                FROM cards c
                LEFT JOIN card_types t ON t.id = c.id_type
                LEFT JOIN rarities r ON r.id = c.id_rarity
                WHERE c.id IN (:cardIds)
                """)
                .param("cardIds", cardIds)
                .query((row, rowNumber) -> mapCard(row))
                .list()
                .forEach(card -> cards.put(card.id(), card));
        return Map.copyOf(cards);
    }

    public Set<CardArtworkKey> findArtworks(Set<Integer> cardIds) {
        if (cardIds.isEmpty()) {
            return Set.of();
        }
        return Set.copyOf(jdbc.sql("""
                SELECT id_card, id_art
                FROM cards_arts
                WHERE id_card IN (:cardIds)
                """)
                .param("cardIds", cardIds)
                .query((row, rowNumber) -> new CardArtworkKey(row.getInt("id_card"), row.getInt("id_art")))
                .list());
    }

    public Map<String, RoundConfiguration> findRoundConfigurations() {
        Map<String, RoundConfiguration> rounds = new HashMap<>();
        jdbc.sql("""
                SELECT round_name, round_number, max_energy, max_cards
                FROM round_configurations
                """)
                .query((row, rowNumber) -> mapRound(row))
                .list()
                .forEach(round -> rounds.put(round.roundName(), round));
        return Map.copyOf(rounds);
    }

    public Map<CardArtworkKey, Integer> findOwnedCopies(int userId, Set<Integer> cardIds,
            Set<CardArtworkKey> requestedArtworks) {
        if (cardIds.isEmpty()) {
            return Map.of();
        }
        Map<CardArtworkKey, Integer> owned = new HashMap<>();
        jdbc.sql("""
                SELECT id_card, id_card_art, owned_replica
                FROM collections
                WHERE id_user = :userId AND id_card IN (:cardIds)
                """)
                .param("userId", userId)
                .param("cardIds", cardIds)
                .query((row, rowNumber) -> {
                    CardArtworkKey key = new CardArtworkKey(row.getInt("id_card"), row.getInt("id_card_art"));
                    int count = row.getInt("owned_replica");
                    if (count <= 0) {
                        throw new IllegalStateException("Collection contains an invalid owned-copy count");
                    }
                    return Map.entry(key, count);
                })
                .list()
                .forEach(entry -> {
                    if (requestedArtworks.contains(entry.getKey())) {
                        owned.put(entry.getKey(), entry.getValue());
                    }
                });
        return Map.copyOf(owned);
    }

    private static CardDefinition mapCard(ResultSet row) throws SQLException {
        return new CardDefinition(row.getInt("id"), row.getString("type_code"),
                (Integer) row.getObject("energy_cost"), (Integer) row.getObject("power"),
                row.getString("rarity_code"), row.getBoolean("is_deleted"));
    }

    private static RoundConfiguration mapRound(ResultSet row) throws SQLException {
        return new RoundConfiguration(row.getString("round_name"), row.getInt("round_number"),
                row.getInt("max_energy"), row.getInt("max_cards"));
    }
}
