package com.dinogo.sysmsg.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.times;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

//sysmsg-start，總共1次修改，第1次//
import com.dinogo.salesii.dto.OrderSysmsgResponse;
import com.dinogo.salesii.service.OrderSysmsgProviderService;
//sysmsg-end，總共1次修改，第1次//
import com.dinogo.sysmsg.dto.request.auto.OrderEventRequest;
import com.dinogo.sysmsg.dto.response.SendResponse;
import com.dinogo.sysmsg.entity.SendEntity;
import com.dinogo.sysmsg.repository.RecordRepository;
import com.dinogo.sysmsg.repository.SendRepository;
import com.dinogo.sysmsg.service.content.OrderMessageContentFactory;
import com.dinogo.sysmsg.service.impl.OrderMessageServiceImpl;
import com.dinogo.sysmsg.service.mapper.SendResponseMapper;

class OrderMessageServiceImplTest {
    private final OrderSysmsgProviderService orders = mock(OrderSysmsgProviderService.class);
    private final SendRepository sends = mock(SendRepository.class);
    private final RecordRepository records = mock(RecordRepository.class);
    private final RecordService recordService = mock(RecordService.class);
    private final TemplateNumService numbers = mock(TemplateNumService.class);
    private final SendResponseMapper mapper = mock(SendResponseMapper.class);
    private OrderMessageServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new OrderMessageServiceImpl(orders, sends, records, recordService, numbers,
                new OrderMessageContentFactory(), mapper);
        when(sends.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(mapper.toResponse(any())).thenReturn(new SendResponse());
    }

    @Test
    void selfCheckRepairsOnlyMissingSellerRecipient() {
        when(orders.getOrderForSysmsg(45)).thenReturn(order(45, "PAID"));
        when(records.existsByOrderIdAndOrderStatusAndMsgtoMemberId(45, "PAID", 7))
                .thenReturn(true);
        when(numbers.generateMsgFunction("AS")).thenReturn("AS-001");

        assertEquals(1, service.createOrderEventMessages(request(45)).size());
        verify(records, times(1)).existsByOrderIdAndOrderStatusAndMsgtoMemberId(45, "PAID", 7);
        verify(records, times(1)).existsByOrderIdAndOrderStatusAndMsgtoSellerId(45, "PAID", 5);
        verify(recordService).createOrderRecord(any(), org.mockito.ArgumentMatchers.isNull(),
                org.mockito.ArgumentMatchers.eq(5), org.mockito.ArgumentMatchers.eq(45),
                org.mockito.ArgumentMatchers.eq("PAID"));
    }

    @Test
    void deliveredStringCreatesBothMessagesWithoutOrderStatusEnumDependency() {
        when(orders.getOrderForSysmsg(50)).thenReturn(order(50, "DELIVERED"));
        when(numbers.generateMsgFunction("AC")).thenReturn("AC-001");
        when(numbers.generateMsgFunction("AS")).thenReturn("AS-001");

        List<SendResponse> result = service.createOrderEventMessages(request(50));

        assertEquals(2, result.size());
        verify(numbers).generateMsgFunction("AC");
        verify(numbers).generateMsgFunction("AS");
        verify(recordService).createOrderRecord(any(), org.mockito.ArgumentMatchers.eq(7),
                org.mockito.ArgumentMatchers.isNull(), org.mockito.ArgumentMatchers.eq(50),
                org.mockito.ArgumentMatchers.eq("DELIVERED"));
        verify(recordService).createOrderRecord(any(), org.mockito.ArgumentMatchers.isNull(),
                org.mockito.ArgumentMatchers.eq(5), org.mockito.ArgumentMatchers.eq(50),
                org.mockito.ArgumentMatchers.eq("DELIVERED"));
    }

    @Test
    void unsupportedProcessingSnapshotIsIgnoredWithoutWritingMessages() {
        when(orders.getOrderForSysmsg(60)).thenReturn(order(60, "PROCESSING"));

        assertEquals(List.of(), service.createOrderEventMessages(request(60)));
        verify(sends, never()).save(any());
        verify(recordService, never()).createOrderRecord(any(), any(), any(), any(), any());
    }

    @Test
    void cashOnDeliveryProcessingCreatesCustomerAndSellerPlacedMessages() {
        OrderSysmsgResponse snapshot = new OrderSysmsgResponse(
                62, "ORD-62", 7, 5, "PROCESSING", List.of(),
                new BigDecimal("1680.00"), 1, "任意顯示名稱", "CASH_ON_DELIVERY",
                LocalDateTime.now(), null, null);
        when(orders.getOrderForSysmsg(62)).thenReturn(snapshot);
        when(numbers.generateMsgFunction("AC")).thenReturn("AC-001");
        when(numbers.generateMsgFunction("AS")).thenReturn("AS-001");

        assertEquals(2, service.createOrderEventMessages(request(62)).size());
        verify(recordService).createOrderRecord(any(), org.mockito.ArgumentMatchers.eq(7),
                org.mockito.ArgumentMatchers.isNull(), org.mockito.ArgumentMatchers.eq(62),
                org.mockito.ArgumentMatchers.eq("PROCESSING"));
        verify(recordService).createOrderRecord(any(), org.mockito.ArgumentMatchers.isNull(),
                org.mockito.ArgumentMatchers.eq(5), org.mockito.ArgumentMatchers.eq(62),
                org.mockito.ArgumentMatchers.eq("PROCESSING"));
        verify(numbers).generateMsgFunction("AS");
    }

