package com.liandrejohn.legalportal.cases;

import static org.hamcrest.Matchers.hasSize;
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

/** Cases are inserted directly, since submitting and closing have no API yet. */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class ListCasesIntegrationTest {

    private static final String CASES_PATH = "/api/v1/cases";

    private static final String PASSWORD = "password123456";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private RegistrationService registrationService;

    private String email;

    private String categoryName;

    private UUID categoryId;

    @BeforeEach
    void setUp() {
        email = registerClient();
        categoryName = "Test Category " + UUID.randomUUID();
        categoryId = jdbcTemplate.queryForObject(
                "INSERT INTO legal_categories (name, is_active) VALUES (?, true) RETURNING id", UUID.class,
                categoryName);
    }

    @Test
    void returnsTheClientsCasesNewestFirst() throws Exception {
        UUID clientId = clientProfileId(email);
        UUID draft = insertCase(clientId, "Employment dispute", "DRAFT", "2026-10-13T08:00:00Z", false);
        UUID submitted = insertCase(clientId, "Property dispute", "SUBMITTED", "2026-10-15T08:00:00Z", false);
        UUID closed = insertCase(clientId, "Unpaid wages", "CLOSED", "2026-10-14T08:00:00Z", false);

        listCases(login(email)).andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.content", hasSize(3)))
            .andExpect(jsonPath("$.content[0].id").value(submitted.toString()))
            .andExpect(jsonPath("$.content[0].title").value("Property dispute"))
            .andExpect(jsonPath("$.content[0].category").value(categoryName))
            .andExpect(jsonPath("$.content[0].status").value("SUBMITTED"))
            .andExpect(jsonPath("$.content[0].createdAt").value("2026-10-15T08:00:00Z"))
            .andExpect(jsonPath("$.content[0].clientId").doesNotExist())
            .andExpect(jsonPath("$.content[0].description").doesNotExist())
            .andExpect(jsonPath("$.content[0].deletedAt").doesNotExist())
            .andExpect(jsonPath("$.content[1].id").value(closed.toString()))
            .andExpect(jsonPath("$.content[1].status").value("CLOSED"))
            .andExpect(jsonPath("$.content[2].id").value(draft.toString()))
            .andExpect(jsonPath("$.content[2].status").value("DRAFT"));
    }

    @Test
    void listsADraftCreatedThroughTheApi() throws Exception {
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
        String body = created.getResponse().getContentAsString();

        listCases(session).andExpect(status().isOk())
            .andExpect(jsonPath("$.content", hasSize(1)))
            .andExpect(jsonPath("$.content[0].id").value((String) JsonPath.read(body, "$.id")))
            .andExpect(jsonPath("$.content[0].category").value(categoryName))
            .andExpect(jsonPath("$.content[0].status").value("DRAFT"))
            .andExpect(jsonPath("$.content[0].createdAt").value((String) JsonPath.read(body, "$.createdAt")));
    }

    @Test
    void excludesAnotherClientsCases() throws Exception {
        UUID own = insertCase(clientProfileId(email), "Employment dispute", "DRAFT", "2026-10-15T08:00:00Z", false);
        insertCase(clientProfileId(registerClient()), "Someone else's case", "SUBMITTED", "2026-10-16T08:00:00Z",
                false);

        listCases(login(email)).andExpect(status().isOk())
            .andExpect(jsonPath("$.content", hasSize(1)))
            .andExpect(jsonPath("$.content[0].id").value(own.toString()));
    }

    @Test
    void excludesSoftDeletedCases() throws Exception {
        UUID clientId = clientProfileId(email);
        UUID kept = insertCase(clientId, "Employment dispute", "DRAFT", "2026-10-15T08:00:00Z", false);
        insertCase(clientId, "Deleted draft", "DRAFT", "2026-10-16T08:00:00Z", true);

        listCases(login(email)).andExpect(status().isOk())
            .andExpect(jsonPath("$.content", hasSize(1)))
            .andExpect(jsonPath("$.content[0].id").value(kept.toString()));
    }

    @Test
    void returnsAnEmptyListWhenTheClientHasNoCases() throws Exception {
        insertCase(clientProfileId(registerClient()), "Someone else's case", "DRAFT", "2026-10-16T08:00:00Z",
                false);

        listCases(login(email)).andExpect(status().isOk())
            .andExpect(content().json("""
                    {"content":[]}
                    """, true));
    }

    @Test
    void unauthenticatedRequestIsRejected() throws Exception {
        mockMvc.perform(get(CASES_PATH))
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
        insertCase(clientProfileId(email), "Employment dispute", "DRAFT", "2026-10-15T08:00:00Z", false);
        Cookie session = login(email);
        jdbcTemplate.update("UPDATE users SET status = 'SUSPENDED' WHERE email = ?", email);

        listCases(session).andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.errorCode").value("UNAUTHORIZED"));
    }

    private ResultActions listCases(Cookie session) throws Exception {
        return mockMvc.perform(get(CASES_PATH).cookie(session));
    }

    private UUID insertCase(UUID clientId, String title, String caseStatus, String createdAt, boolean deleted) {
        return jdbcTemplate.queryForObject("""
                INSERT INTO cases (client_id, category_id, title, status, created_at, updated_at, deleted_at)
                VALUES (?, ?, ?, ?, ?::timestamptz, ?::timestamptz, CASE WHEN ? THEN now() END)
                RETURNING id
                """, UUID.class, clientId, categoryId, title, caseStatus, createdAt, createdAt, deleted);
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
