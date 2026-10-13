package com.dinogo.seller.dto;

import java.math.BigDecimal;

public record SellerWalletResponse(
        BigDecimal availableBalance,
        BigDecimal pendingBalance,
        BigDecimal withdrawnBalance,
        String bankName,
        String bankAccountLast4,
        boolean bankVerified) {
}
