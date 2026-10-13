package com.dinogo.sysmsg.entity;

import org.hibernate.annotations.Nationalized;
import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.Table;


@Entity
@PrimaryKeyJoinColumn(name = "send_order_id")
@Table(
    name = "send_order",
    schema = "sysmsg"
)
public class SendOrderEntity extends SendEntity {

    /**
     * ------------------------------------------------------------
     * order_id
     * ------------------------------------------------------------
     *
     * 來源：
     *     order 模組 API
     *
     * sysmsg 不自行維護訂單。
     *
     * 假設 order 模組提供：
     *
     *     OrderInfoResponse
     *
     * 後續：
     *
     *     OrderMessageService
     *         ↓
     *     OrderInfoResponse
     *         ↓
     *     SendOrderEntity
     */
    @Column(
        name = "order_id",
        nullable = false
    )
    private Integer orderId;

    @Nationalized
    @Column(name = "order_no", length = 30, nullable = false)
    private String orderNo;

    /**
     * 訂單金額。
     *
     * 來源：
     *     order 模組 API
     */
    @Column(
        name = "total_amount",
        precision = 12,
        scale = 2,
        nullable = false
    )
    private BigDecimal totalAmount;

    /**
     * 付款方式 ID。
     *
     * 來源：
     *     order 模組 API
     */
    @Column(
        name = "payment_method_id"
    )
    private Integer paymentMethodId;

    /**
     * 付款方式名稱。
     *
     * 來源：
     *     order 模組 API
     */
    @Nationalized
    @Column(
        name = "method_name",
        length = 50
    )
    private String methodName;

    /**
     * 訂單建立時間。
     *
     * 來源：
     *     order 模組 API
     *
     * 不由 sysmsg 自行產生。
     */
    @Column(
        name = "created_at",
        nullable = false
    )
    private LocalDateTime createdAt;

    @Nationalized
    @Column(name = "status", length = 30, nullable = false)
    private String status;


    // ============================================================
    // Constructor
    // ============================================================

    /**
     * JPA Constructor。
     *
     * Lombok 對應：
     *
     *     @NoArgsConstructor
     */
    protected SendOrderEntity() {
        super();
    }

    /**
     * 建立 PAID 等訂單訊息使用。
     *
     * 父 Entity 資料由 super 傳入。
     */
    public SendOrderEntity(
        Integer msgfromSellerId,
        String msgFunction,
        String msgLabel,
        String sendTitle,
        String sendContent,
        SendStatus sendStatus,
        Integer orderId,
        String orderNo,
        BigDecimal totalAmount,
        Integer paymentMethodId,
        String methodName,
        LocalDateTime createdAt,
        String status
    ) {

        super(
            msgfromSellerId,
            msgFunction,
            msgLabel,
            sendTitle,
            sendContent,
            sendStatus
        );

        this.orderId = orderId;
        this.orderNo = orderNo;
        this.totalAmount = totalAmount;
        this.paymentMethodId = paymentMethodId;
        this.methodName = methodName;
        this.createdAt = createdAt;
        this.status = status;
    }


    // ============================================================
    // Getter / Setter
    // ============================================================

    /*
     * Lombok：
     *
     *     @Getter
     *     @Setter
     */

    public Integer getOrderId() {
        return orderId;
    }

    public void setOrderId(Integer orderId) {
        this.orderId = orderId;
    }

    public String getOrderNo() { return orderNo; }
    public void setOrderNo(String orderNo) { this.orderNo = orderNo; }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public Integer getPaymentMethodId() {
        return paymentMethodId;
    }

    public void setPaymentMethodId(Integer paymentMethodId) {
        this.paymentMethodId = paymentMethodId;
    }

    public String getMethodName() {
        return methodName;
    }

    public void setMethodName(String methodName) {
        this.methodName = methodName;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
