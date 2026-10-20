package com.liandrejohn.legalportal.client;

import java.util.Optional;
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
        User user = findActiveUser(userId);
        ClientProfile profile = clientProfileRepository.findByUserId(userId).orElseThrow(ProfileNotFoundException::new);
        return ClientProfileResponse.from(user, profile);
    }

    /** Updates the session's client's name and phone; omitted fields are unchanged. Checks match {@link #getProfile}. */
    @Transactional
    ClientProfileResponse updateProfile(UUID userId, UpdateProfileRequest request) {
        User user = findActiveUser(userId);
        ClientProfile profile = clientProfileRepository.findByUserId(userId).orElseThrow(ProfileNotFoundException::new);
        profile.updateDetails(valueOrCurrent(request.getFirstName(), profile.getFirstName()),
                valueOrCurrent(request.getLastName(), profile.getLastName()),
                valueOrCurrent(request.getPhone(), profile.getPhone()));
        return ClientProfileResponse.from(user, profile);
    }

    private User findActiveUser(UUID userId) {
        return userRepository.findById(userId)
            .filter((candidate) -> candidate.getDeletedAt() == null && candidate.getStatus() == UserStatus.ACTIVE)
            .orElseThrow(AuthenticationRequiredException::new);
    }

    /** {@code null} means the field was left out of the request; an empty {@code Optional} clears it. */
    private static <T> T valueOrCurrent(Optional<T> field, T current) {
        return field != null ? field.orElse(null) : current;
    }

}
