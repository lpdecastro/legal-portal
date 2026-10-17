package com.liandrejohn.legalportal.auth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.tags.Tag;

import com.liandrejohn.legalportal.openapi.ApiTags;
import com.liandrejohn.legalportal.security.AuthenticatedUser;
import com.liandrejohn.legalportal.security.AuthenticationRequiredException;
import com.liandrejohn.legalportal.security.SessionLogin;

@Slf4j
@RestController
@Tag(name = ApiTags.AUTH)
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
class CurrentClientController {

    private final CurrentClientService currentClientService;

    private final SessionLogin sessionLogin;

    /**
     * Returns the client behind the session cookie so the SPA can restore its state after a refresh. Requests without
     * an authenticated session are rejected with 401 by the security filter chain before reaching this method.
     */
    @GetMapping("/me")
    AuthenticatedClientResponse me(@AuthenticationPrincipal AuthenticatedUser user, HttpServletRequest request,
            HttpServletResponse response) {
        return currentClientService.findActiveClient(user.getId()).orElseThrow(() -> {
            log.info("Ending session of inactive user {}", user.getId());
            sessionLogin.end(request, response);
            return new AuthenticationRequiredException();
        });
    }

}
