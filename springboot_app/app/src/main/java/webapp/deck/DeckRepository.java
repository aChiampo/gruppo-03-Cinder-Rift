package webapp.deck;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/** Persists decks and individual card-copy rows with ownership-scoped SQL. */
@Repository
public class DeckRepository {
    private final JdbcClient jdbc;

    public DeckRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    public boolean lockActiveUser(int userId) {
        return jdbc.sql("SELECT id FROM users WHERE id = :userId AND NOT is_deleted FOR UPDATE")
                .param("userId", userId)
                .query(Integer.class)
                .optional()
                .isPresent();
    }

    public Optional<DeckRecord> lockOwnedDeck(int userId, int deckId) {
        return jdbc.sql("""
                SELECT d.id, d.id_user, d.is_challengeable
                FROM decks d
                JOIN users u ON u.id = d.id_user AND NOT u.is_deleted
                WHERE d.id = :deckId AND d.id_user = :userId
                FOR UPDATE OF d
                """)
                .param("userId", userId)
                .param("deckId", deckId)
                .query((row, rowNumber) -> mapDeck(row))
                .optional();
    }

    public Optional<DeckRecord> findOwnedDeck(int userId, int deckId) {
        return jdbc.sql("""
                SELECT d.id, d.id_user, d.is_challengeable
                FROM decks d
                JOIN users u ON u.id = d.id_user AND NOT u.is_deleted
                WHERE d.id = :deckId AND d.id_user = :userId
                """)
                .param("userId", userId)
                .param("deckId", deckId)
                .query((row, rowNumber) -> mapDeck(row))
                .optional();
    }

    public List<DeckRecord> findOwnedDecks(int userId) {
        return jdbc.sql("""
                SELECT d.id, d.id_user, d.is_challengeable
                FROM decks d
                JOIN users u ON u.id = d.id_user AND NOT u.is_deleted
                WHERE d.id_user = :userId
                ORDER BY d.id
                """)
                .param("userId", userId)
                .query((row, rowNumber) -> mapDeck(row))
                .list();
    }

    public List<DeckEntry> findEntries(int userId, int deckId) {
        return jdbc.sql("""
                SELECT dc.id, dc.id_card, dc.id_card_art, dc.destination
                FROM decks_cards dc
                JOIN decks d ON d.id = dc.id_deck
                JOIN users u ON u.id = d.id_user AND NOT u.is_deleted
                WHERE d.id = :deckId AND d.id_user = :userId
                ORDER BY dc.id
                """)
                .param("userId", userId)
                .param("deckId", deckId)
                .query((row, rowNumber) -> mapEntry(row))
                .list();
    }

    public Optional<DeckRecord> createDeck(int userId) {
        return jdbc.sql("""
                INSERT INTO decks (id_user)
                SELECT id FROM users WHERE id = :userId AND NOT is_deleted
                RETURNING id, id_user, is_challengeable
                """)
                .param("userId", userId)
                .query((row, rowNumber) -> mapDeck(row))
                .optional();
    }

    public boolean setChallengeable(int userId, int deckId, boolean challengeable) {
        return jdbc.sql("""
                UPDATE decks d SET is_challengeable = :challengeable
                FROM users u
                WHERE d.id = :deckId AND d.id_user = :userId
                  AND u.id = d.id_user AND NOT u.is_deleted
                """)
                .param("userId", userId)
                .param("deckId", deckId)
                .param("challengeable", challengeable)
                .update() == 1;
    }

    public boolean deleteDeck(int userId, int deckId) {
        return jdbc.sql("""
                DELETE FROM decks d
                USING users u
                WHERE d.id = :deckId AND d.id_user = :userId
                  AND u.id = d.id_user AND NOT u.is_deleted
                """)
                .param("userId", userId)
                .param("deckId", deckId)
                .update() == 1;
    }

    public void deleteEntries(int userId, int deckId) {
        jdbc.sql("""
                DELETE FROM decks_cards dc
                USING decks d, users u
                WHERE dc.id_deck = d.id AND d.id = :deckId AND d.id_user = :userId
                  AND u.id = d.id_user AND NOT u.is_deleted
                """)
                .param("userId", userId)
                .param("deckId", deckId)
                .update();
    }

    public Optional<DeckEntry> addEntry(int userId, int deckId, DeckEntry entry) {
        return jdbc.sql("""
                INSERT INTO decks_cards (id_deck, id_card, id_card_art, destination)
                SELECT d.id, :cardId, :cardArtId, :destination
                FROM decks d
                JOIN users u ON u.id = d.id_user AND NOT u.is_deleted
                WHERE d.id = :deckId AND d.id_user = :userId
                RETURNING id, id_card, id_card_art, destination
                """)
                .param("userId", userId)
                .param("deckId", deckId)
                .param("cardId", entry.cardId())
                .param("cardArtId", entry.cardArtId())
                .param("destination", entry.destination())
                .query((row, rowNumber) -> mapEntry(row))
                .optional();
    }

    public boolean updateEntryDestination(int userId, int deckId, int entryId, String destination) {
        return jdbc.sql("""
                UPDATE decks_cards dc SET destination = :destination
                FROM decks d
                JOIN users u ON u.id = d.id_user AND NOT u.is_deleted
                WHERE dc.id = :entryId AND dc.id_deck = d.id
                  AND d.id = :deckId AND d.id_user = :userId
                """)
                .param("userId", userId)
                .param("deckId", deckId)
                .param("entryId", entryId)
                .param("destination", destination)
                .update() == 1;
    }

    public boolean deleteEntry(int userId, int deckId, int entryId) {
        return jdbc.sql("""
                DELETE FROM decks_cards dc
                USING decks d, users u
                WHERE dc.id = :entryId AND dc.id_deck = d.id
                  AND d.id = :deckId AND d.id_user = :userId
                  AND u.id = d.id_user AND NOT u.is_deleted
                """)
                .param("userId", userId)
                .param("deckId", deckId)
                .param("entryId", entryId)
                .update() == 1;
    }

    private static DeckRecord mapDeck(ResultSet row) throws SQLException {
        return new DeckRecord(row.getInt("id"), row.getInt("id_user"), row.getBoolean("is_challengeable"));
    }

    private static DeckEntry mapEntry(ResultSet row) throws SQLException {
        return new DeckEntry(row.getInt("id"), row.getInt("id_card"),
                row.getInt("id_card_art"), row.getString("destination"));
    }
}
