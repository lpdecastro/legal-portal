package com.liandrejohn.legalportal.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
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
import com.liandrejohn.legalportal.auth.RegisterRequest;
import com.liandrejohn.legalportal.auth.RegistrationService;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class UpdateClientProfileIntegrationTest {

    private static final String PROFILE_PATH = "/api/v1/clients/me";

    private static final String PASSWORD = "password123456";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private RegistrationService registrationService;

    private String email;

    @BeforeEach
    void setUp() {
        email = registerClient("John", "Doe");
    }

    @Test
    void updatesAllFields() throws Exception {
        updateProfile(login(email), """
                {"firstName":"Johnny","lastName":"Smith","phone":"+639123456789"}
                """)
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(content().json("""
                    {"firstName": "Johnny", "lastName": "Smith", "email": "%s", "phone": "+639123456789"}
                    """.formatted(email), true));

        assertThat(profileRow(email)).containsEntry("first_name", "Johnny")
            .containsEntry("last_name", "Smith")
            .containsEntry("phone", "+639123456789");
    }

    @Test
    void updatedProfileIsReturnedByGet() throws Exception {
        Cookie session = login(email);
        updateProfile(session, """
                {"firstName":"Johnny","lastName":"Smith","phone":"+63 (912) 345-6789"}
                """).andExpect(status().isOk());

        mockMvc.perform(get(PROFILE_PATH).cookie(session))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.firstName").value("Johnny"))
            .andExpect(jsonPath("$.lastName").value("Smith"))
            .andExpect(jsonPath("$.phone").value("+63 (912) 345-6789"));
    }

    @Test
    void trimsValues() throws Exception {
        updateProfile(login(email), """
                {"firstName":"  Johnny ","lastName":" Smith  ","phone":"  09123456789 "}
                """)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.firstName").value("Johnny"))
            .andExpect(jsonPath("$.lastName").value("Smith"))
            .andExpect(jsonPath("$.phone").value("09123456789"));
    }

    @Test
    void leavesOmittedFieldsUnchanged() throws Exception {
        setPhone(email, "+639123456789");

        updateProfile(login(email), """
                {"lastName":"Smith"}
                """)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.firstName").value("John"))
            .andExpect(jsonPath("$.lastName").value("Smith"))
            .andExpect(jsonPath("$.phone").value("+639123456789"));
    }

    @ParameterizedTest
    @ValueSource(strings = { "null", "\"\"", "\"   \"" })
    void clearsPhone(String phone) throws Exception {
        setPhone(email, "+639123456789");

        updateProfile(login(email), """
                {"firstName":"John","lastName":"Doe","phone":%s}
                """.formatted(phone))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.phone").value((Object) null));
        assertThat(profileRow(email)).containsEntry("phone", null);
    }

    @Test
    void ignoresEmail() throws Exception {
        updateProfile(login(email), """
                {"firstName":"John","lastName":"Doe","email":"changed@example.com"}
                """)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.email").value(email));

        assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM users WHERE email = ?", Integer.class, email))
            .isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM users WHERE email = 'changed@example.com'",
                Integer.class)).isZero();
    }

    @Test
    void updatesOnlyTheSessionsClientsProfile() throws Exception {
        String other = registerClient("Jane", "Smith");
        Map<String, Object> otherBefore = profileRow(other);

        updateProfile(login(email), """
                {"firstName":"Johnny","lastName":"Doe"}
                """).andExpect(status().isOk());

        assertThat(profileRow(other)).isEqualTo(otherBefore);
    }

    @Test
    void rejectsMissingAndBlankNames() throws Exception {
        Map<String, Object> before = profileRow(email);

        expectValidationError(updateProfile(login(email), """
                {"firstName":null,"lastName":"   "}
                """))
            .andExpect(jsonPath("$.fieldErrors", hasSize(2)))
            .andExpect(jsonPath("$.fieldErrors[*].field", containsInAnyOrder("firstName", "lastName")))
            .andExpect(jsonPath("$.fieldErrors[*].message",
                    containsInAnyOrder("First name is required", "Last name is required")));
        assertThat(profileRow(email)).isEqualTo(before);
    }

    @Test
    void rejectsTooLongValues() throws Exception {
        Map<String, Object> before = profileRow(email);

        expectValidationError(updateProfile(login(email), """
                {"firstName":"%s","lastName":"%s","phone":"%s"}
                """.formatted("a".repeat(101), "b".repeat(101), "1".repeat(31))))
            .andExpect(jsonPath("$.fieldErrors", hasSize(3)))
            .andExpect(jsonPath("$.fieldErrors[*].message",
                    containsInAnyOrder("First name must not exceed 100 characters",
                            "Last name must not exceed 100 characters",
                            "Phone number must not exceed 30 characters")));
        assertThat(profileRow(email)).isEqualTo(before);
    }

    @Test
    void acceptsMaximumLengths() throws Exception {
        updateProfile(login(email), """
                {"firstName":"%s","lastName":"%s","phone":"%s"}
                """.formatted("a".repeat(100), "b".repeat(100), "1".repeat(30)))
            .andExpect(status().isOk());
    }

    @ParameterizedTest
    @ValueSource(strings = { "abc", "0912-ABC-6789", "++639123456789", "+", "0912345678x", "(0912) 345 678 +1" })
    void rejectsInvalidPhone(String phone) throws Exception {
        Map<String, Object> before = profileRow(email);

        expectValidationError(updateProfile(login(email), """
                {"phone":"%s"}
                """.formatted(phone)))
            .andExpect(jsonPath("$.fieldErrors", hasSize(1)))
            .andExpect(jsonPath("$.fieldErrors[0].field").value("phone"))
            .andExpect(jsonPath("$.fieldErrors[0].message").value(
                    "Phone number may only contain digits, spaces, parentheses, hyphens and a leading +"));
        assertThat(profileRow(email)).isEqualTo(before);
    }

    @Test
    void unauthenticatedRequestIsRejected() throws Exception {
        Map<String, Object> before = profileRow(email);
        MvcResult csrf = mockMvc.perform(get("/api/v1/auth/csrf")).andExpect(status().isOk()).andReturn();

        mockMvc
            .perform(patch(PROFILE_PATH).cookie(csrf.getResponse().getCookie("SESSION"))
                .header("X-XSRF-TOKEN", (String) JsonPath.read(csrf.getResponse().getContentAsString(), "$.token"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"firstName":"Johnny"}
                        """))
            .andExpect(status().isUnauthorized())
            .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
            .andExpect(jsonPath("$.status").value(401))
            .andExpect(jsonPath("$.detail").value("Authentication required."))
            .andExpect(jsonPath("$.errorCode").value("UNAUTHORIZED"))
            .andExpect(jsonPath("$.timestamp").isNotEmpty())
            .andExpect(jsonPath("$.traceId").isNotEmpty());
        assertThat(profileRow(email)).isEqualTo(before);
    }

    @Test
    void suspendedClientIsRejected() throws Exception {
        Map<String, Object> before = profileRow(email);
        Cookie session = login(email);
        jdbcTemplate.update("UPDATE users SET status = 'SUSPENDED' WHERE email = ?", email);

        updateProfile(session, """
                {"firstName":"Johnny"}
                """)
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.errorCode").value("UNAUTHORIZED"));
        assertThat(profileRow(email)).isEqualTo(before);
    }

    @Test
    void missingProfileIsNotFound() throws Exception {
        Cookie session = login(email);
        jdbcTemplate.update("DELETE FROM client_profiles WHERE user_id = (SELECT id FROM users WHERE email = ?)",
                email);

        updateProfile(session, """
                {"firstName":"Johnny"}
                """)
            .andExpect(status().isNotFound())
            .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
            .andExpect(jsonPath("$.status").value(404))
            .andExpect(jsonPath("$.detail").value("Profile not found."))
            .andExpect(jsonPath("$.errorCode").value("PROFILE_NOT_FOUND"));
    }

    @Test
    void missingCsrfTokenIsRejected() throws Exception {
        expectCsrfRejected(null);
    }

    @Test
    void invalidCsrfTokenIsRejected() throws Exception {
        expectCsrfRejected("invalid-token");
    }

    private void expectCsrfRejected(String token) throws Exception {
        Map<String, Object> before = profileRow(email);
        var request = patch(PROFILE_PATH).cookie(login(email))
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                    {"firstName":"Johnny"}
                    """);
        if (token != null) {
            request.header("X-XSRF-TOKEN", token);
        }

        mockMvc.perform(request)
            .andExpect(status().isForbidden())
            .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
            .andExpect(jsonPath("$.errorCode").value("FORBIDDEN"));
        assertThat(profileRow(email)).isEqualTo(before);
    }

    private ResultActions expectValidationError(ResultActions result) throws Exception {
        return result.andExpect(status().isBadRequest())
            .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
            .andExpect(jsonPath("$.title").value("Validation Failed"))
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.instance").value(PROFILE_PATH))
            .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"))
            .andExpect(jsonPath("$.timestamp").isNotEmpty())
            .andExpect(jsonPath("$.traceId").isNotEmpty());
    }

    /** Patches with the session's current CSRF token, which login replaced. */
    private ResultActions updateProfile(Cookie session, String body) throws Exception {
        MvcResult csrf = mockMvc.perform(get("/api/v1/auth/csrf").cookie(session))
            .andExpect(status().isOk())
            .andReturn();
        return mockMvc.perform(patch(PROFILE_PATH).cookie(session)
            .header("X-XSRF-TOKEN", (String) JsonPath.read(csrf.getResponse().getContentAsString(), "$.token"))
            .contentType(MediaType.APPLICATION_JSON)
            .content(body));
    }

    private Map<String, Object> profileRow(String clientEmail) {
        return jdbcTemplate.queryForMap("""
                SELECT first_name, last_name, phone, updated_at FROM client_profiles
                WHERE user_id = (SELECT id FROM users WHERE email = ?)
                """, clientEmail);
    }

    private void setPhone(String clientEmail, String phone) {
        jdbcTemplate.update("""
                UPDATE client_profiles SET phone = ?
                WHERE user_id = (SELECT id FROM users WHERE email = ?)
                """, phone, clientEmail);
    }

    /** Logs in through the API and returns the authenticated session cookie. */
    private Cookie login(String clientEmail) throws Exception {
        MvcResult csrf = mockMvc.perform(get("/api/v1/auth/csrf")).andExpect(status().isOk()).andReturn();
        return mockMvc
            .perform(post("/api/v1/auth/login").cookie(csrf.getResponse().getCookie("SESSION"))
                .header("X-XSRF-TOKEN", (String) JsonPath.read(csrf.getResponse().getContentAsString(), "$.token"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"email":"%s","password":"%s"}
                        """.formatted(clientEmail, PASSWORD)))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getCookie("SESSION");
    }

    private String registerClient(String firstName, String lastName) {
        String clientEmail = "client-" + UUID.randomUUID() + "@example.com";
        registrationService.register(new RegisterRequest(firstName, lastName, clientEmail, PASSWORD));
        return clientEmail;
    }

}
