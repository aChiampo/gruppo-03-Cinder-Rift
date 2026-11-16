package webapp.deck;

/** Deck metadata returned by an ownership-scoped query. */
public record DeckRecord(int id, int userId, boolean challengeable) {
}
