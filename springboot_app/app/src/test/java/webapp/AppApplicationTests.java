package webapp;

import com.jayway.jsonpath.JsonPath;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
class AppApplicationTests {
	@Autowired
	private WebApplicationContext context;

	private MockMvc mvc;

	@BeforeEach
	void setUp() {
		mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
	}

	@Test
	void bearerAuthenticationIsRequiredForTheCurrentProfile() throws Exception {
		mvc.perform(get("/api/auth/users/me"))
				.andExpect(status().isUnauthorized())
				.andExpect(header().string(HttpHeaders.WWW_AUTHENTICATE, startsWith("Bearer")));
		mvc.perform(get("/api/auth/users/me").header(HttpHeaders.AUTHORIZATION, "Bearer invalid-token"))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void cookieChangingEndpointsRequireCsrf() throws Exception {
		for (String path : new String[] {"/api/auth/users", "/api/auth/sessions", "/api/auth/session-renewals", "/api/auth/session-revocations",
                "/api/auth/password-reset-requests", "/api/auth/password-resets"}) {
			mvc.perform(post(path)).andExpect(status().isForbidden());
		}

		var bootstrap = mvc.perform(get("/api/auth/csrf-tokens"))
				.andExpect(status().isOk())
				.andReturn().getResponse();
		Cookie cookie = bootstrap.getCookie("__Host-cinder-csrf");
		assertThat(cookie).isNotNull();
		assertThat(cookie.isHttpOnly()).isTrue();
		assertThat(cookie.getSecure()).isTrue();
		assertThat(cookie.getAttribute("SameSite")).isEqualTo("Strict");

		String token = JsonPath.read(bootstrap.getContentAsString(), "$.token");
		mvc.perform(post("/api/auth/sessions").cookie(cookie).header("X-XSRF-TOKEN", "invalid"))
				.andExpect(status().isForbidden());
		// An empty JSON object confirms that the issued token reaches validation without creating a session.
		mvc.perform(post("/api/auth/sessions").cookie(cookie).header("X-XSRF-TOKEN", token)
				.contentType(MediaType.APPLICATION_JSON).content("{}"))
				.andExpect(status().isBadRequest());
	}
}
