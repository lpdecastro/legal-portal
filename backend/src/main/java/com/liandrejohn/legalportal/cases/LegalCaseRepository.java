package com.liandrejohn.legalportal.cases;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface LegalCaseRepository extends JpaRepository<LegalCase, UUID> {
}
