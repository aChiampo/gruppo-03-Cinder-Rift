package webapp.card;

import jakarta.validation.constraints.NotBlank;

/** An artwork variant included when an administrator creates or replaces a card. */
public record CardArtworkWriteRequest(@NotBlank String artCode, String imageUrl) {
}
