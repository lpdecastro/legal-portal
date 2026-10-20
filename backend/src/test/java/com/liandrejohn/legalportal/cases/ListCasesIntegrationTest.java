package com.liandrejohn.legalportal.cases;

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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
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
                    {"content":[],"page":{"size":10,"number":0,"totalElements":0,"totalPages":0}}
                    """, true));
    }

    @Test
    void defaultsToTheFirstPageOfTenWithMetadata() throws Exception {
        UUID clientId = clientProfileId(email);
        for (int day = 1; day <= 12; day++) {
            insertCase(clientId, "Case " + day, "DRAFT", "2026-10-%02dT08:00:00Z".formatted(day), false);
        }

        listCases(login(email)).andExpect(status().isOk())
            .andExpect(jsonPath("$.content", hasSize(10)))
            .andExpect(jsonPath("$.content[0].title").value("Case 12"))
            .andExpect(jsonPath("$.content[9].title").value("Case 3"))
            .andExpect(jsonPath("$.page.size").value(10))
            .andExpect(jsonPath("$.page.number").value(0))
            .andExpect(jsonPath("$.page.totalElements").value(12))
            .andExpect(jsonPath("$.page.totalPages").value(2));
    }

    @Test
    void returnsTheRequestedPage() throws Exception {
        UUID clientId = clientProfileId(email);
        for (int day = 1; day <= 5; day++) {
            insertCase(clientId, "Case " + day, "DRAFT", "2026-10-%02dT08:00:00Z".formatted(day), false);
        }

        listCases(login(email), 2, 2).andExpect(status().isOk())
            .andExpect(jsonPath("$.content", hasSize(1)))
            .andExpect(jsonPath("$.content[0].title").value("Case 1"))
            .andExpect(jsonPath("$.page.size").value(2))
            .andExpect(jsonPath("$.page.number").value(2))
            .andExpect(jsonPath("$.page.totalElements").value(5))
            .andExpect(jsonPath("$.page.totalPages").value(3));
    }

    @Test
    void pagesThroughCasesCreatedAtTheSameInstantWithoutOverlap() throws Exception {
        UUID clientId = clientProfileId(email);
        List<String> expected = new ArrayList<>();
        for (int i = 0; i < 7; i++) {
            expected.add(insertCase(clientId, "Case " + i, "DRAFT", "2026-10-15T08:00:00Z", false).toString());
        }
        expected.sort(Comparator.reverseOrder());
        Cookie session = login(email);

        List<String> seen = new ArrayList<>();
        for (int page = 0; page < 3; page++) {
            String body = listCases(session, page, 3).andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
            seen.addAll(JsonPath.read(body, "$.content[*].id"));
        }

        assertThat(seen).isEqualTo(expected);
    }

    @Test
    void returnsAnEmptyPageBeyondTheLastOne() throws Exception {
        insertCase(clientProfileId(email), "Employment dispute", "DRAFT", "2026-10-15T08:00:00Z", false);

        listCases(login(email), 5, 10).andExpect(status().isOk())
            .andExpect(content().json("""
                    {"content":[],"page":{"size":10,"number":5,"totalElements":1,"totalPages":1}}
                    """, true));
    }

    @Test
    void acceptsTheMaximumPageSize() throws Exception {
        insertCase(clientProfileId(email), "Employment dispute", "DRAFT", "2026-10-15T08:00:00Z", false);

        listCases(login(email), 0, 100).andExpect(status().isOk())
            .andExpect(jsonPath("$.content", hasSize(1)))
            .andExpect(jsonPath("$.page.size").value(100));
    }

    @ParameterizedTest
    @CsvSource({ "-1, 10, page, Page must be 0 or greater", "0, 0, size, Size must be between 1 and 100",
            "0, -5, size, Size must be between 1 and 100", "0, 101, size, Size must be between 1 and 100" })
    void rejectsOutOfRangePaginationParameters(int page, int size, String field, String message) throws Exception {
        listCases(login(email), page, size).andExpect(status().isBadRequest())
            .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
            .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"))
            .andExpect(jsonPath("$.fieldErrors", hasSize(1)))
            .andExpect(jsonPath("$.fieldErrors[0].field").value(field))
            .andExpect(jsonPath("$.fieldErrors[0].message").value(message))
            .andExpect(jsonPath("$.timestamp").isNotEmpty())
            .andExpect(jsonPath("$.traceId").isNotEmpty());
    }

    @Test
    void reportsEveryInvalidPaginationParameter() throws Exception {
        listCases(login(email), -1, 101).andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.fieldErrors", hasSize(2)))
            .andExpect(jsonPath("$.fieldErrors[0].field").value("page"))
            .andExpect(jsonPath("$.fieldErrors[1].field").value("size"));
    }

    @ParameterizedTest
    @CsvSource({ "page, abc", "size, 1.5" })
    void rejectsNonIntegerPaginationParameters(String parameter, String value) throws Exception {
        mockMvc.perform(get(CASES_PATH).param(parameter, value).cookie(login(email)))
            .andExpect(status().isBadRequest())
            .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
            .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"))
            .andExpect(jsonPath("$.detail").value("Parameter '%s' has an invalid value.".formatted(parameter)));
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

    private ResultActions listCases(Cookie session, int page, int size) throws Exception {
        return mockMvc.perform(get(CASES_PATH).param("page", String.valueOf(page))
            .param("size", String.valueOf(size))
            .cookie(session));
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
