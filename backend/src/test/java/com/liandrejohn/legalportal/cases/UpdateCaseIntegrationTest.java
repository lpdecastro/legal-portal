package com.liandrejohn.legalportal.cases;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.sql.Timestamp;
import java.time.Instant;
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
class UpdateCaseIntegrationTest {

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

    private UUID otherCategoryId;

    @BeforeEach
    void setUp() {
        email = registerClient();
        categoryId = insertCategory(true);
        otherCategoryId = insertCategory(true);
    }

    @Test
    void updatesAllFieldsOfADraft() throws Exception {
        UUID id = insertCase(clientProfileId(email), "DRAFT", false);

        MvcResult result = updateCase(login(email), id.toString(), """
                {"title":"Updated employment dispute","categoryId":"%s",
                 "description":"Updated details about my employment dispute.","location":"Quezon City"}
                """.formatted(otherCategoryId))
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.id").value(id.toString()))
            .andExpect(jsonPath("$.title").value("Updated employment dispute"))
            .andExpect(jsonPath("$.categoryId").value(otherCategoryId.toString()))
            .andExpect(jsonPath("$.description").value("Updated details about my employment dispute."))
            .andExpect(jsonPath("$.location").value("Quezon City"))
            .andExpect(jsonPath("$.status").value("DRAFT"))
            .andExpect(jsonPath("$.createdAt").value(CREATED_AT.toString()))
            .andExpect(jsonPath("$.clientId").doesNotExist())
            .andReturn();

        Instant updatedAt = Instant
            .parse(JsonPath.read(result.getResponse().getContentAsString(), "$.updatedAt"));
        assertThat(updatedAt).isAfter(CREATED_AT);

        Map<String, Object> row = caseRow(id);
        assertThat(row.get("title")).isEqualTo("Updated employment dispute");
        assertThat(row.get("category_id")).isEqualTo(otherCategoryId);
        assertThat(row.get("description")).isEqualTo("Updated details about my employment dispute.");
        assertThat(row.get("location")).isEqualTo("Quezon City");
        assertThat(row.get("status")).isEqualTo("DRAFT");
        assertThat(row.get("client_id")).isEqualTo(clientProfileId(email));
        assertThat(((Timestamp) row.get("created_at")).toInstant()).isEqualTo(CREATED_AT);
        assertThat(((Timestamp) row.get("updated_at")).toInstant()).isEqualTo(updatedAt);
    }

    @Test
    void omittedFieldsAreUnchanged() throws Exception {
        UUID id = insertCase(clientProfileId(email), "DRAFT", false);

        updateCase(login(email), id.toString(), """
                {"location":"Makati City"}
                """)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.title").value("Employment dispute"))
            .andExpect(jsonPath("$.categoryId").value(categoryId.toString()))
            .andExpect(jsonPath("$.description").value("I was terminated without prior notice."))
            .andExpect(jsonPath("$.location").value("Makati City"));
    }

    @Test
    void emptyBodyChangesNothing() throws Exception {
        UUID id = insertCase(clientProfileId(email), "DRAFT", false);
        Map<String, Object> before = caseRow(id);

        updateCase(login(email), id.toString(), "{}").andExpect(status().isOk())
            .andExpect(jsonPath("$.title").value("Employment dispute"))
            .andExpect(jsonPath("$.location").value("Quezon City"));
        assertThat(caseRow(id)).isEqualTo(before);
    }

    @Test
    void optionalFieldsCanBeCleared() throws Exception {
        UUID id = insertCase(clientProfileId(email), "DRAFT", false);

        updateCase(login(email), id.toString(), """
                {"description":null,"location":null}
                """)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.title").value("Employment dispute"))
            .andExpect(jsonPath("$.description").isEmpty())
            .andExpect(jsonPath("$.location").isEmpty());

        Map<String, Object> row = caseRow(id);
        assertThat(row.get("description")).isNull();
        assertThat(row.get("location")).isNull();
    }

    @Test
    void inputIsNormalized() throws Exception {
        UUID id = insertCase(clientProfileId(email), "DRAFT", false);

        updateCase(login(email), id.toString(), """
                {"title":"  Updated dispute  ","description":"   ","location":"  Makati City "}
                """)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.title").value("Updated dispute"))
            .andExpect(jsonPath("$.description").isEmpty())
            .andExpect(jsonPath("$.location").value("Makati City"));
        assertThat(caseRow(id).get("description")).isNull();
    }

