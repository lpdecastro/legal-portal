package com.liandrejohn.legalportal.client;

import java.util.UUID;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.liandrejohn.legalportal.error.FieldValidationException;
import com.liandrejohn.legalportal.security.AuthenticationRequiredException;
import com.liandrejohn.legalportal.user.User;
import com.liandrejohn.legalportal.user.UserRepository;
import com.liandrejohn.legalportal.user.UserStatus;

@Slf4j
@Service
@RequiredArgsConstructor
class ChangePasswordService {

    private final UserRepository userRepository;

    private final PasswordEncoder passwordEncoder;

    /**
     * Replaces the session's client's password. Check order: confirmation mismatch 400 → wrong current password 403 →
     * new password equal to the current one 400. The last check runs only once the current password is verified, so
     * it can't be used to guess the password. Inactive accounts get 401, as for the other profile endpoints.
     */
    @Transactional
    void changePassword(UUID userId, ChangePasswordRequest request) {
        User user = userRepository.findById(userId)
            .filter((candidate) -> candidate.getDeletedAt() == null && candidate.getStatus() == UserStatus.ACTIVE)
            .orElseThrow(AuthenticationRequiredException::new);

        if (!request.newPassword().equals(request.confirmPassword())) {
            throw new FieldValidationException("confirmPassword", "Passwords do not match");
        }
        if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            throw new IncorrectPasswordException();
        }
        if (request.newPassword().equals(request.currentPassword())) {
            throw new FieldValidationException("newPassword", "New password must differ from the current password");
        }

        user.changePasswordHash(passwordEncoder.encode(request.newPassword()));
        log.info("Changed password for user {}", user.getId());
    }

}
