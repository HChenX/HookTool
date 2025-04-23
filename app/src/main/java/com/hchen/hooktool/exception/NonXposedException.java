package com.hchen.hooktool.exception;

public class NonXposedException extends RuntimeException {
    public NonXposedException() {
    }

    public NonXposedException(String message) {
        super(message);
    }

    public NonXposedException(String message, Throwable cause) {
        super(message, cause);
    }

    public NonXposedException(Throwable cause) {
        super(cause);
    }
}
