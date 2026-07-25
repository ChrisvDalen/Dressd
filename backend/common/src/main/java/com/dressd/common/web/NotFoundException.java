package com.dressd.common.web;

/** Signals that a requested resource does not exist (or is not the caller's). */
public class NotFoundException extends RuntimeException {

    public NotFoundException(String message) {
        super(message);
    }
}
