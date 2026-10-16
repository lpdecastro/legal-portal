package com.liandrejohn.legalportal.security;

import java.io.IOException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

/** Responds to unauthenticated requests for protected resources with a 401 Problem Details body. */
@Component
@RequiredArgsConstructor
class ProblemDetailsAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ProblemDetailsResponseWriter writer;

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
            AuthenticationException authException) throws IOException {
        writer.write(request, response, HttpStatus.UNAUTHORIZED, "UNAUTHORIZED",
                AuthenticationRequiredException.DETAIL);
    }

}
