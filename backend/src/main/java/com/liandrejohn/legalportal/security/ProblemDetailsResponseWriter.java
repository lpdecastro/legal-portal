package com.liandrejohn.legalportal.security;

import java.io.IOException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

import com.liandrejohn.legalportal.error.ProblemDetails;

/**
 * Writes Problem Details responses from security filters, which run before Spring MVC's exception handling.
 */
@Component
class ProblemDetailsResponseWriter {

	private static final Logger log = LoggerFactory.getLogger(ProblemDetailsResponseWriter.class);

	private final JsonMapper jsonMapper;

	ProblemDetailsResponseWriter(JsonMapper jsonMapper) {
		this.jsonMapper = jsonMapper;
	}

	void write(HttpServletRequest request, HttpServletResponse response, HttpStatus status, String errorCode,
			String detail) throws IOException {
		ProblemDetail problem = ProblemDetails.of(status, errorCode, detail, request.getRequestURI());
		log.warn("{} {} rejected with {} [traceId={}]", request.getMethod(), request.getRequestURI(), errorCode,
				problem.getProperties().get("traceId"));

		response.setStatus(status.value());
		response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
		jsonMapper.writeValue(response.getOutputStream(), problem);
	}

}
