package com.liandrejohn.legalportal.cases;

import org.springframework.http.HttpStatus;

import com.liandrejohn.legalportal.error.ApiException;

/** The case doesn't exist, belongs to another client, or was deleted; the response doesn't say which. */
public class CaseNotFoundException extends ApiException {

    public CaseNotFoundException() {
        super(HttpStatus.NOT_FOUND, "CASE_NOT_FOUND", "Case not found.");
    }

}
