package com.hchen.hooktool.exception;

@Deprecated
public class HookException extends RuntimeException {
    public HookException() {
    }

    public HookException(String message) {
        super(message);
    }

    public HookException(String message, Throwable cause) {
        super(message, cause);
    }

    public HookException(Throwable cause) {
        super(cause);
    }
}
