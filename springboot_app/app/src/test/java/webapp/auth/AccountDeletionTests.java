package webapp.auth;

import java.time.Clock;
import java.time.Duration;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Uses the real controller, service and security chain with simulated database states. */
@SpringJUnitConfig(AccountDeletionTests.DeletionTestConfiguration.class)
@WebAppConfiguration
@TestPropertySource(properties = "app.security.jwt-secret=MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=")
class AccountDeletionTests {
    @Autowired private WebApplicationContext context;
    @Autowired private AccountRepository accounts;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        reset(accounts);
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    @ParameterizedTest
    @ValueSource(strings = {"USER", "ADMIN"})
    void eitherRoleCanDeleteTheirOwnAccountAndExpireTheirCookie(String role) throws Exception {
        var caller = profile(1, role);
        when(accounts.lockAccountsForDeletion(1, 1))
                .thenReturn(List.of(new AccountRepository.AccountState(1, role, false)));
        var response = mvc.perform(delete("/api/auth/users/1").with(as(caller)).with(csrf()))
                .andExpect(status().isNoContent()).andReturn().getResponse();
        var cookie = response.getCookie("__Host-cinder-refresh");
        assertThat(cookie).isNotNull();
        assertThat(cookie.getValue()).isEmpty();
        assertThat(cookie.getMaxAge()).isZero();
        assertThat(cookie.getPath()).isEqualTo("/");
        assertThat(cookie.getSecure()).isTrue();
        assertThat(cookie.isHttpOnly()).isTrue();
        assertThat(cookie.getAttribute("SameSite")).isEqualTo("Strict");
        verify(accounts).revokeAllRefreshTokens(eq(1), any());
        verify(accounts).deletePasswordResetTokens(1);
        verify(accounts).markDeleted(1);
    }

    @ParameterizedTest
    @ValueSource(strings = {"USER", "ADMIN", "OTHER"})
    void adminCanDeleteAnyTargetRole(String targetRole) throws Exception {
        when(accounts.lockAccountsForDeletion(1, 2)).thenReturn(List.of(
                new AccountRepository.AccountState(1, "ADMIN", false),
                new AccountRepository.AccountState(2, targetRole, false)));
        mvc.perform(delete("/api/auth/users/2").with(as(profile(1, "ADMIN"))).with(csrf()))
                .andExpect(status().isNoContent())
                .andExpect(header().doesNotExist(HttpHeaders.SET_COOKIE));
        verify(accounts).revokeAllRefreshTokens(eq(2), any());
        verify(accounts).deletePasswordResetTokens(2);
        verify(accounts).markDeleted(2);
    }

    @Test
    void adminCanRepeatDeletionOfAnAlreadyDeletedTarget() throws Exception {
        when(accounts.lockAccountsForDeletion(1, 2)).thenReturn(List.of(
                new AccountRepository.AccountState(1, "ADMIN", false),
                new AccountRepository.AccountState(2, "USER", true)));
        mvc.perform(delete("/api/auth/users/2").with(as(profile(1, "ADMIN"))).with(csrf()))
                .andExpect(status().isNoContent());
    }

    @ParameterizedTest
    @ValueSource(ints = {2, Integer.MAX_VALUE})
    void userCannotDeleteOrLookUpAnotherAccount(int target) throws Exception {
        mvc.perform(delete("/api/auth/users/{id}", target).with(as(profile(1, "USER"))).with(csrf()))
                .andExpect(status().isForbidden());
        verifyNoInteractions(accounts);
    }

    @Test
    void adminReceivesNotFoundForAnUnknownTarget() throws Exception {
        when(accounts.lockAccountsForDeletion(1, 2))
                .thenReturn(List.of(new AccountRepository.AccountState(1, "ADMIN", false)));
        mvc.perform(delete("/api/auth/users/2").with(as(profile(1, "ADMIN"))).with(csrf()))
                .andExpect(status().isNotFound());
    }

    @Test
    void callerWhoseAdminRoleChangedCanOnlyDeleteThemselves() throws Exception {
        when(accounts.lockAccountsForDeletion(1, 2)).thenReturn(List.of(
                new AccountRepository.AccountState(1, "USER", false),
                new AccountRepository.AccountState(2, "USER", false)));
        mvc.perform(delete("/api/auth/users/2").with(as(profile(1, "ADMIN"))).with(csrf()))
                .andExpect(status().isForbidden());
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void callerWhoseAccountWasDeletedOrRoleBecameUnsupportedIsRejected(boolean deleted) throws Exception {
        String role = deleted ? "ADMIN" : "OTHER";
        when(accounts.lockAccountsForDeletion(1, 1))
                .thenReturn(List.of(new AccountRepository.AccountState(1, role, deleted)));
        mvc.perform(delete("/api/auth/users/1").with(as(profile(1, "ADMIN"))).with(csrf()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void deletionRequiresAuthenticationAnAllowedRoleAndCsrf() throws Exception {
        mvc.perform(delete("/api/auth/users/1").with(csrf())).andExpect(status().isUnauthorized());
        mvc.perform(delete("/api/auth/users/1").with(as(profile(1, "USER"))))
                .andExpect(status().isForbidden());
        mvc.perform(delete("/api/auth/users/1").with(as(profile(1, "USER"))).with(csrf().useInvalidToken()))
                .andExpect(status().isForbidden());
        mvc.perform(delete("/api/auth/users/1").with(as(profile(1, "OTHER"))).with(csrf()))
                .andExpect(status().isForbidden());
        verifyNoInteractions(accounts);
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "-1", "invalid", "2147483648"})
    void targetIdMustBeAPositiveInt(String target) throws Exception {
        mvc.perform(delete("/api/auth/users/{id}", target).with(as(profile(1, "ADMIN"))).with(csrf()))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(accounts);
    }

    private static UserProfile profile(int id, String role) {
        return new UserProfile(id, "Player", "player@example.invalid", role, 1000, null);
    }

    private static RequestPostProcessor as(UserProfile caller) {
        return authentication(UsernamePasswordAuthenticationToken.authenticated(caller, null,
                List.of(new SimpleGrantedAuthority("ROLE_" + caller.role()))));
    }

    @TestConfiguration(proxyBeanMethods = false)
    @EnableWebMvc
    @Import({SecurityConfiguration.class, AuthController.class})
    static class DeletionTestConfiguration {
        @Bean
        AccountRepository accounts() {
            return mock(AccountRepository.class);
        }

        @Bean
        AuthenticationService authentication(AccountRepository accounts, PasswordEncoder passwords,
                JwtEncoder jwtEncoder, Clock clock) {
            return new AuthenticationService(accounts, passwords, jwtEncoder, clock,
                    Duration.ofMinutes(15), Duration.ofDays(14));
        }
    }
}
