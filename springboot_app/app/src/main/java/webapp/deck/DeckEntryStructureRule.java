package webapp.deck;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Component;

/** Rejects malformed occurrence identifiers and destinations before other rules calculate totals. */
@Component
public class DeckEntryStructureRule implements DeckValidationRule {
    @Override
    public List<DeckViolation> validate(DeckValidationContext context) {
        List<DeckViolation> violations = new ArrayList<>();
        Set<Integer> entryIds = new HashSet<>();
        for (DeckEntry entry : context.entries()) {
            if (entry.cardId() <= 0 || entry.cardArtId() <= 0) {
                violations.add(DeckViolation.card("INVALID_CARD_REFERENCE",
                        "Card and artwork IDs must be positive", entry));
            }
            if (entry.destination() == null || entry.destination().isBlank()) {
                violations.add(DeckViolation.card("INVALID_DESTINATION",
                        "Destination must not be blank", entry));
            }
            if (entry.id() != null && !entryIds.add(entry.id())) {
                violations.add(DeckViolation.card("DUPLICATE_ENTRY_ID",
                        "A saved occurrence ID can appear only once", entry));
            }
        }
        return violations;
    }
}
