package com.authguard.usersystem.exception;

import com.authguard.usersystem.dto.ApiResponse;
import jakarta.validation.ConstraintViolationException;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(BizException.class)
    public ResponseEntity<ApiResponse<Void>> handleBizException(BizException exception) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.fail(exception.getCode(), exception.getMessage()));
    }

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNotFoundException(NotFoundException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.fail(exception.getCode(), exception.getMessage()));
    }

    @ExceptionHandler(UnauthenticatedException.class)
    public ResponseEntity<ApiResponse<Void>> handleUnauthenticatedException(UnauthenticatedException exception) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.fail(exception.getCode(), exception.getMessage()));
    }

    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<ApiResponse<Void>> handleForbiddenException(ForbiddenException exception) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.fail(exception.getCode(), exception.getMessage()));
    }

    @ExceptionHandler({IllegalArgumentException.class, IllegalStateException.class})
    public ResponseEntity<ApiResponse<Void>> handleBusinessArgumentException(RuntimeException exception) {
        return ResponseEntity.badRequest().body(ApiResponse.fail(400, exception.getMessage()));
    }

    @ExceptionHandler(SecurityException.class)
    public ResponseEntity<ApiResponse<Void>> handleSecurityException(SecurityException exception) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.fail(403, exception.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleMethodArgumentNotValid(MethodArgumentNotValidException exception) {
        String message = exception.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(this::formatFieldError)
                .collect(Collectors.joining("; "));
        return ResponseEntity.badRequest().body(ApiResponse.fail(400, message));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleConstraintViolation(ConstraintViolationException exception) {
        String message = exception.getConstraintViolations()
                .stream()
                .map(violation -> violation.getPropertyPath() + ": " + violation.getMessage())
                .collect(Collectors.joining("; "));
        return ResponseEntity.badRequest().body(ApiResponse.fail(400, message));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> handleHttpMessageNotReadable(HttpMessageNotReadableException exception) {
        return ResponseEntity.badRequest().body(ApiResponse.fail(400, "请求体格式错误或缺失"));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleException(Exception exception) {
        log.error("Unhandled exception", exception);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.fail(500, "系统内部错误"));
    }

    private String formatFieldError(FieldError error) {
        return error.getField() + ": " + error.getDefaultMessage();
    }
}
