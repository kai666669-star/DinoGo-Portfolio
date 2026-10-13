package com.dinogo.seller.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.dinogo.security.AuthenticatedMember;
import com.dinogo.seller.dto.SellerApplicationRequest;
import com.dinogo.seller.dto.SellerApplicationResponse;
import com.dinogo.seller.service.SellerApplicationService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/member/seller-applications")
public class SellerApplicationController {

    private final SellerApplicationService sellerApplicationService;

    public SellerApplicationController(SellerApplicationService sellerApplicationService) {
        this.sellerApplicationService = sellerApplicationService;
    }

    @PostMapping
    public SellerApplicationResponse submitApplication(
            @AuthenticationPrincipal AuthenticatedMember member,
            @Valid @RequestBody SellerApplicationRequest request) {
        return sellerApplicationService.submitApplication(member.memberId(), request);
    }

    @GetMapping("/me")
    public SellerApplicationResponse getMyLatestApplication(
            @AuthenticationPrincipal AuthenticatedMember member) {
        return sellerApplicationService.getMyLatestApplication(member.memberId());
    }
}
