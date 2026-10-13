package com.dinogo.sysmsg.entity;

import java.util.Locale;

public enum NotificationType {
    ORDER,
    MARKETING;

    /**
     * 會員收件：AC / SC 是訂單訊息。
     * 商家收件：AS 是訂單訊息。
     * OA 可同時給會員與商家，OC 只給會員，OS 只給商家。
     * 不合法的收件人與 prefix 組合直接拒絕，不得誤歸類為行銷。
     */
    public static NotificationType resolve(String msgFunction, boolean sellerRecipient) {
        if (msgFunction == null || msgFunction.length() < 2) {
            throw new IllegalArgumentException("msgFunction 格式不正確");
        }

        String prefix = msgFunction.substring(0, 2).toUpperCase(Locale.ROOT);
        if (sellerRecipient) {
            return switch (prefix) {
                case "AS" -> ORDER;
                case "OA", "OS" -> MARKETING;
                default -> throw invalidRecipient(prefix, true);
            };
        }
        return switch (prefix) {
            case "AC", "SC" -> ORDER;
            case "OA", "OC" -> MARKETING;
            default -> throw invalidRecipient(prefix, false);
        };
    }

    private static IllegalArgumentException invalidRecipient(
            String prefix, boolean sellerRecipient) {
        return new IllegalArgumentException(
                prefix + " 不允許" + (sellerRecipient ? "商家" : "會員") + "收件人");
    }
}
