package com.dinogo.sysmsg.dto.external;

/**External跨模組 API 的資料邊界
 * ============================================================
 * Seller 模組 API
 * ============================================================
 *
 * 目前未提供獨立 Seller API；由帶 Bearer Token 的
 * GET /api/member/profile 取得登入會員對應的 sellerId。
 *
 * ============================================================
 *
 * sysmsg 用途：
 *
 * 1. validateSeller()
 * 2. 確認 seller_id 是否存在
 * 3. 確認登入者是否有該 seller_id
 */
public class SellerInfoResponse {

    private Integer sellerId;

    private Integer memberId;

    private String sellerName;

    private boolean active;

    /** Seller 模組優先提供的 Email；缺失時才以 memberId 向 Member 模組取得。 */
    private String email;

    /** 訂單偏好三態：true=寄送、false=不寄送、null=無法驗證且預設寄送。 */
    /** Seller／Member Provider 提供的訂單 Email 三態偏好。 */
    private Boolean emailOrderNotifications;

    /** 行銷偏好三態：true=寄送、false/null=不寄送。 */
    /** Seller／Member Provider 提供的行銷 Email 三態偏好。 */
    private Boolean emailMarketingNotifications;

    public SellerInfoResponse() {
    }

    public Integer getSellerId() {
        return sellerId;
    }

    public void setSellerId(Integer sellerId) {
        this.sellerId = sellerId;
    }

    public Integer getMemberId() {
        return memberId;
    }

    public void setMemberId(Integer memberId) {
        this.memberId = memberId;
    }

    public String getSellerName() {
        return sellerName;
    }

    public void setSellerName(String sellerName) {
        this.sellerName = sellerName;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public Boolean getEmailOrderNotifications() { return emailOrderNotifications; }
    public void setEmailOrderNotifications(Boolean value) { this.emailOrderNotifications = value; }
    public Boolean getEmailMarketingNotifications() { return emailMarketingNotifications; }
    public void setEmailMarketingNotifications(Boolean value) { this.emailMarketingNotifications = value; }
}
