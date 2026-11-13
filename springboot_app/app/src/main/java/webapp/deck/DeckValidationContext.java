package webapp.deck;

import java.util.List;
import java.util.Map;
import java.util.Set;

/** Immutable catalog, ownership, configuration, and composition snapshot for one validation. */
public record DeckValidationContext(int userId, List<DeckEntry> entries,
        Map<Integer, CardDefinition> cards, Set<CardArtworkKey> artworks,
        Map<String, RoundConfiguration> rounds, Map<CardArtworkKey, Integer> ownedCopies,
        boolean requireComplete) {
    public DeckValidationContext {
        entries = List.copyOf(entries);
        cards = Map.copyOf(cards);
        artworks = Set.copyOf(artworks);
        rounds = Map.copyOf(rounds);
        ownedCopies = Map.copyOf(ownedCopies);
    }
}
