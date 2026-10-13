package com.dinogo.sysmsg.service;

import com.dinogo.member.dto.MemberSysmsgResponse;
//sysmsg-start，總共1次修改，第1次//
import com.dinogo.salesii.dto.OrderSysmsgResponse;
//sysmsg-end，總共1次修改，第1次//
import com.dinogo.seller.dto.SellerSysmsgResponse;
import com.dinogo.sysmsg.dto.external.MemberAuthResponse;
import com.dinogo.sysmsg.dto.external.OrderInfoResponse;
import com.dinogo.sysmsg.dto.external.SellerInfoResponse;

/** 將其他模組的查詢 DTO 轉成 sysmsg 內部使用的資料格式。 */
public final class ModuleDataMapper {
    private ModuleDataMapper() {
    }

    public static MemberAuthResponse member(MemberSysmsgResponse source) {
        MemberAuthResponse target = new MemberAuthResponse();
        target.setMemberId(source.memberId());
        target.setSellerId(source.sellerId());
        target.setAuthenticated(source.authenticated());
        target.setEmail(source.email());
        target.setRole(source.role());
        target.setRoleIds(source.roleIds());
        target.setEmailOrderNotifications(source.emailOrderNotifications());
        target.setEmailMarketingNotifications(source.emailMarketingNotifications());
        return target;
    }

    public static SellerInfoResponse seller(SellerSysmsgResponse source) {
        SellerInfoResponse target = new SellerInfoResponse();
        target.setSellerId(source.sellerId());
        target.setMemberId(source.memberId());
        target.setSellerName(source.sellerName());
        target.setActive(source.active());
        target.setEmail(source.email());
        target.setEmailOrderNotifications(source.emailOrderNotifications());
        target.setEmailMarketingNotifications(source.emailMarketingNotifications());
        return target;
    }

    public static OrderInfoResponse order(OrderSysmsgResponse source) {
        OrderInfoResponse target = new OrderInfoResponse();
        target.setOrderId(source.orderId());
        target.setOrderNo(source.orderNo());
        target.setBuyerId(source.buyerId());
        target.setSellerId(source.sellerId());
        target.setTotalAmount(source.totalAmount());
        target.setPaymentMethodId(source.paymentMethodId());
        target.setMethodName(source.methodName());
        target.setMethodCode(source.methodCode());
        target.setCreatedAt(source.createdAt());
        target.setStatus(source.status());
        target.setCancelReason(source.cancelReason());
        target.setCancelledAt(source.cancelledAt());
        target.setOrderStatus(source.orderStatus());
        target.setPaymentStatus(source.paymentStatus());
        target.setPaidAt(source.paidAt());
        target.setShipmentStatus(source.shipmentStatus());
        target.setShippedAt(source.shippedAt());
        target.setDeliveredAt(source.deliveredAt());
        target.setCompletedAt(source.completedAt());
        return target;
    }
}
