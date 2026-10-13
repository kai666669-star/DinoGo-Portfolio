package com.dinogo.seller.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

import com.dinogo.catalog.dto.ProductDetailResponse;
import com.dinogo.catalog.service.ProductService;
import com.dinogo.security.AuthenticatedMember;
import com.dinogo.seller.dto.SellerProductResponse;
import com.dinogo.seller.service.CurrentSellerService;
import com.dinogo.seller.service.SellerProductService;

@RestController
@RequestMapping("/api/seller/products")
public class SellerProductController {

    private final SellerProductService sellerProductService;
    private final ProductService productService;
    private final CurrentSellerService currentSellerService;

    public SellerProductController(
            SellerProductService sellerProductService,
            ProductService productService,
            CurrentSellerService currentSellerService) {

        this.sellerProductService = sellerProductService;
        this.productService = productService;
        this.currentSellerService = currentSellerService;
    }

    @GetMapping
    public List<SellerProductResponse> getProducts(
            @AuthenticationPrincipal AuthenticatedMember member,
            @RequestParam(required = false) Integer sellerId) {
        Integer currentSellerId = currentSellerService.requireMatchingActiveSellerId(
                member.memberId(), sellerId);
        return sellerProductService.getProducts(currentSellerId);
    }

    @PatchMapping("/{productId}/disable")
    public SellerProductResponse disableProduct(
            @AuthenticationPrincipal AuthenticatedMember member,
            @RequestParam(required = false) Integer sellerId,
            @PathVariable Integer productId) {
        Integer currentSellerId = currentSellerService.requireMatchingActiveSellerId(
                member.memberId(), sellerId);
        return sellerProductService.disableProduct(currentSellerId, productId);
    }

    @GetMapping("/{productId}")
    public ProductDetailResponse getSellerProductDetail(
            @AuthenticationPrincipal AuthenticatedMember member,
            @PathVariable Integer productId) {

        return productService.getSellerProductDetail(productId, member.memberId());
    }
}
