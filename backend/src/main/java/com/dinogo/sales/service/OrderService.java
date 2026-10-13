package com.dinogo.sales.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.dinogo.catalog.repository.ProductRepository;
import com.dinogo.cart.entity.Cart;
import com.dinogo.cart.entity.CartItem;
import com.dinogo.cart.repository.CartItemRepository;
import com.dinogo.cart.repository.CartRepository;
import com.dinogo.catalog.entity.Product;
import com.dinogo.catalog.entity.ProductImage;
import com.dinogo.catalog.entity.ProductSku;
import com.dinogo.catalog.repository.ProductSkuRepository;
import com.dinogo.coupon.service.CouponUsageService;
import com.dinogo.coupon.service.CouponUsageService.AppliedCoupon;
import com.dinogo.coupon.service.CouponUsageService.CouponItem;
import com.dinogo.member.entity.Address;
import com.dinogo.member.repository.AddressRepository;
import com.dinogo.sales.dto.OrderDetailResponse;
import com.dinogo.sales.dto.OrderListResponse;
import com.dinogo.sales.dto.SellerOrderListResponse;
import com.dinogo.sales.dto.order.CreateOrderItemRequest;
import com.dinogo.sales.dto.order.CreateOrderRequest;
import com.dinogo.sales.dto.order.CreateOrderResponse;
import com.dinogo.sales.dto.order.OrderItemResponse;
import com.dinogo.sales.dto.order.OrderPaymentSummary;
import com.dinogo.sales.dto.order.OrderShipmentSummary;
import com.dinogo.sales.entity.Order;
import com.dinogo.sales.entity.OrderItem;
import com.dinogo.sales.entity.OrderStatus;
import com.dinogo.sales.entity.Payment;
import com.dinogo.sales.entity.PaymentStatus;
import com.dinogo.sales.entity.Shipment;
import com.dinogo.sales.entity.ShipmentStatus;
import com.dinogo.sales.exception.InvalidOrderException;
import com.dinogo.sales.exception.OrderNotFoundException;
import com.dinogo.sales.repository.OrderRepository;
import com.dinogo.seller.entity.Seller;
import com.dinogo.seller.repository.SellerRepository;

/** 訂單應用服務；建立訂單時保存商品與收件資料快照，暫不建立付款紀錄。 */
@Service
public class OrderService {

    private static final DateTimeFormatter ORDER_NO_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");
    private static final String CASH_ON_DELIVERY = "CASH_ON_DELIVERY";
    private static final String HOME_DELIVERY = "HOME_DELIVERY";

    private final OrderRepository orderRepository;
    private final AddressRepository addressRepository;
    private final ProductSkuRepository productSkuRepository;
    private final ProductRepository productRepository;
    private final SellerRepository sellerRepository;
    private final CouponUsageService couponUsageService;

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private static final Map<OrderStatus, Set<OrderStatus>> ALLOWED_TRANSITIONS = Map.of(
            OrderStatus.PENDING_PAYMENT,
            Set.of(OrderStatus.PAID, OrderStatus.CANCELLED),

            OrderStatus.PAID,
            Set.of(OrderStatus.PROCESSING),

            OrderStatus.PROCESSING,
            Set.of(OrderStatus.SHIPPED),

            OrderStatus.SHIPPED,
            Set.of(OrderStatus.COMPLETED));

    public OrderService(
            OrderRepository orderRepository,
            AddressRepository addressRepository,
            ProductSkuRepository productSkuRepository,
            ProductRepository productRepository,
            SellerRepository sellerRepository,
            CouponUsageService couponUsageService,
            CartRepository cartRepository,
            CartItemRepository cartItemRepository) {

        this.orderRepository = orderRepository;
        this.addressRepository = addressRepository;
        this.productSkuRepository = productSkuRepository;
        this.productRepository = productRepository;
        this.sellerRepository = sellerRepository;
        this.couponUsageService = couponUsageService;
        this.cartRepository = cartRepository;
        this.cartItemRepository = cartItemRepository;
    }

