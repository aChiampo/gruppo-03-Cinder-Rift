package webapp.auth;

import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestComponent;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringJUnitConfig(SecurityConfigurationTests.SecurityTestConfig.class)
@WebAppConfiguration
// A fixed, nonproduction key keeps these filter-chain tests independent of environment secrets.
@TestPropertySource(properties = "app.security.jwt-secret=MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=")
class SecurityConfigurationTests {
    @Autowired
    private WebApplicationContext context;

    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    @ParameterizedTest
    @ValueSource(strings = {"GET", "HEAD", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"})
    void adminRoutesRejectUsersForEveryMethod(String method) throws Exception {
        mvc.perform(request(HttpMethod.valueOf(method), "/api/admin/test")
                        .with(user("player").roles("USER")))
                .andExpect(status().isForbidden());
    }

    @ParameterizedTest
    @ValueSource(strings = {"GET", "HEAD", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"})
    void adminRoutesAllowAdminsForEveryMethod(String method) throws Exception {
        mvc.perform(request(HttpMethod.valueOf(method), "/api/admin/test")
                        .with(user("administrator").roles("ADMIN")))
                .andExpect(status().isNoContent());
    }

    @ParameterizedTest
    @ValueSource(strings = {"GET", "HEAD", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"})
    void adminRoutesRequireAuthenticationForEveryMethod(String method) throws Exception {
        mvc.perform(request(HttpMethod.valueOf(method), "/api/admin/test"))
                .andExpect(status().isUnauthorized());
    }

    @ParameterizedTest
    @ValueSource(strings = {"/api/auth/signin", "/api/auth/login",
            "/api/auth/password-reset-requests", "/api/auth/password-resets"})
    void publicAuthenticationWritesRequireCsrfAndValidateAnonymousRequests(String path) throws Exception {
        mvc.perform(post(path).contentType("application/json").content("{}"))
                .andExpect(status().isForbidden());
        mvc.perform(post(path).with(csrf().useInvalidToken()).contentType("application/json").content("{}"))
                .andExpect(status().isForbidden());
        mvc.perform(post(path).with(csrf()).contentType("application/json").content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void registrationRejectsAnOversizedNicknameBeforeCallingTheService() throws Exception {
        var authentication = context.getBean(AuthenticationService.class);
        org.mockito.Mockito.clearInvocations(authentication);
        mvc.perform(post("/api/auth/signin").with(csrf()).contentType("application/json")
                        .content("{\"nickname\":\"" + "a".repeat(51)
                                + "\",\"email\":\"player@example.invalid\",\"password\":\"test password\"}"))
                .andExpect(status().isBadRequest());
        org.mockito.Mockito.verifyNoInteractions(authentication);
    }

    @ParameterizedTest
    @ValueSource(strings = {"/api/auth/signin", "/api/auth/login", "/api/auth/session-renewals",
            "/api/auth/logout", "/api/auth/password-reset-requests", "/api/auth/password-resets"})
    void authenticationWritesRequireCsrfEvenWhenABearerHeaderIsPresent(String path) throws Exception {
        mvc.perform(post(path).header("Authorization", "Bearer invalid-token"))
                .andExpect(status().isForbidden());
    }

    @Test
    void currentProfileRequiresAuthenticationAndReturnsThePrincipal() throws Exception {
        mvc.perform(get("/api/auth/user/me"))
                .andExpect(status().isUnauthorized());

        var profile = new UserProfile(42, "RiftPlayer", "player@example.invalid", "USER", 1000, null);
        var principal = UsernamePasswordAuthenticationToken.authenticated(profile, null, java.util.List.of());
        mvc.perform(get("/api/auth/user/me").with(authentication(principal)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(42))
                .andExpect(jsonPath("$.email").value(profile.email()))
                .andExpect(jsonPath("$.role").value(profile.role()));
    }

    @Test
    void logoutRequiresCsrfAndRevokesTheSessionWithAnExpiredRefreshCookie() throws Exception {
        var refreshCookie = new Cookie("__Host-cinder-refresh", "test-refresh-token");
        mvc.perform(post("/api/auth/logout").cookie(refreshCookie))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/auth/logout").cookie(refreshCookie).with(csrf().useInvalidToken()))
                .andExpect(status().isForbidden());

        var response = mvc.perform(post("/api/auth/logout").cookie(refreshCookie).with(csrf()))
                .andExpect(status().isNoContent())
                .andReturn().getResponse();
        var expiredCookie = response.getCookie("__Host-cinder-refresh");
        assertThat(expiredCookie).isNotNull();
        assertThat(expiredCookie.getValue()).isEmpty();
        assertThat(expiredCookie.getMaxAge()).isZero();
        assertThat(expiredCookie.getPath()).isEqualTo("/");
        assertThat(expiredCookie.getSecure()).isTrue();
        assertThat(expiredCookie.isHttpOnly()).isTrue();
        assertThat(expiredCookie.getAttribute("SameSite")).isEqualTo("Strict");
        verify(context.getBean(AuthenticationService.class)).revoke("test-refresh-token");
    }

    @Test
    void anonymousCallerCanRequestAndCompleteRecoveryWithoutReceivingCredentials() throws Exception {
        mvc.perform(post("/api/auth/password-reset-requests").with(csrf()).contentType("application/json")
                        .content("{\"email\":\"player@example.invalid\"}"))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.message").isString())
                .andExpect(jsonPath("$.token").doesNotExist())
                .andExpect(header().doesNotExist("Set-Cookie"));
        mvc.perform(post("/api/auth/password-resets").with(csrf()).contentType("application/json")
                        .content("{\"token\":\"test-token\",\"newPassword\":\"new password\"}"))
                .andExpect(status().isNoContent())
                .andExpect(header().doesNotExist("Set-Cookie"));
    }

    @Test
    void recoveryRejectsInvalidEmailBlankPasswordsAndUnsupportedContent() throws Exception {
        mvc.perform(post("/api/auth/password-reset-requests").with(csrf()).contentType("application/json")
                        .content("{\"email\":\"invalid\"}"))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/auth/password-resets").with(csrf()).contentType("application/json")
                        .content("{\"token\":\"test-token\",\"newPassword\":\"   \"}"))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/auth/password-reset-requests").with(csrf()).contentType("text/plain")
                        .content("email=player@example.invalid"))
                .andExpect(status().isUnsupportedMediaType());
    }

    @TestConfiguration(proxyBeanMethods = false)
    @EnableWebMvc
    @Import({SecurityConfiguration.class, AuthController.class})
    static class SecurityTestConfig {
        @Bean
        AccountRepository accounts() {
            return mock(AccountRepository.class);
        }

        @Bean
        AuthenticationService authentication() {
            return mock(AuthenticationService.class);
        }

        @Bean
        AdminTestController adminTestController() {
            return new AdminTestController();
        }
    }

    // An actual handler distinguishes authorized access from an unrelated 404 for a missing endpoint.
    @TestComponent
    @RestController
    static class AdminTestController {
        @RequestMapping(value = "/api/admin/test", method = {RequestMethod.GET, RequestMethod.HEAD,
                RequestMethod.POST, RequestMethod.PUT, RequestMethod.PATCH, RequestMethod.DELETE, RequestMethod.OPTIONS})
        ResponseEntity<Void> adminResource() {
            return ResponseEntity.noContent().build();
        }
    }
}
