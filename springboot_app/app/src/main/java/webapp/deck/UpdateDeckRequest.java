package webapp.deck;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public record UpdateDeckRequest(@NotNull List<@NotNull @Valid DeckEntryRequest> entries,
        @JsonProperty("isChallengeable") Boolean challengeable) {
    public UpdateDeckRequest {
        challengeable = Boolean.TRUE.equals(challengeable);
    }
}
