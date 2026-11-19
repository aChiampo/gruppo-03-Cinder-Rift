package webapp.card;

import java.util.List;
import java.util.Optional;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/** Writes catalog cards and protects existing collection and deck references. */
@Repository
public class CardManagementRepository {
    private final JdbcClient jdbc;

    public CardManagementRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    public int create(CardWriteRequest card) {
        int cardId = jdbc.sql("""
                INSERT INTO cards (
                    name, description, id_type, energy_cost, power, id_rarity,
                    value_atk, value_def, id_faction, id_expansion
                )
                VALUES (
                    :name, :description, :typeId, :energyCost, :power, :rarityId,
                    :attack, :defense, :factionId, :expansionId
                )
                RETURNING id
                """)
                .param("name", card.name())
                .param("description", card.description())
                .param("typeId", card.typeId())
                .param("energyCost", card.energyCost())
                .param("power", card.power())
                .param("rarityId", card.rarityId())
                .param("attack", card.attack())
                .param("defense", card.defense())
                .param("factionId", card.factionId())
                .param("expansionId", card.expansionId())
                .query(Integer.class)
                .single();
        replaceArtworks(cardId, card.artworks());
        return cardId;
    }

    public Optional<Integer> lockActiveCard(int cardId) {
        return jdbc.sql("""
                SELECT id
                FROM cards
                WHERE id = :cardId AND NOT is_deleted
                FOR UPDATE
                """)
                .param("cardId", cardId)
                .query(Integer.class)
                .optional();
    }

    public boolean isInUse(int cardId) {
        return jdbc.sql("""
                SELECT EXISTS (SELECT 1 FROM collections WHERE id_card = :cardId)
                    OR EXISTS (SELECT 1 FROM decks_cards WHERE id_card = :cardId)
                """)
                .param("cardId", cardId)
                .query(Boolean.class)
                .single();
    }

    public void update(int cardId, CardWriteRequest card) {
        jdbc.sql("""
                UPDATE cards
                SET name = :name,
                    description = :description,
                    id_type = :typeId,
                    energy_cost = :energyCost,
                    power = :power,
                    id_rarity = :rarityId,
                    value_atk = :attack,
                    value_def = :defense,
                    id_faction = :factionId,
                    id_expansion = :expansionId
                WHERE id = :cardId AND NOT is_deleted
                """)
                .param("cardId", cardId)
                .param("name", card.name())
                .param("description", card.description())
                .param("typeId", card.typeId())
                .param("energyCost", card.energyCost())
                .param("power", card.power())
                .param("rarityId", card.rarityId())
                .param("attack", card.attack())
                .param("defense", card.defense())
                .param("factionId", card.factionId())
                .param("expansionId", card.expansionId())
                .update();
        replaceArtworks(cardId, card.artworks());
    }

    public void softDelete(int cardId) {
        jdbc.sql("UPDATE cards SET is_deleted = true WHERE id = :cardId AND NOT is_deleted")
                .param("cardId", cardId)
                .update();
    }

    public boolean typeExists(int typeId) {
        return activeReferenceExists("card_types", typeId);
    }

    public boolean rarityExists(int rarityId) {
        return activeReferenceExists("rarities", rarityId);
    }

    public boolean factionExists(int factionId) {
        return activeReferenceExists("factions", factionId);
    }

    public boolean expansionExists(int expansionId) {
        return activeReferenceExists("expansions", expansionId);
    }

    public boolean artworkUrlAvailable(String imageUrl, Integer excludedCardId) {
        var statement = excludedCardId == null
                ? jdbc.sql("""
                        SELECT NOT EXISTS (
                            SELECT 1 FROM cards_arts WHERE url_card_art = :imageUrl
                        )
                        """)
                : jdbc.sql("""
                        SELECT NOT EXISTS (
                            SELECT 1 FROM cards_arts
                            WHERE url_card_art = :imageUrl AND id_card <> :excludedCardId
                        )
                        """)
                        .param("excludedCardId", excludedCardId);
        statement.param("imageUrl", imageUrl);
        return statement.query(Boolean.class).single();
    }

    private boolean activeReferenceExists(String table, int id) {
        String sql = switch (table) {
            case "card_types", "rarities", "factions", "expansions" ->
                    "SELECT EXISTS (SELECT 1 FROM " + table + " WHERE id = :id AND NOT is_deleted)";
            default -> throw new IllegalArgumentException("Unsupported card reference table");
        };
        return jdbc.sql(sql).param("id", id).query(Boolean.class).single();
    }

    private void replaceArtworks(int cardId, List<CardArtworkWriteRequest> artworks) {
        jdbc.sql("DELETE FROM cards_arts WHERE id_card = :cardId")
                .param("cardId", cardId)
                .update();
        for (CardArtworkWriteRequest artwork : artworks) {
            jdbc.sql("""
                    INSERT INTO cards_arts (id_card, art_code, url_card_art)
                    VALUES (:cardId, :artCode, :imageUrl)
                    """)
                    .param("cardId", cardId)
                    .param("artCode", artwork.artCode())
                    .param("imageUrl", artwork.imageUrl())
                    .update();
        }
    }
}
