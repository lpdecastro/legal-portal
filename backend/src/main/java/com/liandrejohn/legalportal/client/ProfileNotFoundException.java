package com.liandrejohn.legalportal.client;

import org.springframework.http.HttpStatus;

import com.liandrejohn.legalportal.error.ApiException;

/** The session's user has no client profile. */
public class ProfileNotFoundException extends ApiException {

    public ProfileNotFoundException() {
        super(HttpStatus.NOT_FOUND, "PROFILE_NOT_FOUND", "Profile not found.");
    }

}
