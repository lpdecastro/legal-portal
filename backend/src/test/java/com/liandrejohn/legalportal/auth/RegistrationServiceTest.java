package com.liandrejohn.legalportal.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.SQLException;

import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.liandrejohn.legalportal.client.ClientProfile;
import com.liandrejohn.legalportal.client.ClientProfileRepository;
import com.liandrejohn.legalportal.user.User;
import com.liandrejohn.legalportal.user.UserRepository;

@ExtendWith(MockitoExtension.class)
class RegistrationServiceTest {

    private static final RegisterRequest REQUEST = new RegisterRequest("John", "Doe", "john@example.com",
            "password123456");

    @Mock
    private UserRepository userRepository;

    @Mock
    private ClientProfileRepository clientProfileRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private RegistrationService registrationService;

    @Test
    void savesUserWithEncodedPasswordAndProfile() {
        when(passwordEncoder.encode("password123456")).thenReturn("encoded-hash");
        when(clientProfileRepository.save(any(ClientProfile.class))).thenAnswer((invocation) -> invocation.getArgument(0));

        RegistrationResponse response = registrationService.register(REQUEST);

        ArgumentCaptor<User> user = ArgumentCaptor.forClass(User.class);
        verify(userRepository).saveAndFlush(user.capture());
        assertThat(user.getValue().getEmail()).isEqualTo("john@example.com");
        assertThat(user.getValue().getPasswordHash()).isEqualTo("encoded-hash");

        ArgumentCaptor<ClientProfile> profile = ArgumentCaptor.forClass(ClientProfile.class);
        verify(clientProfileRepository).save(profile.capture());
        assertThat(profile.getValue().getUser()).isSameAs(user.getValue());

        assertThat(response.email()).isEqualTo("john@example.com");
        assertThat(response.firstName()).isEqualTo("John");
        assertThat(response.lastName()).isEqualTo("Doe");
    }

    @Test
    void rejectsExistingEmailWithoutSaving() {
        when(userRepository.existsByEmailIgnoreCase("john@example.com")).thenReturn(true);

        assertThatThrownBy(() -> registrationService.register(REQUEST))
            .isInstanceOf(EmailAlreadyExistsException.class);
        verify(userRepository, never()).saveAndFlush(any());
        verify(passwordEncoder, never()).encode(any());
    }

    @Test
    void mapsConcurrentEmailConflictToEmailAlreadyExists() {
        when(userRepository.saveAndFlush(any(User.class))).thenThrow(uniqueViolation("users_email_lower_key"));

        assertThatThrownBy(() -> registrationService.register(REQUEST))
            .isInstanceOf(EmailAlreadyExistsException.class);
        verify(clientProfileRepository, never()).save(any());
    }

    @Test
    void rethrowsOtherIntegrityViolations() {
        DataIntegrityViolationException violation = uniqueViolation("some_other_constraint");
        when(userRepository.saveAndFlush(any(User.class))).thenThrow(violation);

        assertThatThrownBy(() -> registrationService.register(REQUEST)).isSameAs(violation);
    }

    private static DataIntegrityViolationException uniqueViolation(String constraintName) {
        return new DataIntegrityViolationException("duplicate key",
                new ConstraintViolationException("duplicate key", new SQLException("duplicate key"), constraintName));
    }

}
