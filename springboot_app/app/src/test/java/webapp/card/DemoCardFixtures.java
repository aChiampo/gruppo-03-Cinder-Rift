package webapp.card;

import java.util.ArrayList;
import java.util.List;

import org.springframework.jdbc.core.simple.JdbcClient;

/** Builds isolated, regulation-shaped catalog and collection data for API tests. */
final class DemoCardFixtures {
    private static final String[] FACTIONS = {"SAP", "CYB", "ALN", "CLT", "RAD"};

    private DemoCardFixtures() {
    }

    static Fixture seed(JdbcClient jdbc, int ownerId) {
        int expansionId = jdbc.sql("""
                INSERT INTO expansions (code, name, is_active)
                VALUES ('DEMO_FIXTURE', 'Demo fixture set', true)
                RETURNING id
                """)
                .query(Integer.class)
                .single();
        int creatureType = referenceId(jdbc, "card_types", "CREATURE");
        int equipmentType = referenceId(jdbc, "card_types", "EQUIPMENT");
        List<FixtureCard> cards = new ArrayList<>();

        addCreatures(jdbc, expansionId, creatureType, cards);
        addEquipment(jdbc, expansionId, equipmentType, cards);

        FixtureCard first = cards.getFirst();
        FixtureCard unowned = cards.getLast();
        int alternateArtId = insertArtwork(jdbc, first.cardId(), "ALTERNATE_ART",
                "https://example.invalid/demo/cards/" + first.cardId() + "-alternate.svg");
        insertCollection(jdbc, ownerId, first.cardId(), first.artworkId(), 2, 1);
        insertCollection(jdbc, ownerId, first.cardId(), alternateArtId, 1, 0);
        for (FixtureCard card : cards.subList(1, cards.size() - 1)) {
            insertCollection(jdbc, ownerId, card.cardId(), card.artworkId(), 1, 1);
        }
        return new Fixture(first.cardId(), first.artworkId(), alternateArtId,
                unowned.cardId(), expansionId, cards.size());
    }

    private static void addCreatures(
            JdbcClient jdbc, int expansionId, int creatureType, List<FixtureCard> cards) {
        String[][] raritiesByRound = {
            {"COMMON", "COMMON", "COMMON", "COMMON", "COMMON"},
            {"COMMON", "COMMON", "COMMON", "COMMON", "RARE"},
            {"COMMON", "COMMON", "RARE", "RARE", "EPIC"},
            {"COMMON", "COMMON", "RARE", "RARE", "EPIC"},
            {"COMMON", "RARE", "RARE", "EPIC", "LEGENDARY"}
        };
        for (int round = 1; round <= raritiesByRound.length; round++) {
            String[] rarities = raritiesByRound[round - 1];
            for (int slot = 1; slot <= rarities.length; slot++) {
                String rarity = rarities[slot - 1];
                int power = switch (rarity) {
                    case "COMMON" -> 1;
                    case "RARE" -> 2;
                    case "EPIC" -> 3;
                    case "LEGENDARY" -> 5;
                    default -> throw new IllegalStateException("Unsupported fixture rarity");
                };
                int factionIndex = (round + slot - 2) % FACTIONS.length;
                String name = "Demo Creature R%d-%02d".formatted(round, slot);
                cards.add(insertCard(jdbc, name, FACTIONS[factionIndex], rarity, creatureType,
                        expansionId, round, power, 2 + round, 2 + round));
            }
        }
    }

    private static void addEquipment(
            JdbcClient jdbc, int expansionId, int equipmentType, List<FixtureCard> cards) {
        for (int index = 1; index <= 5; index++) {
            String rarity = index <= 3 ? "COMMON" : "RARE";
            int power = index <= 3 ? 1 : 2;
            String faction = FACTIONS[(index - 1) % FACTIONS.length];
            cards.add(insertCard(jdbc, "Demo Equipment %02d".formatted(index), faction, rarity,
                    equipmentType, expansionId, null, power, null, null));
        }
    }

    private static FixtureCard insertCard(
            JdbcClient jdbc, String name, String faction, String rarity, int typeId,
            int expansionId, Integer energy, int power, Integer attack, Integer defense) {
        int cardId = jdbc.sql("""
                INSERT INTO cards (
                    name, id_type, energy_cost, power, id_rarity, value_atk, value_def,
                    id_faction, id_expansion
                )
                VALUES (
                    :name, :typeId, :energy, :power,
                    (SELECT id FROM rarities WHERE code = :rarity),
                    :attack, :defense,
                    (SELECT id FROM factions WHERE code = :faction),
                    :expansionId
                )
                RETURNING id
                """)
                .param("name", name)
                .param("typeId", typeId)
                .param("energy", energy)
                .param("power", power)
                .param("rarity", rarity)
                .param("attack", attack)
                .param("defense", defense)
                .param("faction", faction)
                .param("expansionId", expansionId)
                .query(Integer.class)
                .single();
        int artworkId = insertArtwork(jdbc, cardId, "STANDARD",
                "https://example.invalid/demo/cards/" + cardId + "-standard.svg");
        return new FixtureCard(cardId, artworkId);
    }

    private static int insertArtwork(JdbcClient jdbc, int cardId, String artCode, String imageUrl) {
        return jdbc.sql("""
                INSERT INTO cards_arts (id_card, art_code, url_card_art)
                VALUES (:cardId, :artCode, :imageUrl)
                RETURNING id_art
                """)
                .param("cardId", cardId)
                .param("artCode", artCode)
                .param("imageUrl", imageUrl)
                .query(Integer.class)
                .single();
    }

    private static void insertCollection(
            JdbcClient jdbc, int ownerId, int cardId, int artworkId, int owned, int tradable) {
        jdbc.sql("""
                INSERT INTO collections (id_user, id_card, id_card_art, owned_replica, tradable_replica)
                VALUES (:ownerId, :cardId, :artworkId, :owned, :tradable)
                """)
                .param("ownerId", ownerId)
                .param("cardId", cardId)
                .param("artworkId", artworkId)
                .param("owned", owned)
                .param("tradable", tradable)
                .update();
    }

    private static int referenceId(JdbcClient jdbc, String table, String code) {
        return jdbc.sql("SELECT id FROM " + table + " WHERE code = :code")
                .param("code", code)
                .query(Integer.class)
                .single();
    }

    record Fixture(
            int firstCardId,
            int firstArtworkId,
            int alternateArtworkId,
            int unownedCardId,
            int expansionId,
            int cardCount) {
    }

    private record FixtureCard(int cardId, int artworkId) {
    }
}
