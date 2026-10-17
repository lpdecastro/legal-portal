package com.liandrejohn.legalportal.category;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface LegalCategoryRepository extends JpaRepository<LegalCategory, UUID> {

    List<LegalCategory> findByActiveTrueOrderByNameAsc();

    Optional<LegalCategory> findByIdAndActiveTrue(UUID id);

}
