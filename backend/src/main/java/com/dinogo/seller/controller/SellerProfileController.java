package com.dinogo.seller.controller;

import java.util.List;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.dinogo.security.AuthenticatedMember;
import com.dinogo.seller.dto.SellerProfileRequest;
import com.dinogo.seller.dto.SellerProfileResponse;
import com.dinogo.seller.dto.StorefrontSummaryResponse;
import com.dinogo.seller.service.SellerProfileService;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;

@RestController
public class SellerProfileController {

    private final SellerProfileService sellerProfileService;

    public SellerProfileController(SellerProfileService sellerProfileService) {
        this.sellerProfileService = sellerProfileService;
    }

    @GetMapping("/api/seller/profile")
    public SellerProfileResponse getMyProfile(
            @AuthenticationPrincipal AuthenticatedMember member) {
        return sellerProfileService.getMyProfile(member.memberId());
    }

    @PutMapping("/api/seller/profile")
    public SellerProfileResponse updateMyProfile(
            @AuthenticationPrincipal AuthenticatedMember member,
            @RequestBody SellerProfileRequest request) {
        return sellerProfileService.updateMyProfile(member.memberId(), request);
    }

    @GetMapping("/api/stores/{sellerId}")
    public SellerProfileResponse getPublicStore(
            @org.springframework.web.bind.annotation.PathVariable Integer sellerId) {
        return sellerProfileService.getPublicStore(sellerId);
    }

    @GetMapping("/api/stores/{sellerId}/summary")
    public StorefrontSummaryResponse getStorefrontSummary(
            @org.springframework.web.bind.annotation.PathVariable Integer sellerId) {
        return sellerProfileService.getStorefrontSummary(sellerId);
    }

    // 讓前端或 Postman 可以透過 keyword 搜尋商家。
    @GetMapping("/api/stores/search")
    public List<SellerProfileResponse> searchPublicStores(
            @RequestParam(required = false) String keyword) {
        return sellerProfileService.searchPublicStores(keyword);
    }

    // 前端或 Postman 用 form-data 傳一個 key 叫 file 的圖片，後端用 MultipartFile 接。
    @PostMapping("/api/seller/profile/logo")
    public SellerProfileResponse uploadLogo(
            @AuthenticationPrincipal AuthenticatedMember member,
            @RequestPart("file") MultipartFile file) {
        return sellerProfileService.uploadLogo(member.memberId(), file);
    }
}
