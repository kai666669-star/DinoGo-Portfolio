package com.dinogo.sysmsg.dto.request.template;

import jakarta.validation.constraints.NotNull;

/**
 * 系統範本套用。
 *
 * OA / OC / OS
 *
 * 原 SAVE：
 *     保留
 *
 * 新增：
 *     新 send_id
 *     send_status = SEND
 *
 * 然後建立 Record。
 */
public class SysTemplateApplyRequest {

    /**
     * 要套用的 SAVE send_id。
     */
    @NotNull
    private Integer sendId;

    /**
     * ------------------------------------------------------------
     * OA / OC / OS 收件人
     * ------------------------------------------------------------
     *
     * OA：
     *     系統廣播
     *
     *     所有 member
     *     +
     *     所有 seller
     *
     *     每一筆 Record 只能有一個收件欄位。
     *
     * OC：
     *     member
     *
     * OS：
     *     seller
     *
     * 實際 OA 的全部會員 / 商家清單，
     * 由 Member API / Seller API 取得。
     */
    private Integer msgtoMemberId;

    private Integer msgtoSellerId;

    /*
     * msgfrom_seller_id 不讓前端輸入。
     *
     * 系統後台固定：
     *
     * seller_id = 1
     */

    public SysTemplateApplyRequest() {
    }

    public Integer getSendId() {
        return sendId;
    }

    public void setSendId(Integer sendId) {
        this.sendId = sendId;
    }

    public Integer getMsgtoMemberId() {
        return msgtoMemberId;
    }

    public void setMsgtoMemberId(Integer msgtoMemberId) {
        this.msgtoMemberId = msgtoMemberId;
    }

    public Integer getMsgtoSellerId() {
        return msgtoSellerId;
    }

    public void setMsgtoSellerId(Integer msgtoSellerId) {
        this.msgtoSellerId = msgtoSellerId;
    }
}
