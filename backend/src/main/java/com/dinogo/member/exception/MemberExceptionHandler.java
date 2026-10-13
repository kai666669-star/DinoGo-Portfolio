package com.dinogo.member.exception;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;

import com.dinogo.member.controller.AddressController;
import com.dinogo.member.controller.AdminMemberController;
import com.dinogo.member.controller.LoginController;
import com.dinogo.member.controller.MemberController;
import com.dinogo.member.controller.RegisterController;
import com.dinogo.member.dto.MemberApiErrorResponse;

import jakarta.validation.ConstraintViolationException;

/** 只統一 A 模組 Controller 的 request validation 與 JSON 格式錯誤。 */
@RestControllerAdvice(assignableTypes = {
        RegisterController.class,
        LoginController.class,
        MemberController.class,
        AddressController.class,
        AdminMemberController.class
})
public class MemberExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<MemberApiErrorResponse> handleValidation(
            MethodArgumentNotValidException exception) {

        Map<String, String> fieldErrors = new LinkedHashMap<>();
        exception.getBindingResult().getFieldErrors()
                .forEach(error -> fieldErrors.putIfAbsent(error.getField(), error.getDefaultMessage()));

        return ResponseEntity.badRequest()
                .body(MemberApiErrorResponse.validation(fieldErrors));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<MemberApiErrorResponse> handleUnreadableRequest(
            HttpMessageNotReadableException exception) {

        return ResponseEntity.badRequest()
                .body(MemberApiErrorResponse.from(
                        HttpStatus.BAD_REQUEST,
                        "請求內容格式錯誤"));
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<MemberApiErrorResponse> handleMethodValidation(
            HandlerMethodValidationException exception) {

        return ResponseEntity.badRequest()
                .body(MemberApiErrorResponse.from(
                        HttpStatus.BAD_REQUEST,
                        "請求參數驗證失敗"));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<MemberApiErrorResponse> handleConstraintViolation(
            ConstraintViolationException exception) {

        return ResponseEntity.badRequest()
                .body(MemberApiErrorResponse.from(
                        HttpStatus.BAD_REQUEST,
                        "請求參數驗證失敗"));
    }
}
