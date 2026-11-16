package webapp.deck;

import java.util.List;

/** Validation outcome and calculated summary; validity is not stored in the database. */
public record DeckValidationResult(boolean valid, List<DeckViolation> errors,
        List<DeckViolation> warnings, DeckSummary summary) {
    public DeckValidationResult {
        errors = List.copyOf(errors);
        warnings = List.copyOf(warnings);
    }
}
