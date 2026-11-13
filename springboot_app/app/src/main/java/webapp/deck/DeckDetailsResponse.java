package webapp.deck;

import java.util.List;

public record DeckDetailsResponse(int id, int ownerId, boolean isChallengeable,
        List<DeckEntryResponse> entries, DeckValidationResult validation) {
    static DeckDetailsResponse from(DeckView view) {
        return new DeckDetailsResponse(view.deck().id(), view.deck().userId(), view.deck().challengeable(),
                view.entries().stream().map(DeckEntryResponse::from).toList(), view.validation());
    }
}
