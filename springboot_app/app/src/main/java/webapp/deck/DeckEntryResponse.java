package webapp.deck;

public record DeckEntryResponse(int entryId, int cardId, int cardArtId, String destination) {
    static DeckEntryResponse from(DeckEntry entry) {
        return new DeckEntryResponse(entry.id(), entry.cardId(), entry.cardArtId(), entry.destination());
    }
}
