package com.liandrejohn.legalportal.auth;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Service;

import com.liandrejohn.legalportal.client.ClientProfile;
import com.liandrejohn.legalportal.client.ClientProfileRepository;
import com.liandrejohn.legalportal.security.AuthenticatedUser;

@Slf4j
@Service
@RequiredArgsConstructor
class LoginService {

    private final AuthenticationManager authenticationManager;

    private final LoginAttemptLimiter loginAttemptLimiter;

    private final ClientProfileRepository clientProfileRepository;

    /**
     * Checks the client's credentials. Doesn't touch the session: the caller stores the returned authentication.
     *
     * @throws LoginRateLimitExceededException if too many attempts failed recently for the email or IP
     * @throws InvalidCredentialsException if the email or password is wrong, or the account is inactive
     */
    LoginResult login(LoginRequest request, String clientIp) {
        if (loginAttemptLimiter.isBlocked(request.email(), clientIp)) {
            throw new LoginRateLimitExceededException();
        }

        Authentication authentication;
        try {
            authentication = authenticationManager
                .authenticate(UsernamePasswordAuthenticationToken.unauthenticated(request.email(), request.password()));
        }
        catch (AuthenticationException ex) {
            loginAttemptLimiter.recordFailure(request.email(), clientIp);
            log.info("Failed login from {}: {}", clientIp, ex.getClass().getSimpleName());
            throw new InvalidCredentialsException();
        }
        loginAttemptLimiter.recordSuccess(request.email());

        AuthenticatedUser user = (AuthenticatedUser) authentication.getPrincipal();
        ClientProfile profile = clientProfileRepository.findByUserId(user.getId())
            .orElseThrow(() -> new IllegalStateException("No client profile for user " + user.getId()));

        log.info("Client user {} logged in", user.getId());
        return new LoginResult(authentication, new AuthenticatedClientResponse(user.getId(), user.getUsername(),
                profile.getFirstName(), profile.getLastName()));
    }

}
