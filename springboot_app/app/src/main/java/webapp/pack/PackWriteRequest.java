package webapp.pack;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import tools.jackson.databind.JsonNode;

/** Package display metadata and configuration; opening rules are deliberately not inferred. */
public record PackWriteRequest(
        @JsonProperty("id_artwork_pull_rates") @NotNull @Positive Integer artworkPullRatesId,
        @JsonProperty("pack_name") @NotBlank String name,
        @JsonProperty("pack_description") @NotNull @Valid Description description,
        @JsonProperty("pack_price") @NotNull @PositiveOrZero Integer price,
        @JsonProperty("is_acquirable") @NotNull Boolean acquirable,
        @JsonProperty("pack_image_url") String imageUrl,
        @JsonProperty("allowed_factions") @NotNull List<@NotBlank String> allowedFactions,
        @JsonProperty("allowed_types") @NotNull List<@NotBlank String> allowedTypes,
        @JsonProperty("allowed_expansions") @NotNull List<@NotBlank String> allowedExpansions,
        @JsonProperty("allowed_rarities") @NotNull List<@NotBlank String> allowedRarities,
        @JsonProperty("mercy_threshold") @Positive Integer mercyThreshold,
        @JsonProperty("mercy_rarity") @Positive Integer mercyRarity,
        @JsonProperty("pack_slots") @NotNull List<@NotNull @Positive Integer> packSlots) {

    /** Preview cards use the exact complete F04 representation, including explicit nullable fields. */
    public record Description(
            @JsonProperty("pack_description") @NotBlank String text,
            @JsonProperty("card_number") @NotNull @Positive Integer cardNumber,
            @JsonProperty("granted_number") @NotNull @PositiveOrZero Integer grantedNumber,
            @JsonProperty("granted_rarity") @NotBlank String grantedRarity,
            @JsonProperty("best_pull_rarity") @NotBlank String bestPullRarity,
            @JsonProperty("example_pull") @NotNull Map<@NotBlank String, @NotNull JsonNode> examplePull) {
    }
}
