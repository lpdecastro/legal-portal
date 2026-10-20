package com.liandrejohn.legalportal.cases;

import java.time.Instant;
import java.util.UUID;

/** A case as returned to its owner. */
public record CaseResponse(UUID id, String title, UUID categoryId, String description, String location,
        CaseStatus status, Instant createdAt, Instant updatedAt, Instant submittedAt) {

    static CaseResponse from(LegalCase legalCase) {
        return new CaseResponse(legalCase.getId(), legalCase.getTitle(), legalCase.getCategory().getId(),
                legalCase.getDescription(), legalCase.getLocation(), legalCase.getStatus(), legalCase.getCreatedAt(),
                legalCase.getUpdatedAt(), legalCase.getSubmittedAt());
    }

}
