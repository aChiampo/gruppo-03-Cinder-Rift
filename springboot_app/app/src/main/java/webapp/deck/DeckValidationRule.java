package webapp.deck;

import java.util.List;

/** A composable rule that reports validation errors or draft warnings. */
public interface DeckValidationRule {
    List<DeckViolation> validate(DeckValidationContext context);
}
