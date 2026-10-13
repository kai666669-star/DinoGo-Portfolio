package com.dinogo.sales.dto;

import java.time.LocalDateTime;

public record OrderApiErrorResponse(
        LocalDateTime timestamp,
        int status,
        String error,
        String message,
        String path) {
}