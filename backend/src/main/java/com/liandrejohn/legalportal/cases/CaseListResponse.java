package com.liandrejohn.legalportal.cases;

import java.util.List;

import org.springframework.data.domain.Page;

/** One page of the client's cases, with the paging metadata alongside. */
public record CaseListResponse(List<CaseSummaryResponse> content, PageMetadata page) {

    static CaseListResponse from(Page<CaseSummaryResponse> page) {
        return new CaseListResponse(page.getContent(), new PageMetadata(page.getSize(), page.getNumber(),
                page.getTotalElements(), page.getTotalPages()));
    }

    /** {@code number} is zero-based; {@code size} is the requested page size, not the number of cases returned. */
    public record PageMetadata(int size, int number, long totalElements, int totalPages) {
    }

}
