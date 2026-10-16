package com.liandrejohn.legalportal.auth;

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
class CurrentClientIntegrationTest {

    private static final String ME_PATH = "/api/v1/auth/me";

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
    void authenticatedSessionReturnsTheClient() throws Exception {
        UUID id = jdbcTemplate.queryForObject("SELECT id FROM users WHERE email = ?", UUID.class, email);

        me(login()).andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.id").value(id.toString()))
            .andExpect(jsonPath("$.email").value(email))
            .andExpect(jsonPath("$.firstName").value("John"))
            .andExpect(jsonPath("$.lastName").value("Doe"))
            .andExpect(jsonPath("$.password").doesNotExist())
            .andExpect(jsonPath("$.passwordHash").doesNotExist());
    }

    @Test
    void clientDetailsAreReadFromTheDatabaseOnEachRequest() throws Exception {
        Cookie session = login();
        jdbcTemplate.update(
                "UPDATE client_profiles SET first_name = 'Jane' WHERE user_id = (SELECT id FROM users WHERE email = ?)",
                email);

        me(session).andExpect(status().isOk()).andExpect(jsonPath("$.firstName").value("Jane"));
    }

    @Test
    void missingSessionIsRejected() throws Exception {
        expectAuthenticationRequired(mockMvc.perform(get(ME_PATH)));
    }

    @Test
    void unknownSessionIsRejected() throws Exception {
        expectAuthenticationRequired(me(sessionCookie(UUID.randomUUID().toString())));
    }

    @Test
    void unauthenticatedSessionIsRejected() throws Exception {
        Cookie anonymousSession = mockMvc.perform(get("/api/v1/auth/csrf"))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getCookie("SESSION");

        expectAuthenticationRequired(me(anonymousSession));
    }

    @Test
    void expiredSessionIsRejected() throws Exception {
        Cookie session = login();
        // Spring Session decides expiry from the last access time and the max inactive interval
        jdbcTemplate.update("UPDATE SPRING_SESSION SET LAST_ACCESS_TIME = 0, EXPIRY_TIME = 0 WHERE SESSION_ID = ?",
                sessionId(session));

        expectAuthenticationRequired(me(session));
    }

    @Test
    void suspendedClientIsRejectedAndTheSessionEnded() throws Exception {
        Cookie session = login();
        setStatus("SUSPENDED");

        expectAuthenticationRequired(me(session));

        // Reactivating the account doesn't bring the ended session back
        setStatus("ACTIVE");
        expectAuthenticationRequired(me(session));
    }

    @Test
    void disabledClientIsRejected() throws Exception {
        Cookie session = login();
        setStatus("DISABLED");

        expectAuthenticationRequired(me(session));
    }

    @Test
    void deletedClientIsRejected() throws Exception {
        Cookie session = login();
        jdbcTemplate.update("UPDATE users SET deleted_at = now() WHERE email = ?", email);

        expectAuthenticationRequired(me(session));
    }

    /** Logs in through the API and returns the authenticated session cookie. */
    private Cookie login() throws Exception {
        MvcResult csrf = mockMvc.perform(get("/api/v1/auth/csrf")).andExpect(status().isOk()).andReturn();
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

    private ResultActions me(Cookie session) throws Exception {
        return mockMvc.perform(get(ME_PATH).cookie(session));
    }

    private void setStatus(String status) {
        jdbcTemplate.update("UPDATE users SET status = ? WHERE email = ?", status, email);
    }

    /** The SESSION cookie holds the Base64-encoded session ID. */
    private static String sessionId(Cookie session) {
        return new String(Base64.getDecoder().decode(session.getValue()), StandardCharsets.UTF_8);
    }

    private static Cookie sessionCookie(String sessionId) {
        return new Cookie("SESSION",
                Base64.getEncoder().encodeToString(sessionId.getBytes(StandardCharsets.UTF_8)));
    }

    private static void expectAuthenticationRequired(ResultActions result) throws Exception {
        result.andExpect(status().isUnauthorized())
            .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
            .andExpect(jsonPath("$.type").value("about:blank"))
            .andExpect(jsonPath("$.title").value("Unauthorized"))
            .andExpect(jsonPath("$.status").value(401))
            .andExpect(jsonPath("$.detail").value("Authentication required."))
            .andExpect(jsonPath("$.instance").value(ME_PATH))
            .andExpect(jsonPath("$.errorCode").value("UNAUTHORIZED"))
            .andExpect(jsonPath("$.timestamp").value(matchesPattern("\\d{4}-\\d{2}-\\d{2}T.*Z")))
            .andExpect(jsonPath("$.traceId").isNotEmpty());
    }

}
