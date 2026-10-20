package com.liandrejohn.legalportal.cases;

import java.util.Optional;
import java.util.UUID;

import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;

public interface LegalCaseRepository extends JpaRepository<LegalCase, UUID>, JpaSpecificationExecutor<LegalCase> {

    /** A case visible to its owner: owned by {@code clientId} and not soft-deleted. */
    Optional<LegalCase> findByIdAndClientIdAndDeletedAtIsNull(UUID id, UUID clientId);

    /**
     * Like {@link #findByIdAndClientIdAndDeletedAtIsNull}, but locks the row ({@code SELECT ... FOR UPDATE}) until the
     * transaction ends, so concurrent state changes to the same case run one after another.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<LegalCase> findLockedByIdAndClientIdAndDeletedAtIsNull(UUID id, UUID clientId);

    /**
     * One page of the cases matching {@code spec} (see {@link LegalCaseSpecifications}), with their categories fetched
     * in the same query.
     */
    @Override
    @EntityGraph(attributePaths = "category")
    Page<LegalCase> findAll(Specification<LegalCase> spec, Pageable pageable);

}
