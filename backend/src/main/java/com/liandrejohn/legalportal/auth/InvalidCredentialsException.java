package com.liandrejohn.legalportal.auth;

import org.springframework.http.HttpStatus;

import com.liandrejohn.legalportal.error.ApiException;

/** Covers every failed login (unknown email, wrong password, inactive account) so none can be told apart. */
public class InvalidCredentialsException extends ApiException {

    public InvalidCredentialsException() {
        super(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "Invalid email or password.");
    }

}
