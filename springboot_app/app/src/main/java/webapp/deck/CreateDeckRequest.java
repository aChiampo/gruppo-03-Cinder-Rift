package webapp.deck;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public record CreateDeckRequest(List<@NotNull @Valid DeckEntryRequest> entries) {
}
