package com.liandrejohn.legalportal.cases;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LegalCaseRepository extends JpaRepository<LegalCase, UUID> {

    /** A case visible to its owner: owned by {@code clientId} and not soft-deleted. */
    Optional<LegalCase> findByIdAndClientIdAndDeletedAtIsNull(UUID id, UUID clientId);

    /** The owner's non-deleted cases, newest first, with their categories fetched in the same query. */
    @EntityGraph(attributePaths = "category")
    List<LegalCase> findByClientIdAndDeletedAtIsNullOrderByCreatedAtDescIdDesc(UUID clientId);

}
