package com.liandrejohn.legalportal.dashboard;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
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

/** Cases are inserted directly, so each test controls their status, creation time and deletion. */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class DashboardSummaryIntegrationTest {

    private static final String SUMMARY_PATH = "/api/v1/dashboard/summary";

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
    void countsTheClientsCasesByStatus() throws Exception {
        UUID clientId = clientProfileId(email);
        insertCase(clientId, "Draft 1", "DRAFT", "2026-10-01T08:00:00Z", false);
        insertCase(clientId, "Draft 2", "DRAFT", "2026-10-02T08:00:00Z", false);
        insertCase(clientId, "Draft 3", "DRAFT", "2026-10-03T08:00:00Z", false);
        insertCase(clientId, "Submitted 1", "SUBMITTED", "2026-10-04T08:00:00Z", false);
        insertCase(clientId, "Submitted 2", "SUBMITTED", "2026-10-05T08:00:00Z", false);
        insertCase(clientId, "Closed 1", "CLOSED", "2026-10-06T08:00:00Z", false);

        summary(login(email)).andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.statistics.total").value(6))
            .andExpect(jsonPath("$.statistics.draft").value(3))
            .andExpect(jsonPath("$.statistics.submitted").value(2))
            .andExpect(jsonPath("$.statistics.closed").value(1));
    }

    @Test
    void returnsTheFiveNewestCases() throws Exception {
        UUID clientId = clientProfileId(email);
        for (int day = 1; day <= 7; day++) {
            insertCase(clientId, "Case " + day, day % 2 == 0 ? "SUBMITTED" : "DRAFT",
                    "2026-10-%02dT08:00:00Z".formatted(day), false);
        }

        String body = summary(login(email)).andExpect(status().isOk())
            .andExpect(jsonPath("$.statistics.total").value(7))
            .andExpect(jsonPath("$.recentCases", hasSize(5)))
            .andExpect(jsonPath("$.recentCases[0].title").value("Case 7"))
            .andExpect(jsonPath("$.recentCases[0].status").value("DRAFT"))
            .andExpect(jsonPath("$.recentCases[0].createdAt").value("2026-10-07T08:00:00Z"))
            .andExpect(jsonPath("$.recentCases[1].status").value("SUBMITTED"))
            .andExpect(jsonPath("$.recentCases[0].category").doesNotExist())
            .andExpect(jsonPath("$.recentCases[0].clientId").doesNotExist())
            .andExpect(jsonPath("$.recentCases[0].deletedAt").doesNotExist())
            .andReturn()
            .getResponse()
            .getContentAsString();

        assertThat(JsonPath.<List<String>>read(body, "$.recentCases[*].title")).containsExactly("Case 7", "Case 6",
                "Case 5", "Case 4", "Case 3");
    }

    @Test
    void ordersCasesCreatedAtTheSameInstantById() throws Exception {
        UUID clientId = clientProfileId(email);
        List<String> ids = new ArrayList<>();
        for (int i = 0; i < 6; i++) {
            ids.add(insertCase(clientId, "Case " + i, "DRAFT", "2026-10-15T08:00:00Z", false).toString());
        }
        ids.sort(Comparator.reverseOrder());

        String body = summary(login(email)).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();

        assertThat(JsonPath.<List<String>>read(body, "$.recentCases[*].id")).isEqualTo(ids.subList(0, 5));
    }

    @Test
    void returnsTheRecentCaseFields() throws Exception {
        UUID id = insertCase(clientProfileId(email), "Property dispute", "SUBMITTED", "2026-10-15T08:00:00Z", false);

        summary(login(email)).andExpect(status().isOk()).andExpect(content().json("""
                {
                  "statistics": {"total": 1, "draft": 0, "submitted": 1, "closed": 0},
                  "recentCases": [
                    {"id": "%s", "title": "Property dispute", "status": "SUBMITTED",
                     "createdAt": "2026-10-15T08:00:00Z"}
                  ]
                }
                """.formatted(id), true));
    }

    @Test
    void excludesAnotherClientsAndSoftDeletedCases() throws Exception {
        UUID clientId = clientProfileId(email);
        UUID own = insertCase(clientId, "Property dispute", "SUBMITTED", "2026-10-15T08:00:00Z", false);
        insertCase(clientId, "Deleted draft", "DRAFT", "2026-10-16T08:00:00Z", true);
        insertCase(clientProfileId(registerClient()), "Someone else's case", "CLOSED", "2026-10-17T08:00:00Z",
                false);

        summary(login(email)).andExpect(status().isOk())
            .andExpect(jsonPath("$.statistics.total").value(1))
            .andExpect(jsonPath("$.statistics.draft").value(0))
            .andExpect(jsonPath("$.statistics.submitted").value(1))
            .andExpect(jsonPath("$.statistics.closed").value(0))
            .andExpect(jsonPath("$.recentCases", hasSize(1)))
            .andExpect(jsonPath("$.recentCases[0].id").value(own.toString()));
    }

    @Test
    void returnsZeroCountsAndNoCasesWhenTheClientHasNone() throws Exception {
        insertCase(clientProfileId(registerClient()), "Someone else's case", "DRAFT", "2026-10-16T08:00:00Z",
                false);

        summary(login(email)).andExpect(status().isOk()).andExpect(content().json("""
                {"statistics":{"total":0,"draft":0,"submitted":0,"closed":0},"recentCases":[]}
                """, true));
    }

    @Test
    void unauthenticatedRequestIsRejected() throws Exception {
        mockMvc.perform(get(SUMMARY_PATH))
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

        summary(session).andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.errorCode").value("UNAUTHORIZED"));
    }

    private ResultActions summary(Cookie session) throws Exception {
        return mockMvc.perform(get(SUMMARY_PATH).cookie(session));
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
