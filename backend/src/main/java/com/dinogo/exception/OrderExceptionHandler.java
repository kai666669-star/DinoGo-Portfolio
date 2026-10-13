package com.dinogo.exception;

import java.time.LocalDateTime;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.dinogo.sales.controller.OrderController;
import com.dinogo.sales.controller.PaymentController;
import com.dinogo.sales.controller.SellerOrderController;
import com.dinogo.sales.controller.ShipmentController;
import com.dinogo.sales.dto.OrderApiErrorResponse;
import com.dinogo.sales.exception.InvalidOrderException;
import com.dinogo.sales.exception.OrderNotFoundException;

import jakarta.servlet.http.HttpServletRequest;

@RestControllerAdvice(assignableTypes = {
        OrderController.class,
        PaymentController.class,
        SellerOrderController.class,
        ShipmentController.class
})
public class OrderExceptionHandler {

    @ExceptionHandler(OrderNotFoundException.class)
    public ResponseEntity<OrderApiErrorResponse> handleNotFound(
            OrderNotFoundException exception,
            HttpServletRequest request) {

        return buildResponse(
                HttpStatus.NOT_FOUND,
                exception.getMessage(),
                request.getRequestURI());
    }

    @ExceptionHandler(InvalidOrderException.class)
    public ResponseEntity<OrderApiErrorResponse> handleInvalidOrder(
            InvalidOrderException exception,
            HttpServletRequest request) {

        return buildResponse(
                HttpStatus.BAD_REQUEST,
                exception.getMessage(),
                request.getRequestURI());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<OrderApiErrorResponse> handleValidation(
            MethodArgumentNotValidException exception,
            HttpServletRequest request) {

        String message = exception.getBindingResult()
                .getFieldErrors()
                .stream()
                .findFirst()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .orElse("Request validation failed");

        return buildResponse(
                HttpStatus.BAD_REQUEST,
                message,
                request.getRequestURI());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<OrderApiErrorResponse> handleUnreadableRequest(
            HttpMessageNotReadableException exception,
            HttpServletRequest request) {

        return buildResponse(
                HttpStatus.BAD_REQUEST,
                "Malformed request body or invalid order status",
                request.getRequestURI());
    }

    private ResponseEntity<OrderApiErrorResponse> buildResponse(
            HttpStatus status,
            String message,
            String path) {

        OrderApiErrorResponse response = new OrderApiErrorResponse(
                LocalDateTime.now(),
                status.value(),
                status.getReasonPhrase(),
                message,
                path);

        return ResponseEntity.status(status).body(response);
    }
}
