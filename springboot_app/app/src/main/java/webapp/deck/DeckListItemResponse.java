package webapp.deck;

public record DeckListItemResponse(int id, int ownerId, boolean isChallengeable,
        int cardCount, DeckValidationResult validation) {
    static DeckListItemResponse from(DeckView view) {
        return new DeckListItemResponse(view.deck().id(), view.deck().userId(), view.deck().challengeable(),
                view.entries().size(), view.validation());
    }
}
