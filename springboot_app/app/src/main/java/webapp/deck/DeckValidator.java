package webapp.deck;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Component;

/** Runs all registered validation rules without persisting a validity flag. */
@Component
public class DeckValidator {
    private final List<DeckValidationRule> rules;

    public DeckValidator(List<DeckValidationRule> rules) {
        this.rules = rules.stream()
                .sorted(Comparator.comparing(rule -> rule.getClass().getName()))
                .toList();
    }

    public DeckValidationResult validate(DeckValidationContext context) {
        List<DeckViolation> errors = new ArrayList<>();
        List<DeckViolation> warnings = new ArrayList<>();
        for (DeckValidationRule rule : rules) {
            for (DeckViolation violation : rule.validate(context)) {
                if (violation.severity() == DeckViolation.Severity.WARNING) {
                    warnings.add(violation);
                } else {
                    errors.add(violation);
                }
            }
        }
        return new DeckValidationResult(errors.isEmpty(), errors, warnings, DeckSummary.from(context));
    }
}
