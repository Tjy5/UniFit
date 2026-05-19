package com.suios.admin.common.exception;

import com.suios.admin.common.result.R;
import jakarta.validation.ConstraintViolationException;
import java.util.stream.Collectors;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BizException.class)
    public R<Void> handleBizException(BizException exception) {
        return R.fail(400, exception.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public R<Void> handleMethodArgumentNotValid(MethodArgumentNotValidException exception) {
        String message = exception.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(this::formatFieldError)
                .collect(Collectors.joining("; "));
        return R.fail(400, message);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public R<Void> handleConstraintViolation(ConstraintViolationException exception) {
        return R.fail(400, exception.getMessage());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public R<Void> handleHttpMessageNotReadable(HttpMessageNotReadableException exception) {
        return R.fail(400, "Request body is malformed or missing");
    }

    @ExceptionHandler(Exception.class)
    public R<Void> handleException(Exception exception) {
        return R.fail("Internal server error");
    }

    private String formatFieldError(FieldError error) {
        return error.getField() + ": " + error.getDefaultMessage();
    }
}
