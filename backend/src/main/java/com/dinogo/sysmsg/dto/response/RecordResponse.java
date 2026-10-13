package com.dinogo.sysmsg.dto.response;

import com.dinogo.sysmsg.entity.RecordStatus;

import java.time.LocalDateTime;

/**
 * 單筆 Record。
 */
public class RecordResponse {

    private Integer recordId;

    private Integer sendId;

    private String msgFunction;

    //msg-訊息msgfrom// SC 訊息以 msgfromSellerId 查詢並回傳商店名稱；其他類型由前端顯示系統來源。
    private Integer msgfromSellerId;

    private String storeName;

    private Integer msgtoMemberId;

    private Integer msgtoSellerId;

    private Integer orderId;

    private String orderStatus;

    /** sysmsg.send_order.order_no 的通知建立時快照。 */
    private String orderNo;

    /** 訊息詳情由關聯的 SEND 唯讀取得，不由前端自行提供。 */
    private String msgLabel;

    //msg-訊息title//
    private String sendTitle;

    //msg-訊息content//
    private String sendContent;

    private RecordStatus recordStatus;

    private LocalDateTime recordCreatedAt;

    public RecordResponse() {
    }

    public Integer getRecordId() {
        return recordId;
    }

    public void setRecordId(Integer recordId) {
        this.recordId = recordId;
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

    public String getStoreName() { return storeName; }
    public void setStoreName(String storeName) { this.storeName = storeName; }

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

    public Integer getOrderId() { return orderId; }
    public void setOrderId(Integer orderId) { this.orderId = orderId; }
    public String getOrderStatus() { return orderStatus; }
    public void setOrderStatus(String orderStatus) { this.orderStatus = orderStatus; }
    public String getOrderNo() { return orderNo; }
    public void setOrderNo(String orderNo) { this.orderNo = orderNo; }
    public String getMsgLabel() { return msgLabel; }
    public void setMsgLabel(String msgLabel) { this.msgLabel = msgLabel; }
    public String getSendTitle() { return sendTitle; }
    public void setSendTitle(String sendTitle) { this.sendTitle = sendTitle; }
    public String getSendContent() { return sendContent; }
    public void setSendContent(String sendContent) { this.sendContent = sendContent; }

    public RecordStatus getRecordStatus() {
        return recordStatus;
    }

    public void setRecordStatus(RecordStatus recordStatus) {
        this.recordStatus = recordStatus;
    }

    public LocalDateTime getRecordCreatedAt() {
        return recordCreatedAt;
    }

    public void setRecordCreatedAt(LocalDateTime recordCreatedAt) {
        this.recordCreatedAt = recordCreatedAt;
    }
}
