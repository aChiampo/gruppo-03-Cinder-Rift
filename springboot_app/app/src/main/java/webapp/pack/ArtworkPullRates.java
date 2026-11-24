package webapp.pack;

import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonProperty;

/** Percentages for one artwork roll; slot selection belongs to the future opening policy. */
public record ArtworkPullRates(int id, BigDecimal standard,
        @JsonProperty("extended_art") BigDecimal extendedArt,
        @JsonProperty("full_art") BigDecimal fullArt,
        @JsonProperty("alternate_art") BigDecimal alternateArt) {
}
