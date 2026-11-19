package webapp.collection;

import webapp.card.CardReference;

/** One owned card-art variant and its stored copy counts. */
public record CollectionItem(
        int cardId,
        int cardArtId,
        String artCode,
        String imageUrl,
        String name,
        CardReference faction,
        CardReference rarity,
        Integer energyCost,
        Integer attack,
        Integer defense,
        Integer power,
        CardReference type,
        CardReference expansion,
        int ownedReplica,
        int tradableReplica) {
}
