package com.dinogo.integration;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.mock;

import java.lang.reflect.Method;

import org.junit.jupiter.api.Test;
import org.springframework.transaction.event.TransactionalEventListener;

import com.dinogo.salesii.event.OrderStatusChangedEvent;
import com.dinogo.sysmsg.service.OrderMessageService;

class SysmsgOrderStatusChangedListenerTest {

    @Test
    void handlesBridgeEventEvenWhenPublishedAfterTransactionCompletion() throws Exception {
        OrderMessageService messages = mock(OrderMessageService.class);
        SysmsgOrderStatusChangedListener listener = new SysmsgOrderStatusChangedListener(messages);

        listener.onOrderStatusChanged(new OrderStatusChangedEvent(62));

        verify(messages).createOrderEventMessages(argThat(request -> request.getOrderId().equals(62)));
        Method method = SysmsgOrderStatusChangedListener.class
                .getMethod("onOrderStatusChanged", OrderStatusChangedEvent.class);
        TransactionalEventListener annotation = method.getAnnotation(TransactionalEventListener.class);
        assertTrue(annotation.fallbackExecution(),
                "交易外發布的 Sales bridge 事件不得被 TransactionalEventListener 丟棄");
    }
}
