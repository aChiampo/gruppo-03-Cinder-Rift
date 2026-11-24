package webapp.friendship;

import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import tools.jackson.databind.JsonNode;
import webapp.auth.UserProfile;

/** Bearer-only personal friendship operations from the canonical F13 contract. */
@RestController
@RequestMapping("/api")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Friendships", description = "Personal friend requests and reciprocal friendships")
public class FriendshipController {
    private final FriendshipService friendships;

    FriendshipController(FriendshipService friendships) { this.friendships = friendships; }

    @PostMapping("/friend-requests")
    @Operation(summary = "Send a friend request")
    @ApiResponse(responseCode = "201", description = "Pending request created",
            content = @Content(schema = @Schema(implementation = FriendshipView.class)))
    public ResponseEntity<FriendshipView> send(
            @Parameter(hidden = true) @AuthenticationPrincipal UserProfile user,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(required = true,
                    content = @Content(schema = @Schema(implementation = SendRequest.class)))
            @RequestBody JsonNode body) {
        JsonNode recipient = field(body, "recipientId");
        if (!recipient.isIntegralNumber() || !recipient.canConvertToInt()) throw invalid();
        return ResponseEntity.status(201).body(friendships.send(user.id(), recipient.intValue()));
    }

    @PatchMapping("/friend-requests/{otherUserId}")
    @Operation(summary = "Accept an incoming friend request")
    public FriendshipView accept(@Parameter(hidden = true) @AuthenticationPrincipal UserProfile user,
            @PathVariable String otherUserId,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(required = true,
                    content = @Content(schema = @Schema(implementation = AcceptRequest.class)))
            @RequestBody JsonNode body) {
        int other = id(otherUserId);
        JsonNode status = field(body, "status");
        if (!status.isString()) throw invalid();
        if (!status.stringValue().equals("accepted")) throw validation("Status must be accepted.");
        return friendships.accept(user.id(), other);
    }

    @DeleteMapping("/friend-requests/{otherUserId}")
    @Operation(summary = "Reject an incoming request or cancel an outgoing request")
    @ApiResponse(responseCode = "204", description = "Pending request deleted")
    public ResponseEntity<Void> deleteRequest(@Parameter(hidden = true) @AuthenticationPrincipal UserProfile user,
            @PathVariable String otherUserId) {
        friendships.delete(user.id(), id(otherUserId), false);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me/friend-requests")
    @Operation(summary = "List all incoming or outgoing pending requests")
    public RelationshipList requests(@Parameter(hidden = true) @AuthenticationPrincipal UserProfile user,
            @RequestParam(required = false) String direction) {
        if (!"incoming".equals(direction) && !"outgoing".equals(direction))
            throw validation("Direction must be incoming or outgoing.");
        return new RelationshipList(friendships.list(user.id(), direction));
    }

    @GetMapping("/me/friends")
    @Operation(summary = "List all accepted friendships")
    public RelationshipList friends(@Parameter(hidden = true) @AuthenticationPrincipal UserProfile user) {
        return new RelationshipList(friendships.list(user.id(), "friends"));
    }

    @DeleteMapping("/me/friends/{otherUserId}")
    @Operation(summary = "Remove an accepted friendship")
    @ApiResponse(responseCode = "204", description = "Friendship deleted")
    public ResponseEntity<Void> deleteFriend(@Parameter(hidden = true) @AuthenticationPrincipal UserProfile user,
            @PathVariable String otherUserId) {
        friendships.delete(user.id(), id(otherUserId), true);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/users/{otherUserId}/friendship")
    @Operation(summary = "Get the caller's relationship with another user")
    public FriendshipView relationship(@Parameter(hidden = true) @AuthenticationPrincipal UserProfile user,
            @PathVariable String otherUserId) {
        return friendships.get(user.id(), id(otherUserId));
    }

    private static JsonNode field(JsonNode body, String name) {
        if (body == null || !body.isObject()) throw invalid();
        JsonNode value = body.get(name);
        if (value == null || value.isNull()) throw validation(name + " is required.");
        return value;
    }

    private static int id(String value) {
        try { return Integer.parseInt(value); }
        catch (NumberFormatException exception) { throw invalid(); }
    }

    private static FriendshipFailure invalid() {
        return new FriendshipFailure(400, "INVALID_REQUEST", "The request has an invalid representation.");
    }

    private static FriendshipFailure validation(String detail) {
        return new FriendshipFailure(422, "VALIDATION_ERROR", detail);
    }

    public record RelationshipList(List<FriendshipView> items) { }

    /** Documentation models; tree parsing deliberately avoids permissive scalar coercion. */
    public record SendRequest(@Schema(requiredMode = Schema.RequiredMode.REQUIRED, minimum = "1") Integer recipientId) { }
    public record AcceptRequest(@Schema(requiredMode = Schema.RequiredMode.REQUIRED, allowableValues = "accepted") String status) { }
}
