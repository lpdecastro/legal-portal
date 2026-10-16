package com.liandrejohn.legalportal.client;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ClientProfileRepository extends JpaRepository<ClientProfile, UUID> {

}
