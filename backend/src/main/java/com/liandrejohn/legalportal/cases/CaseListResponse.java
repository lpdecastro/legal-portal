package com.liandrejohn.legalportal.cases;

import java.util.List;

/** The client's cases, wrapped in {@code content} so paging metadata can be added alongside later. */
public record CaseListResponse(List<CaseSummaryResponse> content) {
}
