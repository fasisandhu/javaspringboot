package com.redmath.jobportal.auth.exceptions;

public class UserRegistrationException extends RuntimeException {
    public UserRegistrationException(String message, Throwable cause) {
        super(message, cause);
    }

    public UserRegistrationException(String message) {
        super(message);
    }
}