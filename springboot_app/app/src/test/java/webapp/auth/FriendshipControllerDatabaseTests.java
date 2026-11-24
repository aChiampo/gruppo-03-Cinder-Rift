package webapp.auth;

import java.util.UUID;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;
import webapp.friendship.FriendshipService;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Real JWT, security filters, HTTP representations and PostgreSQL state transitions. */
@SpringBootTest(properties = {"app.security.session-token-cleanup-cron=-", "app.security.password-reset-token-cleanup-cron=-"})
@Transactional
class FriendshipControllerDatabaseTests {
    @Autowired WebApplicationContext context;
    @Autowired AuthenticationService authentication;
    @Autowired FriendshipService friendships;
    @Autowired JdbcClient jdbc;
    MockMvc mvc;
    AuthenticationService.IssuedSession first;
    AuthenticationService.IssuedSession second;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        first = user();
        second = user();
    }

    @ParameterizedTest
    @ValueSource(strings = {"USER", "ADMIN"})
    void lifecycleIsPersonalAndIdenticalForBothRoles(String role) throws Exception {
        jdbc.sql("UPDATE users SET id_role = (SELECT id FROM roles WHERE code = :role) WHERE id = :id")
                .param("role", role).param("id", second.user().id()).update();
        int a = first.user().id(), b = second.user().id();
        mvc.perform(get("/api/users/{id}/friendship", a).header("Authorization", bearer(second)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.relationship").value("none"))
                .andExpect(jsonPath("$.createdAt").value(nullValue())).andExpect(jsonPath("$.acceptedAt").value(nullValue()))
                .andExpect(jsonPath("$.iconUrl").value(nullValue())).andExpect(jsonPath("$.email").doesNotExist());
        mvc.perform(post("/api/friend-requests").header("Authorization", bearer(second))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"recipientId\":" + a + ",\"requested_by\":" + a + "}"))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.relationship").value("outgoing"))
                .andExpect(jsonPath("$.otherUserId").value(a)).andExpect(jsonPath("$.createdAt").isString());
        var created = friendships.get(a, b).createdAt();
        assertThat(jdbc.sql("SELECT requested_by FROM friendships WHERE user_a_id = :id")
                .param("id", a).query(Integer.class).single()).isEqualTo(b);
        mvc.perform(get("/api/me/friend-requests?direction=incoming").header("Authorization", bearer(first)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.items[0].otherUserId").value(b));
        mvc.perform(get("/api/me/friend-requests?direction=outgoing").header("Authorization", bearer(second)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.items[0].otherUserId").value(a));
        problem(patch("/api/friend-requests/{id}", a).header("Authorization", bearer(second))
                .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"accepted\"}"), 403, "FORBIDDEN");
        mvc.perform(patch("/api/friend-requests/{id}", b).header("Authorization", bearer(first))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"accepted\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.relationship").value("friends"))
                .andExpect(jsonPath("$.acceptedAt").isString());
        var accepted = friendships.get(a, b).acceptedAt();
        assertThat(friendships.get(a, b).createdAt()).isEqualTo(created);
        problem(patch("/api/friend-requests/{id}", a).header("Authorization", bearer(second))
                .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"accepted\"}"), 403, "FORBIDDEN");
        problem(patch("/api/friend-requests/{id}", b).header("Authorization", bearer(first))
                .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"accepted\"}"), 409, "ALREADY_FRIENDS");
        assertThat(friendships.get(a, b).acceptedAt()).isEqualTo(accepted);
        problem(delete("/api/friend-requests/{id}", b).header("Authorization", bearer(first)), 409, "FRIEND_REQUEST_NOT_PENDING");
        mvc.perform(get("/api/me/friends").header("Authorization", bearer(first)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.items[0].otherUserId").value(b));
        mvc.perform(delete("/api/me/friends/{id}", a).header("Authorization", bearer(second))).andExpect(status().isNoContent())
                .andExpect(content().string(""));
        problem(delete("/api/me/friends/{id}", a).header("Authorization", bearer(second)), 404, "FRIENDSHIP_NOT_FOUND");
    }

    @Test
    void conflictsCancellationRejectionAndThirdPartyPrivacy() throws Exception {
        int a = first.user().id(), b = second.user().id();
        friendships.send(a, b);
        problem(send(first, "{\"recipientId\":" + b + "}"), 409, "FRIEND_REQUEST_ALREADY_EXISTS");
        problem(send(second, "{\"recipientId\":" + a + "}"), 409, "INCOMING_REQUEST_EXISTS");
        problem(delete("/api/me/friends/{id}", b).header("Authorization", bearer(first)), 409, "FRIENDSHIP_NOT_ACCEPTED");
        var third = user();
        jdbc.sql("UPDATE users SET id_role = (SELECT id FROM roles WHERE code = 'ADMIN') WHERE id = :id")
                .param("id", third.user().id()).update();
        mvc.perform(get("/api/me/friend-requests?direction=incoming").header("Authorization", bearer(third)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.items").isEmpty());
        problem(delete("/api/friend-requests/{id}", b).header("Authorization", bearer(third)), 404, "FRIEND_REQUEST_NOT_FOUND");
        assertThat(friendships.get(a, b).relationship()).isEqualTo("outgoing");
        mvc.perform(delete("/api/friend-requests/{id}", b).header("Authorization", bearer(first))).andExpect(status().isNoContent());
        problem(delete("/api/friend-requests/{id}", b).header("Authorization", bearer(first)), 404, "FRIEND_REQUEST_NOT_FOUND");
        friendships.send(a, b);
        mvc.perform(delete("/api/friend-requests/{id}", a).header("Authorization", bearer(second))).andExpect(status().isNoContent());
        friendships.send(a, b);
        friendships.accept(b, a);
        problem(send(first, "{\"recipientId\":" + b + "}"), 409, "ALREADY_FRIENDS");
    }

    @ParameterizedTest
    @ValueSource(strings = {"[]", "null", "true", "{", "{\"recipientId\":\"2\"}", "{\"recipientId\":1.0}",
            "{\"recipientId\":true}", "{\"recipientId\":2147483648}", "{\"recipientId\":{}}"})
    void malformedCreationInputIs400(String body) throws Exception {
        problem(send(first, body), 400, "INVALID_REQUEST");
    }

    @ParameterizedTest
    @ValueSource(strings = {"{}", "{\"recipientId\":null}", "{\"recipientId\":0}", "{\"recipientId\":-1}"})
    void readableInvalidCreationInputIs422(String body) throws Exception {
        problem(send(first, body), 422, "VALIDATION_ERROR");
    }

    @ParameterizedTest
    @CsvSource({"invalid,400,INVALID_REQUEST", "2147483648,400,INVALID_REQUEST", "0,422,VALIDATION_ERROR", "-1,422,VALIDATION_ERROR"})
    void pathValidationMatchesContract(String id, int status, String code) throws Exception {
        problem(get("/api/users/{id}/friendship", id).header("Authorization", bearer(first)), status, code);
        problem(delete("/api/friend-requests/{id}", id).header("Authorization", bearer(first)), status, code);
        problem(delete("/api/me/friends/{id}", id).header("Authorization", bearer(first)), status, code);
    }

    @Test
    void directionStatusMediaTypeAndMissingBodyValidation() throws Exception {
        for (String query : new String[] {"", "?direction=", "?direction=INCOMING", "?direction=other"})
            problem(get("/api/me/friend-requests" + query).header("Authorization", bearer(first)), 422, "VALIDATION_ERROR");
        for (String body : new String[] {"{}", "{\"status\":null}", "{\"status\":\"pending\"}"})
            problem(patch("/api/friend-requests/{id}", second.user().id()).header("Authorization", bearer(first))
                    .contentType(MediaType.APPLICATION_JSON).content(body), 422, "VALIDATION_ERROR");
        problem(patch("/api/friend-requests/{id}", second.user().id()).header("Authorization", bearer(first))
                .contentType(MediaType.APPLICATION_JSON).content("{\"status\":1}"), 400, "INVALID_REQUEST");
        problem(post("/api/friend-requests").header("Authorization", bearer(first)).contentType(MediaType.TEXT_PLAIN).content("{}"),
                415, "UNSUPPORTED_MEDIA_TYPE");
        problem(post("/api/friend-requests").header("Authorization", bearer(first)).contentType(MediaType.APPLICATION_JSON),
                400, "INVALID_REQUEST");
    }

    @Test
    void selfMissingDeletedUsersAndAbsentRequests() throws Exception {
        problem(send(first, "{\"recipientId\":" + first.user().id() + "}"), 422, "VALIDATION_ERROR");
        problem(get("/api/users/{id}/friendship", first.user().id()).header("Authorization", bearer(first)), 422, "VALIDATION_ERROR");
        problem(get("/api/users/2147483647/friendship").header("Authorization", bearer(first)), 404, "USER_NOT_FOUND");
        problem(patch("/api/friend-requests/{id}", second.user().id()).header("Authorization", bearer(first))
                .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"accepted\"}"), 404, "FRIEND_REQUEST_NOT_FOUND");
        friendships.send(first.user().id(), second.user().id());
        jdbc.sql("UPDATE users SET is_deleted = true WHERE id = :id").param("id", second.user().id()).update();
        problem(send(first, "{\"recipientId\":" + second.user().id() + "}"), 404, "USER_NOT_FOUND");
        mvc.perform(get("/api/me/friend-requests?direction=outgoing").header("Authorization", bearer(first)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.items").isEmpty());
    }

    @Test
    void unauthenticatedInvalidRevokedAndCookieOnlyRequestsUseF13Envelope() throws Exception {
        String[] paths = {"/api/me/friends", "/api/me/friend-requests?direction=incoming", "/api/users/1/friendship"};
        for (String path : paths) {
            problem(get(path), 401, "UNAUTHORIZED");
            problem(get(path).header("Authorization", "Bearer invalid"), 401, "UNAUTHORIZED");
            problem(get(path).cookie(new Cookie("__Host-cinder-refresh", first.refreshToken())), 401, "UNAUTHORIZED");
        }
        problem(post("/api/friend-requests").contentType(MediaType.APPLICATION_JSON).content("{"), 401, "UNAUTHORIZED");
        problem(patch("/api/friend-requests/1").contentType(MediaType.APPLICATION_JSON).content("{}"), 401, "UNAUTHORIZED");
        problem(delete("/api/friend-requests/1"), 401, "UNAUTHORIZED");
        problem(delete("/api/me/friends/1"), 401, "UNAUTHORIZED");
        jdbc.sql("UPDATE refresh_tokens SET revoked_at = now() WHERE id_user = :id").param("id", first.user().id()).update();
        problem(get("/api/me/friends").header("Authorization", bearer(first)), 401, "UNAUTHORIZED");
    }

    @Test
    void completeListsUseBothSidesCurrentProfilesAndStableTimestampOrdering() throws Exception {
        var third = user();
        int a = first.user().id(), b = second.user().id(), c = third.user().id();
        friendships.send(b, a);
        friendships.send(a, c);
        jdbc.sql("UPDATE friendships SET created_at = '2026-01-01T00:00:00Z' WHERE user_a_id = :id")
                .param("id", a).update();
        friendships.accept(a, b);
        friendships.accept(c, a);
        jdbc.sql("UPDATE friendships SET accepted_at = '2026-02-01T00:00:00Z' WHERE user_a_id = :id")
                .param("id", a).update();
        jdbc.sql("UPDATE users SET nickname = :name, url_user_icon = 'https://example.invalid/icon.png' WHERE id = :id")
                .param("name", "Updated-" + UUID.randomUUID()).param("id", b).update();
        mvc.perform(get("/api/me/friends").header("Authorization", bearer(first)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.items.length()").value(2))
                .andExpect(jsonPath("$.items[0].otherUserId").value(b)).andExpect(jsonPath("$.items[1].otherUserId").value(c))
                .andExpect(jsonPath("$.items[0].iconUrl").value("https://example.invalid/icon.png"));
        jdbc.sql("UPDATE friendships SET accepted_at = '2026-03-01T00:00:00Z' WHERE user_b_id = :id")
                .param("id", c).update();
        mvc.perform(get("/api/me/friends").header("Authorization", bearer(first)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.items[0].otherUserId").value(c));
    }

    @Test
    void requestsSortByCreationThenOtherIdAcrossBothPairSides() throws Exception {
        var third = user();
        int a = first.user().id(), b = second.user().id(), c = third.user().id();
        friendships.send(a, b);
        friendships.send(c, b);
        jdbc.sql("UPDATE friendships SET created_at = '2026-01-01T00:00:00Z' WHERE user_a_id = :id OR user_b_id = :id")
                .param("id", b).update();
        mvc.perform(get("/api/me/friend-requests?direction=incoming").header("Authorization", bearer(second)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.items.length()").value(2))
                .andExpect(jsonPath("$.items[0].otherUserId").value(a)).andExpect(jsonPath("$.items[1].otherUserId").value(c));
        jdbc.sql("UPDATE friendships SET created_at = '2026-02-01T00:00:00Z' WHERE requested_by = :id")
                .param("id", c).update();
        mvc.perform(get("/api/me/friend-requests?direction=incoming").header("Authorization", bearer(second)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.items[0].otherUserId").value(c));
    }

    @Test
    void openApiDocumentsEveryF13PathWithBearerSecurityAndRequestSchemas() throws Exception {
        mvc.perform(get("/v3/api-docs")).andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/api/friend-requests'].post.responses['201']").exists())
                .andExpect(jsonPath("$.paths['/api/friend-requests/{otherUserId}'].patch").exists())
                .andExpect(jsonPath("$.paths['/api/friend-requests/{otherUserId}'].delete.responses['204']").exists())
                .andExpect(jsonPath("$.paths['/api/me/friend-requests'].get").exists())
                .andExpect(jsonPath("$.paths['/api/me/friends'].get").exists())
                .andExpect(jsonPath("$.paths['/api/me/friends/{otherUserId}'].delete.responses['204']").exists())
                .andExpect(jsonPath("$.paths['/api/users/{otherUserId}/friendship'].get.security[0].bearerAuth").exists())
                .andExpect(jsonPath("$.components.schemas.SendRequest.properties.recipientId.type").value("integer"));
    }

    @Test
    void softDeletionCleansPendingAndAcceptedOnBothSidesButPreservesOtherPairs() {
        var third = user();
        int a = first.user().id(), b = second.user().id(), c = third.user().id();
        friendships.send(a, b);
        friendships.accept(b, a);
        friendships.send(c, b);
        friendships.send(a, c);
        authentication.deleteAccount(second.user(), b);
        assertThat(jdbc.sql("SELECT count(*) FROM friendships WHERE user_a_id = :id OR user_b_id = :id")
                .param("id", b).query(Integer.class).single()).isZero();
        assertThat(friendships.get(a, c).relationship()).isEqualTo("outgoing");
    }

    private void problem(MockHttpServletRequestBuilder request, int statusCode, String code) throws Exception {
        var result = mvc.perform(request).andExpect(status().is(statusCode))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value("about:blank")).andExpect(jsonPath("$.title").isString())
                .andExpect(jsonPath("$.status").value(statusCode)).andExpect(jsonPath("$.code").value(code))
                .andExpect(jsonPath("$.detail").isString()).andExpect(jsonPath("$.instance").isString());
        if (statusCode == 401) result.andExpect(header().string(HttpHeaders.WWW_AUTHENTICATE, "Bearer"));
    }

    private MockHttpServletRequestBuilder send(AuthenticationService.IssuedSession session, String body) {
        return post("/api/friend-requests").header("Authorization", bearer(session)).contentType(MediaType.APPLICATION_JSON).content(body);
    }

    private AuthenticationService.IssuedSession user() {
        String suffix = UUID.randomUUID().toString();
        return authentication.register("F13-" + suffix, suffix + "@example.invalid", "test password");
    }

    private String bearer(AuthenticationService.IssuedSession session) { return "Bearer " + session.accessToken(); }
}
