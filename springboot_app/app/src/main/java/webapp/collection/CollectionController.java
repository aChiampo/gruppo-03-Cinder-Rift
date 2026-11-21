package webapp.collection;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import webapp.auth.UserProfile;
import webapp.card.CardPage;
import webapp.card.CardSearchParameters;

/** Exposes only the authenticated user's collection. */
@RestController
@RequestMapping("/api/users/me/collections")
@Validated
@Tag(name = "Collections", description = "The authenticated user's owned card variants")
public class CollectionController {
    private final CollectionService collections;

    public CollectionController(CollectionService collections) {
        this.collections = collections;
    }

    @GetMapping
    @Operation(summary = "Get the authenticated user's collection",
            security = @SecurityRequirement(name = "bearerAuth"))
    public CardPage<CollectionItem> getOwnCollection(
            @Parameter(hidden = true) @AuthenticationPrincipal UserProfile user,
            @Valid @ModelAttribute CardSearchParameters parameters) {
        return collections.findOwnCollection(user.id(), parameters.toCriteria());
    }

    @GetMapping("/{cardId}/{cardArtId}")
    @Operation(summary = "Get one artwork variant from the authenticated user's collection",
            security = @SecurityRequirement(name = "bearerAuth"))
    public CollectionItem getOwnCollectionItem(
            @Parameter(hidden = true) @AuthenticationPrincipal UserProfile user,
            @PathVariable @Positive int cardId,
            @PathVariable @Positive int cardArtId) {
        return collections.findOwnCollectionItem(user.id(), cardId, cardArtId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Collection item not found"));
    }
}
