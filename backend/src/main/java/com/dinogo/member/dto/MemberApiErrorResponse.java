package com.dinogo.member.dto;

import java.util.Map;

import org.springframework.http.HttpStatus;

/** 會員、登入與地址 API 共用的錯誤回傳格式。 */
public record MemberApiErrorResponse(
        int status,
        String error,
        String message,
        Map<String, String> fieldErrors) {

    public static MemberApiErrorResponse from(HttpStatus status, String message) {
        return new MemberApiErrorResponse(
                status.value(),
                status.getReasonPhrase(),
                message,
                Map.of());
    }

    public static MemberApiErrorResponse validation(Map<String, String> fieldErrors) {
        return new MemberApiErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                HttpStatus.BAD_REQUEST.getReasonPhrase(),
                "輸入資料驗證失敗",
                Map.copyOf(fieldErrors));
    }
}
