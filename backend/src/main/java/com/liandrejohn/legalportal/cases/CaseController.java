package com.liandrejohn.legalportal.cases;

import java.net.URI;
import java.util.UUID;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
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

    /** Returns one of the session's client's cases; any other case is answered with 404. */
    @GetMapping("/{id}")
    CaseResponse get(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable UUID id) {
        return caseService.getCase(user.getId(), id);
    }

    /** Updates the fields present in the body of one of the session's client's drafts. */
    @PatchMapping("/{id}")
    CaseResponse update(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable UUID id,
            @Valid @RequestBody UpdateCaseRequest request) {
        return caseService.updateDraft(user.getId(), id, request);
    }

}
