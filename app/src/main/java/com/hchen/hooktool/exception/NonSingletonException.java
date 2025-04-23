package com.hchen.hooktool.exception;

public class NonSingletonException extends RuntimeException {
    public NonSingletonException() {
    }

    public NonSingletonException(String message) {
        super(message);
    }

    public NonSingletonException(String message, Throwable cause) {
        super(message, cause);
    }

    public NonSingletonException(Throwable cause) {
        super(cause);
    }
}
