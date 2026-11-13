package webapp.user;

import java.time.Instant;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import webapp.auth.UserProfile;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTests {
    private final UserRepository users = mock(UserRepository.class);
    private final UserService service = new UserService(users);

    @Test
    void returnsPublicProfileForExistingUser() {
        PublicUserProfile expected = new PublicUserProfile(12, "RiftPlayer", 1000, null,
                Instant.parse("2026-11-13T12:00:00Z"));
        when(users.findPublicProfile(12)).thenReturn(Optional.of(expected));

        assertThat(service.getPublicProfile(12)).isEqualTo(expected);
    }

    @Test
    void returnsNotFoundForMissingOrDeletedUser() {
        when(users.findPublicProfile(12)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getPublicProfile(12))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(exception -> ((ResponseStatusException) exception).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void locksAuthenticatedUserAndAppliesOnlyProvidedFields() {
        UserProfile authenticated = new UserProfile(12, "OldName", "old@example.invalid",
                "USER", 1000, "https://example.invalid/old.png");
        UserProfile updated = new UserProfile(12, "NewName", "new@example.invalid",
                "USER", 1000, "https://example.invalid/old.png");
        when(users.lockActiveProfile(12)).thenReturn(Optional.of(authenticated), Optional.of(updated));
        UserProfileUpdateRequest request = new UserProfileUpdateRequest(" NewName ", " NEW@EXAMPLE.INVALID ", null);

        assertThat(service.updateProfile(authenticated, request)).isEqualTo(updated);

        verify(users).updateProfile(12, "NewName", "new@example.invalid",
                "https://example.invalid/old.png");
        verify(users, org.mockito.Mockito.times(2)).lockActiveProfile(12);
    }

    @Test
    void clearsIconWhenEmptyStringIsProvided() {
        UserProfile authenticated = new UserProfile(12, "Player", "player@example.invalid", "USER", 1000, "old");
        UserProfile updated = new UserProfile(12, "Player", "player@example.invalid", "USER", 1000, null);
        when(users.lockActiveProfile(12)).thenReturn(Optional.of(authenticated), Optional.of(updated));

        service.updateProfile(authenticated, new UserProfileUpdateRequest(null, null, ""));

        verify(users).updateProfile(12, "Player", "player@example.invalid", null);
    }

    @Test
    void mapsUniqueConflictsToConflictStatus() {
        UserProfile authenticated = new UserProfile(12, "Player", "player@example.invalid", "USER", 1000, null);
        when(users.lockActiveProfile(12)).thenReturn(Optional.of(authenticated));
        doThrow(new DuplicateKeyException("duplicate")).when(users)
                .updateProfile(12, "Taken", "player@example.invalid", null);

        assertThatThrownBy(() -> service.updateProfile(authenticated,
                new UserProfileUpdateRequest("Taken", null, null)))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(exception -> ((ResponseStatusException) exception).getStatusCode())
                .isEqualTo(HttpStatus.CONFLICT);
    }
}