    @Transactional
    public CreateOrderResponse createOrder(CreateOrderRequest request, Integer buyerId) {
        validateShippingMethod(request.shippingMethod());
        Address address = addressRepository.findById(request.addressId())
                .orElseThrow(() -> new InvalidOrderException(
                        "Address does not exist: " + request.addressId()));
        if (!buyerId.equals(address.getMember().getMemberId())) {
            throw new InvalidOrderException("Address does not belong to buyer");
        }

        Order order = createOrderHeader(request, address, buyerId);
        Set<Integer> skuIds = new HashSet<>();
        Integer sellerId = null;
        BigDecimal subtotalAmount = BigDecimal.ZERO;
        List<CouponItem> couponItems = new ArrayList<>();

        for (CreateOrderItemRequest itemRequest : request.items()) {
            if (itemRequest.quantity() == null || itemRequest.quantity() <= 0) {
                throw new InvalidOrderException("Quantity must be positive");
            }
            if (!skuIds.add(itemRequest.skuId())) {
                throw new InvalidOrderException("Duplicate SKU in order: " + itemRequest.skuId());
            }

            ProductSku sku = productSkuRepository.findById(itemRequest.skuId())
                    .orElseThrow(() -> new InvalidOrderException(
                            "SKU does not exist: " + itemRequest.skuId()));
            if (sku.getStatus() == null || sku.getStatus() != (byte) 1) {
                throw new InvalidOrderException("SKU is not available: " + itemRequest.skuId());
            }
            Product product = sku.getProduct();
            if (product == null || product.getStatus() == null || product.getStatus() != (byte) 1) {
                throw new InvalidOrderException("Product is not available for SKU: " + itemRequest.skuId());
            }
            if (sku.getPrice() == null || sku.getPrice().signum() < 0) {
                throw new InvalidOrderException("SKU price is invalid: " + itemRequest.skuId());
            }
            Integer itemSellerId = product.getSeller().getSellerId();
            if (sellerId == null) {
                sellerId = itemSellerId;
            } else if (!sellerId.equals(itemSellerId)) {
                throw new InvalidOrderException("One order can only contain products from one seller");
            }

            BigDecimal unitPrice = roundToWholeTwd(sku.getPrice());
            BigDecimal itemSubtotal = unitPrice.multiply(BigDecimal.valueOf(itemRequest.quantity()));
            order.addOrderItem(createOrderItem(sku, itemRequest.quantity(), unitPrice, itemSubtotal));
            subtotalAmount = subtotalAmount.add(itemSubtotal);
            couponItems.add(new CouponItem(product, itemSubtotal));
            int updated = productSkuRepository.deductStockIfAvailable(
                    itemRequest.skuId(), itemRequest.quantity());

            if (updated == 0) {
                throw new InvalidOrderException(
                        "Insufficient stock for SKU: " + itemRequest.skuId());
            }
            int soldUpdated = productRepository.increaseSoldCount(
                    product.getProductId(),
                    itemRequest.quantity());

            if (soldUpdated == 0) {
                throw new InvalidOrderException(
                        "Product does not exist: " + product.getProductId());
            }
        }

        // The backend owns all order amount calculations.
        BigDecimal shippingFee = BigDecimal.ZERO;
        BigDecimal discountAmount = BigDecimal.ZERO;
        AppliedCoupon appliedCoupon = null;
        if (request.memberCouponId() != null) {
            appliedCoupon = couponUsageService.validateAndCalculate(
                    request.memberCouponId(),
                    buyerId,
                    sellerId,
                    subtotalAmount,
                    couponItems);
            discountAmount = appliedCoupon.discount();
            order.setMemberCouponId(request.memberCouponId());
        }
        order.setSellerId(sellerId);
        subtotalAmount = roundToWholeTwd(subtotalAmount);
        shippingFee = roundToWholeTwd(shippingFee);
        discountAmount = roundToWholeTwd(discountAmount);
        order.setSubtotalAmount(subtotalAmount);
        order.setShippingFee(shippingFee);
        order.setDiscountAmount(discountAmount);
        order.setTotalAmount(roundToWholeTwd(subtotalAmount.add(shippingFee).subtract(discountAmount)));

        Order savedOrder = orderRepository.save(order);

        if (appliedCoupon != null) {
            couponUsageService.consume(appliedCoupon);
        }

        // 建立訂單成功後，移除本次結帳的購物車商品
        removeOrderedItemsFromCart(
                buyerId,
                skuIds);

        return toResponse(savedOrder);
    }

