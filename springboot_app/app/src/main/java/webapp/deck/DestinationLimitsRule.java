package webapp.deck;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

/** Enforces configured card counts, per-card energy, and aggregate power limits. */
@Component
public class DestinationLimitsRule implements DeckValidationRule {
    @Override
    public List<DeckViolation> validate(DeckValidationContext context) {
        Map<String, Integer> counts = new HashMap<>();
        Map<String, Long> power = new HashMap<>();
        List<DeckViolation> violations = new ArrayList<>();
        for (DeckEntry entry : context.entries()) {
            if (entry.destination() == null) {
                continue;
            }
            RoundConfiguration round = context.rounds().get(entry.destination());
            if (round == null) {
                continue;
            }
            counts.merge(entry.destination(), 1, Integer::sum);
            CardDefinition card = context.cards().get(entry.cardId());
            if (card == null || card.deleted()) {
                continue;
            }
            if (card.energyCost() == null || card.energyCost() < 0) {
                violations.add(DeckViolation.card("INVALID_ENERGY_COST",
                        "Card energy cost is missing or negative", entry));
            } else if (card.energyCost() > round.maxCardEnergy()) {
                violations.add(DeckViolation.card("CARD_ENERGY_LIMIT_EXCEEDED",
                        "Card exceeds its destination's configured per-card energy limit", entry));
            }
            if (card.power() == null || card.power() < 0) {
                violations.add(DeckViolation.card("INVALID_POWER_COST",
                        "Card power cost is missing or negative", entry));
            } else {
                power.merge(entry.destination(), card.power().longValue(), Long::sum);
            }
        }
        context.rounds().forEach((destination, round) -> {
            int count = counts.getOrDefault(destination, 0);
            long totalPower = power.getOrDefault(destination, 0L);
            if (count > round.maxCards()) {
                violations.add(DeckViolation.destination("DESTINATION_CARD_LIMIT_EXCEEDED",
                        "Destination exceeds its configured card limit", destination));
            }
            if (totalPower > round.maxRoundPower()) {
                violations.add(DeckViolation.destination("DESTINATION_POWER_LIMIT_EXCEEDED",
                        "Destination exceeds its configured total power limit", destination));
            }
        });
        return violations;
    }
}
