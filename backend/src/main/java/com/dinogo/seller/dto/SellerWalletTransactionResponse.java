package com.dinogo.seller.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record SellerWalletTransactionResponse(
        Integer orderId,
        String orderNo,
        String transactionType,
        String direction,
        BigDecimal amount,
        String status,
        LocalDateTime occurredAt) {
}
