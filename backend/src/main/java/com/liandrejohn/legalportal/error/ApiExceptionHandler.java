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
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Turns controller exceptions into Problem Details responses. */
@Slf4j
@RestControllerAdvice
class ApiExceptionHandler {

    private static final String VALIDATION_ERROR = "VALIDATION_ERROR";

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

        ProblemDetail problem = ProblemDetails.of(HttpStatus.BAD_REQUEST, VALIDATION_ERROR,
                "One or more fields contain invalid values.", request.getRequestURI());
        problem.setTitle(VALIDATION_TITLE);
        problem.setProperty("fieldErrors", fieldErrors);
        return respond(request, problem);
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

    private static ResponseEntity<ProblemDetail> respond(HttpServletRequest request, ProblemDetail problem) {
        log.warn("{} {} rejected with {} [traceId={}]", request.getMethod(), request.getRequestURI(),
                problem.getProperties().get("errorCode"), problem.getProperties().get("traceId"));
        return ResponseEntity.status(problem.getStatus())
            .contentType(MediaType.APPLICATION_PROBLEM_JSON)
            .body(problem);
    }

}
