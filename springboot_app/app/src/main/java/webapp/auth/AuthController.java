package webapp.auth;

import java.time.Duration;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Adapts F01 sessions to HTTP by sending the access JWT in JSON and the refresh secret in a cookie. */
@RestController
@RequestMapping("/api/auth")
@SecurityScheme(name = "bearerAuth", type = SecuritySchemeType.HTTP, scheme = "bearer", bearerFormat = "JWT")
class AuthController {
    
    private static final String REFRESH_COOKIE = "__Host-cinder-refresh";

    private final AuthenticationService authentication;

    AuthController(AuthenticationService authentication) {
        this.authentication = authentication;
    }

    /**
     * Provides the token the client can read while the matching CSRF cookie stays HttpOnly.
     *
     * @param csrfToken the request token resolved by Spring Security
     * @return the header name and token to send on cookie-writing requests
     */
    @GetMapping("/csrf-tokens")
    @Operation(summary = "Get the CSRF token for browser authentication requests")
    CsrfResponse csrfToken(@Parameter(hidden = true) CsrfToken csrfToken) {
        return new CsrfResponse(csrfToken.getHeaderName(), csrfToken.getToken());
    }

    /**
     * Starts the browser session once the player and their inventory have been created.
     *
     * @param request registration data from the client
     * @return HTTP 201 with the access credentials, profile, and refresh cookie
     */
    @PostMapping("/users")
    @Operation(summary = "Create a player account and sign in")
    @Parameter(name = SecurityConfiguration.CSRF_HEADER, in = ParameterIn.HEADER, required = true)
    ResponseEntity<SessionResponse> register(@Valid @RequestBody RegistrationRequest request) {
        var session = authentication.register(request.nickname(), request.email(), request.password());
        return sessionResponse(session, HttpStatus.CREATED);
    }

    /**
     * Sets the browser's refresh cookie after the credentials have been verified.
     *
     * @param request login credentials from the client
     * @return HTTP 200 with the access credentials, profile, and refresh cookie
     */
    @PostMapping("/sessions")
    @Operation(summary = "Sign in with email and password")
    @Parameter(name = SecurityConfiguration.CSRF_HEADER, in = ParameterIn.HEADER, required = true)
    ResponseEntity<SessionResponse> login(@Valid @RequestBody LoginRequest request) {
        return sessionResponse(authentication.login(request.email(), request.password()), HttpStatus.OK);
    }

    /**
     * Uses the authenticated principal as the sole source of the profile identity.
     *
     * @param user the profile resolved from the validated bearer session
     * @return the caller's private profile
     */
    @GetMapping("/users/me")
    @Operation(summary = "Get the current user's profile", security = @SecurityRequirement(name = "bearerAuth"))
    UserProfile currentUser(@Parameter(hidden = true) @AuthenticationPrincipal UserProfile user) {
        return user;
    }

    /**
     * Replaces the browser cookie only after a successful refresh transaction.
     *
     * @param refreshToken opaque value of the refresh cookie, or {@code null} when absent
     * @return HTTP 200 with the new access credentials, profile, and replacement cookie
     */
    @PostMapping("/session-renewals")
    @Operation(summary = "Rotate the refresh token and return a new access token")
    @Parameter(name = SecurityConfiguration.CSRF_HEADER, in = ParameterIn.HEADER, required = true)
    ResponseEntity<SessionResponse> renew(@CookieValue(name = REFRESH_COOKIE, required = false) String refreshToken) {
        return sessionResponse(authentication.renew(refreshToken), HttpStatus.OK);
    }

    /**
     * Clears the browser's refresh cookie even when the supplied session is already unusable.
     *
     * @param refreshToken opaque value of the refresh cookie, or {@code null} when absent
     * @return HTTP 204 with an expiring cookie
     */
    @PostMapping("/session-revocations")
    @Operation(summary = "Revoke the current session and expire its refresh cookie")
    @Parameter(name = SecurityConfiguration.CSRF_HEADER, in = ParameterIn.HEADER, required = true)
    ResponseEntity<Void> revoke(@CookieValue(name = REFRESH_COOKIE, required = false) String refreshToken) {
        authentication.revoke(refreshToken);
        return ResponseEntity.noContent().header(HttpHeaders.SET_COOKIE, refreshCookie("", Duration.ZERO)).build();
    }

    private static ResponseEntity<SessionResponse> sessionResponse(AuthenticationService.IssuedSession session,
            HttpStatus status) {
        return ResponseEntity.status(status)
                .header(HttpHeaders.SET_COOKIE, refreshCookie(session.refreshToken(), session.refreshLifetime()))
                .body(new SessionResponse(session.accessToken(), "Bearer", session.expiresIn(), session.user()));
    }

    private static String refreshCookie(String value, Duration maxAge) {
        return ResponseCookie.from(REFRESH_COOKIE, value)
                .path("/").secure(true).httpOnly(true).sameSite("Strict").maxAge(maxAge).build().toString();
    }

    record RegistrationRequest(@NotBlank String nickname, @NotBlank @Email @Size(max = 254) String email,
            @NotBlank @JsonProperty(access = JsonProperty.Access.WRITE_ONLY) String password) {
        @Override
        public String toString() {
            return "RegistrationRequest";
        }
    }

    record LoginRequest(@NotBlank @Email @Size(max = 254) String email,
            @NotBlank @JsonProperty(access = JsonProperty.Access.WRITE_ONLY) String password) {
        @Override
        public String toString() {
            return "LoginRequest";
        }
    }

    record SessionResponse(String accessToken, String tokenType, long expiresIn, UserProfile user) {
        @Override
        public String toString() {
            return "SessionResponse[userId=" + user.id() + "]";
        }
    }

    record CsrfResponse(String headerName, String token) {}
}
