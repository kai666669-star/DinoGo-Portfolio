package com.dinogo.cart.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.dinogo.cart.dto.CheckoutPreviewRequest;
import com.dinogo.cart.dto.CheckoutPreviewResponse;
import com.dinogo.cart.service.CheckoutService;
import com.dinogo.security.AuthenticatedMember;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/checkout")
@Validated
public class CheckOutController {

    private final CheckoutService checkoutService;

    public CheckOutController(CheckoutService checkoutService) {
        this.checkoutService = checkoutService;
    }

    @PostMapping("/preview")
    public ResponseEntity<CheckoutPreviewResponse> preview(
            @AuthenticationPrincipal AuthenticatedMember member,
            @Valid @RequestBody CheckoutPreviewRequest request) {

        CheckoutPreviewResponse response = checkoutService.preview(
                request,
                member.memberId());

        return ResponseEntity.ok(response);
    }
}