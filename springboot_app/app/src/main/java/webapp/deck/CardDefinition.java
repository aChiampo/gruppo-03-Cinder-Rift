package webapp.deck;

/** Catalog metadata required to validate a card occurrence. */
public record CardDefinition(int id, String typeCode, Integer energyCost, Integer power,
        String rarityCode, boolean deleted) {
}
