package com.dinogo.review.exception;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.dinogo.review.controller.ReviewController;

/** 僅將 ReviewController 的業務、驗證與併發例外轉成一致的 HTTP 錯誤格式。 */
@RestControllerAdvice(assignableTypes = ReviewController.class)
public class ReviewExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(
            MethodArgumentNotValidException exception) {

        Map<String, String> fields = new LinkedHashMap<>();
        for (FieldError error : exception.getBindingResult().getFieldErrors()) {
            fields.putIfAbsent(error.getField(), error.getDefaultMessage());
        }
        return response(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED", "輸入資料驗證失敗", fields);
    }

    @ExceptionHandler(ReviewNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNotFound(ReviewNotFoundException exception) {
        return response(HttpStatus.NOT_FOUND, "REVIEW_NOT_FOUND", exception.getMessage(), null);
    }

    @ExceptionHandler(ReviewForbiddenException.class)
    public ResponseEntity<Map<String, Object>> handleForbidden(ReviewForbiddenException exception) {
        return response(HttpStatus.FORBIDDEN, "REVIEW_FORBIDDEN", exception.getMessage(), null);
    }

    @ExceptionHandler(AuthenticationCredentialsNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleUnauthenticated(
            AuthenticationCredentialsNotFoundException exception) {
        return response(HttpStatus.UNAUTHORIZED, "AUTHENTICATION_REQUIRED", exception.getMessage(), null);
    }

    @ExceptionHandler({ReviewConflictException.class, OptimisticLockingFailureException.class})
    public ResponseEntity<Map<String, Object>> handleConflict(Exception exception) {
        // 第二個重複建立請求或較舊版本的 UPDATE 會收到 HTTP 409。
        return response(HttpStatus.CONFLICT, "REVIEW_CONFLICT", exception.getMessage(), null);
    }

    @ExceptionHandler({InvalidOrderStateException.class, IllegalArgumentException.class})
    public ResponseEntity<Map<String, Object>> handleBadRequest(RuntimeException exception) {
        return response(HttpStatus.BAD_REQUEST, "BAD_REQUEST", exception.getMessage(), null);
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, Object>> handleInvalidExternalData(
            IllegalStateException exception) {
        // 單體內 Provider 資料不一致屬伺服器整合錯誤，不是上游 HTTP gateway 錯誤。
        return response(HttpStatus.INTERNAL_SERVER_ERROR,
                "MONOLITH_INTEGRATION_INVALID", exception.getMessage(), null);
    }

    private ResponseEntity<Map<String, Object>> response(
            HttpStatus status,
            String error,
            String message,
            Object details) {

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", status.value());
        body.put("error", error);
        body.put("message", message);
        if (details != null) {
            body.put("details", details);
        }
        return ResponseEntity.status(status).body(body);
    }
}
