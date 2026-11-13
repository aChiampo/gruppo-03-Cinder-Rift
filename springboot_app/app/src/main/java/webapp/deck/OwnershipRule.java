package webapp.deck;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

/** Limits each card-artwork occurrence count to the user's owned copies. */
@Component
public class OwnershipRule implements DeckValidationRule {
    @Override
    public List<DeckViolation> validate(DeckValidationContext context) {
        Map<CardArtworkKey, Integer> occurrences = new HashMap<>();
        for (DeckEntry entry : context.entries()) {
            occurrences.merge(new CardArtworkKey(entry.cardId(), entry.cardArtId()), 1, Integer::sum);
        }
        List<DeckViolation> violations = new ArrayList<>();
        occurrences.forEach((key, count) -> {
            int owned = context.ownedCopies().getOrDefault(key, 0);
            if (count > owned) {
                violations.add(new DeckViolation("INSUFFICIENT_OWNERSHIP", "card",
                        "Deck uses more copies of this artwork than the user owns",
                        key.cardId(), key.cardArtId(), null, DeckViolation.Severity.ERROR));
            }
        });
        return violations;
    }
}
