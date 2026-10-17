package com.liandrejohn.legalportal.cases;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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

/** Cases are inserted directly, since submitting, closing and deleting have no API yet. */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class GetCaseIntegrationTest {

    private static final String CASES_PATH = "/api/v1/cases";

    private static final String PASSWORD = "password123456";

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
    void returnsTheClientsOwnDraft() throws Exception {
        UUID id = jdbcTemplate.queryForObject("""
                INSERT INTO cases (client_id, category_id, title, description, location, created_at, updated_at)
                VALUES (?, ?, 'Employment dispute', 'I was terminated without prior notice.', 'Quezon City',
                        '2026-10-15T04:00:00Z', '2026-10-15T05:30:00Z')
                RETURNING id
                """, UUID.class, clientProfileId(email), categoryId);

        getCase(login(email), id.toString()).andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.id").value(id.toString()))
            .andExpect(jsonPath("$.title").value("Employment dispute"))
            .andExpect(jsonPath("$.categoryId").value(categoryId.toString()))
            .andExpect(jsonPath("$.description").value("I was terminated without prior notice."))
            .andExpect(jsonPath("$.location").value("Quezon City"))
            .andExpect(jsonPath("$.status").value("DRAFT"))
            .andExpect(jsonPath("$.createdAt").value("2026-10-15T04:00:00Z"))
            .andExpect(jsonPath("$.updatedAt").value("2026-10-15T05:30:00Z"))
            .andExpect(jsonPath("$.clientId").doesNotExist())
            .andExpect(jsonPath("$.deletedAt").doesNotExist());
    }

    @Test
    void returnsADraftCreatedThroughTheApi() throws Exception {
        Cookie session = login(email);
        MvcResult csrf = mockMvc.perform(get("/api/v1/auth/csrf").cookie(session)).andReturn();
        MvcResult created = mockMvc.perform(post(CASES_PATH).cookie(session)
            .header("X-XSRF-TOKEN", (String) JsonPath.read(csrf.getResponse().getContentAsString(), "$.token"))
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                    {"title":"Employment dispute","categoryId":"%s"}
                    """.formatted(categoryId)))
            .andExpect(status().isCreated())
            .andReturn();

        getCase(session, created.getResponse().getHeader("Location").substring(CASES_PATH.length() + 1))
            .andExpect(status().isOk())
            .andExpect(content().json(created.getResponse().getContentAsString(), true));
    }

    @ParameterizedTest
    @ValueSource(strings = { "SUBMITTED", "CLOSED" })
    void returnsSubmittedAndClosedCases(String caseStatus) throws Exception {
        UUID id = insertCase(clientProfileId(email), caseStatus, false);

        getCase(login(email), id.toString()).andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value(caseStatus));
    }

    @Test
    void anotherClientsCaseIsNotFound() throws Exception {
        UUID othersCase = insertCase(clientProfileId(registerClient()), "DRAFT", false);

        expectNotFound(getCase(login(email), othersCase.toString()), othersCase.toString());
    }

    @Test
    void softDeletedCaseIsNotFound() throws Exception {
        UUID id = insertCase(clientProfileId(email), "DRAFT", true);

        expectNotFound(getCase(login(email), id.toString()), id.toString());
    }

    @Test
    void nonexistentCaseIsNotFound() throws Exception {
        String id = UUID.randomUUID().toString();

        expectNotFound(getCase(login(email), id), id);
    }

    @Test
    void invalidCaseIdIsRejected() throws Exception {
        getCase(login(email), "not-a-uuid").andExpect(status().isBadRequest())
            .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
            .andExpect(jsonPath("$.type").value("about:blank"))
            .andExpect(jsonPath("$.title").value("Validation Failed"))
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.detail").value("Parameter 'id' has an invalid value."))
            .andExpect(jsonPath("$.instance").value(CASES_PATH + "/not-a-uuid"))
            .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"))
            .andExpect(jsonPath("$.timestamp").isNotEmpty())
            .andExpect(jsonPath("$.traceId").isNotEmpty());
    }

    @Test
    void unauthenticatedRequestIsRejected() throws Exception {
        UUID id = insertCase(clientProfileId(email), "DRAFT", false);

        mockMvc.perform(get(CASES_PATH + "/" + id))
            .andExpect(status().isUnauthorized())
            .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
            .andExpect(jsonPath("$.status").value(401))
            .andExpect(jsonPath("$.detail").value("Authentication required."))
            .andExpect(jsonPath("$.errorCode").value("UNAUTHORIZED"))
            .andExpect(jsonPath("$.timestamp").isNotEmpty())
            .andExpect(jsonPath("$.traceId").isNotEmpty());
    }

    @Test
    void suspendedClientIsRejected() throws Exception {
        UUID id = insertCase(clientProfileId(email), "DRAFT", false);
        Cookie session = login(email);
        jdbcTemplate.update("UPDATE users SET status = 'SUSPENDED' WHERE email = ?", email);

        getCase(session, id.toString()).andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.errorCode").value("UNAUTHORIZED"));
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

    private ResultActions getCase(Cookie session, String id) throws Exception {
        return mockMvc.perform(get(CASES_PATH + "/" + id).cookie(session));
    }

    private UUID insertCase(UUID clientId, String caseStatus, boolean deleted) {
        return jdbcTemplate.queryForObject("""
                INSERT INTO cases (client_id, category_id, title, status, deleted_at)
                VALUES (?, ?, 'Employment dispute', ?, CASE WHEN ? THEN now() END)
                RETURNING id
                """, UUID.class, clientId, categoryId, caseStatus, deleted);
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
