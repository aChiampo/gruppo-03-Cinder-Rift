package webapp.pack;

import org.springframework.http.ProblemDetail;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import tools.jackson.databind.JsonNode;
import webapp.auth.UserProfile;

/** Purchases always target the authenticated USER; ADMIN does not inherit that role. */
@RestController
@RequestMapping("/api/users/me/pack-purchases")
@PreAuthorize("hasRole('USER')")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Pack purchases", description = "Spend available Scaglie to acquire unopened packs")
public class PackPurchaseController {
    private final PackPurchaseService purchases;

    public PackPurchaseController(PackPurchaseService purchases) { this.purchases = purchases; }

    @PostMapping
    @Operation(summary = "Purchase unopened packs of one type atomically",
            description = "No deduplication or purchase history. Every successful repeated request is another purchase; do not automatically retry an uncertain outcome.",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(required = true,
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = PurchasePacksRequest.class))))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Debit and unopened-pack credit committed",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = PurchasePacksResponse.class))),
            @ApiResponse(responseCode = "400", description = "INVALID_REQUEST: only positive 32-bit integer packId and quantity",
                    content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "401", description = "UNAUTHENTICATED: Bearer authentication required",
                    content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "403", description = "FORBIDDEN: active USER required",
                    content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "PACK_NOT_FOUND",
                    content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "409", description = "PACK_NOT_ACQUIRABLE, INSUFFICIENT_SCAGLIE or PACK_QUANTITY_LIMIT_EXCEEDED",
                    content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "415", description = "UNSUPPORTED_MEDIA_TYPE: use application/json",
                    content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "500", description = "INTERNAL_ERROR or RESERVATION_STATE_INVALID",
                    content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "503", description = "TEMPORARILY_UNAVAILABLE: contention, transient failure or incomplete reservation storage",
                    content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    })
    public PurchasePacksResponse purchase(@Parameter(hidden = true) @AuthenticationPrincipal UserProfile user,
            @RequestBody JsonNode body) {
        return purchases.purchase(user.id(), PurchasePacksRequest.parse(body));
    }
}
