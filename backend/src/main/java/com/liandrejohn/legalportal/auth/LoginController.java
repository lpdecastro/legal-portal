package com.liandrejohn.legalportal.auth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.tags.Tag;

import com.liandrejohn.legalportal.openapi.ApiTags;
import com.liandrejohn.legalportal.security.SessionLogin;

@RestController
@Tag(name = ApiTags.AUTH)
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
class LoginController {

    private final LoginService loginService;

    private final SessionLogin sessionLogin;

    @PostMapping("/login")
    AuthenticatedClientResponse login(@Valid @RequestBody LoginRequest body, HttpServletRequest request,
            HttpServletResponse response) {
        LoginResult result = loginService.login(body, request.getRemoteAddr());
        sessionLogin.establish(result.authentication(), request, response);
        return result.response();
    }

}
