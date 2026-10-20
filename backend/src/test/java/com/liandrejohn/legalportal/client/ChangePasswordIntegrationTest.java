package com.liandrejohn.legalportal.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import com.jayway.jsonpath.JsonPath;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.ResultMatcher;

import com.liandrejohn.legalportal.TestcontainersConfiguration;
import com.liandrejohn.legalportal.auth.RegisterRequest;
import com.liandrejohn.legalportal.auth.RegistrationService;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@ExtendWith(OutputCaptureExtension.class)
class ChangePasswordIntegrationTest {

    private static final String PASSWORD_PATH = "/api/v1/clients/me/password";

    private static final String PASSWORD = "password123456";

    private static final String NEW_PASSWORD = "NewPassword456!";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private RegistrationService registrationService;

    private String email;

    @BeforeEach
    void setUp() {
        email = "client-" + UUID.randomUUID() + "@example.com";
        registrationService.register(new RegisterRequest("John", "Doe", email, PASSWORD));
    }

    @Test
    void changesPassword(CapturedOutput output) throws Exception {
        String before = passwordHash();

        changePassword(login(PASSWORD), PASSWORD, NEW_PASSWORD, NEW_PASSWORD)
            .andExpect(status().isNoContent())
            .andExpect(content().string(""));

        String after = passwordHash();
        assertThat(after).isNotEqualTo(before).startsWith("$2").doesNotContain(NEW_PASSWORD);
        assertThat(output).doesNotContain(PASSWORD).doesNotContain(NEW_PASSWORD);
    }

    @Test
    void onlyNewPasswordWorksAfterChange() throws Exception {
        changePassword(login(PASSWORD), PASSWORD, NEW_PASSWORD, NEW_PASSWORD).andExpect(status().isNoContent());

        attemptLogin(PASSWORD).andExpect(status().isUnauthorized());
        attemptLogin(NEW_PASSWORD).andExpect(status().isOk());
    }

    @Test
    void sessionStaysSignedIn() throws Exception {
        Cookie session = login(PASSWORD);
        changePassword(session, PASSWORD, NEW_PASSWORD, NEW_PASSWORD).andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/auth/me").cookie(session)).andExpect(status().isOk());
    }

    @Test
    void incorrectCurrentPasswordIsForbidden() throws Exception {
        String before = passwordHash();

        changePassword(login(PASSWORD), "wrongPassword123", NEW_PASSWORD, NEW_PASSWORD)
            .andExpect(status().isForbidden())
            .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
            .andExpect(jsonPath("$.status").value(403))
            .andExpect(jsonPath("$.detail").value("Current password is incorrect."))
            .andExpect(jsonPath("$.errorCode").value("INCORRECT_PASSWORD"))
            .andExpect(jsonPath("$.instance").value(PASSWORD_PATH))
            .andExpect(jsonPath("$.fieldErrors").doesNotExist());
        assertThat(passwordHash()).isEqualTo(before);
    }

    @Test
    void missingFieldsAreRejected() throws Exception {
        String before = passwordHash();

        expectValidationError(changePassword(login(PASSWORD), "{}"))
            .andExpect(jsonPath("$.fieldErrors", hasSize(3)))
            .andExpect(fieldError("currentPassword", "Current password is required"))
            .andExpect(fieldError("newPassword", "New password is required"))
            .andExpect(fieldError("confirmPassword", "Password confirmation is required"));
        assertThat(passwordHash()).isEqualTo(before);
    }

    @Test
    void shortNewPasswordIsRejected() throws Exception {
        expectValidationError(changePassword(login(PASSWORD), PASSWORD, "short", "short"))
            .andExpect(jsonPath("$.fieldErrors", hasSize(1)))
            .andExpect(fieldError("newPassword", "Password must contain at least 12 characters"));
    }

    @Test
    void newPasswordOver72BytesIsRejected() throws Exception {
        String tooLong = "é".repeat(37);

        expectValidationError(changePassword(login(PASSWORD), PASSWORD, tooLong, tooLong))
            .andExpect(jsonPath("$.fieldErrors", hasSize(1)))
            .andExpect(fieldError("newPassword", "Password must not exceed 72 bytes"));
    }

    @Test
    void mismatchedConfirmationIsRejected() throws Exception {
        String before = passwordHash();

        expectValidationError(changePassword(login(PASSWORD), PASSWORD, NEW_PASSWORD, "OtherPassword789!"))
            .andExpect(jsonPath("$.fieldErrors", hasSize(1)))
            .andExpect(fieldError("confirmPassword", "Passwords do not match"));
        assertThat(passwordHash()).isEqualTo(before);
    }

