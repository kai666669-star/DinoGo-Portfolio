package com.dinogo.sysmsg.dto.response;

import com.dinogo.sysmsg.entity.SendStatus;

import java.time.LocalDateTime;

/**
 * 實際訊息建立結果。
 *
 * SEND 專用。
 */
public class SendResponse {

    private Integer sendId;

    private String msgFunction;

    private Integer msgfromSellerId;

    private String msgLabel;

    private String sendTitle;

    private String sendContent;

    /** SC 實際發送時的訂單編號快照；其他訊息為 null。 */
    private String orderNo;

    private SendStatus sendStatus;

    private LocalDateTime sendUpdAt;

    public SendResponse() {
    }

    public Integer getSendId() {
        return sendId;
    }

    public void setSendId(Integer sendId) {
        this.sendId = sendId;
    }

    public String getMsgFunction() {
        return msgFunction;
    }

    public void setMsgFunction(String msgFunction) {
        this.msgFunction = msgFunction;
    }

    public Integer getMsgfromSellerId() {
        return msgfromSellerId;
    }

    public void setMsgfromSellerId(Integer msgfromSellerId) {
        this.msgfromSellerId = msgfromSellerId;
    }

    public String getMsgLabel() {
        return msgLabel;
    }

    public void setMsgLabel(String msgLabel) {
        this.msgLabel = msgLabel;
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

    public String getOrderNo() {
        return orderNo;
    }

    public void setOrderNo(String orderNo) {
        this.orderNo = orderNo;
    }

    public SendStatus getSendStatus() {
        return sendStatus;
    }

    public void setSendStatus(SendStatus sendStatus) {
        this.sendStatus = sendStatus;
    }

    public LocalDateTime getSendUpdAt() {
        return sendUpdAt;
    }

    public void setSendUpdAt(LocalDateTime sendUpdAt) {
        this.sendUpdAt = sendUpdAt;
    }
}
