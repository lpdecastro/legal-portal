package com.liandrejohn.legalportal.security;

import java.io.Serial;
import java.util.Collection;
import java.util.UUID;

import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.User;

/** The logged-in user stored in the session. The username is the user's email. */
@Getter
public class AuthenticatedUser extends User {

    @Serial
    private static final long serialVersionUID = 1L;

    private final UUID id;

    AuthenticatedUser(UUID id, String email, String passwordHash, boolean enabled, boolean accountNonLocked,
            Collection<? extends GrantedAuthority> authorities) {
        super(email, passwordHash, enabled, true, true, accountNonLocked, authorities);
        this.id = id;
    }

}
