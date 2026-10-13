package com.dinogo.sysmsg.dto.request.auto;

import jakarta.validation.constraints.NotNull;

/** order 模組的統一狀態事件；sysmsg 仍會用 orderId 回查權威狀態。 */
public class OrderEventRequest {
    @NotNull
    private Integer orderId;
    public OrderEventRequest() {}
    public Integer getOrderId() { return orderId; }
    public void setOrderId(Integer orderId) { this.orderId = orderId; }
}
