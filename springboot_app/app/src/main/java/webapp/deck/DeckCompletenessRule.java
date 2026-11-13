package webapp.deck;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

/** Reports the proposed 30-card target without preventing incomplete drafts from being saved. */
@Component
public class DeckCompletenessRule implements DeckValidationRule {
    private static final Map<String, Integer> TARGETS = Map.of(
            "ROUND1", 5, "ROUND2", 5, "ROUND3", 5, "ROUND4", 5, "ROUND5", 5, "EQUIPMENT", 5);

    @Override
    public List<DeckViolation> validate(DeckValidationContext context) {
        List<DeckViolation> violations = new ArrayList<>();
        DeckSummary summary = DeckSummary.from(context);
        addTargetViolation(violations, context.requireComplete(), "CREATURE_COUNT_INCOMPLETE",
                "creatures", 25, summary.creatureCount(), null);
        addTargetViolation(violations, context.requireComplete(), "EQUIPMENT_COUNT_INCOMPLETE",
                "equipment", 5, summary.equipmentCount(), "EQUIPMENT");
        TARGETS.forEach((destination, target) -> {
            int count = summary.cardCountByDestination().getOrDefault(destination, 0);
            addTargetViolation(violations, context.requireComplete(), "CARD_COUNT_INCOMPLETE",
                    "round", target, count, destination);
        });
        addTargetViolation(violations, context.requireComplete(), "DECK_COUNT_INCOMPLETE",
                "deck", 30, summary.totalCards(), null);
        return violations;
    }

    private static void addTargetViolation(List<DeckViolation> violations, boolean requireComplete,
            String code, String scope, int target, int actual, String destination) {
        if (actual == target) {
            return;
        }
        String message = "Expected " + target + " " + scope + " but found " + actual;
        if (actual < target && !requireComplete) {
            violations.add(DeckViolation.warning(code, scope, message, destination));
            return;
        }
        String errorCode = actual > target ? code.replace("_INCOMPLETE", "_EXCEEDED") : code;
        violations.add(destination == null
                ? DeckViolation.deck(errorCode, message)
                : DeckViolation.destination(errorCode, message, destination));
    }
}
