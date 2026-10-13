package com.dinogo.sysmsg.controller;

import java.time.LocalDateTime;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;

import com.dinogo.sysmsg.dto.response.SysmsgApiErrorResponse;
import com.dinogo.sysmsg.exception.SysmsgConflictException;
import com.dinogo.sales.exception.OrderNotFoundException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;

/**
 * 功能：只將 sysmsg Controller 拋出的例外轉換為 HTTP 錯誤。
 * 應用：沿用模擬後端各模組的 assignableTypes 限定方式，避免介入 member、order、review 的既有錯誤處理。
 * 回應欄位對齊模擬後端 OrderApiErrorResponse，sysmsg 特有的權限、衝突與狀態例外再由本類補充。
 */
@RestControllerAdvice(assignableTypes = {
        MemberInboxController.class,
        SellerInboxController.class,
        SellerMessageController.class,
        SellerTemplateController.class,
        SendStatusController.class,
        SystemMessageController.class,
        SystemTemplateController.class
})
public class SysmsgExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<SysmsgApiErrorResponse> validation(
            MethodArgumentNotValidException exception,
            HttpServletRequest request) {
        String message = exception.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining("; "));
        return response(HttpStatus.BAD_REQUEST, message, request);
    }

    @ExceptionHandler({
            HttpMessageNotReadableException.class,
            MissingRequestHeaderException.class,
            HandlerMethodValidationException.class,
            ConstraintViolationException.class,
            IllegalArgumentException.class
    })
    public ResponseEntity<SysmsgApiErrorResponse> badRequest(
            Exception exception, HttpServletRequest request) {
        return response(HttpStatus.BAD_REQUEST,
                safeMessage(exception, "請求內容或參數格式錯誤"), request);
    }

    @ExceptionHandler(AuthenticationCredentialsNotFoundException.class)
    public ResponseEntity<SysmsgApiErrorResponse> unauthorized(
            AuthenticationCredentialsNotFoundException exception,
            HttpServletRequest request) {
        return response(HttpStatus.UNAUTHORIZED,
                safeMessage(exception, "尚未登入"), request);
    }

    @ExceptionHandler({SecurityException.class, AccessDeniedException.class})
    public ResponseEntity<SysmsgApiErrorResponse> forbidden(
            RuntimeException exception, HttpServletRequest request) {
        return response(HttpStatus.FORBIDDEN,
                safeMessage(exception, "沒有執行此操作的權限"), request);
    }

    @ExceptionHandler({NoSuchElementException.class, OrderNotFoundException.class})
    public ResponseEntity<SysmsgApiErrorResponse> notFound(
            RuntimeException exception, HttpServletRequest request) {
        return response(HttpStatus.NOT_FOUND,
                safeMessage(exception, "找不到指定資源"), request);
    }

    @ExceptionHandler({
            SysmsgConflictException.class,
            DataIntegrityViolationException.class,
            OptimisticLockingFailureException.class
    })
    public ResponseEntity<SysmsgApiErrorResponse> conflict(
            RuntimeException exception, HttpServletRequest request) {
        return response(HttpStatus.CONFLICT,
                safeMessage(exception, "資料重複或狀態衝突"), request);
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<SysmsgApiErrorResponse> unprocessable(
            IllegalStateException exception, HttpServletRequest request) {
        return response(HttpStatus.UNPROCESSABLE_ENTITY,
                safeMessage(exception, "目前資料狀態無法處理此要求"), request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<SysmsgApiErrorResponse> internalError(
            Exception exception, HttpServletRequest request) {
        return response(HttpStatus.INTERNAL_SERVER_ERROR,
                "系統處理失敗", request);
    }

    private ResponseEntity<SysmsgApiErrorResponse> response(
            HttpStatus status, String message, HttpServletRequest request) {
        SysmsgApiErrorResponse body = new SysmsgApiErrorResponse(
                LocalDateTime.now(),
                status.value(),
                status.getReasonPhrase(),
                message,
                request.getRequestURI());
        return ResponseEntity.status(status).body(body);
    }

    private String safeMessage(Exception exception, String fallback) {
        return exception.getMessage() == null || exception.getMessage().isBlank()
                ? fallback
                : exception.getMessage();
    }
}
