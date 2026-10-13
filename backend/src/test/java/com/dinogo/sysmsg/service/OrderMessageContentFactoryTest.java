package com.dinogo.sysmsg.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

import com.dinogo.sysmsg.dto.external.OrderInfoResponse;
import com.dinogo.sysmsg.service.content.OrderMessageContentFactory;

class OrderMessageContentFactoryTest {
    private final OrderMessageContentFactory factory = new OrderMessageContentFactory();

    @Test
    void cashOnDeliveryProcessingCreatesSellerNewOrderMessage() {
        OrderInfoResponse order = order("ORD-COD-001", new BigDecimal("1680.00"));

        assertEquals("收到新訂單", factory.title(order, "PROCESSING", false));
        String content = factory.content(order, "PROCESSING", false);
        assertTrue(content.contains("貨到付款訂單"));
        assertTrue(content.contains(order.getOrderNo()));
    }

    @Test
    void createsDifferentCompletedContentForCustomerAndSeller() {
        OrderInfoResponse order = order("ORD-001", new BigDecimal("1200"));
        order.setOrderId(60);
        order.setBuyerId(14);
        order.setCreatedAt(LocalDateTime.of(2026, 9, 3, 18, 20));

        assertEquals("親愛的 會員-14 您好:\n"
                + "   感謝您的訂購！您於 2026/09/03 18:20 下單之商品已完成，\n"
                + "   您的訂單編號為 /member/orders/60，\n"
                + "   歡迎您留下評價，感謝您的惠顧！",
                factory.content(order, "COMPLETED", true));
        assertEquals("訂單 ORD-001 已完成，收入金額為 1200。",
                factory.content(order, "COMPLETED", false));
    }

    @Test
    void createsCustomerShippedMessageWithOrderDetailLink() {
        OrderInfoResponse order = order("ORD-001", new BigDecimal("1200"));
        order.setOrderId(61);
        order.setBuyerId(14);
        order.setCreatedAt(LocalDateTime.of(2026, 9, 4, 14, 30));

        assertEquals("訂單已出貨", factory.title(order, "SHIPPED", true));
        assertEquals("親愛的 會員-14 您好:\n"
                + "   感謝您的訂購！您於 2026/09/04 14:30 下單之商品已出貨，\n"
                + "   您的訂單編號為 /member/orders/61，\n"
                + "   隨時點此查詢進度：/member/orders/61，\n"
                + "   請於包裹送達後，7日內取貨，感謝您的惠顧！",
                factory.content(order, "SHIPPED", true));
        assertEquals("訂單已出貨", factory.title(order, "SHIPPED", false));
        assertEquals("訂單 ORD-001 已出貨。", factory.content(order, "SHIPPED", false));
    }

    @Test
    void createsCustomerCancelledMessageWithMemberReasonAndOrderLink() {
        OrderInfoResponse order = order("ORD-002", new BigDecimal("680"));
        order.setOrderId(62);
        order.setBuyerId(15);
        order.setCreatedAt(LocalDateTime.of(2026, 9, 4, 9, 5));
        order.setCancelReason("臨時不需要商品");

        assertEquals("訂單已取消", factory.cancelledTitle(order, true));
        assertEquals("親愛的 會員-15 您好:\n"
                + "   感謝您今日光臨！您於 2026/09/04 09:05 下單之商品已取消，\n"
                + "   您的訂單編號為 /member/orders/62，\n"
                + "   取消原因：\n"
                + "       臨時不需要商品\n"
                + "   歡迎您來信說明，並再次訂購，您的意見是我們最重要的支持！",
                factory.cancelledContent(order, true));
        assertEquals("訂單已取消", factory.cancelledTitle(order, false));
        assertEquals("訂單 ORD-002 已取消，原因：臨時不需要商品。",
                factory.cancelledContent(order, false));
    }

    @Test
    void createsCustomerDeliveredMessageWithOrderLinks() {
        OrderInfoResponse order = order("ORD-003", new BigDecimal("900"));
        order.setOrderId(63);
        order.setBuyerId(16);
        order.setCreatedAt(LocalDateTime.of(2026, 9, 4, 16, 45));

        assertEquals("訂單已到貨", factory.title(order, "DELIVERED", true));
        assertEquals("親愛的 會員-16 您好:\n"
                + "   感謝您的訂購！您於 2026/09/04 16:45 下單之商品已到貨，\n"
                + "   您的訂單編號為 /member/orders/63，\n"
                + "   請於7日內取貨，並於/member/orders/63按下\"完成訂單\"，感謝您的惠顧！",
                factory.content(order, "DELIVERED", true));
        assertEquals("訂單 ORD-003 已送達。", factory.content(order, "DELIVERED", false));
    }

    @Test
    void createsCashOnDeliveryPlacedMessage() {
        OrderInfoResponse order = order("ORD-004", new BigDecimal("1680"));
        order.setOrderId(64);
        order.setBuyerId(17);
        order.setCreatedAt(LocalDateTime.of(2026, 9, 4, 20, 10));

        assertEquals("訂單下單成功", factory.title(order, "PROCESSING", true));
        assertEquals("親愛的 會員-17 您好:\n"
                + "   感謝您的訂購！您於 2026/09/04 20:10 下單之商品已完成下單，\n"
                + "   我們已收到您的訂單，請於到貨後現金付款新台幣共1680元，\n"
                + "   您的訂單編號為 /member/orders/64，\n"
                + "   隨時點此查詢進度：/member/orders/64，\n"
                + "   請於貨物送達後，7日內取貨，感謝您的惠顧！",
                factory.content(order, "PROCESSING", true));
    }

    @Test
    void createsCreditCardPaidMessage() {
        OrderInfoResponse order = order("ORD-005", new BigDecimal("2500"));
        order.setOrderId(65);
        order.setBuyerId(18);
        order.setMethodCode("CREDIT_CARD");
        order.setCreatedAt(LocalDateTime.of(2026, 9, 4, 21, 15));

        assertEquals("親愛的 會員-18 您好:\n"
                + "   感謝您的訂購！您於 2026/09/04 21:15 下單之商品已完成下單，\n"
                + "   我們已收到您的信用卡款項，請核對為本人付款，\n"
                + "   您的訂單編號為 /member/orders/65，\n"
                + "   隨時點此查詢進度：/member/orders/65，\n"
                + "   請於貨物送達後，7日內取貨，感謝您的惠顧！",
                factory.content(order, "PAID", true));
    }

    @Test
    void rejectsUnsupportedStatus() {
        assertThrows(IllegalStateException.class,
                () -> factory.content(order("ORD-001", BigDecimal.ONE), "UNKNOWN", true));
    }

    private OrderInfoResponse order(String orderNo, BigDecimal totalAmount) {
        OrderInfoResponse order = new OrderInfoResponse();
        order.setOrderNo(orderNo);
        order.setTotalAmount(totalAmount);
        return order;
    }
}
