package com.dinogo.sysmsg.dto.response;

import com.dinogo.sysmsg.entity.MemberInbox;
import com.dinogo.sysmsg.entity.RecordStatus;
import com.dinogo.sysmsg.entity.SellerInbox;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * ============================================================
 * 收件匣列表 Response
 * ============================================================
 *
 * 同一個 DTO 同時支援：
 *
 * Member Inbox
 * Seller Inbox
 *
 * ============================================================
 *
 * Member：
 *
 * SYSTEM_INBOX
 * ORDER_INBOX
 * SELLER_INBOX
 *
 * ============================================================
 *
 * Seller：
 *
 * SYSTEM_NOTICE
 * NEW_ORDER
 * PROGRESS_ORDER
 * COMPLETED_ORDER
 */
public class MsgInboxResponse {

    /**
     * Record 主鍵。
     */
    private Integer recordId;

    /**
     * Send 主鍵。
     */
    private Integer sendId;

    /**
     * OA-001 / OC-001 / ...
     */
    private String msgFunction;

    /**
     * 發送者。
     */
    private Integer msgfromSellerId;

    /**
     * 會員收件人。
     *
     * 商家收件匣時為 null。
     */
    private Integer msgtoMemberId;

    /**
     * 商家收件人。
     *
     * 會員收件匣時為 null。
     */
    private Integer msgtoSellerId;

    private Integer orderId;

    private String orderStatus;

    /** sysmsg.send_order.order_no 的通知建立時快照。 */
    private String orderNo;

    /** sysmsg.send_order / send_disorder.total_amount 的通知建立時快照。 */
    private BigDecimal totalAmount;

    /** sysmsg.send_order / send_disorder.method_name 的通知建立時快照。 */
    private String paymentMethodName;

    /**
     * 訊息標籤。
     */
    private String msgLabel;

    /**
     * 標題。
     */
    private String sendTitle;

    /**
     * 內容。
     */
    private String sendContent;

    /**
     * UNREAD / READ / DELETE
     */
    private RecordStatus recordStatus;

    /**
     * Record 建立時間。
     */
    private LocalDateTime recordCreatedAt;

    /**
     * ------------------------------------------------------------
     * Member Inbox 分類
     * ------------------------------------------------------------
     *
     * 如果這筆 Record 是會員收件匣，
     * 由 Service 計算。
     *
     * 否則為 null。
     */
    private MemberInbox memberInbox;

    /**
     * ------------------------------------------------------------
     * Seller Inbox 分類
     * ------------------------------------------------------------
     *
     * 如果這筆 Record 是商家收件匣，
     * 由 Service 計算。
     *
     * 否則為 null。
     */
    private SellerInbox sellerInbox;


    public MsgInboxResponse() {
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
    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }
    public String getPaymentMethodName() { return paymentMethodName; }
    public void setPaymentMethodName(String paymentMethodName) { this.paymentMethodName = paymentMethodName; }

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

    public MemberInbox getMemberInbox() {
        return memberInbox;
    }

    public void setMemberInbox(MemberInbox memberInbox) {
        this.memberInbox = memberInbox;
    }

    public SellerInbox getSellerInbox() {
        return sellerInbox;
    }

    public void setSellerInbox(SellerInbox sellerInbox) {
        this.sellerInbox = sellerInbox;
    }
}