    @Test
    void ownershipAndStatusInTheBodyAreIgnored() throws Exception {
        UUID id = insertCase(clientProfileId(email), "DRAFT", false);
        UUID otherClient = clientProfileId(registerClient());

        updateCase(login(email), id.toString(), """
                {"title":"Updated dispute","clientId":"%s","status":"SUBMITTED"}
                """.formatted(otherClient))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("DRAFT"));

        Map<String, Object> row = caseRow(id);
        assertThat(row.get("client_id")).isEqualTo(clientProfileId(email));
        assertThat(row.get("status")).isEqualTo("DRAFT");
    }

    @Test
    void requiredFieldsCannotBeCleared() throws Exception {
        UUID id = insertCase(clientProfileId(email), "DRAFT", false);
        Map<String, Object> before = caseRow(id);

        expectValidationError(updateCase(login(email), id.toString(), """
                {"title":null,"categoryId":null}
                """), id)
            .andExpect(jsonPath("$.fieldErrors[*].field", contains("categoryId", "title")))
            .andExpect(jsonPath("$.fieldErrors[*].message", contains("Category is required", "Title is required")));
        assertThat(caseRow(id)).isEqualTo(before);
    }

    @Test
    void blankTitleIsRejected() throws Exception {
        UUID id = insertCase(clientProfileId(email), "DRAFT", false);
        Map<String, Object> before = caseRow(id);

        expectValidationError(updateCase(login(email), id.toString(), """
                {"title":"   ","location":"Makati City"}
                """), id)
            .andExpect(jsonPath("$.fieldErrors", hasSize(1)))
            .andExpect(jsonPath("$.fieldErrors[0].field").value("title"))
            .andExpect(jsonPath("$.fieldErrors[0].message").value("Title is required"));
        assertThat(caseRow(id)).isEqualTo(before);
    }

    @Test
    void fieldLengthsAreValidated() throws Exception {
        UUID id = insertCase(clientProfileId(email), "DRAFT", false);
        Map<String, Object> before = caseRow(id);

        expectValidationError(updateCase(login(email), id.toString(), """
                {"title":"%s","location":"%s"}
                """.formatted("a".repeat(201), "b".repeat(256))), id)
            .andExpect(jsonPath("$.fieldErrors[*].field", contains("location", "title")))
            .andExpect(jsonPath("$.fieldErrors[*].message",
                    contains("Location must not exceed 255 characters", "Title must not exceed 200 characters")));
        assertThat(caseRow(id)).isEqualTo(before);
    }

    @Test
    void maximumLengthsAreAccepted() throws Exception {
        UUID id = insertCase(clientProfileId(email), "DRAFT", false);

        updateCase(login(email), id.toString(), """
                {"title":"%s","location":"%s"}
                """.formatted("a".repeat(200), "b".repeat(255))).andExpect(status().isOk());
    }

    @Test
    void inactiveCategoryIsRejected() throws Exception {
        expectCategoryRejected(insertCategory(false));
    }

    @Test
    void unknownCategoryIsRejected() throws Exception {
        expectCategoryRejected(UUID.randomUUID());
    }

    @Test
    void malformedCategoryIdIsRejected() throws Exception {
        UUID id = insertCase(clientProfileId(email), "DRAFT", false);
        Map<String, Object> before = caseRow(id);

        expectValidationError(updateCase(login(email), id.toString(), """
                {"categoryId":"not-a-uuid"}
                """), id);
        assertThat(caseRow(id)).isEqualTo(before);
    }

    @ParameterizedTest
    @ValueSource(strings = { "SUBMITTED", "CLOSED" })
    void nonDraftCasesCannotBeUpdated(String caseStatus) throws Exception {
        UUID id = insertCase(clientProfileId(email), caseStatus, false);
        Map<String, Object> before = caseRow(id);

        // An inactive category would be a 400 on a draft; the state check comes first
        updateCase(login(email), id.toString(), """
                {"title":"Updated dispute","categoryId":"%s"}
                """.formatted(insertCategory(false)))
            .andExpect(status().isConflict())
            .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
            .andExpect(jsonPath("$.type").value("about:blank"))
            .andExpect(jsonPath("$.status").value(409))
            .andExpect(jsonPath("$.detail").value("Only draft cases can be edited."))
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

        expectNotFound(updateCase(login(email), othersCase.toString(), """
                {"title":"Hijacked"}
                """), othersCase.toString());
        assertThat(caseRow(othersCase)).isEqualTo(before);
    }

    @Test
    void anotherClientsSubmittedCaseIsNotFound() throws Exception {
        UUID othersCase = insertCase(clientProfileId(registerClient()), "SUBMITTED", false);

        expectNotFound(updateCase(login(email), othersCase.toString(), """
                {"title":"Hijacked"}
                """), othersCase.toString());
    }

    @Test
    void softDeletedCaseIsNotFound() throws Exception {
        UUID id = insertCase(clientProfileId(email), "DRAFT", true);
        Map<String, Object> before = caseRow(id);

        expectNotFound(updateCase(login(email), id.toString(), """
                {"title":"Updated dispute"}
                """), id.toString());
        assertThat(caseRow(id)).isEqualTo(before);
    }

    @Test
    void nonexistentCaseIsNotFound() throws Exception {
        String id = UUID.randomUUID().toString();

        expectNotFound(updateCase(login(email), id, """
                {"title":"Updated dispute"}
                """), id);
    }

    @Test
    void invalidCaseIdIsRejected() throws Exception {
        updateCase(login(email), "not-a-uuid", """
                {"title":"Updated dispute"}
                """)
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
            .perform(patch(CASES_PATH + "/" + id).cookie(csrf.getResponse().getCookie("SESSION"))
                .header("X-XSRF-TOKEN", (String) JsonPath.read(csrf.getResponse().getContentAsString(), "$.token"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"title":"Updated dispute"}
                        """))
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

        updateCase(session, id.toString(), """
                {"title":"Updated dispute"}
                """)
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
        var request = patch(CASES_PATH + "/" + id).cookie(login(email))
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                    {"title":"Updated dispute"}
                    """);
        if (token != null) {
            request.header("X-XSRF-TOKEN", token);
        }

        mockMvc.perform(request)
            .andExpect(status().isForbidden())
            .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
            .andExpect(jsonPath("$.errorCode").value("FORBIDDEN"));
        assertThat(caseRow(id)).isEqualTo(before);
    }

    private void expectCategoryRejected(UUID rejectedCategoryId) throws Exception {
        UUID id = insertCase(clientProfileId(email), "DRAFT", false);
        Map<String, Object> before = caseRow(id);

        expectValidationError(updateCase(login(email), id.toString(), """
                {"title":"Updated dispute","categoryId":"%s"}
                """.formatted(rejectedCategoryId)), id)
            .andExpect(jsonPath("$.fieldErrors", hasSize(1)))
            .andExpect(jsonPath("$.fieldErrors[0].field").value("categoryId"))
            .andExpect(jsonPath("$.fieldErrors[0].message").value("Select an active legal category"));
        assertThat(caseRow(id)).isEqualTo(before);
    }

    private ResultActions expectValidationError(ResultActions result, UUID id) throws Exception {
        return result.andExpect(status().isBadRequest())
            .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
            .andExpect(jsonPath("$.type").value("about:blank"))
            .andExpect(jsonPath("$.title").value("Validation Failed"))
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.instance").value(CASES_PATH + "/" + id))
            .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"))
            .andExpect(jsonPath("$.timestamp").isNotEmpty())
            .andExpect(jsonPath("$.traceId").isNotEmpty());
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

    /** Patches with the session's current CSRF token, which login replaced. */
    private ResultActions updateCase(Cookie session, String id, String body) throws Exception {
        MvcResult csrf = mockMvc.perform(get("/api/v1/auth/csrf").cookie(session))
            .andExpect(status().isOk())
            .andReturn();
        return mockMvc.perform(patch(CASES_PATH + "/" + id).cookie(session)
            .header("X-XSRF-TOKEN", (String) JsonPath.read(csrf.getResponse().getContentAsString(), "$.token"))
            .contentType(MediaType.APPLICATION_JSON)
            .content(body));
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

    private UUID insertCategory(boolean active) {
        return jdbcTemplate.queryForObject(
                "INSERT INTO legal_categories (name, is_active) VALUES (?, ?) RETURNING id", UUID.class,
                "Test Category " + UUID.randomUUID(), active);
    }

}
