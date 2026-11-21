package webapp.card;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/** Complete editable card definition for the administrative catalog API. */
public record CardWriteRequest(
        @NotBlank String name,
        String description,
        @Positive Integer typeId,
        Integer energyCost,
        Integer power,
        @Positive Integer rarityId,
        Integer attack,
        Integer defense,
        @Positive Integer factionId,
        @Positive Integer expansionId,
        @NotNull List<@Valid CardArtworkWriteRequest> artworks) {
}
