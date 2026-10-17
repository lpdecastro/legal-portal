package com.liandrejohn.legalportal.error;

import java.util.Comparator;
import java.util.List;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Turns controller exceptions into Problem Details responses. */
@Slf4j
@RestControllerAdvice
class ApiExceptionHandler {

    static final String VALIDATION_ERROR = "VALIDATION_ERROR";

    static final String VALIDATION_DETAIL = "One or more fields contain invalid values.";

    private static final String VALIDATION_TITLE = "Validation Failed";

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ProblemDetail> handleInvalidArgument(MethodArgumentNotValidException ex,
            HttpServletRequest request) {
        List<FieldErrorDetail> fieldErrors = ex.getBindingResult()
            .getFieldErrors()
            .stream()
            .map((error) -> new FieldErrorDetail(error.getField(), error.getDefaultMessage()))
            .sorted(Comparator.comparing(FieldErrorDetail::field).thenComparing(FieldErrorDetail::message))
            .toList();

        return respond(request, validationProblem(fieldErrors, request));
    }

    @ExceptionHandler(FieldValidationException.class)
    ResponseEntity<ProblemDetail> handleFieldValidation(FieldValidationException ex, HttpServletRequest request) {
        return respond(request, validationProblem(ex.getFieldErrors(), request));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<ProblemDetail> handleUnreadableBody(HttpServletRequest request) {
        ProblemDetail problem = ProblemDetails.of(HttpStatus.BAD_REQUEST, VALIDATION_ERROR,
                "Request body is missing or malformed.", request.getRequestURI());
        problem.setTitle(VALIDATION_TITLE);
        return respond(request, problem);
    }

    @ExceptionHandler(ApiException.class)
    ResponseEntity<ProblemDetail> handleApiException(ApiException ex, HttpServletRequest request) {
        return respond(request,
                ProblemDetails.of(ex.getStatus(), ex.getErrorCode(), ex.getMessage(), request.getRequestURI()));
    }

    /**
     * Unexpected failures become a generic 500 that reveals nothing about the cause; the details go to the log under
     * the response's trace ID. Spring MVC exceptions (404, 405, ...) and Spring Security exceptions are rethrown so
     * their own resolvers and filters still answer them.
     */
    @ExceptionHandler(Exception.class)
    ResponseEntity<ProblemDetail> handleUnexpected(Exception ex, HttpServletRequest request) throws Exception {
        if (ex instanceof ErrorResponse || ex instanceof AccessDeniedException
                || ex instanceof AuthenticationException) {
            throw ex;
        }
        ProblemDetail problem = ProblemDetails.of(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR",
                "An unexpected error occurred. Please try again later.", request.getRequestURI());
        log.error("{} {} failed [traceId={}]", request.getMethod(), request.getRequestURI(),
                problem.getProperties().get("traceId"), ex);
        return ResponseEntity.status(problem.getStatus())
            .contentType(MediaType.APPLICATION_PROBLEM_JSON)
            .body(problem);
    }

    private static ProblemDetail validationProblem(List<FieldErrorDetail> fieldErrors, HttpServletRequest request) {
        ProblemDetail problem = ProblemDetails.of(HttpStatus.BAD_REQUEST, VALIDATION_ERROR, VALIDATION_DETAIL,
                request.getRequestURI());
        problem.setTitle(VALIDATION_TITLE);
        problem.setProperty("fieldErrors", fieldErrors);
        return problem;
    }

    private static ResponseEntity<ProblemDetail> respond(HttpServletRequest request, ProblemDetail problem) {
        log.warn("{} {} rejected with {} [traceId={}]", request.getMethod(), request.getRequestURI(),
                problem.getProperties().get("errorCode"), problem.getProperties().get("traceId"));
        return ResponseEntity.status(problem.getStatus())
            .contentType(MediaType.APPLICATION_PROBLEM_JSON)
            .body(problem);
    }

}
