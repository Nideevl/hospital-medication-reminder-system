package com.medreminder.callservice.exception;

public class CallInitiationException extends RuntimeException {
    public CallInitiationException(String message) {
        super(message);
    }

    public CallInitiationException(String message, Throwable cause) {
        super(message, cause);
    }
}
