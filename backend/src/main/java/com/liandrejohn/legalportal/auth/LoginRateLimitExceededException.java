package com.liandrejohn.legalportal.auth;

import org.springframework.http.HttpStatus;

import com.liandrejohn.legalportal.error.ApiException;

public class LoginRateLimitExceededException extends ApiException {

    public LoginRateLimitExceededException() {
        super(HttpStatus.TOO_MANY_REQUESTS, "RATE_LIMIT_EXCEEDED", "Too many failed login attempts. Try again later.");
    }

}
