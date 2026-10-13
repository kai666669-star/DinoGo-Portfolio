package com.dinogo.seller.controller;

import java.util.List;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.dinogo.security.AuthenticatedMember;
import com.dinogo.seller.dto.SellerApplicationRejectRequest;
import com.dinogo.seller.dto.SellerApplicationResponse;
import com.dinogo.seller.entity.SellerApplicationStatus;
import com.dinogo.seller.service.SellerApplicationService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/admin/seller-applications")
public class SellerApplicationAdminController {

    private final SellerApplicationService sellerApplicationService;

    public SellerApplicationAdminController(SellerApplicationService sellerApplicationService) {
        this.sellerApplicationService = sellerApplicationService;
    }

    @GetMapping
    public List<SellerApplicationResponse> listApplications(
            @RequestParam(required = false) SellerApplicationStatus status) {
        return sellerApplicationService.listApplications(status);
    }

    @GetMapping("/{applicationId}")
    public SellerApplicationResponse getApplication(
            @PathVariable Integer applicationId) {
        return sellerApplicationService.getApplication(applicationId);
    }

    @PostMapping("/{applicationId}/approve")
    public SellerApplicationResponse approveApplication(
            @AuthenticationPrincipal AuthenticatedMember admin,
            @PathVariable Integer applicationId) {
        return sellerApplicationService.approveApplication(applicationId, admin.memberId());
    }

    @PostMapping("/{applicationId}/reject")
    public SellerApplicationResponse rejectApplication(
            @AuthenticationPrincipal AuthenticatedMember admin,
            @PathVariable Integer applicationId,
            @Valid @RequestBody SellerApplicationRejectRequest request) {
        return sellerApplicationService.rejectApplication(applicationId, admin.memberId(), request);
    }
}