    @Test
    void fullyProcessedDuplicateEventIsIdempotentSuccess() {
        when(orders.getOrderForSysmsg(61)).thenReturn(order(61, "SHIPPED"));
        when(records.existsByOrderIdAndOrderStatusAndMsgtoMemberId(61, "SHIPPED", 7))
                .thenReturn(true);
        when(records.existsByOrderIdAndOrderStatusAndMsgtoSellerId(61, "SHIPPED", 5))
                .thenReturn(true);

        assertEquals(List.of(), service.createOrderEventMessages(request(61)));
        verify(sends, never()).save(any());
    }

    @Test
    void deliveredAndCompletedSnapshotCreatesDeliveredBeforeCompleted() {
        LocalDateTime now = LocalDateTime.now();
        OrderSysmsgResponse snapshot = new OrderSysmsgResponse(
                70, "ORD-70", 7, 5, "COMPLETED", List.of(),
                new BigDecimal("100.00"), 1, "信用卡", "CREDIT_CARD", now, null, null,
                "COMPLETED", "SUCCESS", now.minusDays(2), "DELIVERED",
                now.minusDays(1), now.minusHours(1), now);
        when(orders.getOrderForSysmsg(70)).thenReturn(snapshot);
        when(numbers.generateMsgFunction("AC"))
                .thenReturn("AC-001", "AC-002", "AC-003", "AC-004");
        when(numbers.generateMsgFunction("AS"))
                .thenReturn("AS-001", "AS-002", "AS-003", "AS-004");

        List<SendResponse> result = service.createOrderEventMessages(request(70));

        assertEquals(8, result.size());
        org.mockito.InOrder order = org.mockito.Mockito.inOrder(recordService);
        order.verify(recordService).createOrderRecord(any(), org.mockito.ArgumentMatchers.eq(7),
                org.mockito.ArgumentMatchers.isNull(), org.mockito.ArgumentMatchers.eq(70),
                org.mockito.ArgumentMatchers.eq("PAID"));
        order.verify(recordService).createOrderRecord(any(), org.mockito.ArgumentMatchers.isNull(),
                org.mockito.ArgumentMatchers.eq(5), org.mockito.ArgumentMatchers.eq(70),
                org.mockito.ArgumentMatchers.eq("PAID"));
        order.verify(recordService).createOrderRecord(any(), org.mockito.ArgumentMatchers.eq(7),
                org.mockito.ArgumentMatchers.isNull(), org.mockito.ArgumentMatchers.eq(70),
                org.mockito.ArgumentMatchers.eq("SHIPPED"));
        order.verify(recordService).createOrderRecord(any(), org.mockito.ArgumentMatchers.isNull(),
                org.mockito.ArgumentMatchers.eq(5), org.mockito.ArgumentMatchers.eq(70),
                org.mockito.ArgumentMatchers.eq("SHIPPED"));
        order.verify(recordService).createOrderRecord(any(), org.mockito.ArgumentMatchers.eq(7),
                org.mockito.ArgumentMatchers.isNull(), org.mockito.ArgumentMatchers.eq(70),
                org.mockito.ArgumentMatchers.eq("DELIVERED"));
        order.verify(recordService).createOrderRecord(any(), org.mockito.ArgumentMatchers.isNull(),
                org.mockito.ArgumentMatchers.eq(5), org.mockito.ArgumentMatchers.eq(70),
                org.mockito.ArgumentMatchers.eq("DELIVERED"));
        order.verify(recordService).createOrderRecord(any(), org.mockito.ArgumentMatchers.eq(7),
                org.mockito.ArgumentMatchers.isNull(), org.mockito.ArgumentMatchers.eq(70),
                org.mockito.ArgumentMatchers.eq("COMPLETED"));
        order.verify(recordService).createOrderRecord(any(), org.mockito.ArgumentMatchers.isNull(),
                org.mockito.ArgumentMatchers.eq(5), org.mockito.ArgumentMatchers.eq(70),
                org.mockito.ArgumentMatchers.eq("COMPLETED"));
        verify(recordService, times(8)).createOrderRecord(any(), any(), any(), any(), any());
    }

    private OrderEventRequest request(Integer orderId) {
        OrderEventRequest request = new OrderEventRequest();
        request.setOrderId(orderId);
        return request;
    }

    private OrderSysmsgResponse order(Integer orderId, String status) {
        return new OrderSysmsgResponse(orderId, "ORD-" + orderId, 7, 5, status, List.of(),
                new BigDecimal("100.00"), 1, "信用卡", LocalDateTime.now(), null, null);
    }
}
