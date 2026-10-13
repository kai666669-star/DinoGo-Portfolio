package com.dinogo.member.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** 新增與修改地址共用的輸入格式；memberId 由 JWT 取得，不放在 request。 */
public record AddressRequest(
        // 收件人資料。
        @NotBlank(message = "收件人姓名不可為空")
        @Size(max = 100, message = "收件人姓名不可超過 100 個字")
        String receiverName,

        @NotBlank(message = "收件人電話不可為空")
        @Size(max = 20, message = "收件人電話不可超過 20 個字")
        String receiverPhone,

        // 地址資料。
        @Size(max = 10, message = "郵遞區號不可超過 10 個字")
        String postalCode,

        @NotBlank(message = "縣市不可為空")
        @Size(max = 10, message = "縣市不可超過 10 個字")
        String city,

        @NotBlank(message = "行政區不可為空")
        @Size(max = 10, message = "行政區不可超過 10 個字")
        String district,

        @NotBlank(message = "詳細地址不可為空")
        @Size(max = 255, message = "詳細地址不可超過 255 個字")
        String detailAddress,

        // null 與 false 都代表不主動設為預設地址。
        Boolean isDefault
) {
}
