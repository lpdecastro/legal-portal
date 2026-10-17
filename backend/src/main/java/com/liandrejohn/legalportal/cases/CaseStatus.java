package com.liandrejohn.legalportal.cases;

/** Cases move DRAFT → SUBMITTED → CLOSED; only drafts are editable. */
public enum CaseStatus {
    DRAFT,
    SUBMITTED,
    CLOSED
}
