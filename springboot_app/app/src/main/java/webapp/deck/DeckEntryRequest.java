package webapp.deck;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record DeckEntryRequest(
        @NotNull @Positive Integer cardId,
        @NotNull @Positive Integer cardArtId,
        @NotBlank String destination) {
    DeckEntry toDeckEntry() {
        return new DeckEntry(cardId, cardArtId, destination);
    }
}
