package com.dinogo.seller.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SellerApplicationRejectRequest(
        @NotBlank(message = "駁回原因必填")
        @Size(max = 500, message = "駁回原因不可超過 500 字")
        String rejectReason) {
}
