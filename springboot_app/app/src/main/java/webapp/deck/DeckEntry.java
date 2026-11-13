package webapp.deck;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

/** One saved or proposed card copy in a deck. */
public record DeckEntry(Integer id, @Positive int cardId, @Positive int cardArtId,
        @NotBlank String destination) {
    public DeckEntry(int cardId, int cardArtId, String destination) {
        this(null, cardId, cardArtId, destination);
    }

    DeckEntry withId(int entryId) {
        return new DeckEntry(entryId, cardId, cardArtId, destination);
    }

    DeckEntry withDestination(String newDestination) {
        return new DeckEntry(id, cardId, cardArtId, newDestination);
    }
}
