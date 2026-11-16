package webapp.user;

import java.time.Instant;

/**
 * Public information exposed for an active user.
 *
 * @param id the user's stable identifier
 * @param nickname the user's display name
 * @param rankingPoints the user's current ranking score
 * @param iconUrl the optional user icon URL
 * @param registrationDate the account registration timestamp
 */
public record PublicUserProfile(int id, String nickname, int rankingPoints, String iconUrl,
        Instant registrationDate) {
}
