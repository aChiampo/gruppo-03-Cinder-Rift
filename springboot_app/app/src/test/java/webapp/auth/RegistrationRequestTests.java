package webapp.auth;

import jakarta.validation.Validation;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/** Keeps registration nickname validation consistent with profile updates. */
class RegistrationRequestTests {
    @Test
    void acceptsFiftyCharactersAfterTrimming() {
        var request = request("  " + "a".repeat(50) + "  ");
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            assertThat(factory.getValidator().validate(request)).isEmpty();
        }
        assertThat(request.nickname()).isEqualTo("a".repeat(50));
    }

    @Test
    void rejectsNicknamesLongerThanFiftyCharacters() {
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            assertThat(factory.getValidator().validate(request("a".repeat(51))))
                    .anyMatch(violation -> violation.getPropertyPath().toString().equals("nickname"));
        }
    }

    @Test
    void rejectsMissingAndBlankNicknames() {
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            for (String nickname : new String[] {null, "", "   "}) {
                assertThat(factory.getValidator().validate(request(nickname)))
                        .anyMatch(violation -> violation.getPropertyPath().toString().equals("nickname"));
            }
        }
    }

    private static AuthController.RegistrationRequest request(String nickname) {
        return new AuthController.RegistrationRequest(nickname, "player@example.invalid", "test password");
    }
}
