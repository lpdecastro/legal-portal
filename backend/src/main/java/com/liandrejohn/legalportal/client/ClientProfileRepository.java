package com.liandrejohn.legalportal.client;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.liandrejohn.legalportal.user.User;
import com.liandrejohn.legalportal.user.UserStatus;

public interface ClientProfileRepository extends JpaRepository<ClientProfile, UUID> {

    Optional<ClientProfile> findByUserId(UUID userId);

    /** The user's profile, unless the account was deleted, suspended or disabled. */
    default Optional<ClientProfile> findActiveByUserId(UUID userId) {
        return findByUserId(userId).filter((profile) -> {
            User user = profile.getUser();
            return user.getDeletedAt() == null && user.getStatus() == UserStatus.ACTIVE;
        });
    }

}
