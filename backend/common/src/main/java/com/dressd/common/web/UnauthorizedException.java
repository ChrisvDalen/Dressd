package com.dressd.common.web;

/** Signals a missing, malformed or expired owner credential. */
public class UnauthorizedException extends RuntimeException {

    public UnauthorizedException(String message) {
        super(message);
    }
}
