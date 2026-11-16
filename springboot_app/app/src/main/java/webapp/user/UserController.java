package webapp.user;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import webapp.auth.UserProfile;

/** Authenticated public-profile lookup and self-service profile updates. */
@RestController
@RequestMapping("/api/users")
@Validated
@Tag(name = "Users", description = "Public profile lookup and authenticated profile updates")
public class UserController {
    private final UserService users;

    UserController(UserService users) {
        this.users = users;
    }

    @GetMapping("/{userId}")
    @Operation(summary = "Get a user's public profile", security = @SecurityRequirement(name = "bearerAuth"))
    public PublicUserProfile getPublicProfile(@PathVariable @Positive int userId) {
        return users.getPublicProfile(userId);
    }

    @PatchMapping("/me")
    @Operation(summary = "Update the authenticated user's profile",
            security = @SecurityRequirement(name = "bearerAuth"))
    public UserProfile updateProfile(
            @Parameter(hidden = true) @AuthenticationPrincipal UserProfile user,
            @Valid @RequestBody UserProfileUpdateRequest request) {
        return users.updateProfile(user, request);
    }
}
