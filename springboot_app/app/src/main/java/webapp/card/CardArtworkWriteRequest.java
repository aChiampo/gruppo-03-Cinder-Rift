package webapp.card;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import io.swagger.v3.oas.annotations.media.Schema;

/** An artwork variant included when an administrator creates or replaces a card. */
public record CardArtworkWriteRequest(
        @NotBlank @Pattern(regexp = "STANDARD|EXTENDED_ART|FULL_ART|ALTERNATE_ART")
        @Schema(allowableValues = {"STANDARD", "EXTENDED_ART", "FULL_ART", "ALTERNATE_ART"}) String artCode,
        String imageUrl) {
}
