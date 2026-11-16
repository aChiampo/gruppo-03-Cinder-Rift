package webapp.auth;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.security.core.AuthenticatedPrincipal;

/**
 * The signed-in user's private profile, also used as the Spring Security principal.
 *
 * @param id the stable user ID used as the principal name
 * @param nickname the user's display name
 * @param email the normalized email address
 * @param role the current role code from the database
 * @param rankingPoints the user's current ranking score
 * @param iconUrl the optional user icon URL, or {@code null} when unset
 */
public record UserProfile(int id, String nickname, String email,
        @Schema(allowableValues = {"ADMIN", "USER"}) String role, int rankingPoints, String iconUrl)
        implements AuthenticatedPrincipal {
    /** @return the stable user ID as a string for Spring Security */
    @Override
    @JsonIgnore
    public String getName() {
        return Integer.toString(id);
    }
}
