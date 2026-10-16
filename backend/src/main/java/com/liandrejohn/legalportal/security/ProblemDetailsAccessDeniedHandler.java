package com.liandrejohn.legalportal.security;

import java.io.IOException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.csrf.CsrfException;
import org.springframework.stereotype.Component;

/** Responds to access denials, including missing or invalid CSRF tokens, with a 403 Problem Details body. */
@Component
@RequiredArgsConstructor
class ProblemDetailsAccessDeniedHandler implements AccessDeniedHandler {

    private final ProblemDetailsResponseWriter writer;

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
            AccessDeniedException accessDeniedException) throws IOException {
        String detail = accessDeniedException instanceof CsrfException ? "Invalid or missing CSRF token."
                : "You do not have permission to perform this action.";
        writer.write(request, response, HttpStatus.FORBIDDEN, "FORBIDDEN", detail);
    }

}
