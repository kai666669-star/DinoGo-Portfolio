package com.dinogo.member.dto;

import java.util.List;

/**
 * Member data supplied to the system-message and review modules.
 */
public record MemberSysmsgResponse(
        Integer memberId,
        Integer sellerId,
        boolean authenticated,
        String email,
        String role,
        List<Integer> roleIds,
        Boolean emailOrderNotifications,
        Boolean emailMarketingNotifications) {
}
