package com.liandrejohn.legalportal.client;

import org.springframework.http.HttpStatus;

import com.liandrejohn.legalportal.error.ApiException;

/** The current password sent with a password change doesn't match the account's password. */
public class IncorrectPasswordException extends ApiException {

    public IncorrectPasswordException() {
        super(HttpStatus.FORBIDDEN, "INCORRECT_PASSWORD", "Current password is incorrect.");
    }

}
