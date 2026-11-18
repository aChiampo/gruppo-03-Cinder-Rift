package webapp.deck;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

class DestinationLimitsRuleTests {
    private final DestinationLimitsRule rule = new DestinationLimitsRule();

    @ParameterizedTest
    @CsvSource({"0, 0, 5, 1", "1, 2, 5, 1", "5, 7, 2, 5", "5, 1, 5, 5"})
    void acceptsEnergyAndPowerAtOrBelowTheirIndependentLimits(
            int energyCost, int power, int copies, int maxCardEnergy) {
        assertThat(validate(energyCost, power, copies, maxCardEnergy, 14)).isEmpty();
    }

    @Test
    void reportsEveryCopyAboveTheCardEnergyLimit() {
        assertThat(validate(2, 1, 2, 1, 14))
                .hasSize(2)
                .allSatisfy(violation -> {
                    assertThat(violation.code()).isEqualTo("CARD_ENERGY_LIMIT_EXCEEDED");
                    assertThat(violation.scope()).isEqualTo("card");
                    assertThat(violation.cardId()).isEqualTo(1);
                    assertThat(violation.destination()).isEqualTo("ROUND1");
                });
    }

    @Test
    void countsEveryCopyWhenPowerExceedsTheRoundLimit() {
        assertThat(validate(1, 3, 5, 1, 14))
                .singleElement()
                .satisfies(violation -> {
                    assertThat(violation.code()).isEqualTo("DESTINATION_POWER_LIMIT_EXCEEDED");
                    assertThat(violation.scope()).isEqualTo("round");
                    assertThat(violation.destination()).isEqualTo("ROUND1");
                });
    }

    @ParameterizedTest
    @CsvSource(value = {
            "NULL, 1, INVALID_ENERGY_COST",
            "-1, 1, INVALID_ENERGY_COST",
            "1, NULL, INVALID_POWER_COST",
            "1, -1, INVALID_POWER_COST"
    }, nullValues = "NULL")
    void rejectsMissingOrNegativeCosts(Integer energyCost, Integer power, String code) {
        assertThat(validate(energyCost, power, 1, 1, 14))
                .extracting(DeckViolation::code).containsExactly(code);
    }

    @Test
    void checksPowerEvenWhenEnergyIsInvalid() {
        assertThat(validate(null, 15, 1, 1, 14))
                .extracting(DeckViolation::code)
                .containsExactlyInAnyOrder("INVALID_ENERGY_COST", "DESTINATION_POWER_LIMIT_EXCEEDED");
    }

    @Test
    void checksEnergyEvenWhenPowerIsInvalid() {
        assertThat(validate(2, null, 1, 1, 14))
                .extracting(DeckViolation::code)
                .containsExactlyInAnyOrder("CARD_ENERGY_LIMIT_EXCEEDED", "INVALID_POWER_COST");
    }

    @Test
    void sumsPowerWithoutIntegerOverflow() {
        assertThat(validate(1, Integer.MAX_VALUE, 2, 1, Integer.MAX_VALUE))
                .extracting(DeckViolation::code).containsExactly("DESTINATION_POWER_LIMIT_EXCEEDED");
    }

    @Test
    void keepsDestinationPowerTotalsIndependentAndEnforcesCardCounts() {
        List<DeckEntry> entries = List.of(new DeckEntry(1, 11, "ROUND1"),
                new DeckEntry(1, 11, "ROUND2"));
        Map<String, RoundConfiguration> rounds = Map.of(
                "ROUND1", new RoundConfiguration("ROUND1", 1, 1, 14, 5),
                "ROUND2", new RoundConfiguration("ROUND2", 2, 2, 14, 5));
        DeckValidationContext context = new DeckValidationContext(7, entries,
                Map.of(1, new CardDefinition(1, "CREATURE", 1, 8, "COMMON", false)),
                Set.of(), rounds, Map.of(), false);

        assertThat(rule.validate(context)).isEmpty();
        assertThat(validate(1, 1, 6, 1, 14))
                .extracting(DeckViolation::code).containsExactly("DESTINATION_CARD_LIMIT_EXCEEDED");
    }

    private List<DeckViolation> validate(Integer energyCost, Integer power, int copies,
            int maxCardEnergy, int maxRoundPower) {
        DeckValidationContext context = new DeckValidationContext(7,
                Collections.nCopies(copies, new DeckEntry(1, 11, "ROUND1")),
                Map.of(1, new CardDefinition(1, "CREATURE", energyCost, power, "COMMON", false)),
                Set.of(), Map.of("ROUND1", new RoundConfiguration("ROUND1", 1,
                        maxCardEnergy, maxRoundPower, 5)), Map.of(), false);
        return rule.validate(context);
    }
}
