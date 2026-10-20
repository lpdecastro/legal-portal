package com.liandrejohn.legalportal.cases;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import jakarta.persistence.LockModeType;
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

    /** The owner's non-deleted cases, newest first, with their categories fetched in the same query. */
    @EntityGraph(attributePaths = "category")
    List<LegalCase> findByClientIdAndDeletedAtIsNullOrderByCreatedAtDescIdDesc(UUID clientId);

}
