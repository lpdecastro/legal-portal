package com.liandrejohn.legalportal.client;

import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.liandrejohn.legalportal.security.AuthenticationRequiredException;
import com.liandrejohn.legalportal.user.User;
import com.liandrejohn.legalportal.user.UserRepository;
import com.liandrejohn.legalportal.user.UserStatus;

@Service
@RequiredArgsConstructor
class ClientProfileService {

    private final UserRepository userRepository;

    private final ClientProfileRepository clientProfileRepository;

    /**
     * Loads the session's client's profile. Accounts deleted, suspended or disabled after login are rejected with 401,
     * as for case endpoints; an active account without a profile gets 404.
     */
    @Transactional(readOnly = true)
    ClientProfileResponse getProfile(UUID userId) {
        User user = userRepository.findById(userId)
            .filter((candidate) -> candidate.getDeletedAt() == null && candidate.getStatus() == UserStatus.ACTIVE)
            .orElseThrow(AuthenticationRequiredException::new);
        ClientProfile profile = clientProfileRepository.findByUserId(userId).orElseThrow(ProfileNotFoundException::new);
        return ClientProfileResponse.from(user, profile);
    }

}
