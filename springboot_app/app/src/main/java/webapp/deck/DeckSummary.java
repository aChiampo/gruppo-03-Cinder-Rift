package webapp.deck;

import java.util.Map;

/** Calculated deck counts and energy totals, never persisted as validity state. */
public record DeckSummary(int totalCards, int creatureCount, int equipmentCount,
        Map<String, Integer> cardCountByDestination, Map<String, Long> energyByDestination) {
    static DeckSummary from(DeckValidationContext context) {
        int creatures = 0;
        int equipment = 0;
        var counts = new java.util.HashMap<String, Integer>();
        var energy = new java.util.HashMap<String, Long>();
        for (DeckEntry entry : context.entries()) {
            if (entry.destination() != null && !entry.destination().isBlank()) {
                counts.merge(entry.destination(), 1, Integer::sum);
            }
            CardDefinition card = context.cards().get(entry.cardId());
            if (card != null && !card.deleted()) {
                if ("CREATURE".equals(card.typeCode())) {
                    creatures++;
                } else if ("EQUIPMENT".equals(card.typeCode())) {
                    equipment++;
                }
                if (entry.destination() != null && card.energyCost() != null) {
                    energy.merge(entry.destination(), (long) card.energyCost(), Long::sum);
                }
            }
        }
        return new DeckSummary(context.entries().size(), creatures, equipment,
                Map.copyOf(counts), Map.copyOf(energy));
    }
}
