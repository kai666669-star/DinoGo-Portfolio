package com.dinogo.sysmsg.service.content;

import java.time.format.DateTimeFormatter;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.dinogo.sysmsg.dto.external.OrderInfoResponse;

/** 只負責訂單通知的標題與內容，不負責狀態驗證或儲存。 */
@Component
public class OrderMessageContentFactory {
    private static final DateTimeFormatter ORDER_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm");
    private static final Map<String, String> CUSTOMER_TITLES = Map.of(
            "PROCESSING", "訂單下單成功",
            "PAID", "訂單付款成功",
            "SHIPPED", "訂單已出貨",
            "DELIVERED", "訂單已到貨",
            "COMPLETED", "訂單已完成");
    private static final Map<String, String> SELLER_TITLES = Map.of(
            "PROCESSING", "收到新訂單",
            "PAID", "收到新訂單",
            "SHIPPED", "訂單已出貨",
            "DELIVERED", "訂單已送達",
            "COMPLETED", "訂單完成與收款");

    public String title(String status, boolean customer) {
        return (customer ? CUSTOMER_TITLES : SELLER_TITLES).get(status);
    }

    public String title(OrderInfoResponse order, String status, boolean customer) {
        return title(status, customer);
    }

    public String content(OrderInfoResponse order, String status, boolean customer) {
        return switch (status) {
            case "PROCESSING" -> customer
                    ? cashOnDeliveryOrderCreatedContent(order)
                    : cashOnDeliverySellerContent(order);
            case "PAID" -> customer
                    ? ("CREDIT_CARD".equals(order.getMethodCode())
                            ? creditCardPaidCustomerContent(order)
                            : "訂單 " + order.getOrderNo() + " 已付款成功。")
                    : "收到訂單 " + order.getOrderNo() + "，請開始處理。";
            case "SHIPPED" -> customer ? shippedCustomerContent(order)
                    : "訂單 " + order.getOrderNo() + " 已出貨。";
            case "DELIVERED" -> customer ? deliveredCustomerContent(order)
                    : "訂單 " + order.getOrderNo() + " 已送達。";
            case "COMPLETED" -> customer
                    ? completedCustomerContent(order)
                    : "訂單 " + order.getOrderNo() + " 已完成，收入金額為 " + order.getTotalAmount() + "。";
            default -> throw new IllegalStateException("無法產生訂單通知內容：" + status);
        };
    }

    public String cancelledContent(OrderInfoResponse order) {
        String reason = order.getCancelReason() == null ? "未提供原因" : order.getCancelReason();
        return "訂單 " + order.getOrderNo() + " 已取消，原因：" + reason + "。";
    }

    public String cancelledTitle(OrderInfoResponse order, boolean customer) {
        return "訂單已取消";
    }

    public String cancelledContent(OrderInfoResponse order, boolean customer) {
        if (!customer) return cancelledContent(order);
        if (order.getOrderId() == null || order.getBuyerId() == null || order.getCreatedAt() == null) {
            throw new IllegalStateException("訂單缺少 orderId、buyerId 或 createdAt，無法產生取消通知");
        }
        String reason = order.getCancelReason() == null ? "未提供原因" : order.getCancelReason();
        return "親愛的 會員-" + order.getBuyerId() + " 您好:\n"
                + "   感謝您今日光臨！您於 " + order.getCreatedAt().format(ORDER_TIME_FORMATTER) + " 下單之商品已取消，\n"
                + "   您的訂單編號為 /member/orders/" + order.getOrderId() + "，\n"
                + "   取消原因：\n"
                + "       " + reason + "\n"
                + "   歡迎您來信說明，並再次訂購，您的意見是我們最重要的支持！";
    }

    private String shippedCustomerContent(OrderInfoResponse order) {
        if (order.getOrderId() == null || order.getBuyerId() == null || order.getCreatedAt() == null) {
            throw new IllegalStateException("訂單缺少 orderId、buyerId 或 createdAt，無法產生出貨通知");
        }
        String orderPath = "/member/orders/" + order.getOrderId();
        return "親愛的 會員-" + order.getBuyerId() + " 您好:\n"
                + "   感謝您的訂購！您於 " + order.getCreatedAt().format(ORDER_TIME_FORMATTER) + " 下單之商品已出貨，\n"
                + "   您的訂單編號為 " + orderPath + "，\n"
                + "   隨時點此查詢進度：" + orderPath + "，\n"
                + "   請於包裹送達後，7日內取貨，感謝您的惠顧！";
    }

