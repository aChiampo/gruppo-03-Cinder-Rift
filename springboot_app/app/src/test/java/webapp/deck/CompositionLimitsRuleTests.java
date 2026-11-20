package webapp.deck;

import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import static org.assertj.core.api.Assertions.*;

class CompositionLimitsRuleTests {
    private final CompositionLimitsRule rule = new CompositionLimitsRule();

    private DeckValidationContext context(String type, String rarity, int power, int count, boolean complete,
            boolean sameCard) {
        List<DeckEntry> entries = new ArrayList<>();
        Map<Integer, CardDefinition> cards = new HashMap<>();
        for (int index = 0; index < count; index++) {
            int id = sameCard ? 1 : index + 1;
            cards.put(id, new CardDefinition(id, type, 1, power, rarity, false));
            // Different artwork IDs deliberately exercise a copy limit across variants and rounds.
            entries.add(new DeckEntry(id, index + 100, "EQUIPMENT".equals(type) ? "EQUIPMENT" : "ROUND" + (index % 5 + 1)));
        }
        return new DeckValidationContext(1, entries, cards, Set.of(), Map.of(), Map.of(), complete);
    }

    @ParameterizedTest
    @CsvSource({"COMMON,1,true", "COMMON,2,true", "COMMON,0,false", "COMMON,3,false",
            "RARE,2,true", "RARE,3,true", "RARE,1,false", "RARE,4,false",
            "EPIC,3,true", "EPIC,4,true", "EPIC,2,false", "EPIC,5,false",
            "LEGENDARY,5,true", "LEGENDARY,4,false", "LEGENDARY,6,false"})
    void enforcesInclusiveRarityPowerRanges(String rarity, int power, boolean valid) {
        var issues = rule.validate(context("CREATURE", rarity, power, 1, false, false));
        assertThat(issues.stream().noneMatch(v -> v.code().equals("RARITY_POWER_OUT_OF_RANGE"))).isEqualTo(valid);
    }

    @ParameterizedTest
    @CsvSource({"CREATURE,COMMON,13", "EQUIPMENT,COMMON,3"})
    void commonMinimumIsAWarningForDraftsAndErrorForPlay(String type, String rarity, int minimum) {
        var draft = rule.validate(context(type, rarity, 1, minimum - 1, false, false));
        assertThat(draft).anyMatch(v -> v.code().equals(type + "_COMMON_MINIMUM_NOT_MET")
                && v.severity() == DeckViolation.Severity.WARNING);
        var complete = rule.validate(context(type, rarity, 1, minimum - 1, true, false));
        assertThat(complete).anyMatch(v -> v.code().equals(type + "_COMMON_MINIMUM_NOT_MET")
                && v.severity() == DeckViolation.Severity.ERROR);
        assertThat(rule.validate(context(type, rarity, 1, minimum, true, false)))
                .noneMatch(v -> v.code().equals(type + "_COMMON_MINIMUM_NOT_MET"));
    }

    @ParameterizedTest
    @CsvSource({"CREATURE,RARE,8", "CREATURE,EPIC,3", "CREATURE,LEGENDARY,1",
            "EQUIPMENT,RARE,2", "EQUIPMENT,EPIC,1", "EQUIPMENT,LEGENDARY,0"})
    void rarityQuotasCountEveryCopyAndRejectAboveTheInclusiveMaximum(String type, String rarity, int maximum) {
        assertThat(rule.validate(context(type, rarity, 3, maximum, false, false)))
                .noneMatch(v -> v.code().equals(type + "_RARITY_LIMIT_EXCEEDED"));
        assertThat(rule.validate(context(type, rarity, 3, maximum + 1, false, false)))
                .anyMatch(v -> v.code().equals(type + "_RARITY_LIMIT_EXCEEDED"));
    }

    @ParameterizedTest
    @CsvSource({"CREATURE,2", "EQUIPMENT,1"})
    void copyLimitAppliesAcrossArtworksAndDestinations(String type, int maximum) {
        assertThat(rule.validate(context(type, "COMMON", 1, maximum, false, true)))
                .noneMatch(v -> v.code().equals("CARD_COPY_LIMIT_EXCEEDED"));
        assertThat(rule.validate(context(type, "COMMON", 1, maximum + 1, false, true)))
                .anyMatch(v -> v.code().equals("CARD_COPY_LIMIT_EXCEEDED"));
    }

    @Test
    void enforcesSeparateBudgetsWithoutOverflowOrBorrowing() {
        assertThat(rule.validate(context("CREATURE", "LEGENDARY", 5, 13, false, false)))
                .noneMatch(v -> v.code().equals("CREATURE_POWER_LIMIT_EXCEEDED"));
        assertThat(rule.validate(context("CREATURE", "LEGENDARY", 5, 14, false, false)))
                .anyMatch(v -> v.code().equals("CREATURE_POWER_LIMIT_EXCEEDED"));
        assertThat(rule.validate(context("EQUIPMENT", "COMMON", 2, 5, false, false)))
                .noneMatch(v -> v.code().equals("EQUIPMENT_POWER_LIMIT_EXCEEDED"));
        assertThat(rule.validate(context("EQUIPMENT", "RARE", 3, 4, false, false)))
                .anyMatch(v -> v.code().equals("EQUIPMENT_POWER_LIMIT_EXCEEDED"));
        assertThat(rule.validate(context("CREATURE", "COMMON", Integer.MAX_VALUE, 2, false, false)))
                .anyMatch(v -> v.code().equals("CREATURE_POWER_LIMIT_EXCEEDED"));
    }

    @Test
    void rejectsUnknownOrMissingRarity() {
        for (String rarity : Arrays.asList(null, "OTHER"))
            assertThat(rule.validate(context("CREATURE", rarity, 1, 1, false, false)))
                    .anyMatch(v -> v.code().equals("INVALID_RARITY"));
    }
}
