package com.dinogo.sysmsg.service.mapper;

import java.math.BigDecimal;

import org.springframework.stereotype.Component;

import com.dinogo.sysmsg.dto.response.MsgInboxResponse;
import com.dinogo.sysmsg.dto.response.RecordResponse;
import com.dinogo.sysmsg.entity.RecordEntity;
import com.dinogo.sysmsg.entity.SendDisorderEntity;
import com.dinogo.sysmsg.entity.SendEntity;
import com.dinogo.sysmsg.entity.SendOrderEntity;

/** 集中處理 Record 與 Inbox DTO 映射。 */
@Component
public class RecordResponseMapper {
    public RecordResponse toResponse(RecordEntity record) {
        SendEntity send = record.getSend();
        RecordResponse target = new RecordResponse();
        copyCommon(record, send, target);
        return target;
    }

    public MsgInboxResponse toInboxResponse(RecordEntity record) {
        SendEntity send = record.getSend();
        MsgInboxResponse target = new MsgInboxResponse();
        target.setRecordId(record.getRecordId());
        target.setSendId(send.getSendId());
        target.setMsgFunction(record.getMsgFunction());
        target.setMsgfromSellerId(record.getMsgfromSellerId());
        target.setMsgtoMemberId(record.getMsgtoMemberId());
        target.setMsgtoSellerId(record.getMsgtoSellerId());
        target.setOrderId(record.getOrderId());
        target.setOrderStatus(record.getOrderStatus());
        target.setOrderNo(orderNo(send));
        target.setTotalAmount(totalAmount(send));
        target.setPaymentMethodName(paymentMethodName(send));
        target.setMsgLabel(send.getMsgLabel());
        target.setSendTitle(send.getSendTitle());
        target.setSendContent(send.getSendContent());
        target.setRecordStatus(record.getRecordStatus());
        target.setRecordCreatedAt(record.getRecordCreatedAt());
        target.setMemberInbox(record.getMemberInbox());
        target.setSellerInbox(record.getSellerInbox());
        return target;
    }

    private void copyCommon(RecordEntity record, SendEntity send, RecordResponse target) {
        target.setRecordId(record.getRecordId());
        target.setSendId(send.getSendId());
        target.setMsgFunction(record.getMsgFunction());
        target.setMsgfromSellerId(record.getMsgfromSellerId());
        target.setMsgtoMemberId(record.getMsgtoMemberId());
        target.setMsgtoSellerId(record.getMsgtoSellerId());
        target.setOrderId(record.getOrderId());
        target.setOrderStatus(record.getOrderStatus());
        target.setOrderNo(orderNo(send));
        target.setMsgLabel(send.getMsgLabel());
        target.setSendTitle(send.getSendTitle());
        target.setSendContent(send.getSendContent());
        target.setRecordStatus(record.getRecordStatus());
        target.setRecordCreatedAt(record.getRecordCreatedAt());
    }

    private String orderNo(SendEntity send) {
        if (send instanceof SendOrderEntity orderSend) return orderSend.getOrderNo();
        if (send instanceof SendDisorderEntity disorderSend) return disorderSend.getOrderNo();
        return null;
    }

    private BigDecimal totalAmount(SendEntity send) {
        if (send instanceof SendOrderEntity orderSend) return orderSend.getTotalAmount();
        if (send instanceof SendDisorderEntity disorderSend) return disorderSend.getTotalAmount();
        return null;
    }

    private String paymentMethodName(SendEntity send) {
        if (send instanceof SendOrderEntity orderSend) return orderSend.getMethodName();
        if (send instanceof SendDisorderEntity disorderSend) return disorderSend.getMethodName();
        return null;
    }
}
