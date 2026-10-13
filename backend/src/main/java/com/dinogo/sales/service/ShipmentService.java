package com.dinogo.sales.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.dinogo.catalog.entity.Product;
import com.dinogo.catalog.repository.ProductRepository;
import com.dinogo.sales.dto.shipment.CreateShipmentRequest;
import com.dinogo.sales.dto.shipment.ShipmentResponse;
import com.dinogo.sales.dto.shipment.UpdateShipmentStatusRequest;
import com.dinogo.sales.dto.shipment.UpdateShipmentTrackingInfoRequest;
import com.dinogo.sales.entity.Order;
import com.dinogo.sales.entity.OrderItem;
import com.dinogo.sales.entity.OrderStatus;
import com.dinogo.sales.entity.PaymentStatus;
import com.dinogo.sales.entity.Shipment;
import com.dinogo.sales.entity.ShipmentStatus;
import com.dinogo.sales.entity.ShipmentEvent;
import com.dinogo.sales.entity.ShipmentEventSource;
import com.dinogo.sales.entity.ShipmentEventType;
import com.dinogo.sales.dto.shipment.ShipmentEventResponse;
import com.dinogo.sales.dto.shipment.SimulateTcatEventRequest;
import com.dinogo.sales.exception.InvalidOrderException;
import com.dinogo.sales.exception.OrderNotFoundException;
import com.dinogo.sales.repository.OrderRepository;
import com.dinogo.sales.repository.PaymentRepository;
import com.dinogo.sales.repository.ShipmentRepository;
import com.dinogo.sales.repository.ShipmentEventRepository;
import com.dinogo.seller.entity.Seller;
import com.dinogo.seller.repository.SellerRepository;

@Service
public class ShipmentService {

    private static final String CASH_ON_DELIVERY = "CASH_ON_DELIVERY";
    private static final Set<OrderStatus> SHIPMENT_CREATION_STATUSES = Set.of(OrderStatus.PAID, OrderStatus.PROCESSING);
    private static final Map<ShipmentStatus, Set<ShipmentStatus>> ALLOWED_TRANSITIONS = Map.of(
            ShipmentStatus.PREPARING, Set.of(ShipmentStatus.SHIPPED),
            ShipmentStatus.SHIPPED, Set.of(ShipmentStatus.AVAILABLE_FOR_PICKUP));

    private final ShipmentRepository shipmentRepository;
    private final ShipmentEventRepository shipmentEventRepository;
    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final SellerRepository sellerRepository;
    private final ProductRepository productRepository;

    public ShipmentService(
            ShipmentRepository shipmentRepository,
            ShipmentEventRepository shipmentEventRepository,
            OrderRepository orderRepository,
            PaymentRepository paymentRepository,
            SellerRepository sellerRepository,
            ProductRepository productRepository) {

        this.shipmentRepository = shipmentRepository;
        this.shipmentEventRepository = shipmentEventRepository;
        this.orderRepository = orderRepository;
        this.paymentRepository = paymentRepository;
        this.sellerRepository = sellerRepository;
        this.productRepository = productRepository;
    }

    @Transactional
    public ShipmentResponse createShipment(
            Integer orderId,
            Integer memberId,
            CreateShipmentRequest request) {
        Seller seller = getActiveSellerByMemberId(memberId);
        Order order = orderRepository
                .findForShipmentCreation(orderId, seller.getSellerId())
                .orElseThrow(() -> new OrderNotFoundException("Order does not exist"));

        if (!SHIPMENT_CREATION_STATUSES.contains(order.getStatus())) {
            throw new InvalidOrderException(
                    "Only paid or processing orders can create a shipment");
        }
        if (shipmentRepository.existsByOrderOrderId(orderId)) {
            throw new InvalidOrderException("Shipment already exists for this order");
        }

        Shipment shipment = new Shipment();
        shipment.setOrder(order);
        shipment.setCarrierName(normalize(request.carrierName()));
        shipment.setTrackingNo(normalize(request.trackingNo()));
        shipment.setStatus(ShipmentStatus.PREPARING);

        Shipment savedShipment = shipmentRepository.save(shipment);
        recordEvent(savedShipment, ShipmentEventType.LABEL_CREATED, ShipmentEventSource.SELLER, "賣家已建立寄件資料");
        return toResponse(savedShipment);
    }

    @Transactional(readOnly = true)
    public ShipmentResponse getShipment(Integer orderId, Integer memberId) {
        Shipment shipment = shipmentRepository.findByOrderOrderId(orderId)
                .orElseThrow(() -> new OrderNotFoundException("Shipment does not exist"));
        Order order = shipment.getOrder();

        if (!memberId.equals(order.getBuyerId()) && !isOrderSeller(order, memberId)) {
            throw new OrderNotFoundException("Shipment does not exist");
        }

        return toResponse(shipment);
    }

