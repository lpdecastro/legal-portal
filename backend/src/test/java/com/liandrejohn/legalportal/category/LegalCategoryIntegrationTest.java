package com.liandrejohn.legalportal.category;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import com.liandrejohn.legalportal.TestcontainersConfiguration;

/**
 * Each test replaces the seeded categories with its own inside a transaction that is rolled back afterwards, so the
 * seed data is left intact for other tests. MockMvc runs requests on the test thread, so they see the test's rows.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@Transactional
class LegalCategoryIntegrationTest {

    private static final String CATEGORIES_PATH = "/api/v1/legal-categories";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void removeSeededCategories() {
        // Cases left by other test classes reference categories
        jdbcTemplate.update("DELETE FROM cases");
        jdbcTemplate.update("DELETE FROM legal_categories");
    }

    @Test
    void returnsActiveCategoriesWithTheirDetails() throws Exception {
        UUID id = insertCategory("Family Law", "Divorce, custody, and other family matters", true);

        listCategories().andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$", hasSize(1)))
            .andExpect(jsonPath("$[0].id").value(id.toString()))
            .andExpect(jsonPath("$[0].name").value("Family Law"))
            .andExpect(jsonPath("$[0].description").value("Divorce, custody, and other family matters"))
            .andExpect(jsonPath("$[0].active").doesNotExist())
            .andExpect(jsonPath("$[0].createdAt").doesNotExist());
    }

    @Test
    void returnsCategoriesWithoutADescription() throws Exception {
        insertCategory("Tax Law", null, true);

        listCategories().andExpect(status().isOk())
            .andExpect(jsonPath("$[0].name").value("Tax Law"))
            .andExpect(jsonPath("$[0].description").isEmpty());
    }

    @Test
    void excludesInactiveCategories() throws Exception {
        insertCategory("Family Law", null, true);
        insertCategory("Maritime Law", null, false);

        listCategories().andExpect(status().isOk()).andExpect(jsonPath("$[*].name", contains("Family Law")));
    }

    @Test
    void sortsCategoriesByName() throws Exception {
        insertCategory("Property Law", null, true);
        insertCategory("Employment Law", null, true);
        insertCategory("Family Law", null, true);

        listCategories().andExpect(status().isOk())
            .andExpect(jsonPath("$[*].name", contains("Employment Law", "Family Law", "Property Law")));
    }

    @Test
    void returnsAnEmptyListWithoutActiveCategories() throws Exception {
        insertCategory("Maritime Law", null, false);

        listCategories().andExpect(status().isOk()).andExpect(content().json("[]"));
    }

    @Test
    void unauthenticatedRequestIsRejected() throws Exception {
        mockMvc.perform(get(CATEGORIES_PATH))
            .andExpect(status().isUnauthorized())
            .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
            .andExpect(jsonPath("$.type").value("about:blank"))
            .andExpect(jsonPath("$.status").value(401))
            .andExpect(jsonPath("$.detail").value("Authentication required."))
            .andExpect(jsonPath("$.instance").value(CATEGORIES_PATH))
            .andExpect(jsonPath("$.errorCode").value("UNAUTHORIZED"))
            .andExpect(jsonPath("$.timestamp").value(matchesPattern("\\d{4}-\\d{2}-\\d{2}T.*Z")))
            .andExpect(jsonPath("$.traceId").isNotEmpty());
    }

    private ResultActions listCategories() throws Exception {
        return mockMvc.perform(get(CATEGORIES_PATH).with(user("client")));
    }

    private UUID insertCategory(String name, String description, boolean active) {
        return jdbcTemplate.queryForObject(
                "INSERT INTO legal_categories (name, description, is_active) VALUES (?, ?, ?) RETURNING id",
                UUID.class, name, description, active);
    }

}
