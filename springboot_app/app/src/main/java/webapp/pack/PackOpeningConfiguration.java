package webapp.pack;

import java.util.List;
import java.util.Map;
import webapp.card.CardArtwork;
import webapp.card.CardReference;

/** Validated, transaction-local configuration and eligible card pools. */
record PackOpeningConfiguration(List<SlotPullType> slots, Map<Integer, List<DrawCard>> cardsByRarity,
        ArtworkPullRates artwork, Integer threshold, CardReference mercyRarity) {
    boolean mercyEnabled() { return threshold != null && mercyRarity != null; }

    record DrawCard(int id, String name, CardReference rarity, List<CardArtwork> artworks) { }
}
