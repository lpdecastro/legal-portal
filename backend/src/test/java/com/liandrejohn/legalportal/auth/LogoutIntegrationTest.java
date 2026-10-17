package com.liandrejohn.legalportal.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.UUID;

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

import com.liandrejohn.legalportal.TestcontainersConfiguration;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class LogoutIntegrationTest {

    private static final String LOGOUT_PATH = "/api/v1/auth/logout";

    private static final String PASSWORD = "password123456";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private RegistrationService registrationService;

    private String email;

    @BeforeEach
    void registerClient() {
        email = "client-" + UUID.randomUUID() + "@example.com";
        registrationService.register(new RegisterRequest("John", "Doe", email, PASSWORD));
    }

    @Test
    void logoutEndsTheSession() throws Exception {
        Cookie session = login();

        MvcResult result = logout(session, csrfToken(session)).andExpect(status().isNoContent()).andReturn();

        assertThat(result.getResponse().getContentAsString()).isEmpty();
        Cookie cleared = result.getResponse().getCookie("SESSION");
        assertThat(cleared).isNotNull();
        assertThat(cleared.getValue()).isEmpty();
        assertThat(cleared.getMaxAge()).isZero();
        assertThat(sessionExists(session)).isFalse();
    }

    @Test
    void endedSessionIsRejectedOnProtectedApis() throws Exception {
        Cookie session = login();
        logout(session, csrfToken(session)).andExpect(status().isNoContent());

        expectProblem(mockMvc.perform(get("/api/v1/auth/me").cookie(session)), 401, "Unauthorized", "UNAUTHORIZED",
                "Authentication required.", "/api/v1/auth/me");
    }

    @Test
    void missingCsrfTokenIsRejectedAndTheSessionKept() throws Exception {
        Cookie session = login();

        expectCsrfRejected(mockMvc.perform(post(LOGOUT_PATH).cookie(session)));

        assertThat(sessionExists(session)).isTrue();
        mockMvc.perform(get("/api/v1/auth/me").cookie(session)).andExpect(status().isOk());
    }

    @Test
    void invalidCsrfTokenIsRejectedAndTheSessionKept() throws Exception {
        Cookie session = login();

        expectCsrfRejected(logout(session, "not-the-token"));

        assertThat(sessionExists(session)).isTrue();
    }

    @Test
    void preLoginCsrfTokenIsRejected() throws Exception {
        // Login replaces the CSRF token, so the one fetched before logging in no longer works
        MvcResult csrf = mockMvc.perform(get("/api/v1/auth/csrf")).andExpect(status().isOk()).andReturn();
        String preLoginToken = JsonPath.read(csrf.getResponse().getContentAsString(), "$.token");
        Cookie session = login(csrf);

        expectCsrfRejected(logout(session, preLoginToken));
    }

    @Test
    void unauthenticatedSessionIsRejected() throws Exception {
        Cookie anonymousSession = mockMvc.perform(get("/api/v1/auth/csrf"))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getCookie("SESSION");

        expectProblem(logout(anonymousSession, csrfToken(anonymousSession)), 401, "Unauthorized", "UNAUTHORIZED",
                "Authentication required.", LOGOUT_PATH);
    }

    /** Logs in through the API and returns the authenticated session cookie. */
    private Cookie login() throws Exception {
        return login(mockMvc.perform(get("/api/v1/auth/csrf")).andExpect(status().isOk()).andReturn());
    }

    private Cookie login(MvcResult csrf) throws Exception {
        return mockMvc
            .perform(post("/api/v1/auth/login").cookie(csrf.getResponse().getCookie("SESSION"))
                .header("X-XSRF-TOKEN", (String) JsonPath.read(csrf.getResponse().getContentAsString(), "$.token"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"email":"%s","password":"%s"}
                        """.formatted(email, PASSWORD)))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getCookie("SESSION");
    }

    /** Fetches the session's current CSRF token, as the SPA does before each state-changing request. */
    private String csrfToken(Cookie session) throws Exception {
        String body = mockMvc.perform(get("/api/v1/auth/csrf").cookie(session))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
        return JsonPath.read(body, "$.token");
    }

    private ResultActions logout(Cookie session, String csrfToken) throws Exception {
        return mockMvc.perform(post(LOGOUT_PATH).cookie(session).header("X-XSRF-TOKEN", csrfToken));
    }

    private boolean sessionExists(Cookie session) {
        String sessionId = new String(Base64.getDecoder().decode(session.getValue()), StandardCharsets.UTF_8);
        Integer count = jdbcTemplate.queryForObject("SELECT count(*) FROM SPRING_SESSION WHERE SESSION_ID = ?",
                Integer.class, sessionId);
        return count != null && count > 0;
    }

    private static void expectCsrfRejected(ResultActions result) throws Exception {
        expectProblem(result, 403, "Forbidden", "FORBIDDEN", "Invalid or missing CSRF token.", LOGOUT_PATH);
    }

    private static void expectProblem(ResultActions result, int status, String title, String errorCode,
            String detail, String instance) throws Exception {
        result.andExpect(status().is(status))
            .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
            .andExpect(jsonPath("$.type").value("about:blank"))
            .andExpect(jsonPath("$.title").value(title))
            .andExpect(jsonPath("$.status").value(status))
            .andExpect(jsonPath("$.detail").value(detail))
            .andExpect(jsonPath("$.instance").value(instance))
            .andExpect(jsonPath("$.errorCode").value(errorCode))
            .andExpect(jsonPath("$.timestamp").value(matchesPattern("\\d{4}-\\d{2}-\\d{2}T.*Z")))
            .andExpect(jsonPath("$.traceId").isNotEmpty());
    }

}
