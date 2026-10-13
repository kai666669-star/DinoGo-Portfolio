package com.dinogo.integration;

//sysmsg-start，總共1次修改，第1次//
import org.springframework.stereotype.Component;
import org.springframework.scheduling.annotation.Async;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.dinogo.salesii.event.OrderStatusChangedEvent;
import com.dinogo.sysmsg.dto.request.auto.OrderEventRequest;
import com.dinogo.sysmsg.service.OrderMessageService;

/**
 * 單體部署的 Sales → sysmsg 接線。
 * Sales 交易提交後才非同步執行；sysmsg 另開 REQUIRED 交易並依 orderId 回查權威快照。
 */
@Component
public class SysmsgOrderStatusChangedListener {
    private final OrderMessageService orderMessages;

    public SysmsgOrderStatusChangedListener(OrderMessageService orderMessages) {
        this.orderMessages = orderMessages;
    }

    @Async
    @TransactionalEventListener(
            phase = TransactionPhase.AFTER_COMMIT,
            fallbackExecution = true)
    public void onOrderStatusChanged(OrderStatusChangedEvent event) {
        OrderEventRequest request = new OrderEventRequest();
        request.setOrderId(event.orderId());
        orderMessages.createOrderEventMessages(request);
    }
}
//sysmsg-end，總共1次修改，第1次//