    @Transactional(readOnly = true)
    public java.util.List<ShipmentEventResponse> getShipmentEvents(Integer orderId, Integer memberId) {
        Shipment shipment = shipmentRepository.findByOrderOrderId(orderId)
                .orElseThrow(() -> new OrderNotFoundException("Shipment does not exist"));
        if (!memberId.equals(shipment.getOrder().getBuyerId()) && !isOrderSeller(shipment.getOrder(), memberId)) {
            throw new OrderNotFoundException("Shipment does not exist");
        }
        return shipmentEventRepository.findByShipmentShipmentIdOrderByOccurredAtAsc(shipment.getShipmentId()).stream()
                .map(event -> new ShipmentEventResponse(event.getShipmentEventId(), event.getEventType(), event.getSource(), event.getRemark(), event.getOccurredAt())).toList();
    }

    @Transactional
    public ShipmentResponse updateShipmentStatus(
            Integer orderId,
            Integer memberId,
            UpdateShipmentStatusRequest request) {
        Seller seller = getActiveSellerByMemberId(memberId);
        Shipment shipment = shipmentRepository
                .findForStatusUpdate(orderId, seller.getSellerId())
                .orElseThrow(() -> new OrderNotFoundException("Shipment does not exist"));
        ShipmentStatus currentStatus = shipment.getStatus();
        ShipmentStatus targetStatus = request.status();

        if (currentStatus == targetStatus) {
            return toResponse(shipment);
        }
        if (!ALLOWED_TRANSITIONS.getOrDefault(currentStatus, Set.of()).contains(targetStatus)) {
            throw new InvalidOrderException(
                    "Invalid shipment status transition: "
                            + currentStatus + " -> " + targetStatus);
        }

        LocalDateTime now = LocalDateTime.now();
        Order order = shipment.getOrder();
        if (targetStatus == ShipmentStatus.SHIPPED) {
            if (order.getStatus() != OrderStatus.PROCESSING && order.getStatus() != OrderStatus.PAID) {
                throw new InvalidOrderException(
                        "Order must be paid or processing before shipment can be shipped");
            }
            shipment.setShippedAt(now);
            order.setStatus(OrderStatus.SHIPPED);
            recordEvent(shipment, ShipmentEventType.HANDED_OVER, ShipmentEventSource.SELLER, "賣家已確認交寄");
        } else if (targetStatus == ShipmentStatus.AVAILABLE_FOR_PICKUP) {
            shipment.setAvailablePickupAt(now);
            recordEvent(shipment, ShipmentEventType.AVAILABLE_FOR_PICKUP, ShipmentEventSource.SYSTEM, "包裹已可取貨");
        }

        shipment.setStatus(targetStatus);
        return toResponse(shipmentRepository.save(shipment));
    }

    @Transactional
    public ShipmentResponse updateShipmentTrackingInfo(
            Integer orderId,
            Integer memberId,
            UpdateShipmentTrackingInfoRequest request) {
        Seller seller = getActiveSellerByMemberId(memberId);
        Shipment shipment = shipmentRepository
                .findForStatusUpdate(orderId, seller.getSellerId())
                .orElseThrow(() -> new OrderNotFoundException("Shipment does not exist"));

        if (shipment.getStatus() != ShipmentStatus.PREPARING) {
            throw new InvalidOrderException(
                    "Tracking information can only be updated while shipment is preparing");
        }

        shipment.setCarrierName(normalize(request.carrierName()));
        shipment.setTrackingNo(normalize(request.trackingNo()));
        return toResponse(shipmentRepository.save(shipment));
    }

    @Transactional
    public ShipmentResponse confirmDelivery(Integer orderId, Integer buyerId) {
        Shipment shipment = shipmentRepository
                .findForDeliveryConfirmation(orderId, buyerId)
                .orElseThrow(() -> new OrderNotFoundException("Shipment does not exist"));

        if (shipment.getStatus() == ShipmentStatus.DELIVERED) {
            Order order = shipment.getOrder();
            if (order.getStatus() == OrderStatus.SHIPPED) {
                order.setStatus(OrderStatus.COMPLETED);
            } else if (order.getStatus() != OrderStatus.COMPLETED) {
                throw new InvalidOrderException(
                        "Delivered shipment has an incompatible order status: "
                                + order.getStatus());
            }

            LocalDateTime completedAt = order.getCompletedAt();
            if (completedAt == null) {
                completedAt = shipment.getDeliveredAt() != null
                        ? shipment.getDeliveredAt()
                        : LocalDateTime.now();
                order.setCompletedAt(completedAt);
            }

            completeCashOnDeliveryPayment(orderId, completedAt);
            return toResponse(shipment);
        }
        if (shipment.getStatus() != ShipmentStatus.AVAILABLE_FOR_PICKUP
                && shipment.getStatus() != ShipmentStatus.DELIVERED) {
            throw new InvalidOrderException(
                    "Only delivered or available-for-pickup shipments can be completed");
        }

        Order order = shipment.getOrder();
        if (order.getStatus() != OrderStatus.SHIPPED) {
            throw new InvalidOrderException(
                    "Order must be shipped before delivery can be confirmed");
        }

        LocalDateTime now = LocalDateTime.now();

        shipment.setStatus(ShipmentStatus.DELIVERED);
        shipment.setDeliveredAt(now);
        recordEvent(shipment, ShipmentEventType.DELIVERED, ShipmentEventSource.BUYER, "買家已確認收貨");

        order.setStatus(OrderStatus.COMPLETED);
        order.setCompletedAt(now);

        // 訂單真正完成後，增加商品銷量
        increaseSoldCount(order);

        completeCashOnDeliveryPayment(orderId, now);

        return toResponse(shipmentRepository.save(shipment));
    }

