package com.liandrejohn.legalportal.error;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/** A business error that maps directly to a Problem Details response. */
@Getter
public abstract class ApiException extends RuntimeException {

    private final HttpStatus status;

    private final String errorCode;

    protected ApiException(HttpStatus status, String errorCode, String detail) {
        super(detail);
        this.status = status;
        this.errorCode = errorCode;
    }

}
