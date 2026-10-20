package com.liandrejohn.legalportal.client;

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

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class ClientProfileIntegrationTest {

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
    void returnsTheClientsProfile() throws Exception {
        jdbcTemplate.update("""
                UPDATE client_profiles SET phone = '+639123456789'
                WHERE user_id = (SELECT id FROM users WHERE email = ?)
                """, email);

        profile(login(email)).andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(content().json("""
                    {"firstName": "John", "lastName": "Doe", "email": "%s", "phone": "+639123456789"}
                    """.formatted(email), true));
    }

    @Test
    void returnsNullPhoneWhenNotSet() throws Exception {
        profile(login(email)).andExpect(status().isOk())
            .andExpect(jsonPath("$.phone").value((Object) null))
            .andExpect(jsonPath("$.firstName").value("John"));
    }

    @Test
    void returnsOnlyTheSessionsClientsProfile() throws Exception {
        String other = registerClient("Jane", "Smith");

        profile(login(other)).andExpect(status().isOk())
            .andExpect(jsonPath("$.firstName").value("Jane"))
            .andExpect(jsonPath("$.lastName").value("Smith"))
            .andExpect(jsonPath("$.email").value(other));
    }

    @Test
    void doesNotExposeSensitiveFields() throws Exception {
        profile(login(email)).andExpect(status().isOk())
            .andExpect(jsonPath("$.id").doesNotExist())
            .andExpect(jsonPath("$.userId").doesNotExist())
            .andExpect(jsonPath("$.password").doesNotExist())
            .andExpect(jsonPath("$.passwordHash").doesNotExist())
            .andExpect(jsonPath("$.status").doesNotExist())
            .andExpect(jsonPath("$.role").doesNotExist());
    }

    @Test
    void unauthenticatedRequestIsRejected() throws Exception {
        mockMvc.perform(get(PROFILE_PATH))
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
        Cookie session = login(email);
        jdbcTemplate.update("UPDATE users SET status = 'SUSPENDED' WHERE email = ?", email);

        profile(session).andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.errorCode").value("UNAUTHORIZED"));
    }

    @Test
    void missingProfileIsNotFound() throws Exception {
        Cookie session = login(email);
        jdbcTemplate.update("DELETE FROM client_profiles WHERE user_id = (SELECT id FROM users WHERE email = ?)",
                email);

        profile(session).andExpect(status().isNotFound())
            .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
            .andExpect(jsonPath("$.status").value(404))
            .andExpect(jsonPath("$.detail").value("Profile not found."))
            .andExpect(jsonPath("$.errorCode").value("PROFILE_NOT_FOUND"));
    }

    private ResultActions profile(Cookie session) throws Exception {
        return mockMvc.perform(get(PROFILE_PATH).cookie(session));
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
