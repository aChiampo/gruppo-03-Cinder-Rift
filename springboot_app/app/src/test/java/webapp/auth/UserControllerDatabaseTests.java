package webapp.auth;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {"app.security.session-token-cleanup-cron=-",
        "app.security.password-reset-token-cleanup-cron=-"})
@Transactional
class UserControllerDatabaseTests {
    @Autowired private WebApplicationContext context;
    @Autowired private AuthenticationService authentication;
    @Autowired private JdbcClient jdbc;

    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    @Test
    void readsAndUpdatesProfilesUsingTheAuthenticatedIdentity() throws Exception {
        AuthenticationService.IssuedSession caller = createUser();
        AuthenticationService.IssuedSession target = createUser();
        String authorization = bearer(caller);

        mvc.perform(get("/api/auth/users/me").header(HttpHeaders.AUTHORIZATION, authorization))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(caller.user().id()))
                .andExpect(jsonPath("$.email").value(caller.user().email()));

        mvc.perform(get("/api/users/{userId}", target.user().id()).header(HttpHeaders.AUTHORIZATION, authorization))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(target.user().id()))
                .andExpect(jsonPath("$.nickname").value(target.user().nickname()))
                .andExpect(jsonPath("$.rankingPoints").value(1000))
                .andExpect(jsonPath("$.registrationDate").isNotEmpty())
                .andExpect(jsonPath("$.email").doesNotExist());

        mvc.perform(patch("/api/users/me").header(HttpHeaders.AUTHORIZATION, authorization)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nickname": " Updated Player ",
                                  "email": " Updated@Example.Invalid ",
                                  "iconUrl": ""
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(caller.user().id()))
                .andExpect(jsonPath("$.nickname").value("Updated Player"))
                .andExpect(jsonPath("$.email").value("updated@example.invalid"))
                .andExpect(jsonPath("$.iconUrl").value(org.hamcrest.Matchers.nullValue()));

        jdbc.sql("UPDATE users SET is_deleted = true WHERE id = :userId")
                .param("userId", target.user().id())
                .update();
        mvc.perform(get("/api/users/{userId}", target.user().id()).header(HttpHeaders.AUTHORIZATION, authorization))
                .andExpect(status().isNotFound());
        mvc.perform(get("/api/users/{userId}", Integer.MAX_VALUE).header(HttpHeaders.AUTHORIZATION, authorization))
                .andExpect(status().isNotFound());
    }

    @Test
    void rejectsBlankNicknameAndConflictingEmail() throws Exception {
        AuthenticationService.IssuedSession caller = createUser();
        AuthenticationService.IssuedSession other = createUser();
        String authorization = bearer(caller);

        mvc.perform(patch("/api/users/me").header(HttpHeaders.AUTHORIZATION, authorization)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nickname\":\"   \"}"))
                .andExpect(status().isBadRequest());

        mvc.perform(patch("/api/users/me").header(HttpHeaders.AUTHORIZATION, authorization)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nickname\":\"" + "x".repeat(51) + "\"}"))
                .andExpect(status().isBadRequest());

        mvc.perform(patch("/api/users/me").header(HttpHeaders.AUTHORIZATION, authorization)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + other.user().email() + "\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    void rejectsAConflictingNickname() throws Exception {
        AuthenticationService.IssuedSession caller = createUser();
        AuthenticationService.IssuedSession other = createUser();

        mvc.perform(patch("/api/users/me").header(HttpHeaders.AUTHORIZATION, bearer(caller))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nickname\":\"" + other.user().nickname() + "\"}"))
                .andExpect(status().isConflict());
    }

    private AuthenticationService.IssuedSession createUser() {
        String suffix = UUID.randomUUID().toString().replace("-", "");
        return authentication.register("Player-" + suffix, suffix + "@example.invalid", "Test password 1");
    }

    private static String bearer(AuthenticationService.IssuedSession session) {
        return "Bearer " + session.accessToken();
    }
}
