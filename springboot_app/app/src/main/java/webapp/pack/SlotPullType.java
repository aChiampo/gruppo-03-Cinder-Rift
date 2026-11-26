package webapp.pack;

import java.math.BigDecimal;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

/** Reusable rarity distribution; omitted rarities have zero probability. */
public record SlotPullType(int id, String description,
        @JsonProperty("pull_rates") List<RarityPullRate> pullRates) {
    public record RarityPullRate(@JsonProperty("id_rarity") int rarityId,
            @JsonProperty("rarity_code") String rarityCode, BigDecimal probability) {
    }
}
