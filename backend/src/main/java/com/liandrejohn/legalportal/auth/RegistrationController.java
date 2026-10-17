package com.liandrejohn.legalportal.auth;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.tags.Tag;

import com.liandrejohn.legalportal.openapi.ApiTags;

@RestController
@Tag(name = ApiTags.AUTH)
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
class RegistrationController {

    private final RegistrationService registrationService;

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    RegistrationResponse register(@Valid @RequestBody RegisterRequest request) {
        return registrationService.register(request);
    }

}
