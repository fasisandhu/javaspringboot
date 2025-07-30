package com.redmath.jobportal.exceptions;

public class UnauthorizedJobAccessException extends RuntimeException {
    public UnauthorizedJobAccessException(String message) {
        super(message);
    }
}