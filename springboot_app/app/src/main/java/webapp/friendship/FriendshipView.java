package webapp.friendship;

import java.time.Instant;
import com.fasterxml.jackson.annotation.JsonInclude;

/** Public profile and relationship state relative to the authenticated caller. */
@JsonInclude(JsonInclude.Include.ALWAYS)
public record FriendshipView(int otherUserId, String nickname, String iconUrl,
        String relationship, Instant createdAt, Instant acceptedAt) {
}
