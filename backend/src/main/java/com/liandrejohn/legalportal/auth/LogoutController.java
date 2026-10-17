package com.liandrejohn.legalportal.auth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.liandrejohn.legalportal.security.SessionLogin;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
class LogoutController {

    private final SessionLogin sessionLogin;

    /**
     * Ends the client's session. Requests without a valid CSRF token (403) or an authenticated session (401) are
     * rejected by the security filter chain before reaching this method. Spring Session deletes the invalidated
     * session from the database and expires the SESSION cookie.
     */
    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void logout(HttpServletRequest request, HttpServletResponse response) {
        sessionLogin.end(request, response);
    }

}
