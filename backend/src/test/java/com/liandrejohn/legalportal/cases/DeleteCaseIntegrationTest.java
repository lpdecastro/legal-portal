package com.liandrejohn.legalportal.cases;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
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

/**
 * Cases are inserted directly with fixed timestamps, so tests can tell whether a row changed. Submitted, closed and
 * deleted cases are inserted too, since those transitions have no API yet.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class DeleteCaseIntegrationTest {

    private static final String CASES_PATH = "/api/v1/cases";

    private static final String PASSWORD = "password123456";

    private static final Instant CREATED_AT = Instant.parse("2026-10-15T04:00:00Z");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private RegistrationService registrationService;

    private String email;

    private UUID categoryId;

    @BeforeEach
    void setUp() {
        email = registerClient();
        categoryId = jdbcTemplate.queryForObject(
                "INSERT INTO legal_categories (name, is_active) VALUES (?, true) RETURNING id", UUID.class,
                "Test Category " + UUID.randomUUID());
    }

    @Test
    void softDeletesADraft() throws Exception {
        UUID id = insertCase(clientProfileId(email), "DRAFT", false);
        Instant before = Instant.now().truncatedTo(ChronoUnit.MICROS);

        deleteCase(login(email), id.toString())
            .andExpect(status().isNoContent())
            .andExpect(content().string(""));

        // The row stays; only deleted_at (and updated_at) change
        Map<String, Object> row = caseRow(id);
        assertThat(((Timestamp) row.get("deleted_at")).toInstant()).isBetween(before, Instant.now());
        assertThat(row.get("status")).isEqualTo("DRAFT");
        assertThat(row.get("title")).isEqualTo("Employment dispute");
        assertThat(row.get("client_id")).isEqualTo(clientProfileId(email));
        assertThat(((Timestamp) row.get("created_at")).toInstant()).isEqualTo(CREATED_AT);
    }

    @Test
    void deletedCaseCannotBeRetrieved() throws Exception {
        UUID id = insertCase(clientProfileId(email), "DRAFT", false);
        Cookie session = login(email);

        deleteCase(session, id.toString()).andExpect(status().isNoContent());

        expectNotFound(mockMvc.perform(get(CASES_PATH + "/" + id).cookie(session)), id.toString());
    }

    @Test
    void repeatedDeletionIsNotFound() throws Exception {
        UUID id = insertCase(clientProfileId(email), "DRAFT", false);
        Cookie session = login(email);

        deleteCase(session, id.toString()).andExpect(status().isNoContent());
        Map<String, Object> afterFirstDelete = caseRow(id);

        expectNotFound(deleteCase(session, id.toString()), id.toString());
        assertThat(caseRow(id)).isEqualTo(afterFirstDelete);
    }

    @ParameterizedTest
    @ValueSource(strings = { "SUBMITTED", "CLOSED" })
    void nonDraftCasesCannotBeDeleted(String caseStatus) throws Exception {
        UUID id = insertCase(clientProfileId(email), caseStatus, false);
        Map<String, Object> before = caseRow(id);

        deleteCase(login(email), id.toString())
            .andExpect(status().isConflict())
            .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
            .andExpect(jsonPath("$.type").value("about:blank"))
            .andExpect(jsonPath("$.status").value(409))
            .andExpect(jsonPath("$.detail").value("Only draft cases can be deleted."))
            .andExpect(jsonPath("$.instance").value(CASES_PATH + "/" + id))
            .andExpect(jsonPath("$.errorCode").value("INVALID_CASE_STATE"))
            .andExpect(jsonPath("$.timestamp").isNotEmpty())
            .andExpect(jsonPath("$.traceId").isNotEmpty());
        assertThat(caseRow(id)).isEqualTo(before);
    }

    @Test
    void anotherClientsCaseIsNotFound() throws Exception {
        UUID othersCase = insertCase(clientProfileId(registerClient()), "DRAFT", false);
        Map<String, Object> before = caseRow(othersCase);

        expectNotFound(deleteCase(login(email), othersCase.toString()), othersCase.toString());
        assertThat(caseRow(othersCase)).isEqualTo(before);
    }

    @Test
    void anotherClientsSubmittedCaseIsNotFound() throws Exception {
        UUID othersCase = insertCase(clientProfileId(registerClient()), "SUBMITTED", false);

        expectNotFound(deleteCase(login(email), othersCase.toString()), othersCase.toString());
    }

    @Test
    void softDeletedCaseIsNotFound() throws Exception {
        UUID id = insertCase(clientProfileId(email), "DRAFT", true);
        Map<String, Object> before = caseRow(id);

        expectNotFound(deleteCase(login(email), id.toString()), id.toString());
        assertThat(caseRow(id)).isEqualTo(before);
    }

    @Test
    void nonexistentCaseIsNotFound() throws Exception {
        String id = UUID.randomUUID().toString();

        expectNotFound(deleteCase(login(email), id), id);
    }

    @Test
    void invalidCaseIdIsRejected() throws Exception {
        deleteCase(login(email), "not-a-uuid")
            .andExpect(status().isBadRequest())
            .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
            .andExpect(jsonPath("$.detail").value("Parameter 'id' has an invalid value."))
            .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));
    }

    @Test
    void unauthenticatedRequestIsRejected() throws Exception {
        UUID id = insertCase(clientProfileId(email), "DRAFT", false);
        Map<String, Object> before = caseRow(id);
        MvcResult csrf = mockMvc.perform(get("/api/v1/auth/csrf")).andExpect(status().isOk()).andReturn();

        mockMvc
            .perform(delete(CASES_PATH + "/" + id).cookie(csrf.getResponse().getCookie("SESSION"))
                .header("X-XSRF-TOKEN", (String) JsonPath.read(csrf.getResponse().getContentAsString(), "$.token")))
            .andExpect(status().isUnauthorized())
            .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
            .andExpect(jsonPath("$.status").value(401))
            .andExpect(jsonPath("$.detail").value("Authentication required."))
            .andExpect(jsonPath("$.errorCode").value("UNAUTHORIZED"))
            .andExpect(jsonPath("$.timestamp").isNotEmpty())
            .andExpect(jsonPath("$.traceId").isNotEmpty());
        assertThat(caseRow(id)).isEqualTo(before);
    }

    @Test
    void suspendedClientIsRejected() throws Exception {
        UUID id = insertCase(clientProfileId(email), "DRAFT", false);
        Map<String, Object> before = caseRow(id);
        Cookie session = login(email);
        jdbcTemplate.update("UPDATE users SET status = 'SUSPENDED' WHERE email = ?", email);

        deleteCase(session, id.toString())
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.errorCode").value("UNAUTHORIZED"));
        assertThat(caseRow(id)).isEqualTo(before);
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
        UUID id = insertCase(clientProfileId(email), "DRAFT", false);
        Map<String, Object> before = caseRow(id);
        var request = delete(CASES_PATH + "/" + id).cookie(login(email));
        if (token != null) {
            request.header("X-XSRF-TOKEN", token);
        }

        mockMvc.perform(request)
            .andExpect(status().isForbidden())
            .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
            .andExpect(jsonPath("$.errorCode").value("FORBIDDEN"));
        assertThat(caseRow(id)).isEqualTo(before);
    }

    private void expectNotFound(ResultActions result, String id) throws Exception {
        result.andExpect(status().isNotFound())
            .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
            .andExpect(jsonPath("$.type").value("about:blank"))
            .andExpect(jsonPath("$.status").value(404))
            .andExpect(jsonPath("$.detail").value("Case not found."))
            .andExpect(jsonPath("$.instance").value(CASES_PATH + "/" + id))
            .andExpect(jsonPath("$.errorCode").value("CASE_NOT_FOUND"))
            .andExpect(jsonPath("$.timestamp").isNotEmpty())
            .andExpect(jsonPath("$.traceId").isNotEmpty());
    }

    /** Deletes with the session's current CSRF token, which login replaced. */
    private ResultActions deleteCase(Cookie session, String id) throws Exception {
        MvcResult csrf = mockMvc.perform(get("/api/v1/auth/csrf").cookie(session))
            .andExpect(status().isOk())
            .andReturn();
        return mockMvc.perform(delete(CASES_PATH + "/" + id).cookie(session)
            .header("X-XSRF-TOKEN", (String) JsonPath.read(csrf.getResponse().getContentAsString(), "$.token")));
    }

    private Map<String, Object> caseRow(UUID id) {
        return jdbcTemplate.queryForMap("SELECT * FROM cases WHERE id = ?", id);
    }

    private UUID insertCase(UUID clientId, String caseStatus, boolean deleted) {
        return jdbcTemplate.queryForObject("""
                INSERT INTO cases (client_id, category_id, title, description, location, status,
                                   created_at, updated_at, deleted_at)
                VALUES (?, ?, 'Employment dispute', 'I was terminated without prior notice.', 'Quezon City', ?,
                        ?, ?, CASE WHEN ? THEN now() END)
                RETURNING id
                """, UUID.class, clientId, categoryId, caseStatus, Timestamp.from(CREATED_AT),
                Timestamp.from(CREATED_AT), deleted);
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

    private String registerClient() {
        String clientEmail = "client-" + UUID.randomUUID() + "@example.com";
        registrationService.register(new RegisterRequest("John", "Doe", clientEmail, PASSWORD));
        return clientEmail;
    }

    private UUID clientProfileId(String clientEmail) {
        return jdbcTemplate.queryForObject(
                "SELECT p.id FROM client_profiles p JOIN users u ON u.id = p.user_id WHERE u.email = ?", UUID.class,
                clientEmail);
    }

}
