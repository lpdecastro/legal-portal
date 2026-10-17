package com.liandrejohn.legalportal.cases;

import org.springframework.http.HttpStatus;

import com.liandrejohn.legalportal.error.ApiException;

/** The case's status doesn't allow the requested action, e.g. editing a case that is no longer a draft. */
public class InvalidCaseStateException extends ApiException {

    public InvalidCaseStateException(String detail) {
        super(HttpStatus.CONFLICT, "INVALID_CASE_STATE", detail);
    }

}
