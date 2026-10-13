package com.dinogo.sysmsg.dto.response;

/** 商家訊息中心三個分類與全部訊息的未讀數。 */
public record SellerUnreadCountsResponse(
        long all,
        long systemNotice,
        long newOrder,
        long cancelledOrder) {
}
