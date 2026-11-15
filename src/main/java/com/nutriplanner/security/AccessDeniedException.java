package com.nutriplanner.security;

/** The current user is not allowed to perform the requested action. */
public class AccessDeniedException extends RuntimeException {
    public AccessDeniedException(String message) {
        super(message);
    }
}
