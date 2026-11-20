package webapp.deck;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/** Shared V2.1 prototype constraints for drafts, challengeable decks, and battle snapshots. */
@Component
public class CompositionLimitsRule implements DeckValidationRule {
    private static final Map<String, int[]> POWER_RANGES = Map.of(
            "COMMON", new int[]{1, 2}, "RARE", new int[]{2, 3},
            "EPIC", new int[]{3, 4}, "LEGENDARY", new int[]{5, 5});

    @Override
    public List<DeckViolation> validate(DeckValidationContext context) {
        List<DeckViolation> violations = new ArrayList<>();
        Map<Integer, Integer> copies = new HashMap<>();
        Map<String, Integer> creatures = new HashMap<>();
        Map<String, Integer> equipment = new HashMap<>();
        long creaturePower = 0;
        long equipmentPower = 0;
        for (DeckEntry entry : context.entries()) {
            CardDefinition card = context.cards().get(entry.cardId());
            if (card == null || card.deleted()) continue;
            boolean creature = "CREATURE".equals(card.typeCode());
            boolean equip = "EQUIPMENT".equals(card.typeCode());
            if (!creature && !equip) continue;
            int count = copies.merge(card.id(), 1, Integer::sum);
            if (count == (creature ? 3 : 2)) violations.add(DeckViolation.card("CARD_COPY_LIMIT_EXCEEDED",
                    creature ? "At most 2 copies of a creature are allowed in a deck"
                            : "At most 1 copy of an equipment card is allowed in a deck", entry));
            String rarity = card.rarityCode() == null ? "" : card.rarityCode();
            int[] range = POWER_RANGES.get(rarity);
            if (range == null) {
                violations.add(DeckViolation.card("INVALID_RARITY", "Card rarity is missing or unsupported", entry));
            } else {
                (creature ? creatures : equipment).merge(rarity, 1, Integer::sum);
                if (card.power() != null && (card.power() < range[0] || card.power() > range[1]))
                    violations.add(DeckViolation.card("RARITY_POWER_OUT_OF_RANGE",
                            "Card power must be between " + range[0] + " and " + range[1] + " for " + rarity, entry));
            }
            if (card.power() != null && card.power() >= 0) {
                if (creature) creaturePower += card.power();
                else equipmentPower += card.power();
            }
        }
        if (creaturePower > 65) violations.add(DeckViolation.deck("CREATURE_POWER_LIMIT_EXCEEDED",
                "Creature power exceeds the deck budget of 65"));
        if (equipmentPower > 10) violations.add(DeckViolation.destination("EQUIPMENT_POWER_LIMIT_EXCEEDED",
                "Equipment power exceeds the deck budget of 10", "EQUIPMENT"));
        minimumCommon(violations, context.requireComplete(), "CREATURE", creatures, 13);
        minimumCommon(violations, context.requireComplete(), "EQUIPMENT", equipment, 3);
        maximum(violations, "CREATURE", creatures, "RARE", 8);
        maximum(violations, "CREATURE", creatures, "EPIC", 3);
        maximum(violations, "CREATURE", creatures, "LEGENDARY", 1);
        maximum(violations, "EQUIPMENT", equipment, "RARE", 2);
        maximum(violations, "EQUIPMENT", equipment, "EPIC", 1);
        maximum(violations, "EQUIPMENT", equipment, "LEGENDARY", 0);
        return violations;
    }

    private static void minimumCommon(List<DeckViolation> result, boolean complete, String type,
            Map<String, Integer> counts, int minimum) {
        if (counts.getOrDefault("COMMON", 0) >= minimum) return;
        String code = type + "_COMMON_MINIMUM_NOT_MET";
        String message = "Expected at least " + minimum + " common " + type.toLowerCase(java.util.Locale.ROOT) + " cards";
        result.add(complete ? DeckViolation.deck(code, message)
                : DeckViolation.warning(code, "deck", message, null));
    }

    private static void maximum(List<DeckViolation> result, String type, Map<String, Integer> counts,
            String rarity, int maximum) {
        if (counts.getOrDefault(rarity, 0) > maximum)
            result.add(DeckViolation.deck(type + "_RARITY_LIMIT_EXCEEDED",
                    "At most " + maximum + " " + rarity + " " + type + " cards are allowed"));
    }
}
