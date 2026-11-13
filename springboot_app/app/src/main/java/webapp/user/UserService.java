package webapp.user;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import webapp.auth.UserProfile;

/** Applies user profile lookup and update rules. */
@Service
public class UserService {
    private final UserRepository users;

    UserService(UserRepository users) {
        this.users = users;
    }

    @Transactional(readOnly = true)
    public PublicUserProfile getPublicProfile(int userId) {
        return users.findPublicProfile(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    }

    @Transactional
    public UserProfile updateProfile(UserProfile authenticatedUser, UserProfileUpdateRequest request) {
        UserProfile current = users.lockActiveProfile(authenticatedUser.id())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        String nickname = request.nickname() == null ? current.nickname() : request.nickname();
        String email = request.email() == null ? current.email() : request.email();
        String iconUrl = request.iconUrl() == null
                ? current.iconUrl()
                : request.iconUrl().isEmpty() ? null : request.iconUrl();

        try {
            users.updateProfile(current.id(), nickname, email, iconUrl);
        } catch (DuplicateKeyException exception) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email or nickname is already in use");
        }

        return users.lockActiveProfile(current.id())
                .orElseThrow(() -> new IllegalStateException("The updated active user is missing"));
    }
}
