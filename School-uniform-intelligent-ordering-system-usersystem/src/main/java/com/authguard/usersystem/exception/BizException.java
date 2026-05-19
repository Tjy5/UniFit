package com.authguard.usersystem.exception;

public class BizException extends ApiException {
    public BizException(String message) {
        super(400, message);
    }
}
