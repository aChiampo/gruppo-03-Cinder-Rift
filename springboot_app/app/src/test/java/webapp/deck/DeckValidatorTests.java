package webapp.deck;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DeckValidatorTests {
    private final DeckValidator validator = new DeckValidator(List.of(
            new CardAndArtworkExistenceRule(),
            new DestinationCompatibilityRule(),
            new OwnershipRule(),
            new DestinationLimitsRule(),
            new DeckCompletenessRule(),
            new CompositionLimitsRule(),
            new DeckEntryStructureRule()));

    @Test
    void allowsAnIncompleteDraftAndReportsMissingCardsAsWarnings() {
        DeckValidationResult result = validator.validate(context(
                List.of(new DeckEntry(1, 11, "ROUND1")), false, 1, 1));

        assertThat(result.valid()).as("validation errors: %s", result.errors()).isTrue();
        assertThat(result.errors()).isEmpty();
        assertThat(result.warnings()).anyMatch(v -> v.code().equals("DECK_COUNT_INCOMPLETE")
                && v.severity() == DeckViolation.Severity.WARNING);
        assertThat(result.summary().totalCards()).isEqualTo(1);
        assertThat(result.summary().creatureCount()).isEqualTo(1);
        assertThat(result.summary().cardCountByDestination()).containsEntry("ROUND1", 1);
    }

    @Test
    void requiresCompleteDeckWhenChallengeableAndRejectsBadArtworkOrOwnership() {
        List<DeckEntry> entries = List.of(
                new DeckEntry(1, 11, "ROUND1"),
                new DeckEntry(1, 12, "ROUND1"));

        DeckValidationResult result = validator.validate(context(entries, true, 1, 1));

        assertThat(result.valid()).isFalse();
        assertThat(result.errors()).anyMatch(v -> v.code().equals("CARD_ART_NOT_FOUND"));
        assertThat(result.errors()).anyMatch(v -> v.code().equals("INSUFFICIENT_OWNERSHIP"));
        assertThat(result.errors()).anyMatch(v -> v.code().equals("DECK_COUNT_INCOMPLETE"));
        assertThat(result.warnings()).isEmpty();
    }

    @Test
    void rejectsDeletedCardsAndIncompatibleDestinations() {
        DeckValidationContext deleted = context(
                List.of(new DeckEntry(1, 11, "ROUND1")), false, 1, 1, true);
        DeckValidationContext incompatible = context(
                List.of(new DeckEntry(1, 11, "EQUIPMENT")), false, 1, 1);

        assertThat(validator.validate(deleted).errors()).anyMatch(v -> v.code().equals("CARD_NOT_FOUND"));
        assertThat(validator.validate(incompatible).errors())
                .anyMatch(v -> v.code().equals("DESTINATION_TYPE_MISMATCH"));
    }

    @Test
    void reportsMissingCreatureAndEquipmentTargetsForDraftsAndSubmission() {
        List<DeckEntry> entries = List.of(new DeckEntry(1, 11, "ROUND1"));
        DeckValidationResult draft = validator.validate(context(entries, false, 1, 5));
        DeckValidationResult submission = validator.validate(context(entries, true, 1, 5));

        assertThat(draft.warnings()).anyMatch(v -> v.code().equals("CREATURE_COUNT_INCOMPLETE"));
        assertThat(draft.warnings()).anyMatch(v -> v.code().equals("EQUIPMENT_COUNT_INCOMPLETE"));
        assertThat(submission.errors()).anyMatch(v -> v.code().equals("CREATURE_COUNT_INCOMPLETE"));
        assertThat(submission.errors()).anyMatch(v -> v.code().equals("EQUIPMENT_COUNT_INCOMPLETE"));
    }

    @Test
    void rejectsCountsAboveTheCompositionTargets() {
        List<DeckEntry> entries = new ArrayList<>();
        for (String destination : List.of("ROUND1", "ROUND2", "ROUND3", "ROUND4", "ROUND5", "EQUIPMENT")) {
            for (int copy = 0; copy < 5; copy++) {
                boolean equipment = "EQUIPMENT".equals(destination);
                entries.add(new DeckEntry(equipment ? 2 : 1, equipment ? 12 : 11, destination));
            }
        }
        entries.add(new DeckEntry(1, 11, "ROUND1"));

        DeckValidationResult result = validator.validate(completeContext(entries));

        assertThat(result.errors()).anyMatch(v -> v.code().equals("CARD_COUNT_EXCEEDED"));
        assertThat(result.errors()).anyMatch(v -> v.code().equals("CREATURE_COUNT_EXCEEDED"));
    }

    @Test
    void acceptsTheCompleteThirtyCopyComposition() {
        List<DeckEntry> entries = new ArrayList<>();
        int cardId = 1;
        for (String destination : List.of("ROUND1", "ROUND2", "ROUND3", "ROUND4", "ROUND5", "EQUIPMENT")) {
            for (int copy = 0; copy < 5; copy++) {
                boolean equipment = "EQUIPMENT".equals(destination);
                entries.add(new DeckEntry(cardId, cardId + 100, destination));
                cardId++;
            }
        }
        DeckValidationContext context = completeContext(entries);

        DeckValidationResult result = validator.validate(context);

        assertThat(result.valid()).as("validation errors: %s", result.errors()).isTrue();
        assertThat(result.errors()).isEmpty();
        assertThat(result.warnings()).isEmpty();
        assertThat(result.summary().totalCards()).isEqualTo(30);
        assertThat(result.summary().creatureCount()).isEqualTo(25);
        assertThat(result.summary().equipmentCount()).isEqualTo(5);
    }

    private static DeckValidationContext context(List<DeckEntry> entries, boolean requireComplete,
            int owned, int maxCardEnergy) {
        return context(entries, requireComplete, owned, maxCardEnergy, false);
    }

    private static DeckValidationContext context(List<DeckEntry> entries, boolean requireComplete,
            int owned, int maxCardEnergy, boolean deleted) {
        return new DeckValidationContext(7, entries,
                Map.of(1, new CardDefinition(1, "CREATURE", 1, 1, "COMMON", deleted)),
                Set.of(new CardArtworkKey(1, 11)),
                Map.of("ROUND1", new RoundConfiguration("ROUND1", 1, maxCardEnergy, 14, 5),
                        "EQUIPMENT", new RoundConfiguration("EQUIPMENT", 6, 6, 10, 5)),
                Map.of(new CardArtworkKey(1, 11), owned), requireComplete);
    }

    private static DeckValidationContext completeContext(List<DeckEntry> entries) {
        Map<String, RoundConfiguration> rounds = new HashMap<>();
        for (int round = 1; round <= 5; round++) {
            String name = "ROUND" + round;
            rounds.put(name, new RoundConfiguration(name, round, round, 14, 5));
        }
        rounds.put("EQUIPMENT", new RoundConfiguration("EQUIPMENT", 6, 6, 10, 5));
        Map<Integer, CardDefinition> cards = new HashMap<>();
        Map<CardArtworkKey, Integer> owned = new HashMap<>();
        for (DeckEntry entry : entries) {
            cards.put(entry.cardId(), new CardDefinition(entry.cardId(),
                    "EQUIPMENT".equals(entry.destination()) ? "EQUIPMENT" : "CREATURE", 1, 1, "COMMON", false));
            owned.merge(new CardArtworkKey(entry.cardId(), entry.cardArtId()), 1, Integer::sum);
        }
        return new DeckValidationContext(7, entries, cards, owned.keySet(), rounds, owned, true);
    }
}
