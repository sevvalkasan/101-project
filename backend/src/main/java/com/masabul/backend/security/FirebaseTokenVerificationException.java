package com.masabul.backend.security;

public class FirebaseTokenVerificationException extends RuntimeException {

    public FirebaseTokenVerificationException(String message, Throwable cause) {
        super(message, cause);
    }

    public FirebaseTokenVerificationException(String message) {
        super(message);
    }
}
