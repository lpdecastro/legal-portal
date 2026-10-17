package com.liandrejohn.legalportal.cases;

import java.time.Instant;
import java.util.UUID;

/** A case as listed to its owner; {@code category} is the category's name. */
public record CaseSummaryResponse(UUID id, String title, String category, CaseStatus status, Instant createdAt) {

    static CaseSummaryResponse from(LegalCase legalCase) {
        return new CaseSummaryResponse(legalCase.getId(), legalCase.getTitle(), legalCase.getCategory().getName(),
                legalCase.getStatus(), legalCase.getCreatedAt());
    }

}
