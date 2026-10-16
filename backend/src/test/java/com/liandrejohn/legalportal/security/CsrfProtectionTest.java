package com.liandrejohn.legalportal.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import com.liandrejohn.legalportal.TestcontainersConfiguration;

@SpringBootTest
@AutoConfigureMockMvc
@Import({ TestcontainersConfiguration.class, CsrfProtectionTest.ProbeConfiguration.class })
class CsrfProtectionTest {

    private static final String PROBE_PATH = "/api/v1/test/csrf-probe";

    @Autowired
    private MockMvc mockMvc;

    @Test
    void unauthenticatedClientCanRetrieveToken() throws Exception {
        mockMvc.perform(get("/api/v1/auth/csrf"))
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.token").isNotEmpty())
            .andExpect(jsonPath("$.headerName").value("X-XSRF-TOKEN"));
    }

    @Test
    void validTokenLetsAuthenticatedRequestReachTheController() throws Exception {
        CsrfSession session = fetchCsrfSession();

        mockMvc.perform(post(PROBE_PATH).cookie(session.cookie())
            .header("X-XSRF-TOKEN", session.token())
            .with(user("client")))
            .andExpect(status().isNoContent());
    }

    @Test
    void validTokenLetsUnauthenticatedRequestProceedToAuthentication() throws Exception {
        CsrfSession session = fetchCsrfSession();

        mockMvc.perform(post(PROBE_PATH).cookie(session.cookie()).header("X-XSRF-TOKEN", session.token()))
            .andExpect(status().isUnauthorized())
            .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
            .andExpect(jsonPath("$.errorCode").value("UNAUTHORIZED"));
    }

    @Test
    void missingTokenIsRejected() throws Exception {
        CsrfSession session = fetchCsrfSession();

        expectCsrfRejection(mockMvc.perform(post(PROBE_PATH).cookie(session.cookie()).with(user("client"))));
    }

    @Test
    void invalidTokenIsRejected() throws Exception {
        CsrfSession session = fetchCsrfSession();

        expectCsrfRejection(mockMvc.perform(post(PROBE_PATH).cookie(session.cookie())
            .header("X-XSRF-TOKEN", "not-a-valid-token")
            .with(user("client"))));
    }

    @Test
    void tokenFromAnotherSessionIsRejected() throws Exception {
        CsrfSession first = fetchCsrfSession();
        CsrfSession second = fetchCsrfSession();

        expectCsrfRejection(mockMvc.perform(post(PROBE_PATH).cookie(second.cookie())
            .header("X-XSRF-TOKEN", first.token())
            .with(user("client"))));
    }

    @Test
    void tokenWithoutSessionIsRejected() throws Exception {
        CsrfSession session = fetchCsrfSession();

        expectCsrfRejection(mockMvc
            .perform(post(PROBE_PATH).header("X-XSRF-TOKEN", session.token()).with(user("client"))));
    }

    private CsrfSession fetchCsrfSession() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/auth/csrf")).andExpect(status().isOk()).andReturn();
        Cookie cookie = result.getResponse().getCookie("SESSION");
        assertThat(cookie).isNotNull();
        String token = JsonPath.read(result.getResponse().getContentAsString(), "$.token");
        return new CsrfSession(cookie, token);
    }

    private void expectCsrfRejection(ResultActions result) throws Exception {
        result.andExpect(status().isForbidden())
            .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
            .andExpect(jsonPath("$.title").value("Forbidden"))
            .andExpect(jsonPath("$.status").value(403))
            .andExpect(jsonPath("$.detail").value("Invalid or missing CSRF token."))
            .andExpect(jsonPath("$.instance").value(PROBE_PATH))
            .andExpect(jsonPath("$.errorCode").value("FORBIDDEN"))
            .andExpect(jsonPath("$.timestamp").value(matchesPattern("\\d{4}-\\d{2}-\\d{2}T.*Z")))
            .andExpect(jsonPath("$.traceId").isNotEmpty());
    }

    private record CsrfSession(Cookie cookie, String token) {
    }

    @TestConfiguration(proxyBeanMethods = false)
    @Import(ProbeController.class)
    static class ProbeConfiguration {

    }

    /** A state-changing endpoint that only exists in tests, standing in for real APIs. */
    @RestController
    static class ProbeController {

        @PostMapping(PROBE_PATH)
        ResponseEntity<Void> probe() {
            return ResponseEntity.noContent().build();
        }

    }

}
