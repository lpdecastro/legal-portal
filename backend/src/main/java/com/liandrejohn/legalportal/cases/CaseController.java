package com.liandrejohn.legalportal.cases;

import java.net.URI;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.liandrejohn.legalportal.security.AuthenticatedUser;

@RestController
@RequestMapping(CaseController.CASES_PATH)
@RequiredArgsConstructor
class CaseController {

    static final String CASES_PATH = "/api/v1/cases";

    private final CaseService caseService;

    /**
     * Creates a draft owned by the session's client. Requests without an authenticated session are rejected with 401
     * by the security filter chain before reaching this method.
     */
    @PostMapping
    ResponseEntity<CaseResponse> create(@AuthenticationPrincipal AuthenticatedUser user,
            @Valid @RequestBody CreateCaseRequest request) {
        CaseResponse created = caseService.createDraft(user.getId(), request);
        return ResponseEntity.created(URI.create(CASES_PATH + "/" + created.id())).body(created);
    }

}
