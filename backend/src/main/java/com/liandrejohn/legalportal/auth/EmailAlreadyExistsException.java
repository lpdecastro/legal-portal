package com.liandrejohn.legalportal.auth;

import org.springframework.http.HttpStatus;

import com.liandrejohn.legalportal.error.ApiException;

public class EmailAlreadyExistsException extends ApiException {

    public EmailAlreadyExistsException() {
        super(HttpStatus.CONFLICT, "EMAIL_ALREADY_EXISTS", "Email address is already registered.");
    }

}
