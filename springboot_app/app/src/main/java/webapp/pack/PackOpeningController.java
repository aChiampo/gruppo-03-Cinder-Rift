package webapp.pack;

import java.util.List;
import org.springframework.http.ProblemDetail;
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
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import tools.jackson.databind.JsonNode;
import webapp.auth.UserProfile;

/** The principal exclusively determines inventory ownership; ADMIN has no implicit USER role. */
@RestController
@RequestMapping("/api/users/me")
@PreAuthorize("hasRole('USER')")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Pack openings", description = "Own inventory and atomic deferred pack openings")
@ApiResponses({
        @ApiResponse(responseCode = "401", description = "UNAUTHENTICATED: Bearer authentication is required",
                content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class))),
        @ApiResponse(responseCode = "403", description = "FORBIDDEN: USER authorization is required",
                content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class))),
        @ApiResponse(responseCode = "500", description = "INTERNAL_ERROR: the operation failed; opening writes are rolled back",
                content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class))),
        @ApiResponse(responseCode = "503", description = "TEMPORARILY_UNAVAILABLE: transient database or service failure",
                content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
})
public class PackOpeningController {
    private final PackOpeningService opening;

    public PackOpeningController(PackOpeningService opening) { this.opening = opening; }

    @GetMapping("/inventory")
    @Operation(summary = "Get the authenticated user's resource balances")
    @ApiResponse(responseCode = "200", description = "Authenticated user's inventory",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = InventoryView.class)))
    public InventoryView inventory(@Parameter(hidden = true) @AuthenticationPrincipal UserProfile user) {
        return opening.inventory(user.id());
    }

    @GetMapping("/inventory/packs")
    @Operation(summary = "Get all owned packs, including packs no longer acquirable")
    @ApiResponse(responseCode = "200", description = "All owned packs ordered by packId; empty array when none are owned",
            content = @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = OwnedPackView.class))))
    public List<OwnedPackView> packs(@Parameter(hidden = true) @AuthenticationPrincipal UserProfile user) {
        return opening.ownedPacks(user.id());
    }

    @PostMapping("/pack-openings")
    @Operation(summary = "Open one to ten owned packs of the same type atomically",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(required = true,
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = OpenPacksRequest.class))))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Entire batch committed",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = OpenPacksResponse.class))),
            @ApiResponse(responseCode = "400", description = "INVALID_REQUEST: positive integer packId and integer quantity from 1 to 10 only",
                    content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "PACK_NOT_FOUND: the pack definition does not exist",
                    content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "409", description = "OPENING_IN_PROGRESS, INSUFFICIENT_PACK_QUANTITY or PACK_CONFIGURATION_INVALID; no opening writes committed",
                    content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "415", description = "UNSUPPORTED_MEDIA_TYPE: use application/json",
                    content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "503", description = "TEMPORARILY_UNAVAILABLE: transient failure or pack definition locked by administration",
                    content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    })
    public OpenPacksResponse open(@Parameter(hidden = true) @AuthenticationPrincipal UserProfile user,
            @RequestBody JsonNode body) {
        return opening.open(user.id(), OpenPacksRequest.parse(body));
    }
}
