package webapp.user;

import java.util.Locale;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

/** Optional profile fields supplied by an authenticated user. */
public record UserProfileUpdateRequest(@Size(max = 50) String nickname,
        @Email @Size(max = 254) String email, String iconUrl) {

    public UserProfileUpdateRequest {
        if (nickname != null) {
            nickname = nickname.strip();
        }
        if (email != null) {
            email = email.strip().toLowerCase(Locale.ROOT);
        }
    }

    @AssertTrue(message = "nickname must not be blank when provided")
    @JsonIgnore
    public boolean isNicknameValid() {
        return nickname == null || !nickname.isBlank();
    }

    @AssertTrue(message = "email must not be blank when provided")
    @JsonIgnore
    public boolean isEmailValid() {
        return email == null || !email.isBlank();
    }
}
