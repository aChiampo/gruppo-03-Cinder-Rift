package webapp.pack;

import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import tools.jackson.databind.JsonNode;
import webapp.auth.UserProfile;

/** The principal exclusively determines inventory ownership; ADMIN has no implicit USER role. */
@RestController
@RequestMapping("/api/users/me")
@PreAuthorize("hasRole('USER')")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Pack openings", description = "Own inventory and atomic deferred pack openings")
public class PackOpeningController {
    private final PackOpeningService opening;

    public PackOpeningController(PackOpeningService opening) { this.opening = opening; }

    @GetMapping("/inventory")
    @Operation(summary = "Get the authenticated user's resource balances")
    public InventoryView inventory(@Parameter(hidden = true) @AuthenticationPrincipal UserProfile user) {
        return opening.inventory(user.id());
    }

    @GetMapping("/inventory/packs")
    @Operation(summary = "Get all owned packs, including packs no longer acquirable")
    public List<OwnedPackView> packs(@Parameter(hidden = true) @AuthenticationPrincipal UserProfile user) {
        return opening.ownedPacks(user.id());
    }

    @PostMapping("/pack-openings")
    @Operation(summary = "Open one to ten owned packs of the same type atomically",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(required = true,
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = OpenPacksRequest.class))))
    public OpenPacksResponse open(@Parameter(hidden = true) @AuthenticationPrincipal UserProfile user,
            @RequestBody JsonNode body) {
        return opening.open(user.id(), OpenPacksRequest.parse(body));
    }
}
