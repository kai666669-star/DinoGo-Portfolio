package com.dinogo.seller.controller;

import java.time.LocalDate;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.dinogo.security.AuthenticatedMember;
import com.dinogo.seller.dto.SellerSalesInsightResponse;
import com.dinogo.seller.service.SellerSalesInsightService;

@RestController
@RequestMapping("/api/seller/sales-insight")
public class SellerSalesInsightController {

    private final SellerSalesInsightService sellerSalesInsightService;

    public SellerSalesInsightController(SellerSalesInsightService sellerSalesInsightService) {
        this.sellerSalesInsightService = sellerSalesInsightService;
    }

    @GetMapping
    public SellerSalesInsightResponse getSalesInsightStats(
            @AuthenticationPrincipal AuthenticatedMember member,
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate) {
        return sellerSalesInsightService.getStats(member.memberId(), startDate, endDate);
    }

    @PostMapping("/analyze")
    public SellerSalesInsightResponse analyzeSalesInsight(
            @AuthenticationPrincipal AuthenticatedMember member,
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate) {
        return sellerSalesInsightService.analyze(member.memberId(), startDate, endDate);
    }
}
