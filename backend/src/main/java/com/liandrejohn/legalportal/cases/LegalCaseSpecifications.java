package com.liandrejohn.legalportal.cases;

import java.util.Locale;
import java.util.UUID;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.query.EscapeCharacter;

/** Query building blocks for listing cases; combine them with {@link Specification#and}. */
final class LegalCaseSpecifications {

    private static final EscapeCharacter ESCAPE = EscapeCharacter.DEFAULT;

    private LegalCaseSpecifications() {
    }

    /** Cases owned by {@code clientId} that are not soft-deleted. */
    static Specification<LegalCase> visibleTo(UUID clientId) {
        return (root, query, cb) -> cb.and(cb.equal(root.get("client").get("id"), clientId),
                cb.isNull(root.get("deletedAt")));
    }

    /** Cases in {@code status}. */
    static Specification<LegalCase> hasStatus(CaseStatus status) {
        return (root, query, cb) -> cb.equal(root.get("status"), status);
    }

    /**
     * Cases whose title contains {@code term}, ignoring case. {@code %} and {@code _} in the term match literally
     * rather than as wildcards.
     */
    static Specification<LegalCase> titleContains(String term) {
        String pattern = "%" + ESCAPE.escape(term.toLowerCase(Locale.ROOT)) + "%";
        return (root, query, cb) -> cb.like(cb.lower(root.get("title")), pattern, ESCAPE.getEscapeCharacter());
    }

}
