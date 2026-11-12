package webapp.auth;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Pattern;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/** Owns the transaction boundaries for account creation and browser session issuance and rotation. */
@Service
class AuthenticationService {
    static final String ISSUER = "cinder-rift";
    static final String SESSION_CLAIM = "sid";

    private static final Pattern REFRESH_TOKEN = Pattern.compile("[A-Za-z0-9_-]{43}");
    private static final int BCRYPT_MAX_BYTES = 72;

    private final AccountRepository accounts;
    private final PasswordEncoder passwords;
    private final JwtEncoder jwtEncoder;
    private final Clock clock;
    private final Duration accessLifetime;
    private final Duration refreshLifetime;
    private final SecureRandom random = new SecureRandom();
    private final String dummyPasswordHash;

    AuthenticationService(AccountRepository accounts, PasswordEncoder passwords, JwtEncoder jwtEncoder,
            Clock clock, @Value("${app.security.access-token-lifetime:15m}") Duration accessLifetime,
            @Value("${app.security.refresh-token-lifetime:14d}") Duration refreshLifetime) {
        if (accessLifetime.toSeconds() < 1 || refreshLifetime.compareTo(accessLifetime) < 0) {
            throw new IllegalArgumentException("Token lifetimes must be positive and refresh must last at least as long as access");
        }
        this.accounts = accounts;
        this.passwords = passwords;
        this.jwtEncoder = jwtEncoder;
        this.clock = clock;
        this.accessLifetime = accessLifetime;
        this.refreshLifetime = refreshLifetime;
        this.dummyPasswordHash = passwords.encode(UUID.randomUUID().toString());
    }

    /**
     * Registers a player and creates their inventory and first session in the same transaction.
     *
     * @param nickname validated display name, before trimming
     * @param email validated email address, before normalization
     * @param password validated plain-text password to hash with BCrypt
     * @return the credentials and profile for the new session
     * @throws ResponseStatusException if the password exceeds BCrypt's byte limit or the email or nickname is taken
     */
    @Transactional
    public IssuedSession register(String nickname, String email, String password) {
        validatePasswordLength(password);
        UserProfile user = accounts.insertPlayer(nickname.strip(), canonicalEmail(email), passwords.encode(password))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT, "Email or nickname is already in use"));
        accounts.createInventory(user.id());
        return issueSession(user, clock.instant());
    }

    /**
     * Starts an independent session for an active account using its email and password.
     *
     * @param email validated email address, before normalization
     * @param password plain-text password supplied for authentication
     * @return the credentials and profile for the new session
     * @throws ResponseStatusException if the password exceeds BCrypt's byte limit or the credentials are invalid
     */
    @Transactional
    public IssuedSession login(String email, String password) {
        validatePasswordLength(password);
        var account = accounts.findByEmail(canonicalEmail(email));
        // Run BCrypt for unknown and deleted accounts too, reducing the timing difference from a wrong password.
        String hash = account.map(AccountRepository.Account::passwordHash).orElse(dummyPasswordHash);
        if (!passwords.matches(password, hash) || account.isEmpty()) {
            throw unauthorized("Invalid credentials");
        }
        return issueSession(account.orElseThrow().profile(), clock.instant());
    }

    /**
     * Consumes one usable refresh token and issues its replacement in the same transaction.
     * A failure to issue the replacement rolls back the revocation.
     *
     * @param refreshToken opaque token from the browser cookie, or {@code null} when absent
     * @return the replacement credentials and the current profile
     * @throws ResponseStatusException if the token is unusable or its account is inactive
     */
    @Transactional
    public IssuedSession renew(String refreshToken) {
        if (!isRefreshToken(refreshToken)) {
            throw unauthorized("Invalid refresh token");
        }
        String hash = hashToken(refreshToken);
        // Lock the account before touching the token row. Read the time after any wait for the lock.
        UserProfile user = accounts.findRefreshTokenOwner(hash).flatMap(accounts::lockActiveProfile)
                .orElseThrow(() -> unauthorized("Invalid refresh token"));
        Instant now = clock.instant();
        if (!accounts.revokeRefreshToken(hash, now)) {
            throw unauthorized("Invalid refresh token");
        }
        return issueSession(user, now);
    }

    /**
     * Revokes the presented session and its linked access token. Repeating the operation is safe.
     *
     * @param refreshToken opaque token from the browser cookie, or {@code null} when absent
     */
    public void revoke(String refreshToken) {
        if (isRefreshToken(refreshToken)) {
            accounts.revokeRefreshToken(hashToken(refreshToken), clock.instant());
        }
    }

    private IssuedSession issueSession(UserProfile user, Instant now) {
        byte[] tokenBytes = new byte[32];
        random.nextBytes(tokenBytes);
        String refreshToken = Base64.getUrlEncoder().withoutPadding().encodeToString(tokenBytes);
        // The persisted digest locates the refresh token. The row ID lets rotation or logout invalidate the JWT.
        int sessionId = accounts.insertRefreshToken(user.id(), hashToken(refreshToken), now, now.plus(refreshLifetime));
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(ISSUER)
                .subject(user.getName())
                .claim(SESSION_CLAIM, Integer.toString(sessionId))
                .issuedAt(now)
                .expiresAt(now.plus(accessLifetime))
                .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        String accessToken = jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
        return new IssuedSession(accessToken, accessLifetime.toSeconds(), user, refreshToken, refreshLifetime);
    }

    private static String canonicalEmail(String email) {
        return email.strip().toLowerCase(Locale.ROOT);
    }

    private static void validatePasswordLength(String password) {
        if (password.getBytes(StandardCharsets.UTF_8).length > BCRYPT_MAX_BYTES) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Password exceeds 72 UTF-8 bytes");
        }
    }

    private static boolean isRefreshToken(String token) {
        return token != null && REFRESH_TOKEN.matcher(token).matches();
    }

    private static String hashToken(String token) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(token.getBytes(StandardCharsets.US_ASCII)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    private static ResponseStatusException unauthorized(String reason) {
        return new ResponseStatusException(HttpStatus.UNAUTHORIZED, reason);
    }

    /** Internal result carrying both credentials so the controller can send the refresh token only as a cookie. */
    record IssuedSession(String accessToken, long expiresIn, UserProfile user, String refreshToken,
            Duration refreshLifetime) {
        @Override
        public String toString() {
            return "IssuedSession[userId=" + user.id() + "]";
        }
    }
}
