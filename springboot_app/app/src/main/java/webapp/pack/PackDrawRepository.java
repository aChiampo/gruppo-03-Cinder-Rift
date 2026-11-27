package webapp.pack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import webapp.card.CardArtwork;
import webapp.card.CardReference;
import webapp.pack.PackOpeningConfiguration.DrawCard;

/** Reads and protects the catalog rows used by an opening or configuration validation. */
@Repository
public class PackDrawRepository {
    private final JdbcClient jdbc;

    public PackDrawRepository(JdbcClient jdbc) { this.jdbc = jdbc; }

    public Optional<ArtworkPullRates> artwork(int id) {
        return jdbc.sql("SELECT * FROM artwork_pull_rates WHERE id = :id FOR SHARE")
                .param("id", id).query((r, n) -> new ArtworkPullRates(id, r.getBigDecimal("standard"),
                        r.getBigDecimal("extended_art"), r.getBigDecimal("full_art"),
                        r.getBigDecimal("alternate_art"))).optional();
    }

    public Optional<CardReference> rarity(int id) {
        return jdbc.sql("SELECT id, code, name FROM rarities WHERE id = :id AND NOT is_deleted FOR SHARE")
                .param("id", id).query((r, n) -> new CardReference(r.getInt("id"),
                        r.getString("code"), r.getString("name"))).optional();
    }

    public Map<Integer, SlotPullType> slotTypes(List<Integer> ids) {
        var rates = new LinkedHashMap<Integer, List<SlotPullType.RarityPullRate>>();
        jdbc.sql("""
                SELECT t.id, p.id_rarity, r.code, p.probability
                FROM slot_pull_type t JOIN type_pull_rate p ON p.id_slot_pull_type = t.id
                JOIN rarities r ON r.id = p.id_rarity AND NOT r.is_deleted
                WHERE t.id IN (:ids) ORDER BY t.id, p.id_rarity FOR SHARE OF t, p, r
                """).param("ids", ids).query((r, n) -> Map.entry(r.getInt("id"),
                        new SlotPullType.RarityPullRate(r.getInt("id_rarity"), r.getString("code"),
                                r.getBigDecimal("probability"))))
                .list().forEach(e -> rates.computeIfAbsent(e.getKey(), ignored -> new ArrayList<>()).add(e.getValue()));
        var result = new HashMap<Integer, SlotPullType>();
        rates.forEach((id, values) -> result.put(id, new SlotPullType(id, "", List.copyOf(values))));
        return result;
    }

    public List<DrawCard> eligibleCards(PackWriteRequest pack) {
        List<DrawCard> cards = jdbc.sql("""
                SELECT c.id, c.name, r.id AS rarity_id, r.code, r.name AS rarity_name
                FROM cards c
                JOIN factions f ON f.id = c.id_faction AND NOT f.is_deleted
                JOIN card_types t ON t.id = c.id_type AND NOT t.is_deleted
                JOIN expansions e ON e.id = c.id_expansion AND NOT e.is_deleted
                JOIN rarities r ON r.id = c.id_rarity AND NOT r.is_deleted
                WHERE NOT c.is_deleted AND f.code IN (:factions) AND t.code IN (:types)
                    AND e.code IN (:expansions) AND r.code IN (:rarities)
                ORDER BY c.id FOR SHARE OF c, f, t, e, r
                """).param("factions", pack.allowedFactions()).param("types", pack.allowedTypes())
                .param("expansions", pack.allowedExpansions()).param("rarities", pack.allowedRarities())
                .query((r, n) -> new DrawCard(r.getInt("id"), r.getString("name"),
                        new CardReference(r.getInt("rarity_id"), r.getString("code"),
                                r.getString("rarity_name")), List.of())).list();
        if (cards.isEmpty()) return cards;
        var artworks = new HashMap<Integer, List<CardArtwork>>();
        jdbc.sql("""
                SELECT id_card, id_art, art_code, url_card_art FROM cards_arts
                WHERE id_card IN (:ids) ORDER BY id_card, id_art FOR SHARE
                """).param("ids", cards.stream().map(DrawCard::id).toList())
                .query((r, n) -> Map.entry(r.getInt("id_card"), new CardArtwork(r.getInt("id_art"),
                        r.getString("art_code"), r.getString("url_card_art")))).list()
                .forEach(e -> artworks.computeIfAbsent(e.getKey(), ignored -> new ArrayList<>()).add(e.getValue()));
        return cards.stream().map(c -> new DrawCard(c.id(), c.name(), c.rarity(),
                List.copyOf(artworks.getOrDefault(c.id(), List.of())))).toList();
    }
}
