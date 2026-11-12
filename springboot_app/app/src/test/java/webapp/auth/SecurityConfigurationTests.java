package webapp.auth;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestComponent;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
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
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
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

    @TestConfiguration(proxyBeanMethods = false)
    @EnableWebMvc
    @Import(SecurityConfiguration.class)
    static class SecurityTestConfig {
        @Bean
        AccountRepository accounts() {
            return mock(AccountRepository.class);
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
