package webapp.card;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import webapp.auth.UserProfile;

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
}
