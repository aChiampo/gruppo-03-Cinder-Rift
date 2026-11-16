package webapp.deck;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

/** Matches creature and equipment cards to their allowed destination groups. */
@Component
public class DestinationCompatibilityRule implements DeckValidationRule {
    @Override
    public List<DeckViolation> validate(DeckValidationContext context) {
        List<DeckViolation> violations = new ArrayList<>();
        for (DeckEntry entry : context.entries()) {
            if (!context.rounds().containsKey(entry.destination())) {
                violations.add(DeckViolation.destination("DESTINATION_NOT_FOUND",
                        "Destination is not configured", entry.destination()));
                continue;
            }
            CardDefinition card = context.cards().get(entry.cardId());
            if (card == null || card.deleted()) {
                continue;
            }
            boolean compatible = switch (card.typeCode() == null ? "" : card.typeCode()) {
                case "CREATURE" -> entry.destination().matches("ROUND[1-5]");
                case "EQUIPMENT" -> "EQUIPMENT".equals(entry.destination());
                default -> false;
            };
            if (!compatible) {
                violations.add(DeckViolation.card("DESTINATION_TYPE_MISMATCH",
                        "Card type is not compatible with destination", entry));
            }
        }
        return violations;
    }
}
