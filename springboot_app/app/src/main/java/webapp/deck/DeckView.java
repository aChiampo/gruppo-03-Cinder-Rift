package webapp.deck;

import java.util.List;

record DeckView(DeckRecord deck, List<DeckEntry> entries, DeckValidationResult validation) {
    DeckView {
        entries = List.copyOf(entries);
    }
}
