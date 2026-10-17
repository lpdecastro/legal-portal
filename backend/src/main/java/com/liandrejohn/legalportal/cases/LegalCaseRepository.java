package com.liandrejohn.legalportal.cases;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface LegalCaseRepository extends JpaRepository<LegalCase, UUID> {

    /** A case visible to its owner: owned by {@code clientId} and not soft-deleted. */
    Optional<LegalCase> findByIdAndClientIdAndDeletedAtIsNull(UUID id, UUID clientId);

}