    private void completeCashOnDeliveryPayment(Integer orderId, LocalDateTime paidAt) {
        paymentRepository
                .findFirstByOrderOrderIdAndStatusAndPaymentMethodMethodCode(
                        orderId, PaymentStatus.PENDING, CASH_ON_DELIVERY)
                .ifPresent(payment -> {
                    payment.setStatus(PaymentStatus.SUCCESS);
                    payment.setPaidAt(paidAt);
                });
    }

    private boolean isOrderSeller(Order order, Integer memberId) {
        return sellerRepository.findByMember_MemberId(memberId)
                .filter(seller -> "ACTIVE".equals(seller.getStatus()))
                .map(Seller::getSellerId)
                .filter(order.getSellerId()::equals)
                .isPresent();
    }

    private Seller getActiveSellerByMemberId(Integer memberId) {
        Seller seller = sellerRepository.findByMember_MemberId(memberId)
                .orElseThrow(() -> new OrderNotFoundException("Seller does not exist"));
        if (!"ACTIVE".equals(seller.getStatus())) {
            throw new OrderNotFoundException("Seller is inactive");
        }
        return seller;
    }

    private String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    @Transactional
    public ShipmentResponse simulateTcatEvent(Integer orderId, Integer memberId, SimulateTcatEventRequest request) {
        Seller seller = getActiveSellerByMemberId(memberId);
        Shipment shipment = shipmentRepository.findForStatusUpdate(orderId, seller.getSellerId())
                .orElseThrow(() -> new OrderNotFoundException("Shipment does not exist"));
        List<ShipmentEvent> events = shipmentEventRepository
                .findByShipmentShipmentIdOrderByOccurredAtAsc(shipment.getShipmentId());
        ShipmentEventType previous = events.isEmpty() ? null : events.get(events.size() - 1).getEventType();
        ShipmentEventType target = request.eventType();
        if (!isValidTcatTransition(previous, target)) {
            throw new InvalidOrderException("Invalid simulated TCat event transition: " + previous + " -> " + target);
        }
        recordEvent(shipment, target, ShipmentEventSource.CARRIER,
                (shipment.getCarrierName() == null ? "物流商" : shipment.getCarrierName()) + "模擬回報");
        if (target == ShipmentEventType.DELIVERED) {
            LocalDateTime now = LocalDateTime.now();
            shipment.setStatus(ShipmentStatus.DELIVERED);
            shipment.setDeliveredAt(now);
        }
        return toResponse(shipmentRepository.save(shipment));
    }

    private void recordEvent(Shipment shipment, ShipmentEventType type,
            ShipmentEventSource source, String remark) {
        ShipmentEvent event = new ShipmentEvent();
        event.setShipment(shipment);
        event.setEventType(type);
        event.setSource(source);
        event.setRemark(remark);
        event.setOccurredAt(LocalDateTime.now());
        shipmentEventRepository.save(event);
    }

    private boolean isValidTcatTransition(ShipmentEventType previous, ShipmentEventType target) {
        return (previous == ShipmentEventType.HANDED_OVER && target == ShipmentEventType.IN_TRANSIT)
                || (previous == ShipmentEventType.IN_TRANSIT && target == ShipmentEventType.OUT_FOR_DELIVERY)
                || (previous == ShipmentEventType.OUT_FOR_DELIVERY && target == ShipmentEventType.DELIVERED);
    }

    private ShipmentResponse toResponse(Shipment shipment) {
        return new ShipmentResponse(
                shipment.getShipmentId(),
                shipment.getOrder().getOrderId(),
                shipment.getCarrierName(),
                shipment.getTrackingNo(),
                shipment.getStatus(),
                shipment.getShippedAt(),
                shipment.getAvailablePickupAt(),
                shipment.getDeliveredAt(),
                shipment.getDeliveryPhotoUrl(),
                shipment.getCreatedAt(),
                shipment.getUpdatedAt());
    }

    // 完成訂單後商品soldCount + 1
    private void increaseSoldCount(Order order) {

        for (OrderItem item : order.getOrderItems()) {

            Product product = productRepository.findById(item.getProductId())
                    .orElseThrow(() -> new InvalidOrderException(
                            "Product does not exist: " + item.getProductId()));

            int currentSoldCount = product.getSoldCount() == null
                    ? 0
                    : product.getSoldCount();

            product.setSoldCount(
                    currentSoldCount + item.getQuantity());

            productRepository.save(product);
        }
    }
}
