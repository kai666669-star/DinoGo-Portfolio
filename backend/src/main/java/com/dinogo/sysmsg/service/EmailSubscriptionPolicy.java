package com.dinogo.sysmsg.service;

import com.dinogo.sysmsg.entity.NotificationType;

/**
 * Email 訂閱三態的唯一判斷規則。
 *
 * ORDER：true 寄送、false 不寄送、null 代表無法驗證而預設寄送。
 * MARKETING：只有 true 寄送；false 或 null 都不寄送。
 */
public final class EmailSubscriptionPolicy {
    private EmailSubscriptionPolicy() {
    }

    public static boolean allows(NotificationType type, Boolean preference) {
        if (type == null) {
            throw new IllegalArgumentException("notificationType 不可為 null");
        }
        return type == NotificationType.ORDER
                ? !Boolean.FALSE.equals(preference)
                : Boolean.TRUE.equals(preference);
    }
}
