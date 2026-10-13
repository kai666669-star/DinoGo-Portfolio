package com.dinogo.seller.dto;

//rev+msg-start，總共1次修改，第1次//
/**
 * review 使用 sellerId/memberId/active 驗證商家；sysmsg 另使用名稱、信箱及通知偏好。
 * Email 訂單偏好為 null 時，sysmsg 採預設同意；行銷偏好仍須明確為 true。
 */
public record SellerSysmsgResponse(
        Integer sellerId,
        Integer memberId,
        boolean active,
        String sellerName,
        String email,
        /** true=寄送、false=不寄送、null=無法驗證且訂單通知預設寄送。 */
        Boolean emailOrderNotifications,
        /** true=寄送；false 或 null 均不寄送行銷通知。 */
        Boolean emailMarketingNotifications) {
    public SellerSysmsgResponse(Integer sellerId, Integer memberId, boolean active) {
        this(sellerId, memberId, active, null, null, null, null);
    }
}
//rev+msg-end，總共1次修改，第1次//
