package webapp.pack;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonUnwrapped;
import com.fasterxml.jackson.annotation.JsonProperty;

/** Flat catalog representation preserving the package field names in the F03 contract. */
public record PackView(int id, @JsonUnwrapped PackWriteRequest definition,
        @JsonProperty("artwork_pull_rates") ArtworkPullRates artworkPullRates,
        @JsonProperty("slot_pull_types") List<SlotPullType> slotPullTypes) {
}
