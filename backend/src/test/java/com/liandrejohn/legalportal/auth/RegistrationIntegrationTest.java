package com.liandrejohn.legalportal.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Map;
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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;

import com.liandrejohn.legalportal.TestcontainersConfiguration;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class RegistrationIntegrationTest {

    private static final String REGISTER_PATH = "/api/v1/auth/register";

    private static final String PASSWORD = "password123456";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private Cookie sessionCookie;

    private String csrfToken;

    @BeforeEach
    void fetchCsrfToken() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/auth/csrf")).andExpect(status().isOk()).andReturn();
        sessionCookie = result.getResponse().getCookie("SESSION");
        csrfToken = JsonPath.read(result.getResponse().getContentAsString(), "$.token");
    }

    @Test
    void validRegistrationCreatesUserAndClientProfile() throws Exception {
        String email = uniqueEmail();

        MvcResult result = register(body("John", "Doe", email, PASSWORD)).andExpect(status().isCreated())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.id").isNotEmpty())
            .andExpect(jsonPath("$.email").value(email))
            .andExpect(jsonPath("$.firstName").value("John"))
            .andExpect(jsonPath("$.lastName").value("Doe"))
            .andExpect(jsonPath("$.password").doesNotExist())
            .andExpect(jsonPath("$.passwordHash").doesNotExist())
            .andReturn();
        UUID id = UUID.fromString(JsonPath.read(result.getResponse().getContentAsString(), "$.id"));

        assertThat(id.version()).isEqualTo(7);

        Map<String, Object> user = jdbcTemplate.queryForMap("SELECT * FROM users WHERE id = ?", id);
        assertThat(user).containsEntry("email", email)
            .containsEntry("role", "CLIENT")
            .containsEntry("status", "ACTIVE")
            .containsEntry("email_verified", false);
        assertThat(user.get("created_at")).isNotNull();

        Map<String, Object> profile = jdbcTemplate.queryForMap("SELECT * FROM client_profiles WHERE user_id = ?", id);
        assertThat(profile).containsEntry("first_name", "John").containsEntry("last_name", "Doe");
        assertThat(((UUID) profile.get("id")).version()).isEqualTo(7);
    }

    @Test
    void passwordIsStoredAsBcryptHash() throws Exception {
        String email = uniqueEmail();
        register(body("John", "Doe", email, PASSWORD)).andExpect(status().isCreated());

        String hash = jdbcTemplate.queryForObject("SELECT password_hash FROM users WHERE email = ?", String.class,
                email);
        assertThat(hash).isNotEqualTo(PASSWORD).matches("^\\$2[aby]\\$\\d{2}\\$.{53}$");
        assertThat(passwordEncoder.matches(PASSWORD, hash)).isTrue();
    }

    @Test
    void emailAndNamesAreNormalized() throws Exception {
        String email = uniqueEmail();

        register(body("  John ", " Doe  ", "  " + email.toUpperCase() + " ", PASSWORD))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.email").value(email))
            .andExpect(jsonPath("$.firstName").value("John"))
            .andExpect(jsonPath("$.lastName").value("Doe"));
    }

    @Test
    void duplicateEmailIsRejectedCaseInsensitively() throws Exception {
        String email = uniqueEmail();
        register(body("John", "Doe", email, PASSWORD)).andExpect(status().isCreated());

        register(body("Jane", "Doe", email.toUpperCase(), PASSWORD)).andExpect(status().isConflict())
            .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
            .andExpect(jsonPath("$.type").value("about:blank"))
            .andExpect(jsonPath("$.title").value("Conflict"))
            .andExpect(jsonPath("$.status").value(409))
            .andExpect(jsonPath("$.detail").value("Email address is already registered."))
            .andExpect(jsonPath("$.instance").value(REGISTER_PATH))
            .andExpect(jsonPath("$.errorCode").value("EMAIL_ALREADY_EXISTS"))
            .andExpect(jsonPath("$.timestamp").value(matchesPattern("\\d{4}-\\d{2}-\\d{2}T.*Z")))
            .andExpect(jsonPath("$.traceId").isNotEmpty());

        Integer count = jdbcTemplate.queryForObject("SELECT count(*) FROM users WHERE lower(email) = ?",
                Integer.class, email);
        assertThat(count).isEqualTo(1);
    }

    @Test
    void invalidEmailAndShortPasswordReturnFieldErrors() throws Exception {
        expectValidationError(register(body("John", "Doe", "not-an-email", "short")))
            .andExpect(jsonPath("$.fieldErrors", hasSize(2)))
            .andExpect(jsonPath("$.fieldErrors[0].field").value("email"))
            .andExpect(jsonPath("$.fieldErrors[0].message").value("Invalid email address"))
            .andExpect(jsonPath("$.fieldErrors[1].field").value("password"))
            .andExpect(jsonPath("$.fieldErrors[1].message").value("Password must contain at least 12 characters"));
    }

    @Test
    void missingFieldsReturnFieldErrors() throws Exception {
        expectValidationError(register("{}")).andExpect(jsonPath("$.fieldErrors[*].field",
                containsInAnyOrder("email", "firstName", "lastName", "password")))
            .andExpect(jsonPath("$.fieldErrors[*].message", containsInAnyOrder("Email is required",
                    "First name is required", "Last name is required", "Password is required")));
    }

    @Test
    void overlongValuesAreRejected() throws Exception {
        String longName = "a".repeat(101);
        // 25 three-byte characters: 25 characters but 75 bytes, over BCrypt's 72-byte limit
        String multibytePassword = "€".repeat(25);

        expectValidationError(register(body(longName, longName, uniqueEmail(), multibytePassword)))
            .andExpect(jsonPath("$.fieldErrors[*].message",
                    containsInAnyOrder("First name must not exceed 100 characters",
                            "Last name must not exceed 100 characters", "Password must not exceed 72 bytes")));
    }

    @Test
    void malformedBodyIsRejected() throws Exception {
        expectValidationError(register("{not json")).andExpect(jsonPath("$.detail")
            .value("Request body is missing or malformed."));
    }

    @Test
    void requestWithoutCsrfTokenIsRejected() throws Exception {
        String email = uniqueEmail();

        mockMvc
            .perform(post(REGISTER_PATH).cookie(sessionCookie)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body("John", "Doe", email, PASSWORD)))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.errorCode").value("FORBIDDEN"));

        Integer count = jdbcTemplate.queryForObject("SELECT count(*) FROM users WHERE email = ?", Integer.class,
                email);
        assertThat(count).isZero();
    }

    @Test
    void registrationDoesNotLogTheClientIn() throws Exception {
        register(body("John", "Doe", uniqueEmail(), PASSWORD)).andExpect(status().isCreated());

        // Any non-public endpoint still requires authentication for this session
        mockMvc.perform(get("/api/v1/clients/me").cookie(sessionCookie))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.errorCode").value("UNAUTHORIZED"));
    }

    private ResultActions register(String json) throws Exception {
        return mockMvc.perform(post(REGISTER_PATH).cookie(sessionCookie)
            .header("X-XSRF-TOKEN", csrfToken)
            .contentType(MediaType.APPLICATION_JSON)
            .content(json));
    }

    private static ResultActions expectValidationError(ResultActions result) throws Exception {
        return result.andExpect(status().isBadRequest())
            .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
            .andExpect(jsonPath("$.type").value("about:blank"))
            .andExpect(jsonPath("$.title").value("Validation Failed"))
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.instance").value(REGISTER_PATH))
            .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"))
            .andExpect(jsonPath("$.timestamp").isNotEmpty())
            .andExpect(jsonPath("$.traceId").isNotEmpty());
    }

    private static String body(String firstName, String lastName, String email, String password) {
        return """
                {"firstName":"%s","lastName":"%s","email":"%s","password":"%s"}
                """.formatted(firstName, lastName, email, password);
    }

    private static String uniqueEmail() {
        return "client-" + UUID.randomUUID() + "@example.com";
    }

}
