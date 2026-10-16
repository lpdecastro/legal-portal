package com.liandrejohn.legalportal.security;

import java.util.List;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.liandrejohn.legalportal.user.User;
import com.liandrejohn.legalportal.user.UserRepository;
import com.liandrejohn.legalportal.user.UserStatus;

/** Loads users by email for login. Deleted users are treated as unknown; suspended and disabled ones can't log in. */
@Service
@RequiredArgsConstructor
class ClientUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String email) {
        User user = userRepository.findByEmailIgnoreCaseAndDeletedAtIsNull(email)
            .orElseThrow(() -> new UsernameNotFoundException("No active user with that email"));
        return new AuthenticatedUser(user.getId(), user.getEmail(), user.getPasswordHash(),
                user.getStatus() != UserStatus.DISABLED, user.getStatus() != UserStatus.SUSPENDED,
                List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole())));
    }

}
