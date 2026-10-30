package com.agrimarket.service;

/** A problem with what the user entered; the message is shown to them as-is. */
public class ValidationException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public ValidationException(String message) {
        super(message);
    }
}
