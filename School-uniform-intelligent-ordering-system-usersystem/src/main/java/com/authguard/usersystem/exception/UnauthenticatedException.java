package com.authguard.usersystem.exception;

public class UnauthenticatedException extends ApiException {
    public UnauthenticatedException(String message) {
        super(401, message);
    }
}
