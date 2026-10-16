package com.liandrejohn.legalportal.auth;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.liandrejohn.legalportal.client.ClientProfile;
import com.liandrejohn.legalportal.client.ClientProfileRepository;
import com.liandrejohn.legalportal.user.User;
import com.liandrejohn.legalportal.user.UserRepository;

@Slf4j
@Service
@RequiredArgsConstructor
public class RegistrationService {

    private static final String EMAIL_UNIQUE_INDEX = "users_email_lower_key";

    private final UserRepository userRepository;

    private final ClientProfileRepository clientProfileRepository;

    private final PasswordEncoder passwordEncoder;

    /** Creates a client user and profile. Does not authenticate the new client. */
    @Transactional
    public RegistrationResponse register(RegisterRequest request) {
        if (userRepository.existsByEmailIgnoreCase(request.email())) {
            throw new EmailAlreadyExistsException();
        }

        User user = new User(request.email(), passwordEncoder.encode(request.password()));
        try {
            // Flush now so a concurrent registration that slipped past the check hits the unique index here
            userRepository.saveAndFlush(user);
        }
        catch (DataIntegrityViolationException ex) {
            if (isEmailConflict(ex)) {
                throw new EmailAlreadyExistsException();
            }
            throw ex;
        }

        ClientProfile profile = clientProfileRepository
            .save(new ClientProfile(user, request.firstName(), request.lastName()));

        log.info("Registered client user {}", user.getId());
        return new RegistrationResponse(user.getId(), user.getEmail(), profile.getFirstName(), profile.getLastName());
    }

    private static boolean isEmailConflict(DataIntegrityViolationException ex) {
        return ex.getCause() instanceof ConstraintViolationException violation
                && EMAIL_UNIQUE_INDEX.equals(violation.getConstraintName());
    }

}
