package com.liandrejohn.legalportal.cases;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

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
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import com.liandrejohn.legalportal.TestcontainersConfiguration;
import com.liandrejohn.legalportal.auth.RegisterRequest;
import com.liandrejohn.legalportal.auth.RegistrationService;

/**
 * Cases are inserted directly with fixed timestamps, so tests can tell whether a row changed. Incomplete drafts and
 * closed cases are inserted too, since the API can't produce every combination.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class SubmitCaseIntegrationTest {

    private static final String CASES_PATH = "/api/v1/cases";

    private static final String PASSWORD = "password123456";

    private static final Instant CREATED_AT = Instant.parse("2026-10-15T04:00:00Z");

    private static final String TITLE = "Employment dispute";

    private static final String DESCRIPTION = "I was terminated without prior notice.";

    private static final String LOCATION = "Quezon City";

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
        categoryId = insertCategory(true);
    }

    @Test
    void submitsADraft() throws Exception {
        UUID id = insertCase(clientProfileId(email), "DRAFT", false);
        Instant before = Instant.now().truncatedTo(ChronoUnit.MICROS);

        MvcResult result = submit(login(email), id.toString())
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.id").value(id.toString()))
            .andExpect(jsonPath("$.title").value(TITLE))
            .andExpect(jsonPath("$.categoryId").value(categoryId.toString()))
            .andExpect(jsonPath("$.description").value(DESCRIPTION))
            .andExpect(jsonPath("$.location").value(LOCATION))
            .andExpect(jsonPath("$.status").value("SUBMITTED"))
            .andExpect(jsonPath("$.createdAt").value(CREATED_AT.toString()))
            .andExpect(jsonPath("$.clientId").doesNotExist())
            .andReturn();

        Map<String, Object> row = caseRow(id);
        Instant submittedAt = ((Timestamp) row.get("submitted_at")).toInstant();
        Instant updatedAt = ((Timestamp) row.get("updated_at")).toInstant();
        assertThat(row.get("status")).isEqualTo("SUBMITTED");
        assertThat(submittedAt).isBetween(before, Instant.now());
        assertThat(updatedAt).isBetween(before, Instant.now());
        assertThat(((Timestamp) row.get("created_at")).toInstant()).isEqualTo(CREATED_AT);
        assertThat(row.get("closed_at")).isNull();
        assertThat(row.get("deleted_at")).isNull();

        String body = result.getResponse().getContentAsString();
        assertThat(Instant.parse(JsonPath.read(body, "$.submittedAt"))).isEqualTo(submittedAt);
        assertThat(Instant.parse(JsonPath.read(body, "$.updatedAt"))).isEqualTo(updatedAt);
    }

    @Test
    void submittedCaseIsReturnedByGet() throws Exception {
        UUID id = insertCase(clientProfileId(email), "DRAFT", false);
        Cookie session = login(email);

        submit(session, id.toString()).andExpect(status().isOk());

        mockMvc.perform(get(CASES_PATH + "/" + id).cookie(session))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("SUBMITTED"))
            .andExpect(jsonPath("$.submittedAt").isNotEmpty());
    }

    @Test
    void incompleteDraftIsRejected() throws Exception {
        UUID id = insertDraft(clientProfileId(email), "   ", categoryId, null, " ");
        Map<String, Object> before = caseRow(id);

        submit(login(email), id.toString())
            .andExpect(status().isBadRequest())
            .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
            .andExpect(jsonPath("$.type").value("about:blank"))
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"))
            .andExpect(jsonPath("$.instance").value(CASES_PATH + "/" + id + "/submit"))
            .andExpect(jsonPath("$.fieldErrors.length()").value(3))
            .andExpect(jsonPath("$.fieldErrors[0].field").value("title"))
            .andExpect(jsonPath("$.fieldErrors[0].message").value("Title is required"))
            .andExpect(jsonPath("$.fieldErrors[1].field").value("description"))
            .andExpect(jsonPath("$.fieldErrors[1].message").value("Description is required"))
            .andExpect(jsonPath("$.fieldErrors[2].field").value("location"))
            .andExpect(jsonPath("$.fieldErrors[2].message").value("Location is required"))
            .andExpect(jsonPath("$.timestamp").isNotEmpty())
            .andExpect(jsonPath("$.traceId").isNotEmpty());
        assertThat(caseRow(id)).isEqualTo(before);
    }

    @Test
    void missingDescriptionIsRejected() throws Exception {
        expectSingleFieldError(insertDraft(clientProfileId(email), TITLE, categoryId, "", LOCATION), "description",
                "Description is required");
    }

    @Test
    void missingLocationIsRejected() throws Exception {
        expectSingleFieldError(insertDraft(clientProfileId(email), TITLE, categoryId, DESCRIPTION, null), "location",
                "Location is required");
    }

    @Test
    void inactiveCategoryIsRejected() throws Exception {
        UUID inactive = insertCategory(false);

        expectSingleFieldError(insertDraft(clientProfileId(email), TITLE, inactive, DESCRIPTION, LOCATION),
                "categoryId", "Select an active legal category");
    }

    @Test
    void categoryDeactivatedAfterDraftingIsRejected() throws Exception {
        UUID id = insertCase(clientProfileId(email), "DRAFT", false);
        jdbcTemplate.update("UPDATE legal_categories SET is_active = false WHERE id = ?", categoryId);

        expectSingleFieldError(id, "categoryId", "Select an active legal category");
    }

    @ParameterizedTest
    @ValueSource(strings = { "SUBMITTED", "CLOSED" })
    void nonDraftCasesCannotBeSubmitted(String caseStatus) throws Exception {
        UUID id = insertCase(clientProfileId(email), caseStatus, false);
        Map<String, Object> before = caseRow(id);

        expectInvalidState(submit(login(email), id.toString()), id);
        assertThat(caseRow(id)).isEqualTo(before);
    }

    @Test
    void repeatedSubmissionIsRejected() throws Exception {
        UUID id = insertCase(clientProfileId(email), "DRAFT", false);
        Cookie session = login(email);

        submit(session, id.toString()).andExpect(status().isOk());
        Map<String, Object> afterFirstSubmit = caseRow(id);

        expectInvalidState(submit(session, id.toString()), id);
        assertThat(caseRow(id)).isEqualTo(afterFirstSubmit);
    }

    @Test
    void concurrentSubmissionsSucceedOnlyOnce() throws Exception {
        UUID id = insertCase(clientProfileId(email), "DRAFT", false);
        int requests = 5;
        List<MockHttpServletRequestBuilder> prepared = new ArrayList<>();
        for (int i = 0; i < requests; i++) {
            prepared.add(submitRequest(login(email), id.toString()));
        }

        CountDownLatch start = new CountDownLatch(1);
        List<Future<Integer>> statuses = new ArrayList<>();
        try (ExecutorService executor = Executors.newFixedThreadPool(requests)) {
            for (MockHttpServletRequestBuilder request : prepared) {
                statuses.add(executor.submit(() -> {
                    start.await();
                    return mockMvc.perform(request).andReturn().getResponse().getStatus();
                }));
            }
            start.countDown();
        }

        List<Integer> results = new ArrayList<>();
        for (Future<Integer> future : statuses) {
            results.add(future.get());
        }
        assertThat(results).containsOnlyOnce(200).containsOnly(200, 409);
        assertThat(caseRow(id).get("status")).isEqualTo("SUBMITTED");
    }

    @Test
    void anotherClientsCaseIsNotFound() throws Exception {
        UUID othersCase = insertCase(clientProfileId(registerClient()), "DRAFT", false);
        Map<String, Object> before = caseRow(othersCase);

        expectNotFound(submit(login(email), othersCase.toString()), othersCase.toString());
        assertThat(caseRow(othersCase)).isEqualTo(before);
    }

    @Test
    void anotherClientsSubmittedCaseIsNotFound() throws Exception {
        UUID othersCase = insertCase(clientProfileId(registerClient()), "SUBMITTED", false);

        expectNotFound(submit(login(email), othersCase.toString()), othersCase.toString());
    }

    @Test
    void softDeletedCaseIsNotFound() throws Exception {
        UUID id = insertCase(clientProfileId(email), "DRAFT", true);
        Map<String, Object> before = caseRow(id);

        expectNotFound(submit(login(email), id.toString()), id.toString());
        assertThat(caseRow(id)).isEqualTo(before);
    }

    @Test
    void nonexistentCaseIsNotFound() throws Exception {
        String id = UUID.randomUUID().toString();

        expectNotFound(submit(login(email), id), id);
    }

    @Test
    void invalidCaseIdIsRejected() throws Exception {
        submit(login(email), "not-a-uuid")
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
            .perform(post(CASES_PATH + "/" + id + "/submit").cookie(csrf.getResponse().getCookie("SESSION"))
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

        submit(session, id.toString())
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
        var request = post(CASES_PATH + "/" + id + "/submit").cookie(login(email));
        if (token != null) {
            request.header("X-XSRF-TOKEN", token);
        }

        mockMvc.perform(request)
            .andExpect(status().isForbidden())
            .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
            .andExpect(jsonPath("$.errorCode").value("FORBIDDEN"));
        assertThat(caseRow(id)).isEqualTo(before);
    }

    private void expectSingleFieldError(UUID id, String field, String message) throws Exception {
        Map<String, Object> before = caseRow(id);

        submit(login(email), id.toString())
            .andExpect(status().isBadRequest())
            .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
            .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"))
            .andExpect(jsonPath("$.fieldErrors.length()").value(1))
            .andExpect(jsonPath("$.fieldErrors[0].field").value(field))
            .andExpect(jsonPath("$.fieldErrors[0].message").value(message));
        assertThat(caseRow(id)).isEqualTo(before);
    }

    private void expectInvalidState(ResultActions result, UUID id) throws Exception {
        result.andExpect(status().isConflict())
            .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
            .andExpect(jsonPath("$.type").value("about:blank"))
            .andExpect(jsonPath("$.status").value(409))
            .andExpect(jsonPath("$.detail").value("Only draft cases can be submitted."))
            .andExpect(jsonPath("$.instance").value(CASES_PATH + "/" + id + "/submit"))
            .andExpect(jsonPath("$.errorCode").value("INVALID_CASE_STATE"))
            .andExpect(jsonPath("$.timestamp").isNotEmpty())
            .andExpect(jsonPath("$.traceId").isNotEmpty());
    }

    private void expectNotFound(ResultActions result, String id) throws Exception {
        result.andExpect(status().isNotFound())
            .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
            .andExpect(jsonPath("$.type").value("about:blank"))
            .andExpect(jsonPath("$.status").value(404))
            .andExpect(jsonPath("$.detail").value("Case not found."))
            .andExpect(jsonPath("$.instance").value(CASES_PATH + "/" + id + "/submit"))
            .andExpect(jsonPath("$.errorCode").value("CASE_NOT_FOUND"))
            .andExpect(jsonPath("$.timestamp").isNotEmpty())
            .andExpect(jsonPath("$.traceId").isNotEmpty());
    }

    private ResultActions submit(Cookie session, String id) throws Exception {
        return mockMvc.perform(submitRequest(session, id));
    }

    /** A submit request with the session's current CSRF token, which login replaced. */
    private MockHttpServletRequestBuilder submitRequest(Cookie session, String id) throws Exception {
        MvcResult csrf = mockMvc.perform(get("/api/v1/auth/csrf").cookie(session))
            .andExpect(status().isOk())
            .andReturn();
        return post(CASES_PATH + "/" + id + "/submit").cookie(session)
            .header("X-XSRF-TOKEN", (String) JsonPath.read(csrf.getResponse().getContentAsString(), "$.token"));
    }

    private Map<String, Object> caseRow(UUID id) {
        return jdbcTemplate.queryForMap("SELECT * FROM cases WHERE id = ?", id);
    }

    private UUID insertCategory(boolean active) {
        return jdbcTemplate.queryForObject(
                "INSERT INTO legal_categories (name, is_active) VALUES (?, ?) RETURNING id", UUID.class,
                "Test Category " + UUID.randomUUID(), active);
    }

    /** A complete case in {@code caseStatus}. */
    private UUID insertCase(UUID clientId, String caseStatus, boolean deleted) {
        return jdbcTemplate.queryForObject("""
                INSERT INTO cases (client_id, category_id, title, description, location, status,
                                   created_at, updated_at, submitted_at, closed_at, deleted_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?,
                        CASE WHEN ? <> 'DRAFT' THEN now() END, CASE WHEN ? = 'CLOSED' THEN now() END,
                        CASE WHEN ? THEN now() END)
                RETURNING id
                """, UUID.class, clientId, categoryId, TITLE, DESCRIPTION, LOCATION, caseStatus,
                Timestamp.from(CREATED_AT), Timestamp.from(CREATED_AT), caseStatus, caseStatus, deleted);
    }

    /** A draft with the given fields, which may be incomplete. */
    private UUID insertDraft(UUID clientId, String title, UUID draftCategoryId, String description, String location) {
        return jdbcTemplate.queryForObject("""
                INSERT INTO cases (client_id, category_id, title, description, location, status,
                                   created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, 'DRAFT', ?, ?)
                RETURNING id
                """, UUID.class, clientId, draftCategoryId, title, description, location,
                Timestamp.from(CREATED_AT), Timestamp.from(CREATED_AT));
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
