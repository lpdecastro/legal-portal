package com.liandrejohn.legalportal.security;

import org.springframework.http.HttpStatus;

import com.liandrejohn.legalportal.error.ApiException;

/** The request has no valid authenticated session. */
public class AuthenticationRequiredException extends ApiException {

    static final String DETAIL = "Authentication required.";

    public AuthenticationRequiredException() {
        super(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", DETAIL);
    }

}
