package com.liandrejohn.legalportal.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import com.liandrejohn.legalportal.client.ClientProfile;
import com.liandrejohn.legalportal.client.ClientProfileRepository;
import com.liandrejohn.legalportal.security.AuthenticatedUser;

@ExtendWith(MockitoExtension.class)
class LoginServiceTest {

    private static final LoginRequest REQUEST = new LoginRequest("john@example.com", "password123456");

    private static final String IP = "203.0.113.10";

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private LoginAttemptLimiter loginAttemptLimiter;

    @Mock
    private ClientProfileRepository clientProfileRepository;

    @Mock
    private AuthenticatedUser user;

    @Mock
    private ClientProfile profile;

    @InjectMocks
    private LoginService loginService;

    @Test
    void returnsAuthenticationAndProfileOnSuccess() {
        UUID id = UUID.randomUUID();
        Authentication authentication = UsernamePasswordAuthenticationToken.authenticated(user, null, null);
        when(authenticationManager.authenticate(any())).thenReturn(authentication);
        when(user.getId()).thenReturn(id);
        when(user.getUsername()).thenReturn("john@example.com");
        when(profile.getFirstName()).thenReturn("John");
        when(profile.getLastName()).thenReturn("Doe");
        when(clientProfileRepository.findByUserId(id)).thenReturn(Optional.of(profile));

        LoginResult result = loginService.login(REQUEST, IP);

        assertThat(result.authentication()).isSameAs(authentication);
        assertThat(result.response()).isEqualTo(new LoginResponse(id, "john@example.com", "John", "Doe"));
        verify(loginAttemptLimiter).recordSuccess("john@example.com");
    }

    @Test
    void recordsFailureAndThrowsGenericErrorForBadCredentials() {
        when(authenticationManager.authenticate(any())).thenThrow(new BadCredentialsException("Bad credentials"));

        assertThatThrownBy(() -> loginService.login(REQUEST, IP)).isInstanceOf(InvalidCredentialsException.class)
            .hasMessage("Invalid email or password.");
        verify(loginAttemptLimiter).recordFailure("john@example.com", IP);
    }

    @Test
    void inactiveAccountGetsSameGenericError() {
        when(authenticationManager.authenticate(any())).thenThrow(new DisabledException("User is disabled"));

        assertThatThrownBy(() -> loginService.login(REQUEST, IP)).isInstanceOf(InvalidCredentialsException.class)
            .hasMessage("Invalid email or password.");
        verify(loginAttemptLimiter).recordFailure("john@example.com", IP);
    }

    @Test
    void rejectsBlockedAttemptWithoutAuthenticating() {
        when(loginAttemptLimiter.isBlocked("john@example.com", IP)).thenReturn(true);

        assertThatThrownBy(() -> loginService.login(REQUEST, IP))
            .isInstanceOf(LoginRateLimitExceededException.class);
        verify(authenticationManager, never()).authenticate(any());
    }

}
