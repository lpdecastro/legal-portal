package com.liandrejohn.legalportal.error;

import java.net.URI;
import java.time.Instant;
import java.util.UUID;

import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;

/**
 * Builds RFC 9457 Problem Details with the API's custom extensions: {@code errorCode}, {@code timestamp}
 * and {@code traceId}.
 */
public final class ProblemDetails {

    private static final String TRACE_ID_MDC_KEY = "traceId";

    private static final URI ABOUT_BLANK = URI.create("about:blank");

    private ProblemDetails() {
    }

    public static ProblemDetail of(HttpStatus status, String errorCode, String detail, String instance) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        // Spring 7 omits a default type; the API contract always includes it
        problem.setType(ABOUT_BLANK);
        problem.setInstance(URI.create(instance));
        problem.setProperty("errorCode", errorCode);
        problem.setProperty("timestamp", Instant.now().toString());
        problem.setProperty("traceId", currentTraceId());
        return problem;
    }

    /** Uses the trace ID from the logging context when one is present, otherwise generates one. */
    private static String currentTraceId() {
        String traceId = MDC.get(TRACE_ID_MDC_KEY);
        return traceId != null ? traceId : UUID.randomUUID().toString().replace("-", "").substring(0, 16);
    }

}
