// Groups this class with the application's authentication code.
package webapp.auth;

import java.time.Clock;
import java.time.Instant;
import java.util.Base64;
import java.util.List; 

import javax.crypto.SecretKey; // Represents the shared secret used to sign and verify JWTs.
import javax.crypto.spec.SecretKeySpec; // Associates the decoded key bytes with a cryptographic algorithm.

import org.springframework.beans.factory.annotation.Value; // Identifies servlet dispatches, including error handling.
import org.springframework.context.annotation.Bean; // Injects values from application configuration.
import org.springframework.context.annotation.Configuration; // Registers a method's result as an object managed by Spring.
import org.springframework.http.HttpHeaders; // Marks a class as a source of Spring bean definitions.
import org.springframework.http.HttpMethod; // Provides constants for standard HTTP header names.
import org.springframework.security.authentication.AbstractAuthenticationToken; // Provides HTTP method constants such as GET and POST.
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken; // Base type for an authentication result.
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity; // Holds a principal and authorities.
import org.springframework.security.config.annotation.web.builders.HttpSecurity; // Enables method access annotations.
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;// Builds the HTTP security rules.
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer; // Enables Spring's web security support.
import org.springframework.security.config.http.SessionCreationPolicy; // Supplies the disable method used below.
import org.springframework.security.core.authority.SimpleGrantedAuthority; // Controls use of servlet HTTP sessions for security.
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder; // Represents one permission or role as a string.
import org.springframework.security.crypto.password.PasswordEncoder; // Hashes and verifies passwords using BCrypt.
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator; // Interface used by the authentication service for passwords.
import org.springframework.security.oauth2.jose.jws.MacAlgorithm; // Combines multiple token validation rules.
import org.springframework.security.oauth2.jwt.Jwt; // Names shared-secret signature algorithms such as HS256.
import org.springframework.security.oauth2.jwt.JwtClaimNames; // Represents a decoded JWT and its claims (token data).
import org.springframework.security.oauth2.jwt.JwtClaimValidator; // Provides standard claim names such as exp (expiration).
import org.springframework.security.oauth2.jwt.JwtDecoder; // Validates one JWT claim using a predicate.
import org.springframework.security.oauth2.jwt.JwtEncoder; // Interface for decoding and validating incoming JWTs.
import org.springframework.security.oauth2.jwt.JwtValidators; // Interface for creating signed outgoing JWTs.
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder; // Creates standard validators, including issuer validation.
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder; // Nimbus-backed implementation of JWT decoding and verification.
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException; // Nimbus-backed implementation of JWT signing.
import org.springframework.security.web.SecurityFilterChain; // Signals unusable bearer credentials.
import org.springframework.security.web.csrf.CookieCsrfTokenRepository; // Defines the security filters applied to HTTP requests.
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher; // Stores the expected CSRF token in a browser cookie.
import org.springframework.security.web.util.matcher.OrRequestMatcher; // Matches request paths and methods.
import org.springframework.web.cors.CorsConfiguration; // Matches when any of its child matchers matches.
import org.springframework.web.cors.UrlBasedCorsConfigurationSource; // Describes allowed cross-origin browser requests (CORS).

import jakarta.servlet.DispatcherType; // Associates CORS rules with URL patterns.

/**
 * Applies cookie and CSRF rules to browser sessions and uses bearer JWTs for protected routes.
 * Authentication identifies the caller; authorization decides which resources that caller may use.
 * CSRF (cross-site request forgery) protection requires a separate token on cookie-changing requests.
 */

@Configuration(proxyBeanMethods = false)
// Enables the HTTP security infrastructure that uses the filter chain declared below.
@EnableWebSecurity
@EnableMethodSecurity
class SecurityConfiguration {
    // Shared with the controller and CORS rules so clients use the same CSRF header name everywhere.
    static final String CSRF_HEADER = "X-XSRF-TOKEN";

