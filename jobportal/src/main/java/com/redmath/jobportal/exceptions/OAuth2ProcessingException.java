package com.redmath.jobportal.auth.exceptions;

public class OAuth2ProcessingException extends RuntimeException {
    public OAuth2ProcessingException(String message, Throwable cause) {
        super(message, cause);
    }

    public OAuth2ProcessingException(String message) {
        super(message);
    }
}