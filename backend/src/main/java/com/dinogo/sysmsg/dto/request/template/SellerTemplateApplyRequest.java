package com.dinogo.sysmsg.dto.request.template;

import jakarta.validation.constraints.NotNull;

/**
 * 商家套用 SC 範本。
 *
 * 必須驗證 order。
 *
 * 流程：
 *
 * seller_id
 *     ↓
 * Order API
 *     ↓
 * order_no
 *     ↓
 * OrderInfoResponse
 *     ↓
 * buyer_id
 *     ↓
 * msgto_member_id
 *
 * msgfrom_seller_id：
 *     使用登入商家的 seller_id
 */
public class SellerTemplateApplyRequest {

    /**
     * SAVE 範本 send_id。
     */
    @NotNull
    private Integer sendId;

    /**
     * 商家選擇的訂單。
     *
     * 前端可以使用 orderNo，
     * 後端再向 order 模組 GET。
     */
    @NotNull
    private Integer orderId;

    /*
     * 不接受：
     *
     * seller_id
     * buyer_id
     *
     * 由後端登入資訊 + Order API 決定。
     */

    public SellerTemplateApplyRequest() {
    }

    public Integer getSendId() {
        return sendId;
    }

    public void setSendId(Integer sendId) {
        this.sendId = sendId;
    }

    public Integer getOrderId() {
        return orderId;
    }

    public void setOrderId(Integer orderId) {
        this.orderId = orderId;
    }
}
