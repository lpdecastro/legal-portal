package com.liandrejohn.legalportal.cases;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;

import com.liandrejohn.legalportal.TestcontainersConfiguration;
import com.liandrejohn.legalportal.auth.RegisterRequest;
import com.liandrejohn.legalportal.auth.RegistrationService;

/** Each test registers its own client and categories, so cases are counted per client. */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class CreateCaseIntegrationTest {

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
        categoryId = insertCategory(true);
    }

    @Test
    void createsADraftOwnedByTheSessionsClient() throws Exception {
        MvcResult result = createCase(login(email), """
                {"title":"Employment dispute","categoryId":"%s",
                 "description":"I was terminated without prior notice.","location":"Quezon City"}
                """.formatted(categoryId))
            .andExpect(status().isCreated())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.id").isNotEmpty())
            .andExpect(jsonPath("$.title").value("Employment dispute"))
            .andExpect(jsonPath("$.categoryId").value(categoryId.toString()))
            .andExpect(jsonPath("$.description").value("I was terminated without prior notice."))
            .andExpect(jsonPath("$.location").value("Quezon City"))
            .andExpect(jsonPath("$.status").value("DRAFT"))
            .andExpect(jsonPath("$.createdAt").value(matchesPattern("\\d{4}-\\d{2}-\\d{2}T.*Z")))
            .andExpect(jsonPath("$.clientId").doesNotExist())
            .andExpect(jsonPath("$.deletedAt").doesNotExist())
            .andReturn();

        String id = JsonPath.read(result.getResponse().getContentAsString(), "$.id");
        assertThat(result.getResponse().getHeader("Location")).isEqualTo(CASES_PATH + "/" + id);
        assertThat((String) JsonPath.read(result.getResponse().getContentAsString(), "$.updatedAt"))
            .isEqualTo(JsonPath.read(result.getResponse().getContentAsString(), "$.createdAt"));

        Map<String, Object> row = jdbcTemplate.queryForMap("SELECT * FROM cases WHERE id = ?::uuid", id);
        assertThat(row.get("client_id")).isEqualTo(clientProfileId(email));
        assertThat(row.get("category_id")).isEqualTo(categoryId);
        assertThat(row.get("status")).isEqualTo("DRAFT");
        assertThat(row.get("submitted_at")).isNull();
        assertThat(row.get("closed_at")).isNull();
        assertThat(row.get("deleted_at")).isNull();
    }

    @Test
    void optionalFieldsCanBeOmitted() throws Exception {
        createCase(login(email), """
                {"title":"Employment dispute","categoryId":"%s"}
                """.formatted(categoryId))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.description").isEmpty())
            .andExpect(jsonPath("$.location").isEmpty())
            .andExpect(jsonPath("$.status").value("DRAFT"));
    }

    @Test
    void inputIsNormalized() throws Exception {
        createCase(login(email), """
                {"title":"  Employment dispute  ","categoryId":"%s","description":"   ","location":"  Quezon City "}
                """.formatted(categoryId))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.title").value("Employment dispute"))
            .andExpect(jsonPath("$.description").isEmpty())
            .andExpect(jsonPath("$.location").value("Quezon City"));
    }

    @Test
    void ownershipInTheBodyIsIgnored() throws Exception {
        String otherEmail = registerClient();

        MvcResult result = createCase(login(email), """
                {"title":"Employment dispute","categoryId":"%s","clientId":"%s","status":"SUBMITTED"}
                """.formatted(categoryId, clientProfileId(otherEmail)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.status").value("DRAFT"))
            .andReturn();

        String id = JsonPath.read(result.getResponse().getContentAsString(), "$.id");
        assertThat(jdbcTemplate.queryForObject("SELECT client_id FROM cases WHERE id = ?::uuid", UUID.class, id))
            .isEqualTo(clientProfileId(email));
    }

    @Test
    void requiredFieldsAreValidated() throws Exception {
        expectValidationError(createCase(login(email), "{}"))
            .andExpect(jsonPath("$.fieldErrors[*].field", contains("categoryId", "title")))
            .andExpect(jsonPath("$.fieldErrors[*].message", contains("Category is required", "Title is required")));
        assertNoCases();
    }

    @Test
    void blankTitleIsRejected() throws Exception {
        expectValidationError(createCase(login(email), """
                {"title":"   ","categoryId":"%s"}
                """.formatted(categoryId)))
            .andExpect(jsonPath("$.fieldErrors[0].field").value("title"))
            .andExpect(jsonPath("$.fieldErrors[0].message").value("Title is required"));
        assertNoCases();
    }

    @Test
    void fieldLengthsAreValidated() throws Exception {
        expectValidationError(createCase(login(email), """
                {"title":"%s","categoryId":"%s","location":"%s"}
                """.formatted("a".repeat(201), categoryId, "b".repeat(256))))
            .andExpect(jsonPath("$.fieldErrors[*].field", contains("location", "title")))
            .andExpect(jsonPath("$.fieldErrors[*].message",
                    contains("Location must not exceed 255 characters", "Title must not exceed 200 characters")));
        assertNoCases();
    }

    @Test
    void maximumLengthsAreAccepted() throws Exception {
        createCase(login(email), """
                {"title":"%s","categoryId":"%s","location":"%s"}
                """.formatted("a".repeat(200), categoryId, "b".repeat(255))).andExpect(status().isCreated());
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
        expectValidationError(createCase(login(email), """
                {"title":"Employment dispute","categoryId":"not-a-uuid"}
                """));
        assertNoCases();
    }

    @Test
    void unauthenticatedRequestIsRejected() throws Exception {
        MvcResult csrf = mockMvc.perform(get("/api/v1/auth/csrf")).andExpect(status().isOk()).andReturn();

        mockMvc
            .perform(post(CASES_PATH).cookie(csrf.getResponse().getCookie("SESSION"))
                .header("X-XSRF-TOKEN", (String) JsonPath.read(csrf.getResponse().getContentAsString(), "$.token"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"title":"Employment dispute","categoryId":"%s"}
                        """.formatted(categoryId)))
            .andExpect(status().isUnauthorized())
            .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
            .andExpect(jsonPath("$.type").value("about:blank"))
            .andExpect(jsonPath("$.status").value(401))
            .andExpect(jsonPath("$.detail").value("Authentication required."))
            .andExpect(jsonPath("$.instance").value(CASES_PATH))
            .andExpect(jsonPath("$.errorCode").value("UNAUTHORIZED"))
            .andExpect(jsonPath("$.timestamp").isNotEmpty())
            .andExpect(jsonPath("$.traceId").isNotEmpty());
        assertNoCases();
    }

    @Test
    void suspendedClientIsRejected() throws Exception {
        Cookie session = login(email);
        jdbcTemplate.update("UPDATE users SET status = 'SUSPENDED' WHERE email = ?", email);

        createCase(session, """
                {"title":"Employment dispute","categoryId":"%s"}
                """.formatted(categoryId))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.errorCode").value("UNAUTHORIZED"));
        assertNoCases();
    }

    @Test
    void missingCsrfTokenIsRejected() throws Exception {
        mockMvc
            .perform(post(CASES_PATH).cookie(login(email))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"title":"Employment dispute","categoryId":"%s"}
                        """.formatted(categoryId)))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.errorCode").value("FORBIDDEN"));
        assertNoCases();
    }

    private void expectCategoryRejected(UUID rejectedCategoryId) throws Exception {
        expectValidationError(createCase(login(email), """
                {"title":"Employment dispute","categoryId":"%s"}
                """.formatted(rejectedCategoryId)))
            .andExpect(jsonPath("$.fieldErrors", hasSize(1)))
            .andExpect(jsonPath("$.fieldErrors[0].field").value("categoryId"))
            .andExpect(jsonPath("$.fieldErrors[0].message").value("Select an active legal category"));
        assertNoCases();
    }

    private ResultActions expectValidationError(ResultActions result) throws Exception {
        return result.andExpect(status().isBadRequest())
            .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
            .andExpect(jsonPath("$.type").value("about:blank"))
            .andExpect(jsonPath("$.title").value("Validation Failed"))
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.instance").value(CASES_PATH))
            .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"))
            .andExpect(jsonPath("$.timestamp").isNotEmpty())
            .andExpect(jsonPath("$.traceId").isNotEmpty());
    }

    private void assertNoCases() {
        assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM cases WHERE client_id = ?", Long.class,
                clientProfileId(email)))
            .isZero();
    }

    /** Posts with the session's current CSRF token, which login replaced. */
    private ResultActions createCase(Cookie session, String body) throws Exception {
        MvcResult csrf = mockMvc.perform(get("/api/v1/auth/csrf").cookie(session))
            .andExpect(status().isOk())
            .andReturn();
        return mockMvc.perform(post(CASES_PATH).cookie(session)
            .header("X-XSRF-TOKEN", (String) JsonPath.read(csrf.getResponse().getContentAsString(), "$.token"))
            .contentType(MediaType.APPLICATION_JSON)
            .content(body));
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
