package com.liandrejohn.legalportal.auth;

import org.springframework.security.core.Authentication;

/** A successful login: the authentication to store in the session and the response body. */
record LoginResult(Authentication authentication, AuthenticatedClientResponse response) {
}
