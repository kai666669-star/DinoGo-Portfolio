package com.dinogo.sysmsg.dto.request.send;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 系統後台直接建立 SEND。
 *
 * 允許：
 *
 * OA
 * OC
 * OS
 *
 * 不建立 SAVE。
 *
 * send_status = SEND
 *
 * msgfrom_seller_id 固定：
 *
 *     seller_id = 1
 */
public class SysCreateRequest {

    /**
     * OA / OC / OS
     */
    @NotBlank
    @Pattern(regexp = "OA|OC|OS")
    private String msgType;

    @NotBlank
    @Size(max = 100)
    private String sendTitle;

    @NotBlank
    @Size(max = 1000)
    private String sendContent;

    /**
     * 如果是 OC：
     *     指定會員
     *
     * 如果是 OS：
     *     指定商家
     *
     * 如果是 OA：
     *     未指定 msgtoMemberId 時由 Service 取得所有 member / seller；
     *     有指定時只寄送給該會員。
     */
    private Integer msgtoMemberId;

    private Integer msgtoSellerId;

    public SysCreateRequest() {
    }

    public String getMsgType() {
        return msgType;
    }

    public void setMsgType(String msgType) {
        this.msgType = msgType;
    }

    public String getSendTitle() {
        return sendTitle;
    }

    public void setSendTitle(String sendTitle) {
        this.sendTitle = sendTitle;
    }

    public String getSendContent() {
        return sendContent;
    }

    public void setSendContent(String sendContent) {
        this.sendContent = sendContent;
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
