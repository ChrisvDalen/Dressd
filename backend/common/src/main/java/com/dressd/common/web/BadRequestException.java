package com.dressd.common.web;

/** Signals a client-side error that validation annotations cannot express. */
public class BadRequestException extends RuntimeException {

    public BadRequestException(String message) {
        super(message);
    }
}
