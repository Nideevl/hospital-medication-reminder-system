package com.medreminder.escalationservice.exception;

public class EscalationException extends RuntimeException {
    public EscalationException(String message) {
        super(message);
    }

    public EscalationException(String message, Throwable cause) {
        super(message, cause);
    }
}
