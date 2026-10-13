package com.dinogo.sysmsg.dto.external;

import java.util.List;

/**
 * sysmsg 內部使用的會員資料邊界。
 *
 * <p>單體部署時由 {@code MemberSysmsgProviderService} 取得資料，再由
 * {@code ModuleDataMapper} 轉入本型別；不呼叫 localhost HTTP API，也不直接查詢
 * member schema。用途是收件者有效性、角色路由、登入身分與 Email 偏好判斷。</p>
 */
public class MemberAuthResponse {

    private Integer memberId;

    private Integer sellerId;

    private boolean authenticated;

    /** Member 模組依 memberId 提供的登入 Email；不是訂閱偏好欄位。 */
    private String email;

    private String role;

    /** 角色 ID 以 member.MemberRole 關聯 member.Role 的結果為準；3 為管理員。 */
    private List<Integer> roleIds = List.of();

    /** 訂單偏好三態：true=寄送、false=不寄送、null=無法驗證且預設寄送。 */
    private Boolean emailOrderNotifications;

    /** 行銷偏好三態：true=寄送、false/null=不寄送。 */
    private Boolean emailMarketingNotifications;


    public MemberAuthResponse() {
    }

    public Integer getMemberId() {
        return memberId;
    }

    public void setMemberId(Integer memberId) {
        this.memberId = memberId;
    }

    public Integer getSellerId() {
        return sellerId;
    }

    public void setSellerId(Integer sellerId) {
        this.sellerId = sellerId;
    }

    public boolean isAuthenticated() {
        return authenticated;
    }

    public void setAuthenticated(boolean authenticated) {
        this.authenticated = authenticated;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public List<Integer> getRoleIds() {
        return roleIds;
    }

    public void setRoleIds(List<Integer> roleIds) {
        this.roleIds = roleIds == null ? List.of() : List.copyOf(roleIds);
    }

    public Boolean getEmailOrderNotifications() { return emailOrderNotifications; }
    public void setEmailOrderNotifications(Boolean value) { this.emailOrderNotifications = value; }
    public Boolean getEmailMarketingNotifications() { return emailMarketingNotifications; }
    public void setEmailMarketingNotifications(Boolean value) { this.emailMarketingNotifications = value; }
}
