package com.dinogo.sysmsg.service.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.LinkedHashSet;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;
import com.dinogo.sysmsg.entity.*;
import com.dinogo.sysmsg.dto.external.OrderInfoResponse;
import com.dinogo.sysmsg.dto.request.auto.*;
import com.dinogo.sysmsg.dto.response.SendResponse;
//sysmsg-start，總共1次修改，第1次//
import com.dinogo.salesii.service.OrderSysmsgProviderService;
import com.dinogo.salesii.dto.OrderSysmsgResponse;
//sysmsg-end，總共1次修改，第1次//
import com.dinogo.sysmsg.repository.*;
import com.dinogo.sysmsg.service.*;
import com.dinogo.sysmsg.service.content.OrderMessageContentFactory;
import com.dinogo.sysmsg.service.mapper.SendResponseMapper;

@Service
public class OrderMessageServiceImpl implements OrderMessageService {
    private static final String COD_ORDER_CREATED = "PROCESSING";
    private static final Set<String> CUSTOMER = Set.of(COD_ORDER_CREATED,"PAID","SHIPPED","DELIVERED","COMPLETED");
    private static final Set<String> SELLER = Set.of(COD_ORDER_CREATED,"PAID","SHIPPED","DELIVERED","COMPLETED");
    private final OrderSysmsgProviderService orders;
    private final SendRepository sends;
    private final RecordRepository records;
    private final RecordService recordService;
    private final TemplateNumService numbers;
    private final OrderMessageContentFactory contentFactory;
    private final SendResponseMapper responseMapper;
    public OrderMessageServiceImpl(OrderSysmsgProviderService orders,
            SendRepository sends, RecordRepository records, RecordService recordService,
            TemplateNumService numbers, OrderMessageContentFactory contentFactory,
            SendResponseMapper responseMapper) {
        this.orders = orders;
        this.sends = sends;
        this.records = records;
        this.recordService = recordService;
        this.numbers = numbers;
        this.contentFactory = contentFactory;
        this.responseMapper = responseMapper;
    }

    /**
     * 訂單通知的最外層交易：同一事件建立的 Send、Record 與應建立的
     * RecordChannel 必須全部成功；任一階段失敗即回滾，不留下孤立 Send。
     */
    @Override
    @Transactional(propagation = Propagation.REQUIRED, rollbackFor = Exception.class)
    public List<SendResponse> createOrderEventMessages(OrderEventRequest r){
        return createOrderEventMessagesFromSnapshot(orders.getOrderForSysmsg(r.getOrderId()));
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED, rollbackFor = Exception.class)
    public List<SendResponse> createOrderEventMessagesFromSnapshot(OrderSysmsgResponse snapshot){
        OrderInfoResponse order=require(ModuleDataMapper.order(snapshot));
        List<SendResponse> result=new ArrayList<>();
        for (String status : notificationStatuses(order)) {
            if (COD_ORDER_CREATED.equals(status)) {
                boolean customerExists = records.existsByOrderIdAndOrderStatusAndMsgtoMemberId(
                        order.getOrderId(), status, order.getBuyerId());
                boolean sellerExists = records.existsByOrderIdAndOrderStatusAndMsgtoSellerId(
                        order.getOrderId(), status, order.getSellerId());
                if (!customerExists) result.add(normalFromOrder(order, status, true));
                if (!sellerExists) result.add(normalFromOrder(order, status, false));
                continue;
            }
            boolean customerExists = records.existsByOrderIdAndOrderStatusAndMsgtoMemberId(
                    order.getOrderId(), status, order.getBuyerId());
            boolean sellerExists = records.existsByOrderIdAndOrderStatusAndMsgtoSellerId(
                    order.getOrderId(), status, order.getSellerId());
            if (customerExists && sellerExists) {
                continue;
            }
            if ("CANCELLED".equals(status)) {
                if (!customerExists) result.add(cancelledFromOrder(order, status, true));
                if (!sellerExists) result.add(cancelledFromOrder(order, status, false));
            } else {
                if (!customerExists) result.add(normalFromOrder(order, status, true));
                if (!sellerExists) result.add(normalFromOrder(order, status, false));
            }
        }
        return result;
    }

