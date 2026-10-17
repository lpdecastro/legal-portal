package com.liandrejohn.legalportal.error;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.net.URI;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;

class ApiExceptionHandlerTest {

    private final ApiExceptionHandler handler = new ApiExceptionHandler();

    private final MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/legal-categories");

    @Test
    void unexpectedExceptionBecomesAGenericInternalError() throws Exception {
        ResponseEntity<ProblemDetail> response = handler
            .handleUnexpected(new IllegalStateException("connection refused to db-host:5432"), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_PROBLEM_JSON);
        ProblemDetail problem = response.getBody();
        assertThat(problem).isNotNull();
        assertThat(problem.getType()).isEqualTo(URI.create("about:blank"));
        assertThat(problem.getStatus()).isEqualTo(500);
        assertThat(problem.getDetail()).isEqualTo("An unexpected error occurred. Please try again later.");
        assertThat(problem.getInstance()).isEqualTo(URI.create("/api/v1/legal-categories"));
        assertThat(problem.getProperties()).containsEntry("errorCode", "INTERNAL_ERROR")
            .containsKeys("timestamp", "traceId");
    }

    @Test
    void springMvcExceptionsAreLeftToTheirOwnResolvers() {
        HttpRequestMethodNotSupportedException ex = new HttpRequestMethodNotSupportedException(
                HttpMethod.DELETE.name());

        assertThatThrownBy(() -> handler.handleUnexpected(ex, request)).isSameAs(ex);
    }

    @Test
    void springSecurityExceptionsAreLeftToTheFilterChain() {
        AccessDeniedException ex = new AccessDeniedException("denied");

        assertThatThrownBy(() -> handler.handleUnexpected(ex, request)).isSameAs(ex);
    }

}