    private String creditCardPaidCustomerContent(OrderInfoResponse order) {
        if (order.getOrderId() == null || order.getBuyerId() == null || order.getCreatedAt() == null) {
            throw new IllegalStateException("訂單缺少 orderId、buyerId 或 createdAt，無法產生信用卡付款通知");
        }
        String orderPath = "/member/orders/" + order.getOrderId();
        return "親愛的 會員-" + order.getBuyerId() + " 您好:\n"
                + "   感謝您的訂購！您於 " + order.getCreatedAt().format(ORDER_TIME_FORMATTER) + " 下單之商品已完成下單，\n"
                + "   我們已收到您的信用卡款項，請核對為本人付款，\n"
                + "   您的訂單編號為 " + orderPath + "，\n"
                + "   隨時點此查詢進度：" + orderPath + "，\n"
                + "   請於貨物送達後，7日內取貨，感謝您的惠顧！";
    }

    private String completedCustomerContent(OrderInfoResponse order) {
        if (order.getOrderId() == null || order.getBuyerId() == null || order.getCreatedAt() == null) {
            throw new IllegalStateException("訂單缺少 orderId、buyerId 或 createdAt，無法產生完成通知");
        }
        return "親愛的 會員-" + order.getBuyerId() + " 您好:\n"
                + "   感謝您的訂購！您於 " + order.getCreatedAt().format(ORDER_TIME_FORMATTER) + " 下單之商品已完成，\n"
                + "   您的訂單編號為 /member/orders/" + order.getOrderId() + "，\n"
                + "   歡迎您留下評價，感謝您的惠顧！";
    }

    private String deliveredCustomerContent(OrderInfoResponse order) {
        if (order.getOrderId() == null || order.getBuyerId() == null || order.getCreatedAt() == null) {
            throw new IllegalStateException("訂單缺少 orderId、buyerId 或 createdAt，無法產生到貨通知");
        }
        String orderPath = "/member/orders/" + order.getOrderId();
        return "親愛的 會員-" + order.getBuyerId() + " 您好:\n"
                + "   感謝您的訂購！您於 " + order.getCreatedAt().format(ORDER_TIME_FORMATTER) + " 下單之商品已到貨，\n"
                + "   您的訂單編號為 " + orderPath + "，\n"
                + "   請於7日內取貨，並於" + orderPath + "按下\"完成訂單\"，感謝您的惠顧！";
    }

    private String cashOnDeliveryOrderCreatedContent(OrderInfoResponse order) {
        if (order.getOrderId() == null || order.getBuyerId() == null
                || order.getCreatedAt() == null || order.getTotalAmount() == null) {
            throw new IllegalStateException("訂單缺少 orderId、buyerId、createdAt 或 totalAmount，無法產生貨到付款下單通知");
        }
        String orderPath = "/member/orders/" + order.getOrderId();
        return "親愛的 會員-" + order.getBuyerId() + " 您好:\n"
                + "   感謝您的訂購！您於 " + order.getCreatedAt().format(ORDER_TIME_FORMATTER) + " 下單之商品已完成下單，\n"
                + "   我們已收到您的訂單，請於到貨後現金付款新台幣共" + order.getTotalAmount() + "元，\n"
                + "   您的訂單編號為 " + orderPath + "，\n"
                + "   隨時點此查詢進度：" + orderPath + "，\n"
                + "   請於貨物送達後，7日內取貨，感謝您的惠顧！";
    }

    private String cashOnDeliverySellerContent(OrderInfoResponse order) {
        if (order.getOrderNo() == null || order.getTotalAmount() == null) {
            throw new IllegalStateException("訂單缺少 orderNo 或 totalAmount，無法產生商家貨到付款通知");
        }
        return "收到貨到付款訂單 " + order.getOrderNo()
                + "，訂單金額為新台幣 " + order.getTotalAmount() + " 元，請開始處理。";
    }
}