    /** 依權威生命週期快照補齊尚未建立的里程碑，固定先 DELIVERED 再 COMPLETED。 */
    private List<String> notificationStatuses(OrderInfoResponse order) {
        LinkedHashSet<String> statuses = new LinkedHashSet<>();
        String orderStatus = normalize(order.getOrderStatus() == null
                ? order.getStatus() : order.getOrderStatus());
        if ("CANCELLED".equals(orderStatus)) {
            return List.of("CANCELLED");
        }
        if ("CASH_ON_DELIVERY".equals(order.getMethodCode())) {
            statuses.add(COD_ORDER_CREATED);
        }
        if ("SUCCESS".equalsIgnoreCase(order.getPaymentStatus()) && order.getPaidAt() != null) {
            statuses.add("PAID");
        }
        if (order.getShippedAt() != null) {
            statuses.add("SHIPPED");
        }
        if ("DELIVERED".equalsIgnoreCase(order.getShipmentStatus())) {
            statuses.add("DELIVERED");
        }
        if ("COMPLETED".equals(orderStatus)) {
            statuses.add("COMPLETED");
        }
        if (statuses.isEmpty()) {
            String projected = normalize(order.getStatus());
            if (CUSTOMER.contains(projected) && !COD_ORDER_CREATED.equals(projected)) {
                statuses.add(projected);
            }
        }
        return List.copyOf(statuses);
    }
    private SendResponse normalFromOrder(OrderInfoResponse o,String status,boolean customer){
        if(!(customer?CUSTOMER:SELLER).contains(status))throw new IllegalStateException((customer?"AC":"AS")+" 不支援訂單狀態："+status);
        String prefix=customer?"AC":"AS";
        String title=contentFactory.title(o, status, customer); //msg-title//
        String content=contentFactory.content(o,status,customer); //msg-content//
        SendOrderEntity send=new SendOrderEntity(1,numbers.generateMsgFunction(prefix),o.getOrderNo(),title,content,SendStatus.SEND,o.getOrderId(),o.getOrderNo(),o.getTotalAmount(),o.getPaymentMethodId(),o.getMethodName(),o.getCreatedAt(),status);
        sends.save(send);
        recordService.createOrderRecord(send.getSendId(), customer ? o.getBuyerId() : null,
                customer ? null : o.getSellerId(), o.getOrderId(), status);
        return responseMapper.toResponse(send);
    }
    private SendResponse cancelledFromOrder(OrderInfoResponse o,String status,boolean customer){
        if (!"CANCELLED".equals(status)) {
            throw new IllegalStateException("訂單不是 CANCELLED");
        }
        String prefix=customer?"AC":"AS";
        String title=contentFactory.cancelledTitle(o, customer); //msg-title//
        String content=contentFactory.cancelledContent(o, customer); //msg-content//
        SendDisorderEntity send=new SendDisorderEntity(1,numbers.generateMsgFunction(prefix),o.getOrderNo(),title,content,SendStatus.SEND,o.getOrderId(),o.getOrderNo(),o.getTotalAmount(),o.getPaymentMethodId(),o.getMethodName(),o.getCancelReason(),o.getCancelledAt(),status);
        sends.save(send);
        recordService.createOrderRecord(send.getSendId(), customer ? o.getBuyerId() : null,
                customer ? null : o.getSellerId(), o.getOrderId(), status);
        return responseMapper.toResponse(send);
    }
    private OrderInfoResponse require(OrderInfoResponse o){
        if(o==null||o.getOrderId()==null||o.getBuyerId()==null||o.getSellerId()==null||o.getOrderNo()==null
                ||o.getTotalAmount()==null)
            throw new IllegalStateException("Order API 回傳資料不完整");
        return o;
    }
    private String normalize(String status) {
        if (status == null) {
            throw new IllegalStateException("Order API 未回傳狀態");
        }
        return status.trim().toUpperCase();
    }
}
