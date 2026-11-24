package webapp.friendship;

import com.fasterxml.jackson.annotation.JsonInclude;

/** The six required F13 Problem Details fields, shared by MVC and security filters. */
@JsonInclude(JsonInclude.Include.ALWAYS)
public record FriendshipError(String type, String title, int status, String detail,
        String instance, String code) {
}
