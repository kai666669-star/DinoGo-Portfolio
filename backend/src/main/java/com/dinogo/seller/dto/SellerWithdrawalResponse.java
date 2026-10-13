package com.dinogo.seller.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.dinogo.seller.entity.WithdrawalStatus;

public record SellerWithdrawalResponse(
        Integer withdrawalId,
        BigDecimal amount,
        WithdrawalStatus status,
        LocalDateTime requestedAt,
        SellerWalletResponse wallet) {
}
