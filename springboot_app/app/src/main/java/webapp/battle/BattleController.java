package webapp.battle;

import java.time.Clock;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.async.DeferredResult;
import tools.jackson.databind.JsonNode;
import webapp.auth.UserProfile;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;

/** Public F07 boundary: the caller selects a deck, never identity or simulation statistics. */
@RestController
@RequestMapping("/api/battle")
public class BattleController {
    private final BattleService battles;
    private final Clock clock;

    public BattleController(BattleService battles, Clock clock) { this.battles = battles; this.clock = clock; }

    @PostMapping(value = "/pve/{stageId}", consumes = "application/json", produces = "application/json")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @Operation(summary = "Simulate a PvE battle using an owned, playable deck",
            security = @SecurityRequirement(name = "bearerAuth"),
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(required = true,
                    content = @Content(schema = @Schema(implementation = PublicRequest.class))),
            responses = {
                @ApiResponse(responseCode = "200", description = "Battle completed",
                        content = @Content(schema = @Schema(implementation = BattleResponse.class))),
                @ApiResponse(responseCode = "409", description = "Battle in progress or stage event inactive"),
                @ApiResponse(responseCode = "422", description = "Deck is not playable"),
                @ApiResponse(responseCode = "504", description = "Battle deadline exceeded")})
    public DeferredResult<Object> execute(@Parameter(hidden = true) @AuthenticationPrincipal UserProfile user,
            @PathVariable String stageId, @RequestBody JsonNode body, HttpServletRequest request) {
        if (body == null || !body.isObject() || body.size() != 1 || !body.has("deckId")) throw badRequest();
        JsonNode deckId = body.get("deckId");
        if (!deckId.isIntegralNumber() || !deckId.canConvertToInt() || deckId.intValue() <= 0) throw badRequest();
        BattleBudget budget = (BattleBudget) request.getAttribute(BattleBudget.ATTRIBUTE);
        if (budget == null) budget = BattleBudget.start(clock);
        return battles.execute(user, deckId.intValue(), stageId, request.getRequestURI(), budget);
    }

    private static BattleFailure badRequest() {
        return new BattleFailure(HttpStatus.BAD_REQUEST, "Bad Request",
                "The request must contain only a positive integer deckId.");
    }

    @ExceptionHandler(BattleFailure.class)
    public ResponseEntity<ProblemDetail> handle(BattleFailure failure, HttpServletRequest request) {
        return failure.response(request.getRequestURI());
    }

    public record PublicRequest(@Schema(requiredMode = Schema.RequiredMode.REQUIRED, minimum = "1",
            maximum = "2147483647") int deckId) { }
}
