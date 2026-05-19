package com.authguard.usersystem.exception;

public abstract class ApiException extends RuntimeException {
    private final int code;

    protected ApiException(int code, String message) {
        super(message);
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}
