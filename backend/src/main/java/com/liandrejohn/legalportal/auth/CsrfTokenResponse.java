package com.liandrejohn.legalportal.auth;

public record CsrfTokenResponse(String token, String headerName) {
}
