package com.dinogo.sysmsg.service.validation;

import java.util.Locale;
import java.util.Set;

import com.dinogo.sysmsg.dto.external.OrderInfoResponse;

/** 無狀態的共用輸入與跨模組資料驗證。 */
public final class SysmsgValidation {
    private static final Set<String> SYSTEM_PREFIXES = Set.of("OA", "OC", "OS");

    private SysmsgValidation() {
    }

    public static String required(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + "不可空白");
        }
        return value.trim();
    }

    public static String systemPrefix(String value) {
        String prefix = required(value, "msgType").toUpperCase(Locale.ROOT);
        if (!SYSTEM_PREFIXES.contains(prefix)) {
            throw new IllegalArgumentException("系統訊息僅允許 OA/OC/OS");
        }
        return prefix;
    }

    public static Integer positive(Integer value, String name) {
        if (value == null || value <= 0) {
            throw new IllegalArgumentException(name + " 必須是正整數");
        }
        return value;
    }

    public static OrderInfoResponse completeOrder(OrderInfoResponse order) {
        if (order == null || order.getOrderId() == null || order.getBuyerId() == null
                || order.getSellerId() == null || order.getOrderNo() == null
                || order.getOrderNo().isBlank()) {
            throw new IllegalStateException("Order API 回傳資料不完整");
        }
        return order;
    }
}
