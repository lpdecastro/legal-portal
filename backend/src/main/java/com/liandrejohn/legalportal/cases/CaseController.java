package com.liandrejohn.legalportal.cases;

import java.net.URI;
import java.util.UUID;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.tags.Tag;

import com.liandrejohn.legalportal.openapi.ApiTags;
import com.liandrejohn.legalportal.security.AuthenticatedUser;

@RestController
@Tag(name = ApiTags.CASES)
@RequestMapping(CaseController.CASES_PATH)
@RequiredArgsConstructor
class CaseController {

    static final String CASES_PATH = "/api/v1/cases";

    static final String DEFAULT_PAGE_SIZE = "10";

    static final int MAX_PAGE_SIZE = 100;

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

    /**
     * Lists one zero-based page of the session's client's cases, newest first, optionally only those in
     * {@code status} and with titles containing {@code search} (ignoring case). Unknown statuses and out-of-range
     * {@code page} or {@code size} values are rejected with 400 rather than clamped.
     */
    @GetMapping
    CaseListResponse list(@AuthenticationPrincipal AuthenticatedUser user,
            @RequestParam(required = false) CaseStatus status, @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") @Min(value = 0, message = "Page must be 0 or greater") int page,
            @RequestParam(defaultValue = DEFAULT_PAGE_SIZE)
            @Min(value = 1, message = "Size must be between 1 and " + MAX_PAGE_SIZE)
            @Max(value = MAX_PAGE_SIZE, message = "Size must be between 1 and " + MAX_PAGE_SIZE) int size) {
        return caseService.listCases(user.getId(), status, search, page, size);
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

    /** Submits one of the session's client's drafts. */
    @PostMapping("/{id}/submit")
    CaseResponse submit(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable UUID id) {
        return caseService.submitDraft(user.getId(), id);
    }

    /** Closes one of the session's client's submitted cases. */
    @PostMapping("/{id}/close")
    CaseResponse close(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable UUID id) {
        return caseService.closeCase(user.getId(), id);
    }

    /** Soft-deletes one of the session's client's drafts. */
    @DeleteMapping("/{id}")
    ResponseEntity<Void> delete(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable UUID id) {
        caseService.deleteDraft(user.getId(), id);
        return ResponseEntity.noContent().build();
    }

}
