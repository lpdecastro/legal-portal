package com.liandrejohn.legalportal.auth;

import java.util.Optional;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.liandrejohn.legalportal.client.ClientProfile;
import com.liandrejohn.legalportal.client.ClientProfileRepository;
import com.liandrejohn.legalportal.user.UserRepository;
import com.liandrejohn.legalportal.user.UserStatus;

@Service
@RequiredArgsConstructor
class CurrentClientService {

    private final UserRepository userRepository;

    private final ClientProfileRepository clientProfileRepository;

    /**
     * Loads the session's client from the database. Empty if the account was deleted, suspended or disabled after
     * the session started.
     */
    @Transactional(readOnly = true)
    Optional<AuthenticatedClientResponse> findActiveClient(UUID userId) {
        return userRepository.findById(userId)
            .filter((user) -> user.getDeletedAt() == null && user.getStatus() == UserStatus.ACTIVE)
            .map((user) -> {
                ClientProfile profile = clientProfileRepository.findByUserId(user.getId())
                    .orElseThrow(() -> new IllegalStateException("No client profile for user " + user.getId()));
                return new AuthenticatedClientResponse(user.getId(), user.getEmail(), profile.getFirstName(),
                        profile.getLastName());
            });
    }

}
