package com.dinogo.sales.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.dinogo.cart.repository.CartItemRepository;
import com.dinogo.cart.repository.CartRepository;
import com.dinogo.catalog.entity.Product;
import com.dinogo.catalog.entity.ProductSku;
import com.dinogo.catalog.repository.ProductRepository;
import com.dinogo.catalog.repository.ProductSkuRepository;
import com.dinogo.coupon.service.CouponUsageService;
import com.dinogo.member.entity.Address;
import com.dinogo.member.entity.Member;
import com.dinogo.member.repository.AddressRepository;
import com.dinogo.sales.dto.OrderDetailResponse;
import com.dinogo.sales.dto.order.CreateOrderItemRequest;
import com.dinogo.sales.dto.order.CreateOrderRequest;
import com.dinogo.sales.dto.order.CreateOrderResponse;
import com.dinogo.sales.entity.Order;
import com.dinogo.sales.entity.OrderItem;
import com.dinogo.sales.entity.OrderStatus;
import com.dinogo.sales.entity.Payment;
import com.dinogo.sales.entity.PaymentMethod;
import com.dinogo.sales.entity.PaymentStatus;
import com.dinogo.sales.entity.Shipment;
import com.dinogo.sales.entity.ShipmentStatus;
import com.dinogo.sales.exception.OrderNotFoundException;
import com.dinogo.sales.repository.OrderRepository;
import com.dinogo.seller.entity.Seller;
import com.dinogo.seller.repository.SellerRepository;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

        @Mock
        private OrderRepository orderRepository;
        @Mock
        private AddressRepository addressRepository;
        @Mock
        private ProductSkuRepository productSkuRepository;
        @Mock
        private SellerRepository sellerRepository;
        @Mock
        private CouponUsageService couponUsageService;
        @Mock
        private CartRepository cartRepository;
        @Mock
        private CartItemRepository cartItemRepository;
        @Captor
        private ArgumentCaptor<Order> orderCaptor;

        private OrderService orderService;

        @Mock
        private ProductRepository productRepository;

        @BeforeEach
        void setUp() {
                orderService = new OrderService(
                                orderRepository,
                                addressRepository,
                                productSkuRepository,
                                productRepository,
                                sellerRepository,
                                couponUsageService,
                                cartRepository,
                                cartItemRepository);
        }

        @Test
        void createOrderBuildsSnapshotsCalculatesAmountsAndDeductsStock() {
                mockOwnedAddress(1, 10);
                ProductSku sku = mockSku(100, 200, 300, "Keyboard", new BigDecimal("500.00"), 5);
                when(productSkuRepository.findById(100)).thenReturn(Optional.of(sku));
                when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
                        Order order = invocation.getArgument(0);
                        order.setOrderId(99);
                        return order;
                });

                when(productSkuRepository.deductStockIfAvailable(100, 2)).thenReturn(1);
                CreateOrderResponse response = orderService.createOrder(request(
                                List.of(new CreateOrderItemRequest(100, 2))), 1);

                assertThat(response.orderId()).isEqualTo(99);
                assertThat(response.status()).isEqualTo(OrderStatus.PENDING_PAYMENT);
                assertThat(response.subtotalAmount()).isEqualByComparingTo("1000.00");
                assertThat(response.shippingFee()).isEqualByComparingTo("0.00");
                assertThat(response.totalAmount()).isEqualByComparingTo("1000.00");

                org.mockito.Mockito.verify(orderRepository).save(orderCaptor.capture());
                Order savedOrder = orderCaptor.getValue();
                assertThat(savedOrder.getSellerId()).isEqualTo(300);
                assertThat(savedOrder.getReceiverName()).isEqualTo("王小明");
                assertThat(savedOrder.getOrderItems()).hasSize(1);
                OrderItem savedItem = savedOrder.getOrderItems().getFirst();
                assertThat(savedItem.getProductName()).isEqualTo("Keyboard");
                assertThat(savedItem.getUnitPrice()).isEqualByComparingTo("500.00");
                assertThat(savedItem.getQuantity()).isEqualTo(2);
                assertThat(savedItem.getSubtotal()).isEqualByComparingTo("1000.00");

                // Order history must keep the values captured at checkout even if the
                // catalog product is renamed or repriced afterwards.
                when(sku.getProduct().getProductName()).thenReturn("Keyboard New Name");
                sku.setPrice(new BigDecimal("999.00"));
                assertThat(sku.getProduct().getProductName()).isEqualTo("Keyboard New Name");
                assertThat(sku.getPrice()).isEqualByComparingTo("999.00");
                assertThat(savedItem.getProductName()).isEqualTo("Keyboard");
                assertThat(savedItem.getUnitPrice()).isEqualByComparingTo("500.00");
                assertThat(savedItem.getSubtotal()).isEqualByComparingTo("1000.00");
                assertThat(savedOrder.getPayments()).isEmpty();
        }

        @Test
        void createOrderRejectsAddressOwnedByAnotherMember() {
                mockOwnedAddress(2, 10);

                assertThatThrownBy(() -> orderService.createOrder(request(
                                List.of(new CreateOrderItemRequest(100, 1))), 1))
                                .isInstanceOf(IllegalArgumentException.class)
                                .hasMessage("Address does not belong to buyer");
        }

        @Test
        void createOrderRejectsInsufficientStock() {
                mockOwnedAddress(1, 10);
                ProductSku sku = mockSku(100, 200, 300, "Keyboard", new BigDecimal("500.00"), 1);
                when(productSkuRepository.findById(100)).thenReturn(Optional.of(sku));
                when(productSkuRepository.deductStockIfAvailable(100, 2)).thenReturn(0);

                assertThatThrownBy(() -> orderService.createOrder(request(
                                List.of(new CreateOrderItemRequest(100, 2))), 1))
                                .isInstanceOf(IllegalArgumentException.class)
                                .hasMessage("Insufficient stock for SKU: 100");
        }

        @Test
        void createOrderRejectsNonPositiveQuantity() {
                mockOwnedAddress(1, 10);

                assertThatThrownBy(() -> orderService.createOrder(request(
                                List.of(new CreateOrderItemRequest(100, 0))), 1))
                                .isInstanceOf(IllegalArgumentException.class)
                                .hasMessage("Quantity must be positive");

                verify(productSkuRepository, never()).findById(100);
                verify(orderRepository, never()).save(any(Order.class));
        }

        @Test
        void createOrderRejectsUnavailableProduct() {
                mockOwnedAddress(1, 10);
                ProductSku sku = mockSku(100, 200, 300, "Keyboard", new BigDecimal("500.00"), 5);
                when(sku.getProduct().getStatus()).thenReturn((byte) 2);
                when(productSkuRepository.findById(100)).thenReturn(Optional.of(sku));

                assertThatThrownBy(() -> orderService.createOrder(request(
                                List.of(new CreateOrderItemRequest(100, 1))), 1))
                                .isInstanceOf(IllegalArgumentException.class)
                                .hasMessage("Product is not available for SKU: 100");

                verify(productSkuRepository, never()).deductStockIfAvailable(100, 1);
                verify(orderRepository, never()).save(any(Order.class));
        }

        @Test
        void createOrderRejectsInvalidDatabasePrice() {
                mockOwnedAddress(1, 10);
                ProductSku sku = mockSku(100, 200, 300, "Keyboard", new BigDecimal("-1.00"), 5);
                when(productSkuRepository.findById(100)).thenReturn(Optional.of(sku));

                assertThatThrownBy(() -> orderService.createOrder(request(
                                List.of(new CreateOrderItemRequest(100, 1))), 1))
                                .isInstanceOf(IllegalArgumentException.class)
                                .hasMessage("SKU price is invalid: 100");

                verify(productSkuRepository, never()).deductStockIfAvailable(100, 1);
                verify(orderRepository, never()).save(any(Order.class));
        }

        @Test
        void createOrderRejectsProductsFromDifferentSellers() {
                mockOwnedAddress(1, 10);
                ProductSku firstSku = mockSku(100, 200, 300, "Keyboard", BigDecimal.TEN, 5);
                ProductSku secondSku = mockSku(101, 201, 301, "Mouse", BigDecimal.TEN, 5);
                when(productSkuRepository.findById(100))
                                .thenReturn(Optional.of(firstSku));
                when(productSkuRepository.findById(101))
                                .thenReturn(Optional.of(secondSku));
                when(productSkuRepository.deductStockIfAvailable(100, 1)).thenReturn(1);

                assertThatThrownBy(() -> orderService.createOrder(request(
                                List.of(new CreateOrderItemRequest(100, 1), new CreateOrderItemRequest(101, 1))), 1))
                                .isInstanceOf(IllegalArgumentException.class)
                                .hasMessage("One order can only contain products from one seller");
        }

        @Test
        void cancelOrderUsesLockedQueryAndRestoresStockOnce() {
                Order order = new Order();
                order.setOrderId(99);
                order.setBuyerId(6);
                order.setStatus(OrderStatus.PENDING_PAYMENT);
                OrderItem item = new OrderItem();
                item.setSkuId(100);
                item.setQuantity(2);
                order.addOrderItem(item);

                when(orderRepository.findForCancellation(99, 6)).thenReturn(Optional.of(order));
                when(productSkuRepository.restoreStock(100, 2)).thenReturn(1);

                orderService.cancelOrder(99, 6, "buyer cancelled");

                verify(orderRepository).findForCancellation(99, 6);
                verify(orderRepository, never()).findByOrderIdAndBuyerId(99, 6);
                verify(productSkuRepository).restoreStock(100, 2);
                assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        }

        @Test
        void cancelPaidOrderIsRejectedWithoutRestoringStockOrChangingPayment() {
                Order order = new Order();
                order.setOrderId(99);
                order.setBuyerId(6);
                order.setStatus(OrderStatus.PAID);

                OrderItem item = new OrderItem();
                item.setSkuId(100);
                item.setQuantity(2);
                order.addOrderItem(item);

                PaymentMethod method = new PaymentMethod();
                method.setMethodCode("CREDIT_CARD");
                method.setMethodName("信用卡");

                Payment payment = new Payment();
                payment.setPaymentId(10);
                payment.setOrder(order);
                payment.setPaymentMethod(method);
                payment.setStatus(PaymentStatus.SUCCESS);
                order.getPayments().add(payment);

                when(orderRepository.findForCancellation(99, 6)).thenReturn(Optional.of(order));
                assertThatThrownBy(() -> orderService.cancelOrder(99, 6, "buyer cancelled"))
                                .isInstanceOf(IllegalArgumentException.class)
                                .hasMessage("Only pending-payment orders or unshipped cash-on-delivery orders can be cancelled");

                verify(productSkuRepository, never()).restoreStock(100, 2);
                assertThat(order.getStatus()).isEqualTo(OrderStatus.PAID);
                assertThat(payment.getStatus()).isEqualTo(PaymentStatus.SUCCESS);
        }

        @Test
        void buyerCanCancelUnshippedCashOnDeliveryOrderAndRestoreStock() {
                Order order = new Order();
                order.setOrderId(99);
                order.setBuyerId(6);
                order.setStatus(OrderStatus.PROCESSING);
                OrderItem item = new OrderItem();
                item.setSkuId(100);
                item.setQuantity(2);
                order.addOrderItem(item);

                PaymentMethod method = new PaymentMethod();
                method.setMethodCode("CASH_ON_DELIVERY");
                Payment payment = new Payment();
                payment.setOrder(order);
                payment.setPaymentMethod(method);
                payment.setStatus(PaymentStatus.PENDING);
                order.getPayments().add(payment);

                when(orderRepository.findForCancellation(99, 6)).thenReturn(Optional.of(order));
                when(productSkuRepository.restoreStock(100, 2)).thenReturn(1);

                orderService.cancelOrder(99, 6, "buyer cancelled before shipment");

                verify(productSkuRepository).restoreStock(100, 2);
                assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
                assertThat(payment.getStatus()).isEqualTo(PaymentStatus.CANCELLED);
        }

        @Test
        void updateStatusRejectsPaidOutsidePaymentFlow() {
                assertThatThrownBy(() -> orderService.updateStatusBySeller(99, 1, OrderStatus.PAID, null))
                                .isInstanceOf(IllegalArgumentException.class)
                                .hasMessage("Order status PAID can only be set by the payment flow");

                verify(orderRepository, never()).findById(99);
        }

        @Test
        void getMemberOrdersIncludesShipmentSummary() {
                Order order = new Order();
                order.setOrderId(99);
                order.setOrderNo("DG-99");
                order.setBuyerId(6);
                order.setSellerId(300);
                order.setStatus(OrderStatus.SHIPPED);
                order.setTotalAmount(new BigDecimal("1280.00"));

                Shipment shipment = new Shipment();
                shipment.setShipmentId(30);
                shipment.setOrder(order);
                shipment.setStatus(ShipmentStatus.AVAILABLE_FOR_PICKUP);
                order.setShipment(shipment);

                when(orderRepository.findByBuyerIdOrderByCreatedAtDesc(6)).thenReturn(List.of(order));

                var response = orderService.getMemberOrders(6);

                assertThat(response).hasSize(1);
                assertThat(response.getFirst().shipment().status())
                                .isEqualTo(ShipmentStatus.AVAILABLE_FOR_PICKUP);
        }

        @Test
        void getMemberOrderIncludesLatestPaymentAndShipment() {
                Order order = new Order();
                order.setOrderId(99);
                order.setBuyerId(6);

                PaymentMethod method = new PaymentMethod();
                method.setMethodCode("CREDIT_CARD");
                method.setMethodName("信用卡");
                Payment payment = new Payment();
                payment.setPaymentId(20);
                payment.setOrder(order);
                payment.setPaymentMethod(method);
                payment.setStatus(PaymentStatus.SUCCESS);
                payment.setCreatedAt(LocalDateTime.of(2026, 8, 17, 12, 0));

                Payment olderPayment = new Payment();
                olderPayment.setPaymentId(19);
                olderPayment.setOrder(order);
                olderPayment.setPaymentMethod(method);
                olderPayment.setStatus(PaymentStatus.FAILED);
                olderPayment.setCreatedAt(LocalDateTime.of(2026, 8, 17, 11, 0));
                order.getPayments().add(olderPayment);
                order.getPayments().add(payment);

                Shipment shipment = new Shipment();
                shipment.setShipmentId(30);
                shipment.setOrder(order);
                shipment.setCarrierName("黑貓宅急便");
                shipment.setTrackingNo("TRACK-001");
                shipment.setStatus(ShipmentStatus.SHIPPED);
                order.setShipment(shipment);

                when(orderRepository.findByOrderIdAndBuyerId(99, 6))
                                .thenReturn(Optional.of(order));

                OrderDetailResponse response = orderService.getMemberOrder(99, 6);

                assertThat(response.payment().paymentId()).isEqualTo(20);
                assertThat(response.payment().paymentMethodName()).isEqualTo("信用卡");
                assertThat(response.payment().status()).isEqualTo(PaymentStatus.SUCCESS);
                assertThat(response.shipment().shipmentId()).isEqualTo(30);
                assertThat(response.shipment().carrierName()).isEqualTo("黑貓宅急便");
                assertThat(response.shipment().status()).isEqualTo(ShipmentStatus.SHIPPED);
        }

        @Test
        void getMemberOrderReturnsNullPaymentAndShipmentBeforeTheyExist() {
                Order order = new Order();
                order.setOrderId(99);
                order.setBuyerId(6);
                when(orderRepository.findByOrderIdAndBuyerId(99, 6))
                                .thenReturn(Optional.of(order));

                OrderDetailResponse response = orderService.getMemberOrder(99, 6);

                assertThat(response.payment()).isNull();
                assertThat(response.shipment()).isNull();
        }

        @Test
        void getSellerOrderUsesAuthenticatedSellerOwnership() {
                Seller seller = mock(Seller.class);
                when(seller.getSellerId()).thenReturn(300);
                when(seller.getStatus()).thenReturn("ACTIVE");
                when(sellerRepository.findByMember_MemberId(6)).thenReturn(Optional.of(seller));
                Order order = new Order();
                order.setOrderId(99);
                order.setSellerId(300);
                when(orderRepository.findByOrderIdAndSellerId(99, 300)).thenReturn(Optional.of(order));

                OrderDetailResponse response = orderService.getSellerOrder(99, 6);

                assertThat(response.orderId()).isEqualTo(99);
                verify(orderRepository).findByOrderIdAndSellerId(99, 300);
                verify(orderRepository, never()).findById(99);
        }

        @Test
        void getSellerOrdersReturnsOnlyAuthenticatedSellerOrdersNewestFirst() {
                Seller seller = mock(Seller.class);
                when(seller.getSellerId()).thenReturn(300);
                when(seller.getStatus()).thenReturn("ACTIVE");
                when(sellerRepository.findByMember_MemberId(6)).thenReturn(Optional.of(seller));

                Order order = new Order();
                order.setOrderId(99);
                order.setOrderNo("DG-99");
                order.setBuyerId(8);
                order.setSellerId(300);
                order.setStatus(OrderStatus.PAID);
                order.setTotalAmount(new BigDecimal("1280.00"));
                when(orderRepository.findBySellerIdOrderByCreatedAtDesc(300)).thenReturn(List.of(order));

                var response = orderService.getSellerOrders(6);

                assertThat(response).hasSize(1);
                assertThat(response.getFirst().orderId()).isEqualTo(99);
                assertThat(response.getFirst().buyerId()).isEqualTo(8);
                verify(orderRepository).findBySellerIdOrderByCreatedAtDesc(300);
                verify(orderRepository, never()).findAll();
        }

        @Test
        void getSellerOrdersRejectsInactiveSeller() {
                Seller seller = mock(Seller.class);
                when(seller.getStatus()).thenReturn("SUSPENDED");
                when(sellerRepository.findByMember_MemberId(6)).thenReturn(Optional.of(seller));

                assertThatThrownBy(() -> orderService.getSellerOrders(6))
                                .isInstanceOf(OrderNotFoundException.class)
                                .hasMessage("Seller is inactive");

                verify(orderRepository, never()).findBySellerIdOrderByCreatedAtDesc(any());
        }

        @Test
        void getSellerOrderHidesOrdersOwnedByAnotherSeller() {
                Seller seller = mock(Seller.class);
                when(seller.getSellerId()).thenReturn(300);
                when(seller.getStatus()).thenReturn("ACTIVE");
                when(sellerRepository.findByMember_MemberId(6)).thenReturn(Optional.of(seller));
                when(orderRepository.findByOrderIdAndSellerId(99, 300)).thenReturn(Optional.empty());

                assertThatThrownBy(() -> orderService.getSellerOrder(99, 6))
                                .isInstanceOf(OrderNotFoundException.class)
                                .hasMessage("Order does not exist");
        }

        @Test
        void updateStatusRejectsShippedOutsideShipmentFlow() {
                assertThatThrownBy(() -> orderService.updateStatusBySeller(
                                99, 1, OrderStatus.SHIPPED, null))
                                .isInstanceOf(IllegalArgumentException.class)
                                .hasMessage("Order status SHIPPED can only be set by the shipment flow");

                verify(sellerRepository, never()).findByMember_MemberId(1);
        }

        @Test
        void updateStatusRejectsCompletedOutsideShipmentFlow() {
                assertThatThrownBy(() -> orderService.updateStatusBySeller(
                                99, 1, OrderStatus.COMPLETED, null))
                                .isInstanceOf(IllegalArgumentException.class)
                                .hasMessage("Order status COMPLETED can only be set by the shipment flow");

                verify(sellerRepository, never()).findByMember_MemberId(1);
        }

        @Test
        void updateStatusAllowsOwningSeller() {
                // Arrange：JWT 登入會員 6 對應賣家 300
                Seller seller = mock(Seller.class);
                when(seller.getSellerId()).thenReturn(300);
                when(seller.getStatus()).thenReturn("ACTIVE");
                when(sellerRepository.findByMember_MemberId(6))
                                .thenReturn(Optional.of(seller));

                // 訂單 99 屬於賣家 300，目前狀態為 PAID
                Order order = new Order();
                order.setOrderId(99);
                order.setSellerId(300);
                order.setStatus(OrderStatus.PAID);

                when(orderRepository.findByOrderIdAndSellerId(99, 300))
                                .thenReturn(Optional.of(order));

                // Act：PAID → PROCESSING 是合法狀態轉換
                OrderDetailResponse response = orderService.updateStatusBySeller(
                                99,
                                6,
                                OrderStatus.PROCESSING,
                                null);

                // Assert
                assertThat(order.getStatus())
                                .isEqualTo(OrderStatus.PROCESSING);

                assertThat(response.status())
                                .isEqualTo(OrderStatus.PROCESSING);

                verify(sellerRepository).findByMember_MemberId(6);
                verify(orderRepository)
                                .findByOrderIdAndSellerId(99, 300);

                // 確認沒有繞過 ownership，直接用 orderId 查詢
                verify(orderRepository, never()).findById(99);
        }

        @Test
        void updateStatusRejectsOrderOwnedByAnotherSeller() {
                // Arrange：登入會員 6 對應賣家 300
                Seller seller = mock(Seller.class);
                when(seller.getSellerId()).thenReturn(300);
                when(seller.getStatus()).thenReturn("ACTIVE");
                when(sellerRepository.findByMember_MemberId(6))
                                .thenReturn(Optional.of(seller));

                // 訂單 99 不屬於賣家 300，所以 ownership 查詢找不到
                when(orderRepository.findByOrderIdAndSellerId(99, 300))
                                .thenReturn(Optional.empty());

                // Act + Assert
                assertThatThrownBy(() -> orderService.updateStatusBySeller(
                                99,
                                6,
                                OrderStatus.PROCESSING,
                                null))
                                .isInstanceOf(OrderNotFoundException.class)
                                .hasMessage("Order does not exist");

                verify(sellerRepository).findByMember_MemberId(6);
                verify(orderRepository)
                                .findByOrderIdAndSellerId(99, 300);

                // 確認不能改用 findById() 繞過 seller ownership
                verify(orderRepository, never()).findById(99);
        }

        private CreateOrderRequest request(List<CreateOrderItemRequest> items) {
                return new CreateOrderRequest(10, "HOME_DELIVERY", "請小心包裝", null, items);
        }

        private void mockOwnedAddress(Integer memberId, Integer addressId) {
                Member member = new Member();
                member.setMemberId(memberId);
                Address address = new Address();
                address.setAddressId(addressId);
                address.setMember(member);
                address.setReceiverName("王小明");
                address.setReceiverPhone("0912345678");
                address.setPostalCode("100");
                address.setCity("台北市");
                address.setDistrict("中正區");
                address.setDetailAddress("測試路 1 號");
                when(addressRepository.findById(addressId)).thenReturn(Optional.of(address));
        }

        private ProductSku mockSku(
                        Integer skuId,
                        Integer productId,
                        Integer sellerId,
                        String productName,
                        BigDecimal price,
                        Integer stock) {
                Seller seller = mock(Seller.class);
                lenient().when(seller.getSellerId()).thenReturn(sellerId);

                Product product = mock(Product.class);
                lenient().when(product.getProductId()).thenReturn(productId);
                lenient().when(product.getProductName()).thenReturn(productName);
                lenient().when(product.getSeller()).thenReturn(seller);
                lenient().when(product.getImages()).thenReturn(new ArrayList<>());
                lenient().when(product.getStatus()).thenReturn((byte) 1);

                ProductSku sku = new ProductSku();
                sku.setSkuId(skuId);
                sku.setProduct(product);
                sku.setPrice(price);
                sku.setStock(stock);
                sku.setStatus((byte) 1);
                sku.setSpec1Name("顏色");
                sku.setSpec1Value("黑色");
                return sku;
        }
}
