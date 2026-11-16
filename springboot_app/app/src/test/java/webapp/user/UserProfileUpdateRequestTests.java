package webapp.user;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Validation;
import jakarta.validation.Validator;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class UserProfileUpdateRequestTests {
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void normalizesProvidedNicknameAndEmail() throws Exception {
        UserProfileUpdateRequest request = objectMapper.readValue(
                """
                        {"nickname":"  RiftPlayer  ","email":"  PLAYER@Example.Invalid "}
                        """,
                UserProfileUpdateRequest.class);

        assertThat(request.nickname()).isEqualTo("RiftPlayer");
        assertThat(request.email()).isEqualTo("player@example.invalid");
        assertThat(request.iconUrl()).isNull();
        assertThat(validator.validate(request)).isEmpty();
    }

    @Test
    void omittedOrNullFieldsRemainUnchangedAndEmptyIconCanBeCleared() throws Exception {
        UserProfileUpdateRequest withNullIcon = objectMapper.readValue("{\"iconUrl\":null}",
                UserProfileUpdateRequest.class);
        UserProfileUpdateRequest withEmptyIcon = objectMapper.readValue("{\"iconUrl\":\"\"}",
                UserProfileUpdateRequest.class);
        UserProfileUpdateRequest empty = objectMapper.readValue("{}", UserProfileUpdateRequest.class);

        assertThat(withNullIcon.iconUrl()).isNull();
        assertThat(withEmptyIcon.iconUrl()).isEmpty();
        assertThat(empty.nickname()).isNull();
        assertThat(empty.email()).isNull();
        assertThat(empty.iconUrl()).isNull();
    }

    @Test
    void rejectsBlankNicknameAndInvalidEmail() {
        UserProfileUpdateRequest request = new UserProfileUpdateRequest("   ", "not-an-email", null);

        assertThat(validator.validate(request)).hasSize(2);
    }

    @Test
    void enforcesTheTrimmedNicknameLengthLimit() {
        UserProfileUpdateRequest maximum = new UserProfileUpdateRequest("x".repeat(50), null, null);
        UserProfileUpdateRequest tooLong = new UserProfileUpdateRequest("x".repeat(51), null, null);

        assertThat(validator.validate(maximum)).isEmpty();
        assertThat(validator.validate(tooLong)).hasSize(1);
    }
}
