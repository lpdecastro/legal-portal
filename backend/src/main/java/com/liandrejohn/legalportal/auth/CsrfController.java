package com.liandrejohn.legalportal.auth;

import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.tags.Tag;

import com.liandrejohn.legalportal.openapi.ApiTags;

@RestController
@Tag(name = ApiTags.AUTH)
@RequestMapping("/api/v1/auth")
class CsrfController {

    /** Returns the session's CSRF token, creating the session and token if they don't exist yet. */
    @GetMapping("/csrf")
    CsrfTokenResponse csrf(CsrfToken csrfToken) {
        return new CsrfTokenResponse(csrfToken.getToken(), csrfToken.getHeaderName());
    }

}
