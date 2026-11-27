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

/** Exposes HTTP + polling endpoints for direct friendly PvP challenges between accepted friends. */
@RestController
@RequestMapping("/api/pvp/challenges")
public class PvpChallengeController {
    private final PvpChallengeService service;
    private final Clock clock;

    public PvpChallengeController(PvpChallengeService service, Clock clock) {
        this.service = service;
        this.clock = clock;
    }

    @PostMapping(consumes = "application/json", produces = "application/json")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @Operation(summary = "Send a direct friendly PvP challenge to an accepted friend",
            security = @SecurityRequirement(name = "bearerAuth"),
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(required = true,
                    content = @Content(schema = @Schema(implementation = CreateChallengeRequest.class))),
            responses = {
                @ApiResponse(responseCode = "201", description = "Friendly challenge created in PENDING state"),
                @ApiResponse(responseCode = "400", description = "Invalid payload or self-challenge"),
                @ApiResponse(responseCode = "403", description = "Target user is not an accepted friend"),
                @ApiResponse(responseCode = "422", description = "Deck is not playable")})
    public ResponseEntity<PvpChallengeResponse> create(
            @Parameter(hidden = true) @AuthenticationPrincipal UserProfile user,
            @RequestBody JsonNode body,
            HttpServletRequest request) {
        if (body == null || !body.isObject() || body.size() != 2
                || !body.has("opponentUserId") || !body.has("deckId")) {
            throw badCreateRequest();
        }
        int opponentUserId = positiveInt(body.get("opponentUserId"));
        int deckId = positiveInt(body.get("deckId"));
        if (opponentUserId <= 0 || deckId <= 0) {
            throw badCreateRequest();
        }
        PvpChallengeResponse created = service.createChallenge(user, opponentUserId, deckId, budget(request));
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping(produces = "application/json")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @Operation(summary = "List pending incoming and outgoing friendly challenges for the authenticated user",
            security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<PvpChallengesResponse> list(
            @Parameter(hidden = true) @AuthenticationPrincipal UserProfile user) {
        return ResponseEntity.ok(service.listChallenges(user));
    }

    @GetMapping(value = "/{challengeId}", produces = "application/json")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @Operation(summary = "Poll the status of a direct friendly challenge",
            security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<PvpChallengeResponse> get(
            @Parameter(hidden = true) @AuthenticationPrincipal UserProfile user,
            @PathVariable int challengeId) {
        return ResponseEntity.ok(service.getChallenge(user, challengeId));
    }

    @PostMapping(value = "/{challengeId}/accept", consumes = "application/json", produces = "application/json")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @Operation(summary = "Accept a pending friendly challenge and execute the friendly PvP battle",
            security = @SecurityRequirement(name = "bearerAuth"),
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(required = true,
                    content = @Content(schema = @Schema(implementation = BattleController.PublicRequest.class))),
            responses = {
                @ApiResponse(responseCode = "200", description = "Challenge completed and battle report returned"),
                @ApiResponse(responseCode = "403", description = "Caller is not the challenged opponent"),
                @ApiResponse(responseCode = "404", description = "Challenge or deck not found"),
                @ApiResponse(responseCode = "409", description = "Challenge is not pending or battle in progress"),
                @ApiResponse(responseCode = "422", description = "Deck is not playable")})
    public ResponseEntity<PvpChallengeResponse> accept(
            @Parameter(hidden = true) @AuthenticationPrincipal UserProfile user,
            @PathVariable int challengeId,
            @RequestBody JsonNode body,
            HttpServletRequest request) {
        if (body == null || !body.isObject() || body.size() != 1 || !body.has("deckId")) {
            throw badAcceptRequest();
        }
        int deckId = positiveInt(body.get("deckId"));
        if (deckId <= 0) {
            throw badAcceptRequest();
        }
        return ResponseEntity.ok(service.acceptChallenge(user, challengeId, deckId, budget(request)));
    }

    @PostMapping(value = "/{challengeId}/decline", produces = "application/json")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @Operation(summary = "Decline (opponent) or cancel (challenger) a pending friendly challenge",
            security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<PvpChallengeResponse> decline(
            @Parameter(hidden = true) @AuthenticationPrincipal UserProfile user,
            @PathVariable int challengeId) {
        return ResponseEntity.ok(service.declineChallenge(user, challengeId));
    }

    private BattleBudget budget(HttpServletRequest request) {
        BattleBudget budget = (BattleBudget) request.getAttribute(BattleBudget.ATTRIBUTE);
        return budget == null ? BattleBudget.start(clock) : budget;
    }

    private static int positiveInt(JsonNode node) {
        if (node == null || !node.isIntegralNumber() || !node.canConvertToInt()) {
            return -1;
        }
        return node.intValue();
    }

    private static BattleFailure badCreateRequest() {
        return new BattleFailure(HttpStatus.BAD_REQUEST, "Bad Request",
                "The request must contain only positive integer opponentUserId and deckId.");
    }

    private static BattleFailure badAcceptRequest() {
        return new BattleFailure(HttpStatus.BAD_REQUEST, "Bad Request",
                "The request must contain only a positive integer deckId.");
    }

    @ExceptionHandler(BattleFailure.class)
    public ResponseEntity<ProblemDetail> handle(BattleFailure failure, HttpServletRequest request) {
        return failure.response(request.getRequestURI());
    }

    public record CreateChallengeRequest(int opponentUserId, int deckId) {
    }
}
