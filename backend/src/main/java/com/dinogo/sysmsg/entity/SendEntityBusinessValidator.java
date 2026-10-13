package com.dinogo.sysmsg.entity;

import java.util.Locale;
import java.util.Set;

/** Send 父子表與 msg_function prefix 的業務一致性檢查。 */
final class SendEntityBusinessValidator {
    private static final Set<String> SUPPORTED_PREFIXES = Set.of("OA", "OC", "OS", "AC", "AS", "SC");

    private SendEntityBusinessValidator() {
    }

    static void validate(SendEntity send) {
        String function = send.getMsgFunction();
        if (function == null || function.length() < 2) {
            throw new IllegalStateException("msg_function 格式不正確");
        }
        String prefix = function.substring(0, 2).toUpperCase(Locale.ROOT);
        boolean order = send instanceof SendOrderEntity;
        boolean disorder = send instanceof SendDisorderEntity;
        boolean seller = send instanceof SendSellerEntity;
        int subtypeCount = (order ? 1 : 0) + (disorder ? 1 : 0) + (seller ? 1 : 0);

        if (subtypeCount > 1) {
            throw new IllegalStateException("一個 Send 最多只能使用一種子表");
        }
        if (("AC".equals(prefix) || "AS".equals(prefix)) && !(order || disorder)) {
            throw new IllegalStateException(prefix + " 必須使用 send_order 或 send_disorder");
        }
        if ("SC".equals(prefix) && !seller) {
            throw new IllegalStateException("SC 必須使用 send_seller");
        }
        if (Set.of("OA", "OC", "OS").contains(prefix) && subtypeCount != 0) {
            throw new IllegalStateException(prefix + " 只能使用 send 父表");
        }
        if (!SUPPORTED_PREFIXES.contains(prefix)) {
            throw new IllegalStateException("不支援的 msg_function prefix：" + prefix);
        }
    }
}
