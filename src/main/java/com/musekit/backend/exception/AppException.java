package com.musekit.backend.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Application-specific exceptions for MuseKit backend.
 *
 * Auth-related exceptions (EmailAlreadyExistsException, InvalidOtpException)
 * have been removed — authentication is now handled by ZITADEL.
 */
public class AppException {

    @ResponseStatus(HttpStatus.NOT_FOUND)
    public static class UserNotFoundException extends RuntimeException {
        public UserNotFoundException(String message) {
            super(message);
        }
    }
}