    private void removeOrderedItemsFromCart(
            Integer buyerId,
            Set<Integer> skuIds) {

        Cart cart = cartRepository
                .findByMemberMemberId(buyerId)
                .orElse(null);

        if (cart == null) {
            return;
        }

        List<CartItem> cartItems = cartItemRepository
                .findByCartCartId(cart.getCartId());

        List<CartItem> itemsToDelete = cartItems.stream()
                .filter(item -> item.getProductSku() != null
                        && skuIds.contains(
                                item.getProductSku().getSkuId()))
                .toList();

        if (!itemsToDelete.isEmpty()) {
            cartItemRepository.deleteAll(itemsToDelete);
        }
    }

    @Transactional(readOnly = true)
    public List<OrderListResponse> getMemberOrders(Integer buyerId) {
        return orderRepository.findByBuyerIdOrderByCreatedAtDesc(buyerId)
                .stream()
                .map(this::toListResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public OrderDetailResponse getMemberOrder(
            Integer orderId,
            Integer buyerId) {

        Order order = orderRepository
                .findByOrderIdAndBuyerId(orderId, buyerId)
                .orElseThrow(() -> new OrderNotFoundException("Order does not exist"));

        return toDetailResponse(order);
    }

    @Transactional(readOnly = true)
    public OrderDetailResponse getSellerOrder(Integer orderId, Integer memberId) {
        Seller seller = getSellerByMemberId(memberId);
        Order order = orderRepository
                .findByOrderIdAndSellerId(orderId, seller.getSellerId())
                .orElseThrow(() -> new OrderNotFoundException("Order does not exist"));
        return toDetailResponse(order);
    }

    @Transactional(readOnly = true)
    public List<SellerOrderListResponse> getSellerOrders(Integer memberId) {
        Seller seller = getSellerByMemberId(memberId);
        return orderRepository.findBySellerIdOrderByCreatedAtDesc(seller.getSellerId()).stream()
                .map(this::toSellerListResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<SellerOrderListResponse> getSellerOrders(
            Integer memberId,
            LocalDate startDate,
            LocalDate endDate) {
        if (startDate == null && endDate == null) {
            return getSellerOrders(memberId);
        }
        if (startDate == null || endDate == null || endDate.isBefore(startDate)) {
            throw new IllegalArgumentException("查詢結束日期不可早於開始日期，且日期不可為空");
        }

        Seller seller = getSellerByMemberId(memberId);
        return orderRepository
                .findBySellerIdAndCreatedAtGreaterThanEqualAndCreatedAtLessThanOrderByCreatedAtDesc(
                        seller.getSellerId(),
                        startDate.atStartOfDay(),
                        endDate.plusDays(1).atStartOfDay())
                .stream()
                .map(this::toSellerListResponse)
                .toList();
    }

    @Transactional
    public OrderDetailResponse updateStatusBySeller(
            Integer orderId,
            Integer memberId,
            OrderStatus targetStatus,
            String reason) {
        if (targetStatus == OrderStatus.CANCELLED) {
            throw new InvalidOrderException(
                    "Use the cancellation endpoint to cancel an order");
        }
        if (targetStatus == OrderStatus.PAID) {
            throw new InvalidOrderException(
                    "Order status PAID can only be set by the payment flow");
        }
        if (targetStatus == OrderStatus.SHIPPED
                || targetStatus == OrderStatus.COMPLETED) {
            throw new InvalidOrderException(
                    "Order status " + targetStatus
                            + " can only be set by the shipment flow");
        }
        Seller seller = getSellerByMemberId(memberId);

        Order order = orderRepository
                .findByOrderIdAndSellerId(orderId, seller.getSellerId())
                .orElseThrow(() -> new OrderNotFoundException("Order does not exist"));

        validateStatusTransition(order.getStatus(), targetStatus);

        order.setStatus(targetStatus);

        if (targetStatus == OrderStatus.COMPLETED) {
            order.setCompletedAt(LocalDateTime.now());
        }

        return toDetailResponse(order);
    }

    @Transactional
    public OrderDetailResponse cancelOrder(
            Integer orderId,
            Integer buyerId,
            String reason) {

        Order order = orderRepository
                .findForCancellation(orderId, buyerId)
                .orElseThrow(() -> new OrderNotFoundException("Order does not exist"));

        validateBuyerCancellation(order);

        restoreStock(order);
        cancelPayments(order);

        order.setStatus(OrderStatus.CANCELLED);
        order.setCancelReason(reason);
        order.setCancelledBy("BUYER");
        order.setCancelledAt(LocalDateTime.now());

        return toDetailResponse(order);
    }

    private void cancelPayments(Order order) {
        order.getPayments().stream()
                .filter(payment -> payment.getStatus() == PaymentStatus.PENDING)
                .forEach(payment -> payment.setStatus(PaymentStatus.CANCELLED));
    }

    private void validateBuyerCancellation(Order order) {
        if (order.getStatus() == OrderStatus.PENDING_PAYMENT) {
            return;
        }

        boolean isUnshippedCashOnDeliveryOrder = order.getStatus() == OrderStatus.PROCESSING
                && (order.getShipment() == null
                        || order.getShipment().getStatus() == ShipmentStatus.PREPARING)
                && order.getPayments().stream().anyMatch(payment -> payment.getStatus() == PaymentStatus.PENDING
                        && CASH_ON_DELIVERY.equals(
                                payment.getPaymentMethod().getMethodCode()));
        if (!isUnshippedCashOnDeliveryOrder) {
            throw new InvalidOrderException(
                    "Only pending-payment orders or unshipped cash-on-delivery orders can be cancelled");
        }
    }

    private void validateShippingMethod(String shippingMethod) {
        if (!HOME_DELIVERY.equals(shippingMethod)) {
            throw new InvalidOrderException("目前僅支援宅配");
        }
    }

    private void restoreStock(Order order) {

        for (OrderItem item : order.getOrderItems()) {

            int updated = productSkuRepository.restoreStock(
                    item.getSkuId(),
                    item.getQuantity());

            if (updated == 0) {
                throw new InvalidOrderException(
                        "SKU does not exist: " + item.getSkuId());
            }

            productRepository.decreaseSoldCount(
                    item.getProductId(),
                    item.getQuantity());
        }
    }

    private void validateStatusTransition(
            OrderStatus current,
            OrderStatus target) {

        Set<OrderStatus> allowed = ALLOWED_TRANSITIONS.getOrDefault(current, Set.of());

        if (!allowed.contains(target)) {
            throw new InvalidOrderException(
                    "Invalid order status transition: "
                            + current + " -> " + target);
        }
    }

    private Order createOrderHeader(CreateOrderRequest request, Address address, Integer buyerId) {
        Order order = new Order();
        order.setOrderNo(generateOrderNo());
        order.setBuyerId(buyerId);
        order.setAddressId(address.getAddressId());
        order.setReceiverName(address.getReceiverName());
        order.setReceiverPhone(address.getReceiverPhone());
        order.setShippingPostalCode(address.getPostalCode());
        order.setShippingCity(address.getCity());
        order.setShippingDistrict(address.getDistrict());
        order.setShippingDetailAddress(address.getDetailAddress());
        order.setStatus(OrderStatus.PENDING_PAYMENT);
        order.setBuyerRemark(request.buyerRemark());
        return order;
    }

    private OrderItem createOrderItem(ProductSku sku, Integer quantity, BigDecimal unitPrice, BigDecimal subtotal) {
        Product product = sku.getProduct();
        OrderItem item = new OrderItem();
        item.setProductId(product.getProductId());
        item.setSkuId(sku.getSkuId());
        item.setProductName(product.getProductName());
        item.setSkuSpec(buildSkuSpec(sku));
        item.setProductImageUrl(findMainImageUrl(product));
        item.setUnitPrice(unitPrice);
        item.setQuantity(quantity);
        item.setSubtotal(subtotal);
        item.setIsReviewed(false);
        return item;
    }

    private BigDecimal roundToWholeTwd(BigDecimal amount) {
        return amount.setScale(0, RoundingMode.HALF_UP);
    }

    private String buildSkuSpec(ProductSku sku) {
        String first = formatSpec(sku.getSpec1Name(), sku.getSpec1Value());
        String second = formatSpec(sku.getSpec2Name(), sku.getSpec2Value());
        if (first == null)
            return second;
        if (second == null)
            return first;
        return first + " / " + second;
    }

    private String formatSpec(String name, String value) {
        if (value == null || value.isBlank())
            return null;
        return name == null || name.isBlank() ? value : name + ": " + value;
    }

    private String findMainImageUrl(Product product) {

        return product.getImages().stream()
                .filter(image -> Boolean.TRUE.equals(image.getIsMain()))
                .findFirst()
                .or(() -> product.getImages().stream()
                        .min(Comparator.comparing(ProductImage::getSortOrder)))
                .map(ProductImage::getImageUrl)
                .orElse(null);
    }

    private String generateOrderNo() {
        return "ORD" + LocalDateTime.now().format(ORDER_NO_TIME_FORMAT)
                + UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
    }

    private CreateOrderResponse toResponse(Order order) {
        return new CreateOrderResponse(
                order.getOrderId(),
                order.getOrderNo(),
                order.getStatus(),
                order.getSubtotalAmount(),
                order.getShippingFee(),
                order.getDiscountAmount(),
                order.getTotalAmount(),
                order.getCreatedAt());
    }

    private OrderListResponse toListResponse(Order order) {
        List<OrderItemResponse> items = order.getOrderItems()
                .stream()
                .map(this::toItemResponse)
                .toList();

        return new OrderListResponse(
                order.getOrderId(),
                order.getOrderNo(),
                order.getSellerId(),
                order.getStatus(),
                order.getTotalAmount(),
                order.getCreatedAt(),
                items,
                toShipmentSummary(order.getShipment()));
    }

    private SellerOrderListResponse toSellerListResponse(Order order) {
        List<OrderItemResponse> items = order.getOrderItems().stream()
                .map(this::toItemResponse)
                .toList();

        return new SellerOrderListResponse(
                order.getOrderId(),
                order.getOrderNo(),
                order.getBuyerId(),
                order.getStatus(),
                order.getTotalAmount(),
                order.getDiscountAmount(),
                order.getCreatedAt(),
                items);
    }

    private Seller getSellerByMemberId(Integer memberId) {
        Seller seller = sellerRepository.findByMember_MemberId(memberId)
                .orElseThrow(() -> new OrderNotFoundException("Seller does not exist"));
        if (!"ACTIVE".equals(seller.getStatus())) {
            throw new OrderNotFoundException("Seller is inactive");
        }
        return seller;
    }

    private OrderDetailResponse toDetailResponse(Order order) {
        List<OrderItemResponse> items = order.getOrderItems()
                .stream()
                .map(this::toItemResponse)
                .toList();

        return new OrderDetailResponse(
                order.getOrderId(),
                order.getOrderNo(),
                order.getBuyerId(),
                order.getSellerId(),
                order.getAddressId(),
                order.getReceiverName(),
                order.getReceiverPhone(),
                order.getShippingPostalCode(),
                order.getShippingCity(),
                order.getShippingDistrict(),
                order.getShippingDetailAddress(),
                order.getStatus(),
                order.getSubtotalAmount(),
                order.getShippingFee(),
                order.getDiscountAmount(),
                order.getTotalAmount(),
                order.getBuyerRemark(),
                order.getCancelReason(),
                order.getCancelledBy(),
                order.getCancelledAt(),
                order.getCompletedAt(),
                order.getCreatedAt(),
                order.getUpdatedAt(),
                items,
                toPaymentSummary(order),
                toShipmentSummary(order.getShipment()));
    }

    private OrderPaymentSummary toPaymentSummary(Order order) {
        Payment payment = order.getPayments().stream()
                .max(Comparator.comparing(
                        Payment::getCreatedAt,
                        Comparator.nullsFirst(Comparator.naturalOrder()))
                        .thenComparing(Payment::getPaymentId,
                                Comparator.nullsFirst(Comparator.naturalOrder())))
                .orElse(null);
        if (payment == null) {
            return null;
        }
        return new OrderPaymentSummary(
                payment.getPaymentId(),
                payment.getPaymentMethod().getMethodCode(),
                payment.getPaymentMethod().getMethodName(),
                payment.getStatus(),
                payment.getFailureReason(),
                payment.getPaidAt(),
                payment.getCreatedAt());
    }

    private OrderShipmentSummary toShipmentSummary(Shipment shipment) {
        if (shipment == null) {
            return null;
        }
        return new OrderShipmentSummary(
                shipment.getShipmentId(),
                shipment.getCarrierName(),
                shipment.getTrackingNo(),
                shipment.getStatus(),
                shipment.getShippedAt(),
                shipment.getAvailablePickupAt(),
                shipment.getDeliveredAt());
    }

    private OrderItemResponse toItemResponse(OrderItem item) {
        return new OrderItemResponse(
                item.getOrderItemId(),
                item.getProductId(),
                item.getSkuId(),
                item.getProductName(),
                item.getSkuSpec(),
                item.getProductImageUrl(),
                item.getUnitPrice(),
                item.getQuantity(),
                item.getSubtotal(),
                item.getIsReviewed());
    }
}
