package com.liandrejohn.legalportal.category;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface LegalCategoryRepository extends JpaRepository<LegalCategory, UUID> {

    List<LegalCategory> findByActiveTrueOrderByNameAsc();

}
