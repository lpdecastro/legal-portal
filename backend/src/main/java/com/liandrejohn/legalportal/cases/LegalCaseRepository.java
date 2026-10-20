package com.liandrejohn.legalportal.cases;

import java.util.Optional;
import java.util.UUID;

import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

public interface LegalCaseRepository extends JpaRepository<LegalCase, UUID> {

    /** A case visible to its owner: owned by {@code clientId} and not soft-deleted. */
    Optional<LegalCase> findByIdAndClientIdAndDeletedAtIsNull(UUID id, UUID clientId);

    /**
     * Like {@link #findByIdAndClientIdAndDeletedAtIsNull}, but locks the row ({@code SELECT ... FOR UPDATE}) until the
     * transaction ends, so concurrent state changes to the same case run one after another.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<LegalCase> findLockedByIdAndClientIdAndDeletedAtIsNull(UUID id, UUID clientId);

    /** One page of the owner's non-deleted cases, with their categories fetched in the same query. */
    @EntityGraph(attributePaths = "category")
    Page<LegalCase> findByClientIdAndDeletedAtIsNull(UUID clientId, Pageable pageable);

}
