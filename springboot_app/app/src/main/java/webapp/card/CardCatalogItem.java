package webapp.card;

import java.util.List;

/** Catalog details and every artwork variant for one card. */
public record CardCatalogItem(
        int id,
        String name,
        CardReference faction,
        CardReference rarity,
        Integer energyCost,
        Integer attack,
        Integer defense,
        Integer power,
        CardReference type,
        CardReference expansion,
        List<CardArtwork> artworks) {
}
