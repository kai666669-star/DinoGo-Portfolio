package com.dinogo.seller.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SellerApplicationRequest(
        @NotBlank(message = "店鋪名稱必填")
        @Size(max = 100, message = "店鋪名稱不可超過 100 字")
        String storeName,

        @Size(max = 500, message = "店鋪介紹不可超過 500 字")
        String storeDescription,

        @Size(max = 500, message = "Logo URL 不可超過 500 字")
        String storeLogoUrl) {
}
