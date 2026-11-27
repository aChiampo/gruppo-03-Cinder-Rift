package webapp.pack;

import java.util.List;
import com.fasterxml.jackson.annotation.JsonInclude;
import webapp.card.CardReference;

/** Immediate batch result; it is not an opening-history resource. */
@JsonInclude(JsonInclude.Include.ALWAYS)
public record OpenPacksResponse(int packId, int quantityOpened, int remainingPackQuantity,
        List<OpenedPackView> packs, MercyView mercy) {
    public record OpenedPackView(int sequence, List<PulledCardView> cards) { }

    @JsonInclude(JsonInclude.Include.ALWAYS)
    public record PulledCardView(int slot, int cardId, int cardArtId, String artCode,
            String name, CardReference rarity, String imageUrl) { }

    public record MercyView(int threshold, int counter, CardReference rarity) { }
}
