package com.liandrejohn.legalportal.client;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.tags.Tag;

import com.liandrejohn.legalportal.openapi.ApiTags;
import com.liandrejohn.legalportal.security.AuthenticatedUser;

@RestController
@Tag(name = ApiTags.PROFILE)
@RequestMapping("/api/v1/clients")
@RequiredArgsConstructor
class ClientProfileController {

    private final ClientProfileService clientProfileService;

    /** The session's client's profile. */
    @GetMapping("/me")
    ClientProfileResponse me(@AuthenticationPrincipal AuthenticatedUser user) {
        return clientProfileService.getProfile(user.getId());
    }

    /** Partially updates the session's client's first name, last name and phone. Email can't be changed. */
    @PatchMapping("/me")
    ClientProfileResponse update(@AuthenticationPrincipal AuthenticatedUser user,
            @Valid @RequestBody UpdateProfileRequest request) {
        return clientProfileService.updateProfile(user.getId(), request);
    }

}
