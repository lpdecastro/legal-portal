package com.liandrejohn.legalportal.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import com.jayway.jsonpath.JsonPath;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import com.liandrejohn.legalportal.TestcontainersConfiguration;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class LoginIntegrationTest {

    private static final String LOGIN_PATH = "/api/v1/auth/login";

    /** Any protected endpoint; it doesn't exist yet, so authenticated requests get a 404 instead of a 401. */
    private static final String PROTECTED_PATH = "/api/v1/clients/me";

    private static final String PASSWORD = "password123456";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private RegistrationService registrationService;

    /** Each test logs in from its own IP so per-IP failure counts don't carry over between tests. */
    private final String clientIp = "10.%d.%d.%d".formatted(ThreadLocalRandom.current().nextInt(256),
            ThreadLocalRandom.current().nextInt(256), ThreadLocalRandom.current().nextInt(1, 255));

    private String email;

    private Cookie sessionCookie;

    private String csrfToken;

    @BeforeEach
    void registerClientAndFetchCsrfToken() throws Exception {
        email = uniqueEmail();
        registrationService.register(new RegisterRequest("John", "Doe", email, PASSWORD));

        MvcResult result = mockMvc.perform(get("/api/v1/auth/csrf")).andExpect(status().isOk()).andReturn();
        sessionCookie = result.getResponse().getCookie("SESSION");
        csrfToken = JsonPath.read(result.getResponse().getContentAsString(), "$.token");
    }

    @Test
    void validCredentialsReturnClientAndAuthenticateTheSession() throws Exception {
        UUID id = jdbcTemplate.queryForObject("SELECT id FROM users WHERE email = ?", UUID.class, email);

        MvcResult result = login(body(email, PASSWORD)).andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.id").value(id.toString()))
            .andExpect(jsonPath("$.email").value(email))
            .andExpect(jsonPath("$.firstName").value("John"))
            .andExpect(jsonPath("$.lastName").value("Doe"))
            .andExpect(jsonPath("$.password").doesNotExist())
            .andExpect(jsonPath("$.passwordHash").doesNotExist())
            .andReturn();

        Cookie authenticatedCookie = result.getResponse().getCookie("SESSION");
        assertThat(authenticatedCookie).isNotNull();
        mockMvc.perform(get(PROTECTED_PATH).cookie(authenticatedCookie)).andExpect(status().isNotFound());
    }

    @Test
    void loginChangesTheSessionId() throws Exception {
        MvcResult result = login(body(email, PASSWORD)).andExpect(status().isOk()).andReturn();

        Cookie authenticatedCookie = result.getResponse().getCookie("SESSION");
        assertThat(authenticatedCookie.getValue()).isNotEqualTo(sessionCookie.getValue());
        // The pre-login session ID no longer identifies any session
        mockMvc.perform(get(PROTECTED_PATH).cookie(sessionCookie)).andExpect(status().isUnauthorized());
    }

    @Test
    void loginReplacesTheCsrfToken() throws Exception {
        Cookie authenticatedCookie = login(body(email, PASSWORD)).andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getCookie("SESSION");

        // The pre-login token is rejected on the new session
        mockMvc
            .perform(post(PROTECTED_PATH).cookie(authenticatedCookie)
                .header("X-XSRF-TOKEN", csrfToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.errorCode").value("FORBIDDEN"));
    }

    @Test
    void emailIsMatchedCaseInsensitivelyAndTrimmed() throws Exception {
        login(body("  " + email.toUpperCase() + " ", PASSWORD)).andExpect(status().isOk())
            .andExpect(jsonPath("$.email").value(email));
    }

    @Test
    void wrongPasswordAndUnknownEmailReturnTheSameGenericError() throws Exception {
        expectInvalidCredentials(login(body(email, "wrong-password-123")));
        expectInvalidCredentials(login(body(uniqueEmail(), PASSWORD)));
    }

    @Test
    void failedLoginDoesNotAuthenticateTheSession() throws Exception {
        expectInvalidCredentials(login(body(email, "wrong-password-123")));

        mockMvc.perform(get(PROTECTED_PATH).cookie(sessionCookie)).andExpect(status().isUnauthorized());
    }

    @Test
    void suspendedAccountCannotLogIn() throws Exception {
        setStatus("SUSPENDED");

        expectInvalidCredentials(login(body(email, PASSWORD)));
    }

    @Test
    void disabledAccountCannotLogIn() throws Exception {
        setStatus("DISABLED");

        expectInvalidCredentials(login(body(email, PASSWORD)));
    }

    @Test
    void deletedAccountCannotLogIn() throws Exception {
        jdbcTemplate.update("UPDATE users SET deleted_at = now() WHERE email = ?", email);

        expectInvalidCredentials(login(body(email, PASSWORD)));
    }

    @Test
    void passwordOverBcryptLimitIsRejectedAsInvalid() throws Exception {
        expectInvalidCredentials(login(body(email, "€".repeat(25))));
    }

    @Test
    void repeatedFailuresAreRateLimited() throws Exception {
        for (int i = 0; i < 5; i++) {
            expectInvalidCredentials(login(body(email, "wrong-password-123")));
        }

        // Blocked even with the right password
        login(body(email, PASSWORD)).andExpect(status().isTooManyRequests())
            .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
            .andExpect(jsonPath("$.type").value("about:blank"))
            .andExpect(jsonPath("$.title").value("Too Many Requests"))
            .andExpect(jsonPath("$.status").value(429))
            .andExpect(jsonPath("$.detail").value("Too many failed login attempts. Try again later."))
            .andExpect(jsonPath("$.errorCode").value("RATE_LIMIT_EXCEEDED"))
            .andExpect(jsonPath("$.timestamp").isNotEmpty())
            .andExpect(jsonPath("$.traceId").isNotEmpty());
    }

    @Test
    void unknownEmailsAreRateLimitedLikeKnownOnes() throws Exception {
        String unknownEmail = uniqueEmail();
        for (int i = 0; i < 5; i++) {
            expectInvalidCredentials(login(body(unknownEmail, PASSWORD)));
        }

        login(body(unknownEmail, PASSWORD)).andExpect(status().isTooManyRequests());
    }

    @Test
    void missingFieldsReturnValidationError() throws Exception {
        login("{}").andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"))
            .andExpect(jsonPath("$.fieldErrors[*].field").value(
                    containsInAnyOrder("email", "password")));
    }

    @Test
    void requestWithoutCsrfTokenIsRejected() throws Exception {
        mockMvc
            .perform(withClientIp(post(LOGIN_PATH)).cookie(sessionCookie)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body(email, PASSWORD)))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.errorCode").value("FORBIDDEN"));

        mockMvc.perform(get(PROTECTED_PATH).cookie(sessionCookie)).andExpect(status().isUnauthorized());
    }

    private ResultActions login(String json) throws Exception {
        return mockMvc.perform(withClientIp(post(LOGIN_PATH)).cookie(sessionCookie)
            .header("X-XSRF-TOKEN", csrfToken)
            .contentType(MediaType.APPLICATION_JSON)
            .content(json));
    }

    private MockHttpServletRequestBuilder withClientIp(MockHttpServletRequestBuilder request) {
        return request.with((mockRequest) -> {
            mockRequest.setRemoteAddr(clientIp);
            return mockRequest;
        });
    }

    private static void expectInvalidCredentials(ResultActions result) throws Exception {
        result.andExpect(status().isUnauthorized())
            .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
            .andExpect(jsonPath("$.type").value("about:blank"))
            .andExpect(jsonPath("$.title").value("Unauthorized"))
            .andExpect(jsonPath("$.status").value(401))
            .andExpect(jsonPath("$.detail").value("Invalid email or password."))
            .andExpect(jsonPath("$.instance").value(LOGIN_PATH))
            .andExpect(jsonPath("$.errorCode").value("UNAUTHORIZED"))
            .andExpect(jsonPath("$.timestamp").value(matchesPattern("\\d{4}-\\d{2}-\\d{2}T.*Z")))
            .andExpect(jsonPath("$.traceId").isNotEmpty());
    }

    private void setStatus(String status) {
        jdbcTemplate.update("UPDATE users SET status = ? WHERE email = ?", status, email);
    }

    private static String body(String email, String password) {
        return """
                {"email":"%s","password":"%s"}
                """.formatted(email, password);
    }

    private static String uniqueEmail() {
        return "client-" + UUID.randomUUID() + "@example.com";
    }

}
