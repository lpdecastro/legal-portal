package com.liandrejohn.legalportal.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextHolderStrategy;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Component;

/** Stores a successful authentication in the server-side session so later requests are recognized, or ends it. */
@Component
@RequiredArgsConstructor
public class SessionLogin {

    private final SecurityContextHolderStrategy securityContextHolderStrategy = SecurityContextHolder
        .getContextHolderStrategy();

    private final SessionAuthenticationStrategy sessionAuthenticationStrategy;

    private final SecurityContextRepository securityContextRepository;

    public void establish(Authentication authentication, HttpServletRequest request, HttpServletResponse response) {
        // Changes the session ID (session fixation protection) and replaces the CSRF token
        sessionAuthenticationStrategy.onAuthentication(authentication, request, response);

        SecurityContext context = securityContextHolderStrategy.createEmptyContext();
        context.setAuthentication(authentication);
        securityContextHolderStrategy.setContext(context);
        securityContextRepository.saveContext(context, request, response);
    }

    /** Invalidates the session (Spring Session deletes it from the database) and clears the security context. */
    public void end(HttpServletRequest request, HttpServletResponse response) {
        SecurityContextLogoutHandler logoutHandler = new SecurityContextLogoutHandler();
        logoutHandler.setSecurityContextHolderStrategy(securityContextHolderStrategy);
        logoutHandler.setSecurityContextRepository(securityContextRepository);
        logoutHandler.logout(request, response, null);
    }

}