    @Test
    void newPasswordEqualToCurrentIsRejected() throws Exception {
        String before = passwordHash();

        expectValidationError(changePassword(login(PASSWORD), PASSWORD, PASSWORD, PASSWORD))
            .andExpect(jsonPath("$.fieldErrors", hasSize(1)))
            .andExpect(fieldError("newPassword", "New password must differ from the current password"));
        assertThat(passwordHash()).isEqualTo(before);
    }

    @Test
    void wrongCurrentPasswordIsReportedBeforeReuse() throws Exception {
        // Reusing the real password as the new one mustn't reveal it when the current password is wrong
        changePassword(login(PASSWORD), "wrongPassword123", PASSWORD, PASSWORD)
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.errorCode").value("INCORRECT_PASSWORD"));
    }

    @Test
    void unauthenticatedRequestIsRejected() throws Exception {
        String before = passwordHash();
        MvcResult csrf = mockMvc.perform(get("/api/v1/auth/csrf")).andExpect(status().isOk()).andReturn();

        mockMvc
            .perform(patch(PASSWORD_PATH).cookie(csrf.getResponse().getCookie("SESSION"))
                .header("X-XSRF-TOKEN", (String) JsonPath.read(csrf.getResponse().getContentAsString(), "$.token"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(body(PASSWORD, NEW_PASSWORD, NEW_PASSWORD)))
            .andExpect(status().isUnauthorized())
            .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
            .andExpect(jsonPath("$.errorCode").value("UNAUTHORIZED"));
        assertThat(passwordHash()).isEqualTo(before);
    }

    @Test
    void suspendedAccountIsRejected() throws Exception {
        Cookie session = login(PASSWORD);
        jdbcTemplate.update("UPDATE users SET status = 'SUSPENDED' WHERE email = ?", email);
        String before = passwordHash();

        changePassword(session, PASSWORD, NEW_PASSWORD, NEW_PASSWORD)
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.errorCode").value("UNAUTHORIZED"));
        assertThat(passwordHash()).isEqualTo(before);
    }

    @Test
    void missingCsrfTokenIsRejected() throws Exception {
        String before = passwordHash();

        mockMvc
            .perform(patch(PASSWORD_PATH).cookie(login(PASSWORD))
                .contentType(MediaType.APPLICATION_JSON)
                .content(body(PASSWORD, NEW_PASSWORD, NEW_PASSWORD)))
            .andExpect(status().isForbidden())
            .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
            .andExpect(jsonPath("$.errorCode").value("FORBIDDEN"));
        assertThat(passwordHash()).isEqualTo(before);
    }

    private ResultActions expectValidationError(ResultActions result) throws Exception {
        return result.andExpect(status().isBadRequest())
            .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.instance").value(PASSWORD_PATH))
            .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));
    }

    private static ResultMatcher fieldError(String field, String message) {
        return jsonPath("$.fieldErrors[?(@.field == '%s' && @.message == '%s')]".formatted(field, message),
                hasSize(1));
    }

    private ResultActions changePassword(Cookie session, String current, String newPassword, String confirm)
            throws Exception {
        return changePassword(session, body(current, newPassword, confirm));
    }

    /** Patches with the session's current CSRF token, which login replaced. */
    private ResultActions changePassword(Cookie session, String body) throws Exception {
        MvcResult csrf = mockMvc.perform(get("/api/v1/auth/csrf").cookie(session))
            .andExpect(status().isOk())
            .andReturn();
        return mockMvc.perform(patch(PASSWORD_PATH).cookie(session)
            .header("X-XSRF-TOKEN", (String) JsonPath.read(csrf.getResponse().getContentAsString(), "$.token"))
            .contentType(MediaType.APPLICATION_JSON)
            .content(body));
    }

    private static String body(String current, String newPassword, String confirm) {
        return """
                {"currentPassword":"%s","newPassword":"%s","confirmPassword":"%s"}
                """.formatted(current, newPassword, confirm);
    }

    private String passwordHash() {
        return jdbcTemplate.queryForObject("SELECT password_hash FROM users WHERE email = ?", String.class, email);
    }

    /** Logs in through the API and returns the authenticated session cookie. */
    private Cookie login(String password) throws Exception {
        return attemptLogin(password).andExpect(status().isOk()).andReturn().getResponse().getCookie("SESSION");
    }

    private ResultActions attemptLogin(String password) throws Exception {
        MvcResult csrf = mockMvc.perform(get("/api/v1/auth/csrf")).andExpect(status().isOk()).andReturn();
        return mockMvc.perform(post("/api/v1/auth/login").cookie(csrf.getResponse().getCookie("SESSION"))
            .header("X-XSRF-TOKEN", (String) JsonPath.read(csrf.getResponse().getContentAsString(), "$.token"))
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                    {"email":"%s","password":"%s"}
                    """.formatted(email, password)));
    }

}
