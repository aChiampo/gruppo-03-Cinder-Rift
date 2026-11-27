package webapp.pvp;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Clock;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tools.jackson.databind.JsonNode;
import webapp.auth.UserProfile;
import webapp.battle.BattleBudget;
import webapp.battle.BattleController;
import webapp.battle.BattleFailure;

/** Exposes HTTP + polling endpoints for the ranked PvP matchmaking queue. */
@RestController
@RequestMapping("/api/pvp/queue")
public class PvpQueueController {
    private final PvpQueueService service;
    private final Clock clock;

    public PvpQueueController(PvpQueueService service, Clock clock) {
        this.service = service;
        this.clock = clock;
    }

    @PostMapping(consumes = "application/json", produces = "application/json")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @Operation(summary = "Enter the ranked matchmaking queue with an owned, playable deck",
            security = @SecurityRequirement(name = "bearerAuth"),
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(required = true,
                    content = @Content(schema = @Schema(implementation = BattleController.PublicRequest.class))),
            responses = {
                @ApiResponse(responseCode = "200", description = "Match found and battle completed"),
                @ApiResponse(responseCode = "202", description = "Enqueued and waiting for an opponent"),
                @ApiResponse(responseCode = "409", description = "User already in queue or battle in progress"),
                @ApiResponse(responseCode = "422", description = "Deck is not playable")})
    public ResponseEntity<PvpQueueResponse> enqueue(
            @Parameter(hidden = true) @AuthenticationPrincipal UserProfile user,
            @RequestBody JsonNode body,
            HttpServletRequest request) {
        if (body == null || !body.isObject() || body.size() != 1 || !body.has("deckId")) {
            throw badRequest();
        }
        JsonNode deckId = body.get("deckId");
        if (!deckId.isIntegralNumber() || !deckId.canConvertToInt() || deckId.intValue() <= 0) {
            throw badRequest();
        }
        BattleBudget budget = (BattleBudget) request.getAttribute(BattleBudget.ATTRIBUTE);
        if (budget == null) {
            budget = BattleBudget.start(clock);
        }
        PvpQueueService.JoinResult result = service.joinQueue(user, deckId.intValue(), budget);
        return ResponseEntity.status(result.status()).body(result.body());
    }

    @GetMapping(value = "/{ticketId}", produces = "application/json")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @Operation(summary = "Poll the status of an owned matchmaking queue ticket",
            security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<PvpQueueResponse> status(
            @Parameter(hidden = true) @AuthenticationPrincipal UserProfile user,
            @PathVariable int ticketId) {
        return ResponseEntity.ok(service.getTicketStatus(user, ticketId));
    }

    @DeleteMapping(value = "/{ticketId}", produces = "application/json")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @Operation(summary = "Cancel an owned WAITING matchmaking queue ticket",
            security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<PvpQueueResponse> cancel(
            @Parameter(hidden = true) @AuthenticationPrincipal UserProfile user,
            @PathVariable int ticketId) {
        return ResponseEntity.ok(service.cancelTicket(user, ticketId));
    }

    private static BattleFailure badRequest() {
        return new BattleFailure(HttpStatus.BAD_REQUEST, "Bad Request",
                "The request must contain only a positive integer deckId.");
    }

    @ExceptionHandler(BattleFailure.class)
    public ResponseEntity<ProblemDetail> handle(BattleFailure failure, HttpServletRequest request) {
        return failure.response(request.getRequestURI());
    }
}