    @Bean
    // Spring injects the HTTP builder, account repository, and clock beans by their parameter types.
    SecurityFilterChain securityFilterChain(HttpSecurity http, AccountRepository accounts, Clock clock,
            // Injects the origin list; the value after ':' is the fallback when the property is absent.
            @Value("${app.security.allowed-origins:http://localhost:8082}") List<String> allowedOrigins) {
        var paths = PathPatternRequestMatcher.withDefaults();
        // Public browser authentication writes require CSRF, including password recovery.
        var authenticationWrites = new OrRequestMatcher(
                // Registration creates an account and sets its first refresh cookie.
                paths.matcher(HttpMethod.POST, "/api/auth/users"),
                // Sign-in creates a session and sets a refresh cookie.
                paths.matcher(HttpMethod.POST, "/api/auth/sessions"),
                // Renewal replaces the existing refresh cookie.
                paths.matcher(HttpMethod.POST, "/api/auth/session-renewals"),
                // Sign-out revokes the session and clears its refresh cookie.
                paths.matcher(HttpMethod.POST, "/api/auth/session-revocations"),
                paths.matcher(HttpMethod.POST, "/api/auth/password-reset-requests"),
                paths.matcher(HttpMethod.POST, "/api/auth/password-resets"));

        // The repository saves the expected CSRF token in a cookie and loads it on later requests.
        CookieCsrfTokenRepository csrfTokens = new CookieCsrfTokenRepository();
        // The __Host- prefix requires Secure, Path=/, and no Domain attribute in supporting browsers.
        csrfTokens.setCookieName("__Host-cinder-csrf");
        // The client sends its CSRF token in this header on the matched authentication writes.
        csrfTokens.setHeaderName(CSRF_HEADER);
        // '/' covers the host; Secure restricts cookie transmission to secure connections.
        // HttpOnly prevents JavaScript reading the cookie; SameSite=Strict restricts cross-site cookie sending.
        // The client obtains its readable token from GET /api/auth/csrf-tokens, rather than reading this cookie.
        csrfTokens.setCookieCustomizer(cookie -> cookie.path("/").secure(true).httpOnly(true).sameSite("Strict"));

        // Chained builder calls configure one filter chain; each lambda configures a particular feature.
        return http
                // Applies the browser origin/method/header rules created by the helper at the end of this file.
                .cors(cors -> cors.configurationSource(corsConfiguration(allowedOrigins)))
                .csrf(csrf -> csrf.csrfTokenRepository(csrfTokens).requireCsrfProtectionMatcher(authenticationWrites))
                // Does not create an HTTP session or use one to retrieve authentication; each request supplies credentials.
                // The application still stores refresh sessions in the database, which is separate from HttpSession.
                .sessionManagement(sessions -> sessions.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // Disables remembering the requested URL for a redirect after login.
                .requestCache(AbstractHttpConfigurer::disable)
                // Disables Spring's default logout handling; AuthController handles session revocation instead.
                .logout(AbstractHttpConfigurer::disable)
                // Authorization rules are evaluated in order; the first matching rule applies.
                .authorizeHttpRequests(requests -> requests
                        // Allows servlet error dispatches so handling an error does not require a new authentication.
                        .dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()
                        // These routes require no authenticated user; permitAll does not bypass the CSRF check.
                        .requestMatchers(authenticationWrites).permitAll()
                        // Allows GET access to CSRF bootstrap, health checks, and the API documentation/UI.
                        .requestMatchers(HttpMethod.GET, "/api/auth/csrf-tokens", "/actuator/health",
                                // Includes the OpenAPI document and nested Swagger UI assets.
                                "/v3/api-docs", "/v3/api-docs/**", "/swagger-ui.html", "/swagger-ui/**").permitAll()
                        // Requires ADMIN for every HTTP method under /api/admin, including GET and PATCH.
                        // hasRole("ADMIN") checks the ROLE_ADMIN authority assigned during authentication.
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        // Every request not matched above must have a valid authenticated principal.
                        .anyRequest().authenticated())
                // Reads Authorization: Bearer <JWT> and validates it using the JwtDecoder bean below.
                .oauth2ResourceServer(oauth -> oauth.jwt(jwt -> jwt.jwtAuthenticationConverter(
                        // After JWT validation, the lambda loads the current user/session and creates authentication.
                        token -> authenticate(token, accounts, clock))))
                // Builds and returns the filter chain Spring will use to process requests.
                .build();
    }

    // Exposes one password encoder for injection into AuthenticationService.
    @Bean
    PasswordEncoder passwordEncoder() {
        // BCrypt hashes new passwords with a random salt and can verify a password against a stored hash.
        return new BCryptPasswordEncoder();
    }

    // Exposes a shared time source instead of hard-coding the current time throughout authentication code.
    @Bean
    Clock clock() {
        // Uses the system clock with UTC as its time zone.
        return Clock.systemUTC();
    }

    // Creates the shared signing/verification key once as a Spring bean.
    @Bean
    // This required property contains Base64 text, not a literal password or a key embedded in source code.
    SecretKey jwtKey(@Value("${app.security.jwt-secret}") String encodedKey) {
        byte[] key;
        // Decoding can fail if the configured text is not valid Base64.
        try {
            // Base64 is a text representation, not encryption; decoding recovers the original secret bytes.
            key = Base64.getDecoder().decode(encodedKey);
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("The JWT signing key must be Base64-encoded");
        }
        // HS256 requires at least 256 bits (32 bytes); check decoded bytes, not Base64 character count.
        if (key.length < 32) {
            // Rejects an undersized key while the application is starting.
            throw new IllegalArgumentException("The JWT signing key must contain at least 32 bytes");
        }
        // Wraps the bytes as an HMAC-SHA256 key shared by the encoder and decoder.
        return new SecretKeySpec(key, "HmacSHA256");
    }

    // Makes the JWT signing component available to AuthenticationService.
    @Bean
    // Spring supplies the SecretKey bean produced by jwtKey(...).
    JwtEncoder jwtEncoder(SecretKey jwtKey) {
        // HS256 signs tokens using HMAC-SHA256; signing protects integrity but does not encrypt their contents.
        return NimbusJwtEncoder.withSecretKey(jwtKey).algorithm(MacAlgorithm.HS256).build();
    }

    // Makes the JWT verification component available to the resource-server filters.
    @Bean
    // The same key verifies signatures; the injected clock is used by the strict expiration rule below.
    JwtDecoder jwtDecoder(SecretKey jwtKey, Clock clock) {
        // Accepts HS256 signatures verified with this shared secret.
        var decoder = NimbusJwtDecoder.withSecretKey(jwtKey).macAlgorithm(MacAlgorithm.HS256).build();
        // Require an expiration and enforce it exactly, even within the default clock-skew window.
        // DelegatingOAuth2TokenValidator requires all of these validators to succeed.
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
                // Checks the expected issuer plus the default time checks (expiration and not-before when present).
                JwtValidators.createDefaultWithIssuer(AuthenticationService.ISSUER),
                // 'exp' must exist and be strictly later than now; a token expiring exactly now is invalid.
                new JwtClaimValidator<Instant>(JwtClaimNames.EXP, expiry -> expiry != null && expiry.isAfter(clock.instant()))));
        // Returns the decoder with both signature verification and the configured claim validation.
        return decoder;
    }

    // Converts a validated JWT into Spring's authenticated identity; this helper needs no class instance.
    private static AbstractAuthenticationToken authenticate(Jwt jwt, AccountRepository accounts, Clock clock) {
        // The standard 'sub' (subject) claim contains this application's numeric user ID.
        int userId = parseId(jwt.getSubject());
        // The custom 'sid' claim contains the ID of the database refresh-token row for this session.
        int sessionId = parseId(jwt.getClaimAsString(AuthenticationService.SESSION_CLAIM));
        // Read current account and session state so revocations and role changes take effect on the next request.
        // A valid signature alone is insufficient: the user and their matching session must still be active.
        UserProfile user = accounts.findActiveSessionProfile(userId, sessionId, clock.instant())
                // An empty Optional becomes an authentication failure rather than an authenticated user.
                .orElseThrow(() -> new InvalidBearerTokenException("The session is no longer active"));
        // Uses the profile as principal (caller identity) and null credentials; no password is needed here.
        return UsernamePasswordAuthenticationToken.authenticated(user, null,
                // Grants exactly the current database role, e.g. ROLE_USER or ROLE_ADMIN; no hierarchy is added.
                List.of(new SimpleGrantedAuthority("ROLE_" + user.role())));
    }

    // Accepts only positive IDs that fit in a Java int before they can be used in a database lookup.
    private static int parseId(String claim) {
        // Null, nonnumeric, and out-of-range strings cause Integer.parseInt to throw NumberFormatException.
        try {
            // Parses a base-10 integer from the JWT claim's text.
            int id = Integer.parseInt(claim);
            // Zero and negative numbers are outside this application's accepted ID range.
            if (id > 0) {
                // Successful parsing and a positive value allow the caller to continue authentication.
                return id;
            }
        // Treats parsing failures uniformly; the exception is intentionally not logged or returned.
        } catch (NumberFormatException ignored) {
            // Invalid claims must fail authentication before any database query.
        }
        // Both malformed strings and nonpositive IDs reach the same bearer-token authentication error.
        throw new InvalidBearerTokenException("Invalid session claims");
    }

    // Builds the CORS policy used above; it is a helper method rather than a separate Spring bean.
    private static UrlBasedCorsConfigurationSource corsConfiguration(List<String> allowedOrigins) {
        // CORS tells browsers which other origins may interact with these API routes.
        var config = new CorsConfiguration();
        // An origin consists of scheme, host, and port; use the configured exact origins.
        config.setAllowedOrigins(allowedOrigins);
        // Lists methods accepted by CORS, including OPTIONS for browser preflight checks.
        config.setAllowedMethods(List.of("GET", "HEAD", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        // Allows bearer credentials, JSON content types, and the custom CSRF header in cross-origin requests.
        config.setAllowedHeaders(List.of(HttpHeaders.AUTHORIZATION, HttpHeaders.CONTENT_TYPE, CSRF_HEADER));
        // Allows credentialed browser requests (including cookies); the client must also opt in to sending them.
        config.setAllowCredentials(true);

        // Maps the policy to URL patterns so Spring can find the configuration for each request.
        var source = new UrlBasedCorsConfigurationSource();
        // Applies this CORS policy to API routes; it does not grant authorization to those routes.
        source.registerCorsConfiguration("/api/**", config);
        // Supplies the configured URL-to-policy mapping to the HTTP security builder.
        return source;
    }
}
